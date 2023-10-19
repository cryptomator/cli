package org.cryptomator.cli;

public class Version {
    public static final String IMPLEMENTATION_VERSION = getImplementationVersion();

    private static String getImplementationVersion() {
        return Version.class
                .getPackage()
                .getImplementationVersion();
    }
}
