import express from "express"
import ConfigService from "../../src/config"
import LoggerFactory from "../utils/loggin-factory"

import { BtcWallet } from "../../libs/basedbtc/btc/service"

const app = express()
const logger = LoggerFactory.getLogger('CryptoWalletServer')

BtcWallet.onDownloaded(() => {
    app.get('/api/v1/rawTx/:txid', async (req, res) => {
        const rawTx = await BtcWallet.getRawTransaction(req.params.txid)
        res.json(rawTx)
    }).get('/api/v1/rawTxsByAddr/:address', async (req, res) => {
        const rawTxs = await BtcWallet.getRawTransactionsByAddress(req.params.address)
        res.json(rawTxs)
    })

    logger.info('Bitcoin Wallet Service started')
})
BtcWallet.sync()

app.listen(ConfigService.walletService.port, ConfigService.walletService.host, () => {
    logger.info(`Started on host: ${ConfigService.walletService.host} port: ${ConfigService.walletService.port} `)
})