import { Storage, IConnectionOptions } from "../data-access/storage";
import Utils from "../utils";

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

export class ChainInfoService extends Storage<IChainInfo>
{
    private static instance: ChainInfoService;

    private constructor(args: IConnectionOptions) {
        super('chaininfo', args);
    }

    protected async createIndexes(): Promise<void> {
        await this.collection.createIndex({ chain: 1, network: 1 });
    }

    public async getStatus(chain: SupportedAssets, network: SupportedNetworks) {
        return await this.collection.findOne({ chain, network }) || { chain, network, height: 0, lastBlock: Array(65).join('0') };
    }

    public async setStatus(info: IChainInfo) {
        let { chain, network } = info;

        return (await this.collection.updateOne(
            { chain, network },
            { $set: info },
            { upsert: true }
        )).result.ok == 1;
    }

    public static async start(args: Partial<IConnectionOptions>): Promise<ChainInfoService> {
        Utils.requireNotNull(args.dbname);
        args.schema = 'cryptowallet';

        this.instance = new ChainInfoService(args as IConnectionOptions);
        return await this.instance.connect();
    }

    public static getInstance() {
        if (Utils.isNull(ChainInfoService.instance))
            throw new Error(`Require call to ChainInfoService#Start function before call to ChainInfoService#getInstance`);

        return ChainInfoService.instance;
    }

}