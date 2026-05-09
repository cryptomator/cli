package org.cryptomator.cli;

import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

@Command(
        name = "create",
        header = "Creates a vault",
        description = "Creates a new cryptomator vault at the specified path.",
        parameterListHeading = "%nParameters:%n",
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        optionListHeading = "%nOptions:%n",
        mixinStandardHelpOptions = true)
public class Create implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(Create.class);
    private static final byte[] PEPPER = new byte[0];
    private static final String MASTERKEY_FILE_NAME = "masterkey.cryptomator";
    private static final URI DEFAULT_KEY_ID = URI.create("masterkeyfile:" + MASTERKEY_FILE_NAME);

    @Spec Model.CommandSpec spec;
    @Mixin LoggingMixin loggingMixin;

    @Parameters(
            index = "0",
            paramLabel = "/path/to/vaultDirectory",
            description = "Path to the vault directory")
    Path pathToVault;

    @ArgGroup(multiplicity = "1")
    PasswordSource passwordSource;

    private SecureRandom csprng = null;

    @Override
    public Integer call() throws Exception {
        csprng = SecureRandom.getInstanceStrong();

        try (var passphraseContainer = passwordSource.readPassphrase()) {
            passwordSource.confirmPassphrase();

            // Throw exception if there's something already there.
            Files.createDirectory(pathToVault);

            try (var masterkey = Masterkey.generate(csprng)) {
                persistMasterkey(pathToVault, masterkey, passphraseContainer.content());
                initializeVault(pathToVault, masterkey);
            }
        }

        LOG.info("Vault created successfully in {}", pathToVault);
        return 0;
    }

    private void persistMasterkey(Path path, Masterkey masterkey, char[] passphrase)
            throws IOException {
        Path masterkeyFilePath = path.resolve(MASTERKEY_FILE_NAME);
        MasterkeyFileAccess masterkeyFileAccess = new MasterkeyFileAccess(PEPPER, csprng);
        masterkeyFileAccess.persist(masterkey, masterkeyFilePath, CharBuffer.wrap(passphrase));
    }

    private void initializeVault(Path path, Masterkey masterkey) throws IOException {
        CryptoFileSystemProperties fsProps =
                CryptoFileSystemProperties.cryptoFileSystemProperties()
                        .withCipherCombo(CryptorProvider.Scheme.SIV_GCM)
                        .withKeyLoader(ignored -> masterkey.copy())
                        .build();
        try {
            CryptoFileSystemProvider.initialize(path, fsProps, DEFAULT_KEY_ID);
        } catch (CryptoException e) {
            throw new IOException("Vault initialization failed", e);
        }
    }
}
