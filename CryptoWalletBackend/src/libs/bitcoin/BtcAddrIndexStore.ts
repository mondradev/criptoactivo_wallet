import LoggerFactory from "../../utils/LogginFactory";
import BufferEx from '../../utils/BufferEx'
import level from 'level'
import { getDirectory } from "../../utils/Extras";
import { Transaction, Script, Address, PublicKey, Networks, Output } from "bitcore-lib";
import Bitcoin from ".";
import TimeCounter from "../../utils/TimeCounter";

const Logger = LoggerFactory.getLogger('Bitcoin AddrIndex')
const addrIndexDb = level(getDirectory('db/bitcoin/addr/index'), { keyEncoding: 'hex', valueEncoding: 'hex' })

const cacheCoin = new Map<string, Map<number, Output>>()

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
    private _getAddressFromScript(script: Script) {
        if (!script)
            return null;

        // Obtenemos la función para resolver la dirección pública o devolvemos null.

        return (AddrIndexLevelDb._scriptFnAddress[script.classify().toString()] || (() => null))(script)
    }

    private async _resolveInputs(tx: Transaction, txs: Transaction[]) {
        const found = new Map<string, Transaction>()
        const inputs = new Array<{ txid: Buffer, index: number }>()

        for (const input of tx.inputs)
            if (!input.isNull())
                inputs.push({ txid: input.prevTxId, index: input.outputIndex })

        const outputs = new Map<Buffer, Output>()

        for (const data of inputs) {
            const hash = data.txid.toString('hex')
            if (cacheCoin[hash] && cacheCoin[hash][data.index]) {
                outputs.set(BufferEx.zero().append(data.txid).appendUInt32LE(data.index).toBuffer(), cacheCoin[hash][data.index])

                cacheCoin.get(hash).delete(data.index)

                if (cacheCoin.get(hash).size == 0)
                    cacheCoin.delete(hash)

                continue
            }

            if (found.has(hash)) {
                outputs.set(BufferEx.zero().append(data.txid).appendUInt32LE(data.index).toBuffer(), found.get(hash)[data.index])
                continue
            }

            const prevTx = txs.find(t => t.hash === hash)
            const txRaw = prevTx ? prevTx.toString() : await Bitcoin.Blockchain.getTxRaw(data.txid)

            if (!txRaw) continue

            const tx = new Transaction(txRaw.toString('hex'))

            if (!tx) continue

            if (!found.has(tx.hash))
                found.set(tx.hash, tx)

            outputs.set(BufferEx.zero().append(data.txid).appendUInt32LE(data.index).toBuffer(), tx.outputs[data.index])
        }

        if (inputs.length != outputs.size)
            Logger.warn(`Not found all outputs of tx ${tx.hash}`)

        for (const input of tx.inputs)
            input.output = outputs.get(BufferEx.zero().append(input.prevTxId).appendUInt32LE(input.outputIndex).toBuffer())
    }

    /**
   * Determine las direcciones públicas de cada transacción.
   * 
   * @param txData Transacciones procesadas.
   * @param tx Transacciones con entradas y salidas.
   */
    private _resolveAddresses(tx: Transaction) {
        let addresses = new Array<Buffer>()

        for (let i = 0; i < tx.outputs.length; i++) {
            addresses.push(this._getAddressFromScript(tx.outputs[i].script))

            if (!cacheCoin.has(tx.hash))
                cacheCoin.set(tx.hash, new Map<number, Output>())

            cacheCoin.get(tx.hash).set(i, tx.outputs[i])
        }

        for (let i = 0; i < tx.inputs.length; i++)
            if (tx.inputs[i].output)
                addresses.push(this._getAddressFromScript(tx.inputs[i].output.script));

        addresses.push(...new Set(addresses));

        return addresses.filter(t => t != null)
    }

    public async import(txs: Transaction[], blockHash: Buffer, blockHeight: number) {
        const timer = TimeCounter.begin()
        const batch = addrIndexDb.batch()

        let count = 0

        for (const [index, tx] of txs.entries()) {
            if (!tx.isCoinbase()) await this._resolveInputs(tx, txs)
            const addresses = this._resolveAddresses(tx)

            for (const address of addresses) {
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
            count += addresses.length
        }

        await batch.write()
        timer.stop()

        Logger.trace(`Indexed ${count} addresses in ${timer.toLocalTimeString()} from Block ${blockHash.toString('hex')}`)
    }
}

export const BtcAddrIndexStore = new AddrIndexLevelDb()
export const BtcAddrIndexDb = addrIndexDb