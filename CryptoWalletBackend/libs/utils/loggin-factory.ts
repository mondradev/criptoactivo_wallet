import { LoggerFactoryOptions, LogGroupRule, LogLevel, LFService } from "typescript-logging";
import ConfigService from "../../config.json";

const options = new LoggerFactoryOptions();
options.addLogGroupRule(new LogGroupRule(new RegExp(".*"), LogLevel.fromString(ConfigService.logLevel)));

const LoggerFactory = LFService.createNamedLoggerFactory("LoggerFactory", options);
export default LoggerFactory;