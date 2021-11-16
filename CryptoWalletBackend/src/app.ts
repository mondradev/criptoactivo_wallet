import express from "express"
import BitcoinWalletService from "./resources/bitcoinwalletservice";

const app = express()

    .use(express.json())
    .use(express.urlencoded({ extended: true }))
    .use(BitcoinWalletService)
    .use((req, res) =>
        res.status(200).json({
            message: `Welcome to CryptoWallet, you're connected from ${req.hostname} to ${req.originalUrl}`
        }))

export default app