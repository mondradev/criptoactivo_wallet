import { Storage, IConnectionOptions } from "../data-access/storage";
import ConfigService from "../../src/config";
import { SupportedAssets, SupportedNetworks } from "./chaininfo-store";

export interface ICoin {
    _id: string,
    parentTx: string;
    height: number;
    index: number;
    amount: number;
    address: string;
    multi: boolean;
    script: string;
    spentTx: string;
    spentHeight: number;
    chain: SupportedAssets;
    network: SupportedNetworks;
}

class CoinStore extends Storage<ICoin> {

    public constructor(args: IConnectionOptions) {
        super('coins', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ parentTx: 1, index: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ parentTx: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ address: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ spentTx: 1, chain: 1, network: 1 }, { background: true, partialFilterExpression: { spentTx: { $exists: true } } });
        await this.collection.createIndex({ spentHeight: 1, chain: 1, network: 1 }, { background: true, partialFilterExpression: { spentHeight: { $exists: true, $gt: 0 } } });
        await this.collection.createIndex({ height: 1, chain: 1, network: 1 }, { background: true, partialFilterExpression: { height: { $gt: 0 } } });
    }

}

export const BasedBtcCoinStore = new CoinStore(ConfigService.mongoDb);