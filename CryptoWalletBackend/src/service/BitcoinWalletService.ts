import express from "express"
import Config from '../config'
import { BitcoinProvider } from "../libs/bitcoin/BitcoinProvider"
import * as LoggerFactory from '../utils/LogginFactory'

const app = express()
const logger = LoggerFactory.getLogger('CryptoWalletServer', Config.logLevel)

BitcoinProvider.onDownloaded(() => {
    app.get('/api/v1/rawTx/:txid', async (req, res) => {
        const rawTx = await BitcoinProvider.getRawTransaction(req.params.txid)
        res.json(rawTx)
    }).get('/api/v1/rawTxsByAddr/:address', async (req, res) => {
        const rawTxs = await BitcoinProvider.getRawTransactionsByAddress(req.params.address)
        res.json(rawTxs)
    })

    logger.info('Bitcoin Wallet Service started')
})
BitcoinProvider.sync()

app.listen(Config.walletApi.port, Config.walletApi.host, () => {
    logger.info(`Started on host: ${Config.walletApi.host} port: ${Config.walletApi.port} `)
})

export function BitcoinWalletService(req, res, next) {
    if (!BitcoinProvider.isSynchronized)
        next()


}