package org.cryptomator.cli;

import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
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
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(name = "cryptomator-cli",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Unlocks a cryptomator vault and mounts it into the system.")
public class CryptomatorCli implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(CryptomatorCli.class);
    private static final byte[] PEPPER = new byte[0];


    @Parameters(index = "0", paramLabel = "/path/to/vaultDirectory", description = "Path to the vault directory")
    Path pathToVault;

    @CommandLine.ArgGroup(multiplicity = "1")
    PasswordSource passwordSource;

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    MountOptions mountOptions;

    private SecureRandom csrpg = null;

    @Override
    public Integer call() throws Exception {
        csrpg = SecureRandom.getInstanceStrong();
        CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
                .withKeyLoader(this::loadMasterkey) //
                //TODO: shortening Threshold
                //TODO: maxCleartextname
                .build();
        try (var fs = CryptoFileSystemProvider.newFileSystem(pathToVault, fsProps);
             var mount = mountOptions.mount(fs)) {

            while (true) {
                int c = System.in.read();
                if (c == -1) { //END OF STREAM
                    //TODO: terminate with error?
                    mount.unmount();
                    return 1;
                } else if (c == 0x03) {//Ctrl+C
                    mount.unmount();
                    break;
                }
            }
        } catch (UnmountFailedException e) {
            LOG.error("Regular unmount failed. Just terminating...", e);
        }
        return 0;
    }

    private Masterkey loadMasterkey(URI keyId) {
        try {
            char[] passphrase = passwordSource.readPassphrase();
            Path filePath = pathToVault.resolve("masterkey.cryptomator");

            var masterkey = new MasterkeyFileAccess(PEPPER, csrpg).load(filePath, CharBuffer.wrap(passphrase));
            Arrays.fill(passphrase, '\u0000');
            return masterkey;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new CryptomatorCli())
                .setPosixClusteredShortOptionsAllowed(false)
                .execute(args);
        System.exit(exitCode);
    }
}
