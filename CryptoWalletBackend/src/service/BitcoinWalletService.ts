import express from "express"
import Config from '../config'
import LoggerFactory from '../utils/LogginFactory'
import Bitcoin from "../libs/bitcoin";

const app = express()
const logger = LoggerFactory.getLogger('CryptoWalletServer')

Bitcoin.Blockchain.once('synchronized', () => {
    app.get('/api/v1/rawTx/:txid', async (req, res) => {
        const rawTx = await Bitcoin.Wallet.getRawTransaction(req.params.txid)
        res.json(rawTx)
    }).get('/api/v1/rawTxsByAddr/:address', async (req, res) => {
        const rawTxs = await Bitcoin.Wallet.getRawTransactionsByAddress(req.params.address)
        res.json(rawTxs)
    })

    logger.info('Bitcoin Wallet Service started')
})
Bitcoin.Blockchain.sync()

app.listen(Config.walletApi.port, Config.walletApi.host, () => {
    logger.info(`Started on host: ${Config.walletApi.host} port: ${Config.walletApi.port} `)
})

export function BitcoinWalletService(req, res, next) {

}