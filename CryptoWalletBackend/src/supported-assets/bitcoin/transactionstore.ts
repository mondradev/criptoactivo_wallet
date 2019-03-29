import { Storage, IConnectionOptions } from "../../data-access/storage";
import { Collection } from "mongodb";
import Utils from "../../utils";
import LoggerFactory from "../../services/loggin-factory";

export interface ITransaction {
    txid: string;
    blockHeight: number;
    blockHash: string;
    blockTime: Date;
    coinbase: boolean;
    fee: number;
    size: number;
    lockTime: number;
    value: number;
    hex: Buffer;
    inputsCount: number,
    outputsCount: number
}

export class TransactionStore extends Storage<ITransaction> {

    private static Logger = LoggerFactory.getLogger("TransactionStore");

    private constructor(args: IConnectionOptions) {
        super('transactions', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ txid: 1 }, { background: true });
        await this.collection.createIndex({ blockHash: 1 }, { background: true });
        await this.collection.createIndex({ blockHeight: 1 }, { background: true });
    }

    private static _instance: TransactionStore;

    public static async init(args: IConnectionOptions) {
        if (Utils.isNotNull(this._instance))
            return;
        this._instance = new TransactionStore(args);

        await this._instance.connect();


        if (this._instance.connected)
            TransactionStore.Logger.info("Service started");
    }

    public static get Collection(): Collection<ITransaction> {
        if (Utils.isNull(this._instance))
            throw new Error('Require call TransactionStore#init(args)');

        return this._instance.collection;
    }

    public static get isConnected(): boolean {
        if (Utils.isNull(this._instance))
            return false;

        return this._instance.connected;
    }
}