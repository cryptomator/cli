package org.cryptomator.cli.pwd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

public class PasswordFromStdInputStrategy implements PasswordStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordFromStdInputStrategy.class);

    private final String vaultName;
    private final String inputMessage = "Enter password for vault '%s': ";

    public PasswordFromStdInputStrategy(final String vaultName) {
        this.vaultName = vaultName;
    }

    @Override
    public String password() {
        LOG.info("Vault " + "'" + vaultName + "'" + " password from standard input.");

        String password = "";
        Console console = System.console();
        if (console == null) {
            LOG.warn("No console: non-interactive mode, instead use insecure replacement, PW is shown!");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println(String.format(inputMessage, vaultName));

            try {
                password = reader.readLine();
            } catch (IOException e) {
                LOG.error("There was an error reading line from console.");
                e.printStackTrace();
            }
        } else {
            System.out.println(String.format(inputMessage, vaultName));
            password = new String(console.readPassword());
        }

        return password;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (vaultName.equals("")) {
            throw new IllegalArgumentException("Invalid vault name");
        }
    }
}
