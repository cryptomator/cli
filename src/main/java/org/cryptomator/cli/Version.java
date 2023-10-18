package org.cryptomator.cli;

public class Version {
    public static final String IMPLEMENTATION_VERSION = getImplementationVersion();

    private static String getImplementationVersion() {
        return new Version()
                .getClass()
                .getPackage()
                .getImplementationVersion();
    }
}
