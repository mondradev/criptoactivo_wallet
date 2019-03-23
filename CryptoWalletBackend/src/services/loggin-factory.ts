import { LoggerFactoryOptions, LogGroupRule, LogLevel, LFService } from "typescript-logging";

const options = new LoggerFactoryOptions();
options.addLogGroupRule(new LogGroupRule(new RegExp(".*"), LogLevel.Trace));

const LoggerFactory = LFService.createNamedLoggerFactory("LoggerFactory", options);
export default LoggerFactory;