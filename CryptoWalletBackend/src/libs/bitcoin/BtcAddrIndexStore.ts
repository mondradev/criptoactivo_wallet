import LoggerFactory from "../../utils/LogginFactory"
import BufferEx from '../../utils/BufferEx'
import level from 'level'
import { getDirectory, callAsync } from "../../utils/Extras"
import { Transaction, Script, Address, PublicKey, Networks, Output, Input } from "bitcore-lib"
import TimeCounter from "../../utils/TimeCounter"

const Logger = LoggerFactory.getLogger('Bitcoin AddrIndex')
const addrIndexDb = level(getDirectory('db/bitcoin/addr/index'), { keyEncoding: 'hex', valueEncoding: 'hex' })
const utxoIndexDb = level(getDirectory('db/bitcoin/addr/utxo'), { keyEncoding: 'hex', valueEncoding: 'hex' })

const cacheCoin = new Map<string, Buffer>()

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
            return Buffer.alloc(32, '0')

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (AddrIndexLevelDb._scriptFnAddress[script.classify().toString()] || (() => Buffer.alloc(32, '0')))(script)
    }

    private async _getUTXO(input: Input) {
        const key = BufferEx.zero()
            .append(input.prevTxId)
            .appendUInt32LE(input.outputIndex)
            .toBuffer()
        const keyStr = key.toString('hex')

        if (cacheCoin.has(keyStr))
            return cacheCoin.get(keyStr)

        const address = await callAsync(utxoIndexDb.get, [key], utxoIndexDb)

        if (!address)
            Logger.warn(`Not found UTXO ${key.toString('hex')}`)

        return address
    }

    private async _resolveInputs(tx: Transaction) {
        const addresses = new Array<Buffer>()
        const batch = utxoIndexDb.batch()

        for (let i = 0; i < tx.inputs.length; i++) {
            if (tx.inputs[i].isNull())
                continue

            const address = await this._getUTXO(tx.inputs[i])

            if (!address) continue

            const key = BufferEx.zero().append(tx.inputs[i].prevTxId)
                .appendUInt32LE(tx.inputs[i].outputIndex)
                .toBuffer()

            const keyStr = key.toString('hex')

            cacheCoin.delete(keyStr)

            batch.del(key)

            addresses.push(address)
        }

        await batch.write()

        return addresses
    }

    /**
   * Determine las direcciones públicas de cada transacción.
   * 
   * @param txData Transacciones procesadas.
   * @param tx Transacciones con entradas y salidas.
   */
    private async _resolveOutputs(tx: Transaction) {
        const addresses = new Array<Buffer>()
        const batch = utxoIndexDb.batch()

        for (let i = 0; i < tx.outputs.length; i++) {
            const address = this._getAddresses(tx.outputs[i].script)

            if (!address)
                continue

            const key = BufferEx.zero().append(Buffer.from(tx.hash, 'hex'))
                .appendUInt32LE(i)
                .toBuffer()

            const keyStr = key.toString('hex')

            cacheCoin.set(keyStr, address)

            batch.del(key)
                .put(key, address)

            addresses.push(address)
        }

        await batch.write()

        return addresses
    }

    public async import(txs: Transaction[], blockHash: Buffer, blockHeight: number) {
        const timer = TimeCounter.begin()
        const batch = addrIndexDb.batch()

        let count = 0

        for (const [index, tx] of txs.entries()) {
            const addrsInput = []
            const addrsOutput = await this._resolveOutputs(tx)

            if (!tx.isCoinbase())
                addrsInput.push(...await this._resolveInputs(tx))

            const addrs = [...addrsInput, ...addrsOutput]

            for (const address of addrs) {
                const txid = Buffer.from(tx.hash, 'hex')
                const key = BufferEx.zero().append(address).append(txid).toBuffer()
                const record = BufferEx.zero()
                    .append(txid)
                    .appendUInt32LE(index)
                    .append(blockHash)
                    .appendUInt32LE(blockHeight)
                    .toBuffer()

                batch.del(key).put(key, record)
            }
            count += addrsOutput.length
        }

        await batch.write()
        timer.stop()

        Logger.trace(`Indexed ${count} addresses in ${timer.toLocalTimeString()} from Block ${blockHash.toString('hex')}`)
    }
}

export const BtcAddrIndexStore = new AddrIndexLevelDb()
export const BtcAddrIndexDb = addrIndexDb