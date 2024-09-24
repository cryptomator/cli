[![Build](https://github.com/cryptomator/cli/workflows/Build/badge.svg)](https://github.com/cryptomator/cli/actions?query=workflow%3ABuild)
[![Latest Release](https://img.shields.io/github/release/cryptomator/cli/all.svg)](https://github.com/cryptomator/cli/releases/latest)

# Cryptomator CLI

> [!NOTE]
> Project is currently on hold due.
> GraalVM 22 does not [support shared Arenas and Upcall Handlers of the FFI API](https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/foreign-interface/), but these API features are used by jfuse
> This might get resolved with future GraalVM versions.

This is a minimal command-line application that unlocks vaults of vault format 8.
This project uses [picocli](https://picocli.info/) and [GraalVM](https://www.graalvm.org/) to create a native CLI written in Java.

Requirements:
* GraalVM JDK 22
* OS toolchain to compile C code (msvc/gcc/ etc)
* Maven 3.9.9 (maybe older version work too)
* **Unreleased Cryptofs version (see below)**

The CLI itself is in MVP state:
* existing vaults can be unlocked
* passwords can be enterd via stdin, env var or file
* the desired mounter can be selected and mount options specified
* the cli unlocks exactly one vault
* to lock a vault, terminate the process (e.g. CTRL+C)

The native image can be built with
```
mvn clean package -Pnative
```

## Cryptofs Patch

In [cryptofs](https://github.com/cryptomator/cryptofs), the filesystem wide SecureRandom instance is initialized in a static block of the provider.
Since the CryptoFileSystemProvider is initialized at build time (due to the SPI mechanism of Java), the secure random instance would be included there as well with a fixed seed.
GraalVM blocks compiliation, hence a patch is needed for cryptofs (version 2.7.0).
To fix this. apply the patch file [./cryptofs_patch.diff](cryptofs_patch.diff) in cryptofs and install it to the local maven repository.

## Logging
Currently, the good ol' JUL is used, due to easy integration with graalvm.

## Native Image Remarks
The POM defines an extra profile for native image generation: `native`
The config is based on the tutorial for the graalvm [maven plugin](https://graalvm.github.io/native-build-tools/0.9.21/maven-plugin-quickstart.html).

The `initialize-at-build-time` arguments were added based on the feedback for the graalvm compiler feedback.

The graalvm metadata can be found in `src/main/resources/META-INF/native-image` and was generated with the maven exec plugin with activated nativeimage-agent.
To generate new metadata, adjust the agument config in the pom and run `mvn clean compile exec:exec` and copy the generated metadata from `graalvm-agent` to the above dir.
