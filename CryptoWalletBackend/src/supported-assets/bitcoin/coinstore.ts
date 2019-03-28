import { Storage, IConnectionOptions } from "../../data-access/storage";
import { Collection } from "mongodb";
import Utils from "../../utils";
import LoggerFactory from "../../services/loggin-factory";

export interface ICoin {
    parentTx: string;
    index: number;
    amount: number;
    address: string;
    multi: boolean;
    script: Buffer;
    spendTx: string;
}

export class CoinStore extends Storage<ICoin> {

    private static Logger = LoggerFactory.getLogger("CoinStore");

    private constructor(args: IConnectionOptions) {
        super('coins', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ parentTx: 1, index: 1 }, { background: true });
        await this.collection.createIndex({ spendTx: 1 }, { background: true });
        await this.collection.createIndex({ parentTx: 1 }, { background: true });
        await this.collection.createIndex({ address: 1 }, { background: true });
    }

    private static _instance: CoinStore;

    public static async init(args: IConnectionOptions) {
        if (Utils.isNotNull(this._instance))
            return;
        this._instance = new CoinStore(args);

        await this._instance.connect();

        if (this._instance.connected)
            CoinStore.Logger.info("Service started");
    }

    public static get Collection(): Collection<ICoin> {
        if (Utils.isNull(this._instance))
            throw new Error('Require call CoinStore#init(args)');

        return this._instance.collection;
    }

    public static get isConnected(): boolean {
        if (Utils.isNull(this._instance))
            return false;

        return this._instance.connected;
    }
}