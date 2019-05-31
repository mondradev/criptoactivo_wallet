import * as Extras from './utils/Extras'
import * as yargs from 'yargs'
import ConfigFile from './cwconfig.json';

class ConfigManager {
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

    public get assets(): {
        bitcoin: {
            network: string;
        };
    } {
        return {
            bitcoin: {
                network: Extras.coalesce(yargs.argv['bitcoinNetwork'], ConfigFile.assets.bitcoin.network)
            }
        };
    }
}

const Config = new ConfigManager()

export default Config
