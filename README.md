[![Build](https://github.com/cryptomator/cli/workflows/Build/badge.svg)](https://github.com/cryptomator/cli/actions?query=workflow%3ABuild)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

This is a minimal command-line application that unlocks a single vault of vault format 8 and mounts it into the system.

## Download and Usage

Download the zip file via [GitHub Releases](https://github.com/cryptomator/cli/releases) and unzip it to your desired directory, e.g.

```sh
curl -L https://github.com/cryptomator/cli/releases/download/0.7.0/cryptomator-cli-0.7.0-mac-arm64.dmg --output cryptomator-cli.zip
unzip cryptomator-cli.zip
```

Afterwards, you can directly run Cryptomator-CLI:
```sh
cryptomator-cli unlock \
--password:stdin \
--mounter=org.cryptomator.frontend.fuse.mount.LinuxFuseMountProvider \
--mountPoint=/path/to/empty/dir \
/home/user/myVault
```

For a complete list of options, use the`--help` option.
```shell
cryptomator-cli unlock --help`
```

## FileSystem Integration

For an OS integration of your unlocked vault, cryptomator-cli relies on third party libraries which must be installed seperately.
These are:
* [WinFsp](https://winfsp.dev/) for Windows
* [macFUSE](https://osxfuse.github.io/) or [FUSE-T](https://www.fuse-t.org/) for macOS
* and [libfuse](https://github.com/libfuse/libfuse) for Linux/BSD systems (normally provided by a fuse3 package of your distro, e.g. [ubuntu](https://packages.ubuntu.com/noble/fuse3))

As a fallback, you can [skip filesystem integration](README.md#skip-filesystem-integration).

## Selecting the Mounter

TODO

## Skip Filesystem Integration 

If you don't want a direct integration in the OS, choose `org.cryptomator.frontend.webdav.mount.FallbackMounter` for `--mounter`.
It starts a local WebDAV server, where you can access the vault with any WebDAV client or mounting it into your filesystem manually.

> [!NOTE]
> The WebDAV protocol is supported by all major OSses. Hence, if other mounters fail or show errors when accessing the vault content, you can always use the legacy WebDAV option.
> WebDAV is not the default, because it has a low performance and might have OS dependent restrictions (e.g. maximum file size of 4GB on Windows)

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
