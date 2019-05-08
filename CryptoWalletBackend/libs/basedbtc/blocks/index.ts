import level from 'level';
import LoggerFactory from '../../../libs/utils/loggin-factory';

const blockDb = level('./db/bitcoin/blocks', { keyEncoding: 'hex', valueEncoding: 'json' });
const Logger = LoggerFactory.getLogger('Bitcoin BlockDb');


class Storage {

    public async import(block: { hash: string }) {
        // Verify if require reorg

        // Import txs, if completed then save block

        // await blockDb.batch()
        //     .del(Buffer.from(block.hash, 'hex'))
        //     .put(Buffer.from(block.hash, 'hex'), Block)
        //     .write();

        // Logger.info(`Block saved [Hash=${block.hash}, Txn=${block.transactions.length}, Size=${block.toBuffer().length}, PrevBlock=${Buffer.from(block.header.prevHash).toString('hex')}]`)

        // if (this._batch.length / 2 >= MAX_OPS)
        //     await this.commit()
    }

}

export const BlockStore = new Storage();