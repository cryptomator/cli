[![Build Status](https://travis-ci.org/cryptomator/cli.svg?branch=develop)](https://travis-ci.org/cryptomator/cli)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

This is a minimal command-line program that unlocks vaults which can then be accessed via an embedded WebDAV server.

## Disclaimer

This project is in an early stage and not ready for production use. We recommend to use it only for testing and evaluation purposes.

## Download and Usage

Download the jar file via [GitHub Releases](https://github.com/cryptomator/cli/releases).

Cryptomator CLI depends on Java 8. In addition, the JCE unlimited strength policy files must be installed (needed for 256-bit keys).

```sh
java -jar cryptomator-cli-x.y.z.jar \
    --vault demoVault=/path/to/vault --password demoVault=topSecret \
    --vault otherVault=/path/to/differentVault --passwordfile otherVault=/path/to/fileWithPassword \
    --bind 127.0.0.1 --port 8080
# you can now mount http://localhost:8080/demoVault/
```

Then you can access the vault using any WebDAV client.

### Linux via davfs2

First, you need to create a mount point for your vault

```sh
sudo mkdir /media/your/mounted/folder
```

Then you can mount the vault

```sh
sudo mount -t davfs http://localhost:8080/demoVault/ /media/your/mounted/folder
```

To unmount the vault, run

```sh
sudo umount /media/your/mounted/folder
```

### macOS via AppleScript

Mount the vault with

```sh
osascript -e 'mount volume "http://localhost:8080/demoVault/"'
```

Unmount the vault with

```sh
osascript -e 'tell application "Finder" to if "demoVault" exists then eject "demoVault"'
```

## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license derived from the LGPL for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).
