import yargs from "yargs"
import ConfigFile from '../config.json'
import * as Extras from '../utils/Extras'

export const Config = {
    logLevel: Extras.coalesce(yargs.argv['logLevel'], ConfigFile.logLevel),

    walletApi: {
        host: Extras.coalesce(yargs.argv['walletApiHost'], ConfigFile.walletApi.host),
        port: Extras.coalesce(yargs.argv['walletApiPort'], ConfigFile.walletApi.port)
    },

    assets: {
        bitcoin: {
            network: Extras.coalesce(yargs.argv['bitcoinNetwork'], ConfigFile.assets.bitcoin.network)
        }
    }
}