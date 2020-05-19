package org.cryptomator.cli.pwd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordFromPropertyStrategy implements PasswordStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordFromPropertyStrategy.class);

    private final String vaultName;
    private final String password;

    public PasswordFromPropertyStrategy(final String vaultName, final String password) {
        this.vaultName = vaultName;
        this.password = password;
    }

    @Override
    public String password() {
        LOG.info("Vault " + "'" + vaultName + "'" + " password from property.");
        return this.password;
    }

    @Override
    public void validate() throws IllegalArgumentException {
        if (password.equals("")) {
            throw new IllegalArgumentException("Invalid password");
        }
    }
}
