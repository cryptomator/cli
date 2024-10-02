package org.cryptomator.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.ContextAwareBase;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@ConfiguratorRank(ConfiguratorRank.CUSTOM_HIGH_PRIORITY)
public class LogbackConfigurator extends ContextAwareBase implements Configurator {

    public static final AtomicReference<LogbackConfigurator> INSTANCE = new AtomicReference<>();

    static final Map<String, Level> DEFAULT_LOG_LEVELS = Map.of( //
            Logger.ROOT_LOGGER_NAME, Level.INFO, //
            "org.cryptomator", Level.INFO //
    );

    public static final Map<String, Level> DEBUG_LOG_LEVELS = Map.of( //
            Logger.ROOT_LOGGER_NAME, Level.DEBUG, //
            "org.cryptomator", Level.TRACE //
    );

    @Override
    public ExecutionStatus configure(LoggerContext context) {
        var encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("[%thread] %highlight(%-5level) %cyan(%logger{15}) - %msg %n");
        encoder.start();

        var stdout = new ConsoleAppender<ILoggingEvent>();
        stdout.setWithJansi(true);
        stdout.setContext(context);
        stdout.setName("STDOUT");
        stdout.setEncoder(encoder);
        stdout.start();

        // configure loggers:
        for (var loglevel : DEFAULT_LOG_LEVELS.entrySet()) {
            Logger logger = context.getLogger(loglevel.getKey());
            logger.setLevel(loglevel.getValue());
            logger.setAdditive(false);
            logger.addAppender(stdout);
        }

        //disable fuselocking messages
        Logger fuseLocking = context.getLogger("org.cryptomator.frontend.fuse.locks");
        fuseLocking.setLevel(Level.OFF);

        //make instance accessible
        INSTANCE.compareAndSet(null, this);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }

    /**
     * Adjust the log levels
     *
     * @param logLevels new log levels to use
     */
    public void setLogLevels(Map<String, Level> logLevels) {
        if (context instanceof LoggerContext lc) {
            for (var loglevel : logLevels.entrySet()) {
                Logger logger = lc.getLogger(loglevel.getKey());
                System.out.println(logger.getName());
                logger.setLevel(loglevel.getValue());
            }
        }
    }

}
