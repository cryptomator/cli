package org.cryptomator.cli;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PasswordSource {

    @CommandLine.Option(names = {"--password"}, paramLabel = "Passphrase", description = "Passphrase, read from STDIN")
    boolean passphraseStdin;

    @CommandLine.Option(names = "--password:env", description = "Name of the environment variable containing the passphrase")
    String passphraseEnvironmentVariable = null;

    @CommandLine.Option(names = "--password:file", description = "Path of the file containing the passphrase")
    Path passphraseFile = null;

    @CommandLine.Option(names = "--password:ipc", hidden = true, description = "Used by Cryptomator GUI")
    boolean passphraseIpc = false;


    Passphrase readPassphrase() throws IOException {
        if (passphraseStdin) {
            return readPassphraseFromStdin();
        } else if (passphraseEnvironmentVariable != null) {
            return readPassphraseFromEnvironment();
        } else if (passphraseFile != null) {
            return readPassphraseFromFile();
        } else {
            //TODO: use ipc
            return new Passphrase(new char[]{});
        }
    }

    private Passphrase readPassphraseFromStdin() {
        System.out.println("Enter a value for --password:");
        var console = System.console();
        if (console == null) {
            throw new IllegalStateException("No console found to read password from.");
        }
        return new Passphrase(console.readPassword());
    }

    private Passphrase readPassphraseFromEnvironment() {
        var tmp = System.getenv(passphraseEnvironmentVariable);
        if (tmp == null) {
            throw new ReadingEnvironmentVariableFailedException("Environment variable " + passphraseEnvironmentVariable + " is not defined.");
        }
        char[] result = new char[tmp.length()];
        tmp.getChars(0, tmp.length(), result, 0);
        return new Passphrase(result);
    }

    private Passphrase readPassphraseFromFile() throws ReadingFileFailedException {
        try {
            var bytes = Files.readAllBytes(passphraseFile);
            var byteBuffer = ByteBuffer.wrap(bytes);
            var charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
            return new Passphrase(charBuffer.array());
        } catch (IOException e) {
            throw new ReadingFileFailedException(e);
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
    }

    static class ReadingEnvironmentVariableFailedException extends PasswordSourceException {
        ReadingEnvironmentVariableFailedException(String msg) {
            super(msg);
        }
    }

    record Passphrase(char [] content) implements AutoCloseable {

        @Override
        public void close() {
            Arrays.fill(content, (char) 0);
        }
    }

}
