package org.cryptomator.cli;

import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.mount.UnmountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

@Command(name = "cryptomator-cli",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Unlocks a cryptomator vault and mounts it into the system.")
public class CryptomatorCli implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(CryptomatorCli.class);
    private static final byte[] PEPPER = new byte[0];
    private static final String CONFIG_FILE_NAME = "vault.cryptomator";

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Parameters(index = "0", paramLabel = "/path/to/vaultDirectory", description = "Path to the vault directory")
    Path pathToVault;

    @CommandLine.Option(names = {"--verbose"}, description = "Use verbose logging.")
    boolean verbose = false;

    @CommandLine.ArgGroup(multiplicity = "1")
    PasswordSource passwordSource;

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    MountSetup mountSetup;

    @CommandLine.Option(names = {"--maxCleartextNameLength"}, description = "Maximum cleartext filename length limit of created files. Remark: If this limit is greater than the shortening threshold, it does not have any effect.")
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
        if (verbose) {
            activateVerboseMode();
        }
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
            System.out.println(mount.getMountpoint().uri());
            while (true) {
                int c = System.in.read();
                //TODO: Password piping is currently not supported due to read() returing -1
                if (c == -1 || c == 0x03 || c == 0x04) {//Ctrl+C, Ctrl+D
                    LOG.info("Unmounting and locking vault...");
                    mount.unmount();
                    break;
                }
            }
        } catch (UnmountFailedException e) {
            LOG.error("Regular unmount failed. Just terminating process...", e);
        }
        return 0;
    }

    private void activateVerboseMode() {
        var logConfigurator = LogbackConfigurator.INSTANCE.get();
        if (logConfigurator == null) {
            throw new IllegalStateException("Logging is not configured.");
        }
        logConfigurator.switchToDebug();
        LOG.debug("Activated debug logging");
    }

    private Masterkey loadMasterkey(URI keyId) {
        try (var passphraseContainer = passwordSource.readPassphrase()) {
            Path filePath = pathToVault.resolve("masterkey.cryptomator");
            return new MasterkeyFileAccess(PEPPER, csprng)
                    .load(filePath, CharBuffer.wrap(passphraseContainer.content()));
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public static void main(String... args) {
        int exitCode = new CommandLine(new CryptomatorCli())
                .setPosixClusteredShortOptionsAllowed(false)
                .execute(args);
        System.exit(exitCode);
    }
}
