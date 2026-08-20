package org.cryptomator.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code create} subcommand, run against the packaged app image.
 */
class CreateIT {

    private static final String PASSPHRASE = "correct-horse-battery-staple";
    private static final String PW_ENV_VAR = "TEST_VAULT_PW";

    @TempDir
    Path tmp;

    @Test
    @DisplayName("create initializes a new vault")
    void createInitializesNewVault() throws Exception {
        var vaultPath = tmp.resolve("vault");

        var result = CliApp.run(Map.of(PW_ENV_VAR, PASSPHRASE), "create", "--password:env=" + PW_ENV_VAR, vaultPath.toString());

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(Files.isRegularFile(vaultPath.resolve("vault.cryptomator")), "vault config missing");
        assertTrue(Files.isRegularFile(vaultPath.resolve("masterkey.cryptomator")), "masterkey file missing");
        assertTrue(Files.isDirectory(vaultPath.resolve("d")), "data dir missing");
    }

    @Test
    @DisplayName("create fails if the vault directory already exists")
    void createFailsIfTargetAlreadyExists() throws Exception {
        var existingDir = Files.createDirectory(tmp.resolve("existing"));

        var result = CliApp.run(Map.of(PW_ENV_VAR, PASSPHRASE), "create", "--password:env=" + PW_ENV_VAR, existingDir.toString());

        assertNotEquals(0, result.exitCode(), result.output());
    }
}
