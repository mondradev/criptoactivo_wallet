import level, { AbstractLevelDOWN, LevelUp, LevelUpChain } from 'level'
import TimeCounter from "../../../utils/TimeCounter"

import LoggerFactory from "../../../utils/LogginFactory"
import BufferEx from "../../../utils/BufferEx"
import { getDirectory } from "../../../utils/Extras";
import { Tx } from "../BtcModel";

type DbBinary = LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

const Logger = LoggerFactory.getLogger('Bitcoin TxIndex')

class TxIndexLevelDb {
    public async getTxIndex(txid: Buffer): Promise<Buffer> {
        const txIndexDb: DbBinary = level(getDirectory('db/bitcoin/txs/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })
        try {
            const data = await txIndexDb.get(txid)
            return data
        }
        finally {
            txIndexDb.isOpen() && await txIndexDb.close()
        }
    }
    public async import(txs: Tx[]) {
        if (txs.length == 0)
            return

        const txIndexDb: DbBinary = level(getDirectory('db/bitcoin/txs/index'), { keyEncoding: 'binary', valueEncoding: 'binary' })

        const timer = TimeCounter.begin()
        const idxBatch: LevelUpChain<Buffer, Buffer> = txIndexDb.batch()

        for (const tx of txs) {
            const record = BufferEx.zero()
                .append(tx.blockHash)
                .appendUInt32LE(tx.txIndex)
                .appendUInt32LE(tx.blockHeight)
                .toBuffer()

            idxBatch.del(tx.txID).put(tx.txID, record)
        }

        await idxBatch.write()

        timer.stop()

        Logger.debug(`Indexed ${txs.length} txs in ${timer.toLocalTimeString()} from Block ${Buffer.from(txs[0].blockHash).reverse().toString('hex')}`)

        txIndexDb.isOpen() && await txIndexDb.close()

    }
}

export const BtcTxIndexStore = new TxIndexLevelDb()