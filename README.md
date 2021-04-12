[![Build](https://github.com/cryptomator/cli/workflows/Build/badge.svg)](https://github.com/cryptomator/cli/actions?query=workflow%3ABuild)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

This is a minimal command-line program that unlocks vaults of vault format 7.
After the unlock the vault content  can then be accessed via an embedded WebDAV server.
The minium required Java version is JDK 11.

## Disclaimer

This project is in an early stage and not ready for production use. We recommend to use it only for testing and evaluation purposes.

## Download and Usage

Download the jar file via [GitHub Releases](https://github.com/cryptomator/cli/releases).

Cryptomator CLI requires that at least JDK 11 is present on your system.

```sh
java -jar cryptomator-cli-x.y.z.jar \
    --vault demoVault=/path/to/vault --password demoVault=topSecret \
    --vault otherVault=/path/to/differentVault --passwordfile otherVault=/path/to/fileWithPassword \
    --bind 127.0.0.1 --port 8080
# you can now mount http://localhost:8080/demoVault/
```

## Filesystem integration

Once the vault is unlocked and the webserver started, you can access the vault by any webdav client or directly mounting it in your filesystem.

### Windows via the Windows Explorer GUI

Open the File Explorer, right click on "This PC" and click on the menu item "Map network drive...".
In the window opening up, select a free drive letter as the mounting point, enter in the Folder text box the url logged by the cli application to the terminal window and click the "Finish" button.

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

## Using as a docker image

### Bridge networking with port forward:

:warning: **WARNING: This approach should only be used to test the containerized approach, not in production.** :warning:

The reason is that with port forwarding you need to listen on all interfaces, and potencially other devices on the network could also access your WebDAV server exposing your secret files.

Ideally you would run this in a private docker network with trusted containers built by yourself communicating with each other. **Again, the below example is for testing purposes only to understand how the container would behave in production.**

```sh
docker run --rm -p 8080:8080 \
    -v /path/to/vault:/vaults/vault \
    -v /path/to/differentVault:/vaults/differentVault \
    -v /path/to/fileWithPassword:/passwordFile \
    cryptomator/cli \
    --bind 0.0.0.0 --port 8080 \
    --vault demoVault=/vaults/vault --password demoVault=topSecret \
    --vault otherVault=/vaults/differentVault --passwordfile otherVault=/passwordFile
# you can now mount http://localhost:8080/demoVault/
```

### Host networking:

```sh
docker run --rm --network=host \
    -v /path/to/vault:/vaults/vault \
    -v /path/to/differentVault:/vaults/differentVault \
    -v /path/to/fileWithPassword:/passwordFile \
    cryptomator/cli \
    --bind 127.0.0.1 --port 8080 \
    --vault demoVault=/vaults/vault --password demoVault=topSecret \
    --vault otherVault=/vaults/differentVault --passwordfile otherVault=/passwordFile
# you can now mount http://localhost:8080/demoVault/
```

Then you can access the vault using any WebDAV client.


## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license derived from the LGPL for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).
