import Config from "../config"
import App from "./app"
import Http from 'http'
import LoggerFactory from 'log4js'

const VERSION = 0.5 // 2020-01-31
const Logger = LoggerFactory.getLogger('(CriptoActivo) Server')

Logger.level = Config.logLevel

Logger.info("CriptoActivo Server v%f", VERSION)

Http.createServer(App).listen(Config.walletApi.port, Config.walletApi.host, () => {
    Logger.info(`Server started on http://${Config.walletApi.host}:${Config.walletApi.port}`)
})
