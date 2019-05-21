import { Transaction, Output } from "bitcore-lib"
import level from 'level'
import TimeCounter from "../../utils/TimeCounter"

import * as LoggerFactory from "../../utils/LogginFactory"

const txsDb = level('./db/bitcoin/txs', { keyEncoding: 'hex', valueEncoding: 'hex' })
const idxTxsDb = level('./db/bitcoin/txs/index', { keyEncoding: 'hex', valueEncoding: 'hex' })

const Logger = LoggerFactory.getLogger('TxStore')

class TxLevelDb {
    public getRawTx(txidRaw: Buffer): Promise<Buffer> {
        return new Promise<Buffer>((resolve) => {
            let raw: Buffer = null
            txsDb.createReadStream({ gte: txidRaw, lte: txidRaw })
                .on('data', (data: { key: string, value: string }) => {
                    raw = Buffer.from(data.value, 'hex')
                })
                .on('end', () => {
                    resolve(raw)
                })
        })
    }

    public async import(txs: Transaction[], blockHash: Buffer) {

        const timer = TimeCounter.begin()
        const idxBatch = idxTxsDb.batch()
        let txBatch = txsDb.batch()

        idxBatch.del(blockHash)

        for (const tx of txs) {
            const txid = Buffer.from(tx.hash, 'hex')
            idxBatch.put(blockHash, txid)
            txBatch
                .del(txid)
                .put(txid, tx.toBuffer())

        }

        await txBatch.write()
        await idxBatch.write()

        timer.stop()

        Logger.trace(`Imported ${txs.length} txs in ${timer.toLocalTimeString()}`)
    }
}

export const TxStore = new TxLevelDb()