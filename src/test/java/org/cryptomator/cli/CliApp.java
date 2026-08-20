package org.cryptomator.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test harness to run the packaged cryptomator-cli app image as an external process.
 * <p>
 * The executable is resolved from the system property {@code cli.executable} or, if not set, from the OS specific jpackage output location inside the {@code target} directory.
 */
final class CliApp {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    private CliApp() {
    }

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    static Path executable() {
        var override = System.getProperty("cli.executable");
        if (override != null) {
            return assertExecutable(Path.of(override));
        }
        var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        Path defaultPath;
        if (os.contains("win")) {
            defaultPath = Path.of("target/cryptomator-cli/cryptomator-cli.exe");
        } else if (os.contains("mac")) {
            defaultPath = Path.of("target/cryptomator-cli.app/Contents/MacOS/cryptomator-cli");
        } else {
            defaultPath = Path.of("target/cryptomator-cli/bin/cryptomator-cli");
        }
        return assertExecutable(defaultPath);
    }

    private static Path assertExecutable(Path path) {
        if (!Files.isExecutable(path)) {
            throw new IllegalStateException("CLI app image not found at " + path.toAbsolutePath() + ". Build it with the build script of your OS first or point -Dcli.executable to the binary.");
        }
        return path;
    }

    record Result(int exitCode, String output) {
    }

    /**
     * Runs the CLI to completion.
     *
     * @param env additional environment variables for the process
     * @param args command line arguments
     * @return exit code and combined stdout/stderr output
     */
    static Result run(Map<String, String> env, String... args) throws IOException, InterruptedException {
        try (var cli = start(env, args)) {
            int exitCode = cli.waitForExit(DEFAULT_TIMEOUT);
            return new Result(exitCode, cli.output());
        }
    }

    /**
     * Starts the CLI without waiting for it to exit, e.g. for the long-running unlock command.
     *
     * @param env additional environment variables for the process
     * @param args command line arguments
     * @return handle to the running process
     */
    static CliProcess start(Map<String, String> env, String... args) throws IOException {
        var command = new ArrayList<String>();
        command.add(executable().toAbsolutePath().toString());
        command.addAll(List.of(args));
        var processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        processBuilder.environment().putAll(env);
        return new CliProcess(processBuilder.start());
    }

    /**
     * A running CLI process with continuously captured output.
     */
    static final class CliProcess implements AutoCloseable {

        private final Process process;
        private final StringBuffer output = new StringBuffer();
        private final Thread outputReader;

        private CliProcess(Process process) {
            this.process = process;
            this.outputReader = Thread.ofVirtual().start(this::readOutput);
        }

        private void readOutput() {
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            } catch (IOException e) {
                // stream closed due to process termination
            }
        }

        String output() {
            return output.toString();
        }

        /**
         * Waits until the process output matches the given pattern.
         *
         * @param pattern pattern to search for in the output captured so far
         * @param timeout maximum time to wait
         * @return matcher positioned at the first match, e.g. to extract groups
         */
        Matcher waitForOutput(Pattern pattern, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (true) {
                var matcher = pattern.matcher(output());
                if (matcher.find()) {
                    return matcher;
                }
                if (!process.isAlive() || System.nanoTime() > deadline) {
                    outputReader.join(Duration.ofSeconds(5));
                    var lastAttempt = pattern.matcher(output());
                    if (lastAttempt.find()) {
                        return lastAttempt;
                    }
                    throw new AssertionError("Waiting for output matching '" + pattern + "' " + (process.isAlive() ? "timed out" : "failed, process already exited") + ". Output so far:\n" + output());
                }
                Thread.sleep(100);
            }
        }

        /**
         * Requests graceful termination (SIGTERM on POSIX systems), triggering the CLI shutdown hooks.
         */
        void terminate() {
            process.destroy();
        }

        int waitForExit(Duration timeout) throws InterruptedException {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Process did not exit within " + timeout + ". Output so far:\n" + output());
            }
            outputReader.join(Duration.ofSeconds(5));
            return process.exitValue();
        }

        @Override
        public void close() throws InterruptedException {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            outputReader.join(Duration.ofSeconds(5));
        }
    }
}
