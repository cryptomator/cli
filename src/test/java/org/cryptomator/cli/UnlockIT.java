package org.cryptomator.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@code unlock} subcommand, run against the packaged app image.
 * <p>
 * The vault is served via the WebDAV fallback mounter, so no filesystem driver (FUSE, WinFsp) is required. Real mounts are covered by the tests of the respective mounter libraries.
 */
class UnlockIT {

    private static final String PASSPHRASE = "correct-horse-battery-staple";
    private static final String PW_ENV_VAR = "TEST_VAULT_PW";
    private static final Map<String, String> PW_ENV = Map.of(PW_ENV_VAR, PASSPHRASE);
    private static final String WEBDAV_MOUNTER = "org.cryptomator.frontend.webdav.mount.FallbackMounter";
    private static final Pattern MOUNT_URI_LOG_LINE = Pattern.compile("Unlocked and mounted vault successfully to (\\S+)");
    private static final String FORCED_UNMOUNT_MSG = "GRACEFUL UNMOUNT FAILED";

    @TempDir
    Path tmp;

    Path vaultPath;

    @BeforeEach
    void createVault() throws Exception {
        vaultPath = tmp.resolve("vault");
        var result = CliApp.run(PW_ENV, "create", "--password:env=" + PW_ENV_VAR, vaultPath.toString());
        assertEquals(0, result.exitCode(), result.output());
    }

    @Test
    @DisplayName("unlock serves the vault content via WebDAV")
    void unlockServesVaultContentViaWebDav() throws Exception {
        var unlockArgs = new String[]{"unlock", "--password:env=" + PW_ENV_VAR, "--mounter=" + WEBDAV_MOUNTER, "--loopbackPort=" + freePort(), vaultPath.toString()};
        try (var cli = CliApp.start(PW_ENV, unlockArgs)) {
            var mountUri = cli.waitForOutput(MOUNT_URI_LOG_LINE, Duration.ofMinutes(2)).group(1);
            var fileUri = URI.create(mountUri.endsWith("/") ? mountUri + "test.txt" : mountUri + "/test.txt");

            long ciphertextFilesBefore = countRegularFiles(vaultPath.resolve("d"));
            try (var http = HttpClient.newHttpClient()) {
                var putResponse = http.send(HttpRequest.newBuilder(fileUri).PUT(HttpRequest.BodyPublishers.ofString("hello vault")).build(), HttpResponse.BodyHandlers.discarding());
                assertEquals(2, putResponse.statusCode() / 100, "PUT " + fileUri + " failed with status " + putResponse.statusCode());

                var getResponse = http.send(HttpRequest.newBuilder(fileUri).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, getResponse.statusCode());
                assertEquals("hello vault", getResponse.body());
            }
            assertTrue(countRegularFiles(vaultPath.resolve("d")) > ciphertextFilesBefore, "expected new ciphertext files in the vault data dir");

            // graceful shutdown is only testable on POSIX systems, where destroy() sends SIGTERM and thereby triggers the shutdown hook
            if (!CliApp.isWindows()) {
                cli.terminate();
                cli.waitForExit(Duration.ofMinutes(1));
                assertFalse(cli.output().contains(FORCED_UNMOUNT_MSG), cli.output());
            }
        }
    }

    @Test
    @DisplayName("unlock fails with a wrong passphrase")
    void unlockFailsWithWrongPassphrase() throws Exception {
        var unlockArgs = new String[]{"unlock", "--password:env=" + PW_ENV_VAR, "--mounter=" + WEBDAV_MOUNTER, vaultPath.toString()};
        try (var cli = CliApp.start(Map.of(PW_ENV_VAR, "wrong passphrase"), unlockArgs)) {
            int exitCode = cli.waitForExit(Duration.ofMinutes(2));

            assertNotEquals(0, exitCode, cli.output());
            assertFalse(cli.output().contains("Unlocked and mounted vault successfully"), cli.output());
        }
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static long countRegularFiles(Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }
}
