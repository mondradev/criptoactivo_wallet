import { Transaction } from "bitcore-lib";
import level, { LevelUp, AbstractLevelDOWN } from "level";
import { TxindexEntry } from "./txindexentry";
import { Enconding } from "./enconding";
import { getDirectory } from "../../../../utils";

const TXINDEX_PATH = "db/bitcoin/txindex"

export class TxIndex {

    private _db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>

    public constructor() {
        this._db = level(getDirectory(TXINDEX_PATH), { keyEncoding: 'binary', valueEncoding: 'binary' })
    }

    public async getIndexByHash(txid: Buffer): Promise<TxindexEntry> {
        const raw = await this.get(Enconding.Txindex.key(txid))

        if (!raw)
            return null

        return Enconding.Txindex.decode(raw)
    }

    private async get(key: Buffer): Promise<Buffer> {
        try {
            return await this._db.get(key)
        } catch (ignored) {
            return null
        }
    }

    public async deleteIndexes(txids: Buffer[]) {
        const batchDb = this._db.batch()

        txids.forEach((txid) => batchDb.del(Enconding.Txindex.key(txid)))

        return batchDb.write()
    }

    public async indexing(transactions: Transaction[], blockHash: Buffer) {
        const batchDb = this._db.batch()

        for (const [index, transaction] of transactions.entries()) {
            const hash = transaction._getHash()

            batchDb.put(Enconding.Txindex.key(hash),
                Enconding.Txindex.encode(new TxindexEntry(blockHash, index)))
        }

        return batchDb.write()
    }
}