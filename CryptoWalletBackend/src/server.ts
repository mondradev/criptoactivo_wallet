import Config from "../config"
import App from "./app"
import Http from 'http'
import LoggerFactory from "./utils/LogginFactory";
import Bitcoin from "./libs/bitcoin";

const Logger = LoggerFactory.getLogger('Cryptowallet Backend')

// Bitcoin Node
Bitcoin.Network.startSync()

Http.createServer(App).listen(Config.walletApi.port, Config.walletApi.host, () => {
    Logger.info(`Server started on http://${Config.walletApi.host}:${Config.walletApi.port}`)
})