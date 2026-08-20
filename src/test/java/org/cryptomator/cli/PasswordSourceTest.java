package org.cryptomator.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordSourceTest {

    private static final String PASSPHRASE = "sw0rdfish-äöü";

    @TempDir
    Path tmp;

    @Test
    @DisplayName("readPassphrase fails if no source is specified")
    void readPassphraseFailsIfNoSourceIsSpecified() {
        var source = new PasswordSource();

        assertThrows(IllegalStateException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("readPassphrase returns the stdin input")
    void readPassphraseReturnsStdinInput() throws IOException {
        var source = new PasswordSource();
        source.passphraseStdin = PASSPHRASE.toCharArray();

        try (var passphrase = source.readPassphrase()) {
            assertArrayEquals(PASSPHRASE.toCharArray(), passphrase.content());
        }
    }

    @Test
    @DisplayName("readPassphrase reads from an environment variable")
    void readPassphraseReadsFromEnvironmentVariable() throws IOException {
        var envVarValue = System.getenv().get("JAVA_HOME");
        var source = new PasswordSource();
        source.passphraseEnvironmentVariable = "JAVA_HOME";

        try (var passphrase = source.readPassphrase()) {
            assertArrayEquals(envVarValue.toCharArray(), passphrase.content());
        }
    }

    @Test
    @DisplayName("readPassphrase fails if the environment variable is not defined")
    void readPassphraseFailsIfEnvironmentVariableIsNotDefined() {
        var source = new PasswordSource();
        source.passphraseEnvironmentVariable = "CRYPTOMATOR_CLI_TEST_UNDEFINED_VARIABLE";

        assertThrows(PasswordSource.ReadingEnvironmentVariableFailedException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("readPassphrase reads the utf-8 encoded file content")
    void readPassphraseReadsUtf8FileContent() throws IOException {
        var source = new PasswordSource();
        source.passphraseFile = Files.writeString(tmp.resolve("passphrase.txt"), PASSPHRASE);

        try (var passphrase = source.readPassphrase()) {
            assertArrayEquals(PASSPHRASE.toCharArray(), passphrase.content());
        }
    }

    @Test
    @DisplayName("readPassphrase strips a single trailing newline from the file content")
    void readPassphraseStripsTrailingNewlineFromFileContent() throws IOException {
        var source = new PasswordSource();
        source.passphraseFile = Files.writeString(tmp.resolve("passphrase.txt"), PASSPHRASE + "\n");

        try (var passphrase = source.readPassphrase()) {
            assertArrayEquals(PASSPHRASE.toCharArray(), passphrase.content());
        }
    }

    @Test
    @DisplayName("readPassphrase fails if the file is empty")
    void readPassphraseFailsIfFileIsEmpty() throws IOException {
        var source = new PasswordSource();
        source.passphraseFile = Files.createFile(tmp.resolve("empty.txt"));

        assertThrows(PasswordSource.ReadingFileFailedException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("readPassphrase fails if the file only contains a newline")
    void readPassphraseFailsIfFileOnlyContainsNewline() throws IOException {
        var source = new PasswordSource();
        source.passphraseFile = Files.writeString(tmp.resolve("newline.txt"), "\n");

        assertThrows(PasswordSource.ReadingFileFailedException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("readPassphrase fails if the file does not exist")
    void readPassphraseFailsIfFileDoesNotExist() {
        var source = new PasswordSource();
        source.passphraseFile = tmp.resolve("does-not-exist.txt");

        assertThrows(PasswordSource.ReadingFileFailedException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("readPassphrase fails if the file exceeds the maximum size")
    void readPassphraseFailsIfFileExceedsMaxSize() throws IOException {
        var source = new PasswordSource();
        source.passphraseFile = Files.write(tmp.resolve("huge.txt"), new byte[5_001]);

        assertThrows(PasswordSource.ReadingFileFailedException.class, source::readPassphrase);
    }

    @Test
    @DisplayName("confirmPassphrase is skipped without an interactive console")
    void confirmPassphraseIsSkippedWithoutConsole() {
        var source = new PasswordSource();
        source.passphraseStdin = PASSPHRASE.toCharArray();

        // there is no interactive terminal in the test environment, so the confirmation must be skipped
        assertDoesNotThrow(source::confirmPassphrase);
    }

    @Test
    @DisplayName("closing a passphrase wipes its content")
    void closingPassphraseWipesContent() {
        var content = PASSPHRASE.toCharArray();

        new PasswordSource.Passphrase(content).close();

        assertArrayEquals(new char[content.length], content);
    }
}
