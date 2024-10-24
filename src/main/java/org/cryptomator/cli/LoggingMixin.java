package org.cryptomator.cli;

import picocli.CommandLine.Spec;
import picocli.CommandLine.Model;
import picocli.CommandLine.Option;

public class LoggingMixin {

    @Spec(Spec.Target.MIXEE)
    private Model.CommandSpec mixee;

    boolean isVerbose;

    /**
     * Sets a verbose logging leven on the LoggingMixin of the top-level command.
     * @param isVerbose boolean flag to activate verbose mode
     */
    @Option(names = {"-v", "--verbose"}, description = {
            "Activate verbose mode"})
    public void setVerbose(boolean isVerbose) {
        // Each subcommand that mixes in the LoggingMixin has its own instance
        // of this class, so there may be many LoggingMixin instances.
        // We want to store the verbosity value in a single, central place,
        // so we find the top-level command,
        // and store the verbosity level on our top-level command's LoggingMixin.
        ((CryptomatorCli) mixee.root().userObject()).loggingMixin.isVerbose = isVerbose;
    }
}
