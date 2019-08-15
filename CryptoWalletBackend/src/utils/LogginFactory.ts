import { LoggerFactoryOptions, LogGroupRule, LogLevel, LFService, LoggerFactory, Logger } from "typescript-logging"
import Config from "../../config"




const options = new LoggerFactoryOptions()
options.addLogGroupRule(new LogGroupRule(new RegExp(".*"), LogLevel.fromString(Config.logLevel)))

const factory = LFService.createNamedLoggerFactory("LoggerFactory", options)
const Logger = new class {

    private _factory: LoggerFactory

    constructor(factory: LoggerFactory) {
        this._factory = factory
    }

    public getLogger(name: string) {
        return new class {

            private _logger: Logger
            constructor(logger: Logger) {
                this._logger = logger
            }

            private _replace(message: string, ...args: any[]): string {
                for (const arg of args)
                    message = message.replace(`{}`, arg)

                return message
            }

            public info(message: string, ...args: any[]) {
                const formattedMessage = this._replace(message, ...args)
                this._logger.info(formattedMessage)
            }

            public debug(message: string, ...args: any[]) {
                const formattedMessage = this._replace(message, ...args)
                this._logger.debug(formattedMessage)
            }

            public trace(message: string, ...args: any[]) {
                const formattedMessage = this._replace(message, ...args)
                this._logger.trace(formattedMessage)
            }

            public warn(message: string, ...args: any[]) {
                const formattedMessage = this._replace(message, ...args)
                this._logger.warn(formattedMessage)
            }

            public error(message: string, ...args: any[]) {
                const formattedMessage = this._replace(message, ...args)
                this._logger.error(formattedMessage)
            }

        }(this._factory.getLogger(name))
    }
}(factory)


export default Logger