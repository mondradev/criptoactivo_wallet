import level, { AbstractLevelDOWN, LevelUp, LevelUpChain } from 'level'
import TimeCounter from "../../../utils/TimeCounter"

import LoggerFactory from "../../../utils/LogginFactory"
import BufferHelper from "../../../utils/BufferHelper"
import { getDirectory } from "../../../utils/Extras";
import { Tx } from "../BtcModel";

type DbBinary = LevelUp<AbstractLevelDOWN<Buffer, Buffer>>
const txIndexDb: DbBinary = level(getDirectory('db/bitcoin/txs/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })

const Logger = LoggerFactory.getLogger('Bitcoin TxIndex')

class TxIndexLevelDb {
    public async import(txs: Tx[]) {
        if (txs.length == 0)
            return

        const timer = TimeCounter.begin()

        let idxBatch: LevelUpChain<Buffer, Buffer> = txIndexDb.batch()

        for (const tx of txs) {
            let record = BufferHelper.appendUInt32LE(tx.blockHash, tx.txIndex)
            record = BufferHelper.appendUInt32LE(record, tx.blockHeight)

            if (idxBatch.length > 10000) {
                await idxBatch.write()
                idxBatch = txIndexDb.batch()
            }
            idxBatch.del(tx.txID).put(tx.txID, record)
        }

        await idxBatch.write()

        timer.stop()

        Logger.debug(`Indexed ${txs.length} txs in ${timer.toLocalTimeString()} from Block ${Buffer.from(txs[0].blockHash).reverse().toString('hex')}`)

    }
}

export const BtcTxIndexStore = new TxIndexLevelDb()
export const BtcTxIndexDb = txIndexDb