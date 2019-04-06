import { IBlock, BasedBtcBlockStore } from "./block-store";

export interface IChainInfo {
    chain: string;
    network: string;
    lastBlock: string;
    height: number;
}

export enum SupportedAssets {
    Bitcoin = 'bitcoin'
}

export enum SupportedNetworks {
    mainnet = 'mainnet',
    testnet = 'testnet'
}

class ChainInfoStore {

    private cacheTip: {
        [chain: string]: {
            [network: string]: IBlock
        }
    } = {};

    public async getTip(chain: SupportedAssets, network: SupportedNetworks) {
        if (this.cacheTip[chain] && this.cacheTip[chain][network])
            return this.cacheTip[chain][network];
        return (await BasedBtcBlockStore.collection.find({ chain, network }).sort({ height: -1 }).limit(1).toArray()).shift() || { height: 1 }
    }

    public updateCacheTip(block: IBlock) {
        if (!this.cacheTip[block.chain])
            this.cacheTip[block.chain] = {};

        this.cacheTip[block.chain][block.network] = block;
    }

    public async getHashes(chain: SupportedAssets, network: SupportedNetworks) {
        const hashes = await BasedBtcBlockStore.collection
            .find({ chain, network, processed: true }, { projection: { hash: 1, _id: 0 } })
            .sort({ height: -1 })
            .limit(30)
            .map((block) => block.hash)
            .toArray();

        return hashes && hashes.length > 0 ? hashes : [Array(65).join('0')];
    }
}

export const ChainInfoService = new ChainInfoStore();