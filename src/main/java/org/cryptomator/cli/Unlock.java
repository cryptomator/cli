package org.cryptomator.cli;

import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

@Command(name = "unlock",
        header = "Unlocks a vault",
        description = "Unlocks and mounts the given cryptomator vault, identified by the path to the vault directory." +
                "The unlocked vault is mounted into the local filesystem by the specified mounter." +
                "For a list of available mounters, use the `list-mounters` subcommand.",
        parameterListHeading = "%nParameters:%n",
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        optionListHeading = "%nOptions:%n",

        mixinStandardHelpOptions = true)
public class Unlock implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(Unlock.class);
    private static final byte[] PEPPER = new byte[0];
    private static final String CONFIG_FILE_NAME = "vault.cryptomator";
    private static final String MASTERKEY_FILE_NAME = "masterkey.cryptomator";
    private static final String FORCED_UNMOUNT_MSG = "GRACEFUL UNMOUNT FAILED. Please check if manual cleanups are necessary";

    @Spec
    Model.CommandSpec spec;
    @Mixin
    LoggingMixin loggingMixin;

    @Parameters(index = "0", paramLabel = "/path/to/vaultDirectory", description = "Path to the vault directory")
    Path pathToVault;

    @ArgGroup(multiplicity = "1")
    PasswordSource passwordSource;

    @ArgGroup(exclusive = false, multiplicity = "1")
    MountSetup mountSetup;

    @Option(names = {"--maxCleartextNameLength"}, description = "Maximum cleartext filename length limit of created files. Remark: If this limit is greater than the shortening threshold, it does not have any effect.")
    void setMaxCleartextNameLength(int input) {
        if (input <= 0) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    String.format("Invalid value '%d' for option '--maxCleartextNameLength': " +
                            "value must be a positive Number between 1 and %d.", input, Integer.MAX_VALUE));
        }
        maxCleartextNameLength = input;
    }

    private int maxCleartextNameLength = 0;

    private SecureRandom csprng = null;

    @Override
    public Integer call() throws Exception {
        csprng = SecureRandom.getInstanceStrong();

        var unverifiedConfig = readConfigFromStorage(pathToVault);
        var fsPropsBuilder = CryptoFileSystemProperties.cryptoFileSystemProperties() //
                .withKeyLoader(this::loadMasterkey) //
                .withShorteningThreshold(unverifiedConfig.allegedShorteningThreshold()); //cryptofs checks, if config is signed with masterkey
        if (maxCleartextNameLength > 0) {
            fsPropsBuilder.withMaxCleartextNameLength(maxCleartextNameLength);
        }

        try (var fs = CryptoFileSystemProvider.newFileSystem(pathToVault, fsPropsBuilder.build());
             var mount = mountSetup.mount(fs)) {
            LOG.info("Unlocked and mounted vault successfully to {}", mount.getMountpoint().uri());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> teardown(mount)));
            Thread.currentThread().join();
        }

        throw new IllegalStateException("Application exited without error or receiving shutdown signal.");
    }

    private void teardown(Mount m) {
        try {
            m.close();
        } catch (IOException | UnmountFailedException e) {
            LOG.error(FORCED_UNMOUNT_MSG, e);
        }
    }

    private Masterkey loadMasterkey(URI keyId) {
        Path filePath = pathToVault.resolve(MASTERKEY_FILE_NAME);
        try (var passphraseContainer = passwordSource.readPassphrase()) {
            return new MasterkeyFileAccess(PEPPER, csprng)
                    .load(filePath, CharBuffer.wrap(passphraseContainer.content()));
        } catch (IOException e) {
            LOG.error("Reading {} failed.", filePath, e);
            throw new MasterkeyLoadingFailedException("Unable to load key from file " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Attempts to read the vault config file and parse it without verifying its integrity.
     *
     * @throws IOException if reading the file fails
     */
    static VaultConfig.UnverifiedVaultConfig readConfigFromStorage(Path vaultPath) throws IOException {
        Path configPath = vaultPath.resolve(CONFIG_FILE_NAME);
        LOG.debug("Reading vault config from file {}.", configPath);
        String token = Files.readString(configPath, StandardCharsets.US_ASCII);
        return VaultConfig.decode(token);
    }

}
