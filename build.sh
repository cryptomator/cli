#!/bin/bash

echo "Building cryptomator cli..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven to proceed."
    exit 1
fi

# Check if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "Environment variable JAVA_HOME not defined"
    exit 1
fi

echo "Building java app with maven..."
mvn -B clean package
cp ./LICENSE.txt ./target/
mv ./target/cryptomator-cli-*.jar ./target/mods

echo "Creating JRE with jlink..."
"$JAVA_HOME/bin/jlink" \
    --verbose \
    --output target/runtime \
    --module-path "${JAVA_HOME}/jmods" \
    --add-modules java.base,java.compiler,java.naming,java.xml \
    --strip-native-commands \
    --no-header-files \
    --no-man-pages \
    --strip-debug \
    --compress zip-0

if [ $? -ne 0 ] || [ ! -d ./target/runtime ]; then
    echo "JRE creation with jlink failed."
    exit 1
fi

NATIVE_ACCESS_PACKAGE="no.native.access.needed"
_OS=$(uname -s)
if (echo "$_OS" | grep -q "Linux.*") ; then
    _ARCH=$(uname -m)
    if [ "$_ARCH" = "x86_64" ]; then
        NATIVE_ACCESS_PACKAGE="org.cryptomator.jfuse.linux.amd64"
    elif [ "$_ARCH" = "aarch64"  ]; then
        NATIVE_ACCESS_PACKAGE="org.cryptomator.jfuse.linux.aarch64"
    fi
elif (echo "$_OS" | grep -q "Darwin.*"); then
    NATIVE_ACCESS_PACKAGE="org.cryptomator.jfuse.darwin"
fi

echo "Creating app binary with jpackage..."
"$JAVA_HOME/bin/jpackage" \
    --verbose \
    --type app-image \
    --runtime-image target/runtime \
    --input target/libs \
    --module-path target/mods \
    --module org.cryptomator.cli/org.cryptomator.cli.CryptomatorCli \
    --dest target \
    --name cryptomator-cli \
    --vendor "Skymatic GmbH" \
    --copyright "(C) 2016 - 2024 Skymatic GmbH" \
    --app-version "0.0.1.0" \
    --java-options "-Dorg.cryptomator.cli.version=0.0.1-local" \
    --java-options "--enable-preview" \
    --java-options "--enable-native-access=${NATIVE_ACCESS_PACKAGE}" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --java-options "-Dfile.encoding=utf-8" \

if [ $? -ne 0 ] || [ ! -d ./target/cryptomator-cli ]; then
    echo "Binary creation with jpackage failed."
    exit 1
fi
