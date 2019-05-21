import { LoggerFactoryOptions, LogGroupRule, LogLevel, LFService } from "typescript-logging";

export function getLogger(name: string, logLevel?: 'Trace' | 'Debug' | 'Warn' | 'Error' | 'Info') {
    const options = new LoggerFactoryOptions()
        .addLogGroupRule(
            new LogGroupRule(new RegExp(".*"),
                LogLevel.fromString(logLevel || 'Info'))
        );

    const factory = LFService.createNamedLoggerFactory("LoggerFactory", options);

    return factory.getLogger(name)
}
