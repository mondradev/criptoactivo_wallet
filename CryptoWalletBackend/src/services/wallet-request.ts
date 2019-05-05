import express from "express";
import '../basedbitcoin-assets/bitcoin/btc-wallet-service';
import ConfigService from "../config";
import LoggerFactory from "./loggin-factory";
import { BtcWallet } from "../basedbitcoin-assets/bitcoin/btc-wallet-service";

const app = express();
const logger = LoggerFactory.getLogger('CryptoWalletServer');

BtcWallet.onDownloaded(()=>{
    app.get('/api/v1/rawTx/:txid', async (req, res) => {
        const rawTx = await BtcWallet.getRawTransaction(req.params.txid);
        res.json(rawTx);
    }).get('/api/v1/rawTxsByAddr/:address', async (req, res) => {
        const rawTxs = await BtcWallet.getRawTransactionsByAddress(req.params.address);
        res.json(rawTxs);
    });

    logger.info('Bitcoin Wallet Service started');
});

app.listen(ConfigService.walletService.port, ConfigService.walletService.host, () => {
    logger.info(`Started on host: ${ConfigService.walletService.host} port: ${ConfigService.walletService.port} `);
});