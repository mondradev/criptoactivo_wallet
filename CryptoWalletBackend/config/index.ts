import * as Extras from '../src/utils'
import { argv } from 'yargs'
import ConfigFile from './wallet.json';
import Path from 'path'
import Fs from 'fs'

export class AssetConfig {
    private _isTest: boolean
    private _maxConnections: number

    constructor(config: any) {
        this._isTest = argv[config.name + 'IsTest'] || config.isTest || false
        this._maxConnections = argv[config.name + 'MaxConnections'] || config.maxConnections || 16
    }

    public get isTest() { return this._isTest }
    public get maxConnections() { return this._maxConnections }
}

class ConfigManager {

    private _assets: { [asset: string]: AssetConfig } = {}

    public get logLevel(): string {
        return Extras.coalesce(argv['logLevel'], ConfigFile.logLevel).toLowerCase();
    }

    public get walletApi(): {
        host: string;
        port: number;
    } {
        return {
            host: Extras.coalesce(argv['walletApiHost'], ConfigFile.walletApi.host),
            port: Extras.coalesce(argv['walletApiPort'], ConfigFile.walletApi.port)
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
