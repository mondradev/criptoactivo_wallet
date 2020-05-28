import express from "express"
import { BitcoinWalletService } from "./service/BitcoinWalletService";

const app = express()

// JSON Support
app.use(express.json())

// Bitcoin Support
app.use('/api/v1/btc/:request', (req, res) =>
    BitcoinWalletService(
        { body: req.body, method: req.method, params: req.params },
        (statusCode, data) => res.status(statusCode).json(data)
    )
)

app.use((req, res) => {
    res.status(200).json({
        payload: null, message: `Welcome to CryptoWallet, you're connected from ${req.hostname}`
    })
})

export default app