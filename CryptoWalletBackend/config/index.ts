import * as Extras from '../src/utils'
import * as yargs from 'yargs'
import ConfigFile from './wallet.json';
import Path from 'path'
import Fs from 'fs'

export class AssetConfig {
    private _network: string
    private _port: number
    private _maxConnections: number
    private _seeds: Array<string>

    constructor(config: any) {
        this._network = yargs.argv[config.name + 'Network'] || config.network || 'mainnet'
        this._port = yargs.argv[config.name + 'Port'] || config.port
        this._maxConnections = yargs.argv[config.name + 'MaxConnections'] || config.maxConnections || 16
        this._seeds = config.seeds || []
    }

    public get network() { return this._network }
    public get port() { return this._port }
    public get maxConnections() { return this._maxConnections }
    public get seeds() { return this._seeds }
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

    public get dbConfig(): any {
        return ConfigFile.dbConfig
    }

}

const Config = new ConfigManager()

ConfigManager.loadAssets(Config)

export default Config