package org.cryptomator.cli.pwd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PasswordFromFileStrategy implements PasswordStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordFromFileStrategy.class);

    private final String vaultName;
    private final Path pathToFile;

    public PasswordFromFileStrategy(final String vaultName, final Path pathToFile) {
        this.vaultName = vaultName;
        this.pathToFile = pathToFile;
    }

    @Override
    public String password() {
        LOG.info("Vault " + "'" + vaultName + "'" + " password from file.");

        if (Files.isReadable(pathToFile) && Files.isRegularFile(pathToFile)) {
            try (Stream<String> lines = Files.lines(pathToFile)) {
                return lines.findFirst().get().toString();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (!Files.isReadable(pathToFile)) {
            throw new IllegalArgumentException("Cannot read password from file: " + pathToFile);
        }
    }

}
