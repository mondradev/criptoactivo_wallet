import { Storage, IConnectionOptions } from "../../data-access/storage";
import Utils from "../../utils";
import { Collection } from "mongodb";
import FactoryLogger from '../../services/loggin-factory';

export interface IBlock {
    hash: string;
    ntx: number;
    prevBlock: string;
    height: number;
    version: number;
    nextBlock: string;
    time: Date;
    bits: number;
    nonce: number;
    merkletRoot: string;
    reward: number;
}

export class BlockStore extends Storage<IBlock> {

    private static Logger = FactoryLogger.getLogger('BlockStore');

    private constructor(args: IConnectionOptions) {
        super('blocks', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ hash: 1 });
        await this.collection.createIndex({ prevBlock: 1 });
        await this.collection.createIndex({ processed: 1 });
        await this.collection.createIndex({ height: 1 });
    }

    private static _instance: BlockStore;

    public static async init(args: IConnectionOptions) {
        if (Utils.isNotNull(this._instance))
            return;
        this._instance = new BlockStore(args);

        await this._instance.connect();

        if (this._instance.connected)
            BlockStore.Logger.info("Service started");
    }

    public static get Collection(): Collection<IBlock> {
        if (Utils.isNull(this._instance))
            throw new Error('Require call BlockStore#init(args)');

        return this._instance.collection;
    }

    public static get isConnected(): boolean {
        if (Utils.isNull(this._instance))
            return false;

        return this._instance.connected;
    }

}