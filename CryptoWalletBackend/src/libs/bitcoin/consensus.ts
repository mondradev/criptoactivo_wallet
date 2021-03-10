import Config from "../../../config";

export const BitcoinConfig = Config.getAsset('bitcoin') ||
{
    maxConnections: 4,
    isTest: 'mainnet'
}
