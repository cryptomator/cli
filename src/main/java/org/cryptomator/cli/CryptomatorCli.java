package org.cryptomator.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.RunLast;

@Command(name = "cryptomator-cli",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Unlocks a cryptomator vault and mounts it into the system.",
        subcommands = Unlock.class)
public class CryptomatorCli {

    private static final Logger LOG = LoggerFactory.getLogger(CryptomatorCli.class);
    @Mixin
    LoggingMixin loggingMixin;

    private int executionStrategy(ParseResult parseResult) {
        if (loggingMixin.isVerbose) {
            activateVerboseMode();
        }
        return new RunLast().execute(parseResult); // default execution strategy
    }

    private void activateVerboseMode() {
        var logConfigurator = LogbackConfigurator.INSTANCE.get();
        if (logConfigurator == null) {
            throw new IllegalStateException("Logging is not configured.");
        }
        logConfigurator.switchToDebug();
        LOG.debug("Activated debug logging");
    }


    public static void main(String... args) {
        var app = new CryptomatorCli();
        int exitCode = new CommandLine(app)
                .setPosixClusteredShortOptionsAllowed(false)
                .setExecutionStrategy(app::executionStrategy)
                .execute(args);
        System.exit(exitCode);
    }
}
