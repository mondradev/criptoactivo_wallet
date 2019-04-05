import { Storage, IConnectionOptions } from "../data-access/storage";
import { SupportedAssets, SupportedNetworks } from "../basedbitcoin-assets/chaininfo-store";
import ConfigService from "./../../src/config";

export interface IBlock {
    _id: string;
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
    processed: boolean;
    chain: SupportedAssets;
    network: SupportedNetworks;
}

class BlockStore extends Storage<IBlock> {

    public constructor(args: IConnectionOptions) {
        super('blocks', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ hash: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ prevBlock: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ height: 1, chain: 1, network: 1 }, { background: true });
        await this.collection.createIndex({ processed: 1, chain: 1, network: 1 }, { background: true });
    }
}

export const BasedBtcBlockStore = new BlockStore(ConfigService.mongoDb);