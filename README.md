[![Build](https://github.com/cryptomator/cli/workflows/Build/badge.svg)](https://github.com/cryptomator/cli/actions?query=workflow%3ABuild)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

This is a minimal command-line application that unlocks a single vault of vault format 8 and mounts it into the system.

## Download and Usage

Download the JAR file via [GitHub Releases](https://github.com/cryptomator/cli/releases).

Cryptomator CLI requires that at least JDK/JRE 22 is present on your system.
```sh
java --enable-native-access="ALL-UNNAMED" -jar cryptomator-cli-x.y.z.jar \
    --password:stdin \
    --mounter=org.cryptomator.frontend.fuse.mount.FuseMountProvider \
    --mountPoint=/home/user/existing/empty/dir \
    /path/to/vault
# Be aware that passing the password on the command-line typically makes it visible to anyone on your system!
```

For a complete list of options, start the jar with the `--help` argument.
```shell
java --enable-native-access="ALL-UNNAMED" -jar cryptomator-cli-x.y.z.jar --help
```

## Block Filesystem Integration 

Depending on the chosen mounter, the vault is automatically integrated into the os.
If you don't want a direct integration, choose `org.cryptomator.frontend.webdav.mount.FallbackMounter` for `--mounter`.
It starts a local WebDAV server started, where you can access the vault by any WebDAV client or mounting it into your filesystem manually.

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

## License

This project is dual-licensed under the AGPLv3 for FOSS projects as well as a commercial license derived from the LGPL for independent software vendors and resellers. If you want to use this library in applications, that are *not* licensed under the AGPL, feel free to contact our [support team](https://cryptomator.org/help/).
