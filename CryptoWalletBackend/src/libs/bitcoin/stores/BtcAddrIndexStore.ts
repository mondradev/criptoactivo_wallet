import LoggerFactory from "../../../utils/LogginFactory"
import BufferHelper from '../../../utils/BufferHelper'
import level from 'level'
import { getDirectory } from "../../../utils/Extras"
import { Script, Address, PublicKey, Networks } from "bitcore-lib"
import TimeCounter from "../../../utils/TimeCounter"
import '../../../utils/ArrayExtension'
import { Tx, UTXO } from "../BtcModel";

const Logger = LoggerFactory.getLogger('Bitcoin AddrIndex')
const addrIndexDb = level(getDirectory('db/bitcoin/addr/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const utxoIndexDb = level(getDirectory('db/bitcoin/addr/utxo'), { keyEncoding: 'binary', valueEncoding: 'binary' })

const MAX_CACHE_SIZE = 5000000
const MB = (1024 * 1024)
const CACHE_ITEM_SIZE = 37 * 2 + 21

const cacheCoin = new Map<string, { address?: Buffer }>()

const cacheTask = setInterval(() => {
    if (cacheCoin.size >= MAX_CACHE_SIZE)
        for (const key of cacheCoin.keys())
            if (cacheCoin.size <= MAX_CACHE_SIZE / 2)
                break
            else
                cacheCoin.delete(key)
}, 1000)

// TODO Optimizar lectura de transacciones sin usar
class AddrIndexLevelDb {

    private static _scriptFnAddress = {
        'Pay to public key': (script: Script) => new Address(new PublicKey(script.getPublicKey()), Networks.defaultNetwork).toBuffer(),
        'Pay to public key hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer(),
        'Pay to script hash': (script: Script) => script.toAddress(Networks.defaultNetwork).toBuffer()
    }

    /**
     * Obtiene la dirección del script especificado.
     * 
     * @param script Script de la salida.
     */
    private _getAddresses(script: Script): Buffer {
        if (!script)
            return Buffer.alloc(32, 0)

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (AddrIndexLevelDb._scriptFnAddress[script.classify().toString()] || (() => Buffer.alloc(32, 0)))(script)
    }

    private _resolveUTXO(utxoToFind: Map<string, { utxo: UTXO, address?: Buffer }>) {
        if (utxoToFind.size == 0)
            return Promise.resolve(0)

        Logger.trace(`Try resolve UTXO [Size: ${utxoToFind.size}]`)

        return new Promise<{}>(async (success) => {
            const timer = TimeCounter.begin()

            const idxKeys = {}
            let pending = new Array<string>()

            utxoToFind.forEach((value, key) => idxKeys[value.utxo.toOutpointHex()] = key)
            utxoToFind.forEach((value) => pending.push(value.utxo.toOutpointHex()))
            pending.sort()

            for (let i = 0; i < pending.length; i++) {
                if (cacheCoin.has(pending[i])) {
                    utxoToFind.get(idxKeys[pending[i]]).address = cacheCoin.get(pending[i]).address
                    pending = pending.remove(pending[i])
                    i = -1
                }

            }

            if (pending.length == 0) {
                timer.stop()
                Logger.trace(`Found ${utxoToFind.size} UTXO in ${timer.toLocalTimeString()}`)

                success(utxoToFind.size)
                return
            }

            timer.stop()
            Logger.trace(`Not Found ${pending.length} UTXO from cache in ${timer.toLocalTimeString()}`)

            timer.start()

            await (() => new Promise<void>((resolve) => {
                utxoIndexDb.createReadStream({
                    gte: Buffer.from('00' + pending[0], 'hex'),
                    lte: Buffer.from('00' + pending[pending.length - 1], 'hex')
                })
                    .on('data', (data: { key: Buffer, value: Buffer }) => {
                        if (pending.includes(data.key.toString('hex'))) {
                            utxoToFind.get(idxKeys[data.key.toString('hex').slice(2)]).address = data.value
                            pending = pending.remove(data.key.toString('hex').slice(2))
                        }
                    })
                    .on('end', () => resolve())
            }))()

            if (pending.length == 0) {
                timer.stop()
                Logger.trace(`Found ${utxoToFind.size} UTXO from uspent outputs in ${timer.toLocalTimeString()}`)

                success(utxoToFind.size)
                return
            }

            timer.stop()
            Logger.trace(`Not Found ${pending.length} UTXO in ${timer.toLocalTimeString()}`)

            timer.start()

            pending = pending.map((i: string) => '01' + i.slice(2))

            await (() => new Promise<void>((resolve) => {
                utxoIndexDb.createReadStream({
                    gte: Buffer.from('01' + pending[0], 'hex'),
                    lte: Buffer.from('01' + pending[pending.length - 1], 'hex')
                })
                    .on('data', (data: { key: Buffer, value: Buffer }) => {
                        if (pending.includes(data.key.toString('hex'))) {
                            utxoToFind.get(idxKeys[data.key.toString('hex').slice(2)]).address = data.value
                            pending = pending.remove(data.key.toString('hex').slice(2))
                        }
                    })
                    .on('end', () => resolve())
            }))()

            timer.stop()

            if (pending.length == 0) {
                Logger.trace(`Found ${utxoToFind.size} UTXO from spent outputs in ${timer.toLocalTimeString()}`)

                success(utxoToFind.size)
                return
            }

            for (const utxo of pending)
                Logger.warn(`Not found UTXO ${utxo}`)

            success(utxoToFind.size - pending.length)
        })
    }

    public async import(txs: Tx[]) {
        const timer = TimeCounter.begin()

        let addrBatch = addrIndexDb.batch()
        let idxAddresses = 0
        let utxoBatch = utxoIndexDb.batch()

        for (const tx of txs) {

            for (const txOut of tx.txOut) {
                const address = this._getAddresses(txOut.script)

                if (!address)
                    continue

                const utxo = new UTXO(tx.txID, txOut.txOutIdx)

                cacheCoin.set(utxo.toOutpointHex(), { address })

                if (utxoBatch.length > 10000) {
                    await utxoBatch.write()
                    utxoBatch = utxoIndexDb.batch()
                }

                utxoBatch.del(utxo.toBuffer())
                    .put(utxo.toBuffer(), address)

                if (address.toString('hex') !== Array(65).join('0')) {
                    let addrKey = BufferHelper.appendUInt8(BufferHelper.zero(), 0)
                    addrKey = BufferHelper.append(addrKey, address)
                    addrKey = BufferHelper.append(addrKey, tx.txID)

                    let record = BufferHelper.appendUInt32LE(tx.txID, tx.txIndex)
                    record = BufferHelper.append(record, tx.blockHash)
                    record = BufferHelper.appendUInt32LE(record, tx.blockHeight)

                    if (addrBatch.length > 10000) {
                        await addrBatch.write()
                        addrBatch = addrIndexDb.batch()
                    }

                    addrBatch.del(addrKey).put(addrKey, record)

                    idxAddresses++
                }
            }
        }

        await utxoBatch.write()

        let stxoBatch = utxoIndexDb.batch()

        const stxos = new Map<string, { utxo: UTXO, address?: Buffer }>()

        txs.forEach(t => t.txIn.forEach(i => stxos.set(i.txInID.toString('hex'), { utxo: i.uTxOut })))

        let spent = await this._resolveUTXO(stxos)

        for (const tx of txs) {
            for (const txin of tx.txIn) {
                const key = txin.txInID.toString('hex')

                const utxo = stxos.has(key) ? stxos.get(key).utxo : null
                const address = stxos.has(key) ? stxos.get(key).address : null

                if (!address) continue

                cacheCoin.delete(utxo.toOutpointHex())

                if (stxoBatch.length > 10000) {
                    await stxoBatch.write()
                    stxoBatch = utxoIndexDb.batch()
                }

                stxoBatch.del(utxo.toBuffer())
                    .put(utxo.spent.toBuffer(), address)

                let addrKey = BufferHelper.appendUInt8(BufferHelper.zero(), 1)
                addrKey = BufferHelper.append(addrKey, address)
                addrKey = BufferHelper.append(addrKey, tx.txID)

                let record = Buffer.from(tx.txID)
                record = BufferHelper.appendUInt32LE(record, tx.txIndex)
                record = BufferHelper.append(record, tx.blockHash)
                record = BufferHelper.appendUInt32LE(record, tx.blockHeight)

                if (addrBatch.length > 10000) {
                    await addrBatch.write()
                    addrBatch = addrIndexDb.batch()
                }

                addrBatch.del(addrKey).put(addrKey, record)

                idxAddresses++
            }

        }

        await addrBatch.write()
        await stxoBatch.write()

        timer.stop()

        if (timer.milliseconds > 1000)
            Logger.warn(`Indexed ${idxAddresses} addresses in ${timer.toLocalTimeString()} with ${spent} spent, block ${txs[0].blockHash.reverse().toString('hex')}, cache=${(cacheCoin.size * CACHE_ITEM_SIZE / MB).toFixed(2)} MB(${cacheCoin.size}utxo)`)
        else
            Logger.debug(`Indexed ${idxAddresses} addresses in ${timer.toLocalTimeString()} with ${spent} spent, block ${txs[0].blockHash.reverse().toString('hex')}, cache=${(cacheCoin.size * CACHE_ITEM_SIZE / MB).toFixed(2)} MB(${cacheCoin.size}utxo)`)
    }

    public loadCache() {
        return new Promise<void>((resolve) => {
            Logger.debug(`Open UTXO data [MemUsage=${(process.memoryUsage().rss / MB).toFixed(2)} MB]`)
            utxoIndexDb.createReadStream({
                gte: new UTXO(Buffer.alloc(32, 0), 0).toBuffer(),
                lte: new UTXO(Buffer.alloc(32, 0xFF), 0xFFFFFFFF).toBuffer()
            })
                .on('data', (data: { key: Buffer, value: Buffer }) =>
                    cacheCoin.set(data.key.toString('hex', 1), { address: data.value }))
                .on('end', () => {
                    Logger.info(`Loaded UTXO cache [Total=${cacheCoin.size}, MemUsage=${(process.memoryUsage().rss / MB).toFixed(2)} MB]`)
                    resolve()
                })
        })
    }

    public stopMonitorCache() {
        cacheTask && clearInterval(cacheTask)
    }
}

export const BtcAddrIndexStore = new AddrIndexLevelDb()
export const BtcAddrIndexDb = addrIndexDb
export const BtcUTXOIndexDb = utxoIndexDb