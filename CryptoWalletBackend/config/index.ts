import * as Extras from '../src/utils/Extras'
import * as yargs from 'yargs'
import ConfigFile from './wallet.json';
import Path from 'path'
import Fs from 'fs'

class AssetConfig {

    private _network: string
    private _cacheBlockSizeMB: number
    private _maxParallelDownloadBlock: number

    constructor(config: any) {
        this._network = config.network || 'testnet'
        this._cacheBlockSizeMB = config.cacheBlockSizeMB || 50
        this._maxParallelDownloadBlock = config.maxParallelDownloadBlock || 200
    }

    public get network() { return this._network }

    public get cacheBlockSizeMB() { return this._cacheBlockSizeMB }
    public get maxParallelDownloadBlock() { return this._maxParallelDownloadBlock }

}

class ConfigManager {

    private _assets: { [asset: string]: AssetConfig } = {}

    public get logLevel(): string {
        return Extras.coalesce(yargs.argv['logLevel'], ConfigFile.logLevel).toLowerCase();
    }

    public get walletApi(): {
        host: string;
        port: number;
    } {
        return {
            host: Extras.coalesce(yargs.argv['walletApiHost'], ConfigFile.walletApi.host),
            port: Extras.coalesce(yargs.argv['walletApiPort'], ConfigFile.walletApi.port)
        };
    }

    public getAsset(assetName: string) { return this._assets[assetName] }

    public static loadAssets(configManager: ConfigManager) {
        const confDir = __dirname
        const confs = Fs.readdirSync(confDir)

        confs.forEach((conf) => {
            if (Path.extname(conf) !== '.json' || conf === 'wallet.json')
                return

            const asset: { name: string } = require(Path.join(confDir, conf))

            if (asset)
                configManager._assets[asset.name] = new AssetConfig(asset)
        })
    }

}

const Config = new ConfigManager()

ConfigManager.loadAssets(Config)

export default Config
