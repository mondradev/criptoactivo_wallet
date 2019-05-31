import { Transaction } from "bitcore-lib"
import level from 'level'
import TimeCounter from "../../utils/TimeCounter"

import LoggerFactory from "../../utils/LogginFactory"
import BufferEx from "../../utils/BufferEx"
import { getDirectory } from "../../utils/Extras";
import { BtcAddrIndexStore } from "./BtcAddrIndexStore";

const txIndexDb = level(getDirectory('db/bitcoin/txs/index'), { keyEncoding: 'hex', valueEncoding: 'hex' })

const Logger = LoggerFactory.getLogger('Bitcoin TxIndex')

class TxIndexLevelDb {
    public async import(txs: Transaction[], blockHash: Buffer, blockHeight: number) {

        const timer = TimeCounter.begin()
        const idxBatch = txIndexDb.batch()

        for (const [index, tx] of txs.entries()) {
            const txid = Buffer.from(tx.hash, 'hex')
            const record = BufferEx.zero()
                .append(blockHash)
                .appendUInt32LE(index)
                .appendUInt32LE(blockHeight)
                .toBuffer()

            idxBatch.del(txid).put(txid, record)
        }

        await idxBatch.write()

        await BtcAddrIndexStore.import(txs, blockHash, blockHeight)

        timer.stop()

        Logger.trace(`Indexed ${txs.length} txs in ${timer.toLocalTimeString()} from Block ${blockHash.toString('hex')}`)
    }
}

export const BtcTxIndexStore = new TxIndexLevelDb()
export const BtcTxIndexDb = txIndexDb