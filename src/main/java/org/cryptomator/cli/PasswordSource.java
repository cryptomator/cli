package org.cryptomator.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PasswordSource {

    public static final Logger LOG = LoggerFactory.getLogger(PasswordSource.class);
    private static final int MAX_PASSPHRASE_FILE_SIZE = 5_000; //5KB

    @CommandLine.Option(names = {"--password:stdin"}, paramLabel = "Passphrase", description = "Passphrase, read from STDIN")
    boolean passphraseStdin;

    @CommandLine.Option(names = "--password:env", description = "Name of the environment variable containing the passphrase")
    String passphraseEnvironmentVariable = null;

    @CommandLine.Option(names = "--password:file", description = "Path of the file containing the passphrase. The password file must be utf-8 encoded and must not end with a new line")
    Path passphraseFile = null;

    Passphrase readPassphrase() throws IOException {
        if (passphraseStdin) {
            return readPassphraseFromStdin();
        } else if (passphraseEnvironmentVariable != null) {
            return readPassphraseFromEnvironment();
        } else if (passphraseFile != null) {
            return readPassphraseFromFile();
        }
        throw new IllegalStateException("Passphrase location not specified, but required.");
    }

    private Passphrase readPassphraseFromStdin() {
        LOG.debug("Reading passphrase from STDIN");
        System.out.println("Enter the password:");
        var console = System.console();
        if (console == null) {
            throw new IllegalStateException("No console found to read password from.");
        }
        return new Passphrase(console.readPassword());
    }

    private Passphrase readPassphraseFromEnvironment() {
        LOG.debug("Reading passphrase from env variable '{}'", passphraseEnvironmentVariable);
        var tmp = System.getenv(passphraseEnvironmentVariable);
        if (tmp == null) {
            throw new ReadingEnvironmentVariableFailedException("Environment variable " + passphraseEnvironmentVariable + " is not defined.");
        }
        char[] result = new char[tmp.length()];
        tmp.getChars(0, tmp.length(), result, 0);
        return new Passphrase(result);
    }

    private Passphrase readPassphraseFromFile() throws ReadingFileFailedException {
        LOG.debug("Reading passphrase from file '{}'.", passphraseFile);
        byte[] fileContent = null;
        CharBuffer charWrapper = null;
        try {
            if (Files.size(passphraseFile) > MAX_PASSPHRASE_FILE_SIZE) {
                throw new ReadingFileFailedException("Password file is too big. Max supported size is " + MAX_PASSPHRASE_FILE_SIZE + " bytes.");
            }
            fileContent = Files.readAllBytes(passphraseFile);
            charWrapper = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(fileContent));
            char[] content = new char[charWrapper.limit()];
            charWrapper.get(content);
            return new Passphrase(content);
        } catch (IOException e) {
            throw new ReadingFileFailedException(e);
        } finally {
            if (fileContent != null) {
                Arrays.fill(fileContent, (byte) 0);
            }
            if (charWrapper != null) {
                Arrays.fill(charWrapper.array(), (char) 0x00);
            }
        }
    }

    static class PasswordSourceException extends RuntimeException {
        PasswordSourceException(String msg) {
            super(msg);
        }

        PasswordSourceException(Throwable cause) {
            super(cause);
        }
    }

    static class ReadingFileFailedException extends PasswordSourceException {
        ReadingFileFailedException(Throwable e) {
            super(e);

        }

        public ReadingFileFailedException(String s) {
            super(s);
        }
    }

    static class ReadingEnvironmentVariableFailedException extends PasswordSourceException {
        ReadingEnvironmentVariableFailedException(String msg) {
            super(msg);
        }
    }

    record Passphrase(char[] content) implements AutoCloseable {

        @Override
        public void close() {
            Arrays.fill(content, (char) 0);
        }
    }

}
