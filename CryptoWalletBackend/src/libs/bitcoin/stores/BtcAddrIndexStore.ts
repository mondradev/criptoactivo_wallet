import LoggerFactory from "../../../utils/LogginFactory"
import BufferEx from '../../../utils/BufferEx'
import level from 'level'
import { getDirectory } from "../../../utils/Extras"
import { Script, Address, PublicKey, Networks } from "bitcore-lib"
import TimeCounter from "../../../utils/TimeCounter"
import '../../../utils/ArrayExtension'
import { Tx, UTXO } from "../BtcModel";

const Logger = LoggerFactory.getLogger('Bitcoin AddrIndex')
const addrIndexDb = level(getDirectory('db/bitcoin/addr/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })
const utxoIndexDb = level(getDirectory('db/bitcoin/addr/utxo'), { keyEncoding: 'binary', valueEncoding: 'binary' })

const MAX_CACHE_SIZE = 200000

const cacheCoin = new Map<string, { utxo: UTXO, address?: Buffer }>()

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

            utxoToFind.forEach((value, key) => idxKeys[value.utxo.toHex()] = key)
            utxoToFind.forEach((value) => pending.push(value.utxo.toHex()))
            pending.sort()

            for (let i = 0; i < pending.length; i++) {
                if (cacheCoin.has(pending[i].slice(2))) {
                    utxoToFind.get(idxKeys[pending[i]]).address = cacheCoin.get(pending[i].slice(2)).address
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
                    gte: Buffer.from(pending[0], 'hex'),
                    lte: Buffer.from(pending[pending.length - 1], 'hex')
                })
                    .on('data', (data: { key: Buffer, value: Buffer }) => {
                        if (pending.includes(data.key.toString('hex'))) {
                            utxoToFind.get(idxKeys[data.key.toString('hex')]).address = data.value
                            pending = pending.remove(data.key.toString('hex'))
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
                    gte: Buffer.from(pending[0], 'hex'),
                    lte: Buffer.from(pending[pending.length - 1], 'hex')
                })
                    .on('data', (data: { key: Buffer, value: Buffer }) => {
                        if (pending.includes(data.key.toString('hex'))) {
                            utxoToFind.get(idxKeys[data.key.toString('hex')]).address = data.value
                            pending = pending.remove(data.key.toString('hex'))
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
        const addrBatch = addrIndexDb.batch()

        let idxAddresses = 0

        const utxoBatch = utxoIndexDb.batch()

        for (const tx of txs) {

            for (const txOut of tx.txOut) {
                const address = this._getAddresses(txOut.script)

                if (!address)
                    continue

                const utxo = new UTXO(tx.txID, txOut.txOutIdx)

                cacheCoin.set(utxo.toOutpointHex(), { utxo, address })

                utxoBatch.del(utxo.toBuffer())
                    .put(utxo.toBuffer(), address)

                if (address.toString('hex') !== Array(65).join('0')) {
                    const addrKey = BufferEx.zero()
                        .appendUInt8(0)
                        .append(address)
                        .append(tx.txID)
                        .toBuffer()

                    const record = BufferEx.from(tx.txID)
                        .appendUInt32LE(tx.txIndex)
                        .append(tx.blockHash)
                        .appendUInt32LE(tx.blockHeight)
                        .toBuffer()

                    addrBatch.del(addrKey).put(addrKey, record)

                    idxAddresses++
                }
            }
        }

        await utxoBatch.write()

        const stxoBatch = utxoIndexDb.batch()
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

                stxoBatch.del(utxo.toBuffer())
                    .put(utxo.spent.toBuffer(), address)

                const addrKey = BufferEx.zero()
                    .appendUInt8(1)
                    .append(address)
                    .append(tx.txID)
                    .toBuffer()

                const record = BufferEx.from(tx.txID)
                    .appendUInt32LE(tx.txIndex)
                    .append(tx.blockHash)
                    .appendUInt32LE(tx.blockHeight)
                    .toBuffer()

                addrBatch.del(addrKey).put(addrKey, record)

                idxAddresses++
            }

        }

        await addrBatch.write()

        timer.stop()

        if (timer.milliseconds > 1000)
            Logger.warn(`Indexed ${idxAddresses} addresses in ${timer.toLocalTimeString()} with ${spent} spent, block ${txs[0].blockHash.toString('hex')}, cache=${cacheCoin.size}`)
        else
            Logger.debug(`Indexed ${idxAddresses} addresses in ${timer.toLocalTimeString()} with ${spent} spent, block ${txs[0].blockHash.toString('hex')}, cache=${cacheCoin.size}`)

        return stxoBatch
    }

    public loadCache() {
        return new Promise<void>((resolve) => {
            utxoIndexDb.createReadStream({
                gte: new UTXO(Buffer.alloc(32, 0), 0).toBuffer(),
                lte: new UTXO(Buffer.alloc(32, 0xFF), 0xFFFFFFFF).toBuffer()
            })
                .on('data', (data: { key: Buffer, value: Buffer }) => cacheCoin.set(data.key.toString('hex'), { utxo: UTXO.fromBuffer(data.key), address: data.value }))
                .on('end', () => {
                    Logger.info(`Loaded UTXO cache [Total=${cacheCoin.size}]`)
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