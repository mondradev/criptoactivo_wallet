import Config from "./config"
import { BitcoinWalletService } from "./service/BitcoinWalletService"
import Http from "http"
import App from "./app"

// Bitcoin Support
App.use('api/v1/btc', BitcoinWalletService)

// Config service
App.set('port', Config.walletApi.port)
App.set('host', Config.walletApi.host)

Http.createServer(App)