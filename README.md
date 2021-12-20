[![Build](https://github.com/cryptomator/cli/workflows/Build/badge.svg)](https://github.com/cryptomator/cli/actions?query=workflow%3ABuild)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

This is a minimal command-line application that unlocks vaults of vault format 8.
After unlocking the vaults, its vault content can be accessed via an embedded WebDAV server.
The minimum required Java version is JDK 17.

## Disclaimer

:warning: This project is in an early stage and not ready for production use. We recommend using it only for testing and evaluation purposes.

## Download and Usage

Download the JAR file via [GitHub Releases](https://github.com/cryptomator/cli/releases).

Cryptomator CLI requires that at least JDK 17 is present on your system.

```sh
java -jar cryptomator-cli-x.y.z.jar \
    --vault demoVault=/path/to/vault --password demoVault=topSecret \
    --vault otherVault=/path/to/differentVault --passwordfile otherVault=/path/to/fileWithPassword \
    --vault thirdVault=/path/to/thirdVault  \
    --bind 127.0.0.1 --port 8080
# You can now mount http://localhost:8080/demoVault/
# The password for the third vault is read from stdin
# Be aware that passing the password on the command-line typically makes it visible to anyone on your system!
```

## Filesystem Integration

Once the vault is unlocked and the WebDAV server started, you can access the vault by any WebDAV client or directly mounting it in your filesystem.

### Windows via Windows Explorer

Open the File Explorer, right click on "This PC" and click on the menu item "Map network drive...".

1. In the Drive list, select a drive letter. (Any available letter will do.)
2. In the Folder box, enter the URL logged by the Cryptomator CLI application.
3. Select Finish.

### Linux via davfs2

First, you need to create a mount point for your vault:

```sh
sudo mkdir /media/your/mounted/folder
```

Then you can mount the vault:

```sh
echo | sudo mount -t davfs -o username=,user,gid=1000,uid=1000 http://localhost:8080/demoVault/ /media/your/mounted/folder
# Replace gid/uid with your gid/uid. The echo is used to skip over the password query from davfs
```

To unmount the vault, run:

```sh
sudo umount /media/your/mounted/folder
```

### macOS via AppleScript

Mount the vault with:

```sh
osascript -e 'mount volume "http://localhost:8080/demoVault/"'
```

Unmount the vault with:

```sh
osascript -e 'tell application "Finder" to if "demoVault" exists then eject "demoVault"'
```

## Using as a Docker image

### Bridge Network with Port Forwarding

:warning: **WARNING: This approach should only be used to test the containerized approach, not in production.** :warning:

The reason is that with port forwarding, you need to listen on all interfaces. Other devices on the network could also access your WebDAV server and potentially expose your secret files.

Ideally, you would run this in a private Docker network with trusted containers built by yourself communicating with each other. **Again, the below example is for testing purposes only to understand how the container would behave in production.**

```sh
docker run --rm -p 8080:8080 \
    -v /path/to/vault:/vaults/vault \
    -v /path/to/differentVault:/vaults/differentVault \
    -v /path/to/fileWithPassword:/passwordFile \
    cryptomator/cli \
    --bind 0.0.0.0 --port 8080 \
    --vault demoVault=/vaults/vault --password demoVault=topSecret \
    --vault otherVault=/vaults/differentVault --passwordfile otherVault=/passwordFile
# You can now mount http://localhost:8080/demoVault/
```

### Host Network

```sh
docker run --rm --network=host \
    -v /path/to/vault:/vaults/vault \
    -v /path/to/differentVault:/vaults/differentVault \
    -v /path/to/fileWithPassword:/passwordFile \
    cryptomator/cli \
    --bind 127.0.0.1 --port 8080 \
    --vault demoVault=/vaults/vault --password demoVault=topSecret \
    --vault otherVault=/vaults/differentVault --passwordfile otherVault=/passwordFile
# You can now mount http://localhost:8080/demoVault/
```

Then you can access the vault using any WebDAV client.

## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license derived from the LGPL for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).
