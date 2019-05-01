import { Storage, IConnectionOptions } from "../data-access/storage";
import ConfigService from "../../src/config";
import { SupportedAssets, SupportedNetworks } from "./chaininfo-store";

export interface ITransaction {
    _id: string,
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
    addresses: Array<string>;
    inputsCount: number,
    outputsCount: number,
    chain: SupportedAssets,
    network: SupportedNetworks
}

class TransactionStore extends Storage<ITransaction> {

    public constructor(args: IConnectionOptions) {
        super('txs', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ txid: 1, chain: 1, network: 1 }, { background: true, unique: true });
        await this.collection.createIndex({ addresses: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ blockHash: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ blockHeight: 1, chain: 1, network: 1 }, { background: true });
    }
}

export const BasedBtcTxStore = new TransactionStore(ConfigService.mongoDb);