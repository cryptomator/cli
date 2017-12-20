package org.cryptomator.cli.pwd;

public interface PasswordStrategy {
    String password();
    void validate() throws IllegalArgumentException;
}
