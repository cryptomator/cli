package org.cryptomator.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Smoke test for the packaged app image.
 */
class CryptomatorCliIT {

    @Test
    @DisplayName("--version exits with 0 and prints a version")
    void versionPrintsVersionAndExitsSuccessfully() throws Exception {
        var result = CliApp.run(Map.of(), "--version");

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(result.output().isBlank());
    }
}
