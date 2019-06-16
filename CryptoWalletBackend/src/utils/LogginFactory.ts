import { LoggerFactoryOptions, LogGroupRule, LogLevel, LFService } from "typescript-logging"
import Config from "../../config"

const options = new LoggerFactoryOptions()
options.addLogGroupRule(new LogGroupRule(new RegExp(".*"), LogLevel.fromString(Config.logLevel)))

const LoggerFactory = LFService.createNamedLoggerFactory("LoggerFactory", options)
export default LoggerFactory