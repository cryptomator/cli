[![Build Status](https://travis-ci.org/cryptomator/cli.svg?branch=develop)](https://travis-ci.org/cryptomator/cli)

# Cryptomator CLI version

This is a minimal command line program which unlocks vaults, which can then be accessed via an embedded WebDAV server.

## Disclaimer

This project is in an early stage and not ready for production use. We recommend to use it only for testing and evaluation purposes.

## Download and Usage

Download the jar file via [GitHub Releases](https://github.com/cryptomator/cli/releases)

Cryptomator CLI depends on a Java 8 JRE. In addition the JCE unlimited strength policy files (needed for 256-bit keys) must be installed.

```sh
java -jar cryptomator-cli-x.y.z.jar --bind 0.0.0.0 --port 8080 --vault demoVault=/path/to/vault --password demoVault=topSecret
# you can now mount http://localhost:8080/demoVault/
```

In the current test version passwords can only be provided as a program argument. This will change in the future.

## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license derived from the LGPL for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).
