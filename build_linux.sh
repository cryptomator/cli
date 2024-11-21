#!/bin/bash
set -euxo pipefail

echo "Building cryptomator cli..."

APP_VERSION='0.1.0-local'

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

# Check Java version
MIN_JAVA_VERSION=$(mvn help:evaluate "-Dexpression=jdk.version" -q -DforceStdout)
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version | head -n1 | cut -d' ' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "$MIN_JAVA_VERSION" ]; then
    echo "Java version $JAVA_VERSION is too old. Minimum required version is $MIN_JAVA_VERSION"
    exit 1
fi

echo "Building java app with maven..."
mvn -B clean package
cp ./LICENSE.txt ./target/
mv ./target/cryptomator-cli-*.jar ./target/mods

echo "Creating JRE with jlink..."
envsubst < dist/jlink.args > target/jlink.args
"$JAVA_HOME/bin/jlink" '@./target/jlink.args'

if [ $? -ne 0 ] || [ ! -d ./target/runtime ]; then
    echo "JRE creation with jlink failed."
    exit 1
fi

NATIVE_ACCESS_PACKAGE="no.native.access.available"
_OS=$(uname -s)
if (echo "$_OS" | grep -q "Linux.*") ; then
    _ARCH=$(uname -m)
    if [ "$_ARCH" = "x86_64" ]; then
        NATIVE_ACCESS_PACKAGE="org.cryptomator.jfuse.linux.amd64"
    elif [ "$_ARCH" = "aarch64"  ]; then
        NATIVE_ACCESS_PACKAGE="org.cryptomator.jfuse.linux.aarch64"
    else
        echo "Warning: Unsupported Linux architecture for FUSE mounter: $_ARCH"
        echo "FUSE supported architectures: x86_64, aarch64"
    fi
fi

JP_APP_VERSION='99.9.9'
envsubst < dist/jpackage.args > target/jpackage.args

echo "Creating app binary with jpackage..."
"$JAVA_HOME/bin/jpackage" '@./target/jpackage.args'

if [ $? -ne 0 ] || [ ! -d ./target/cryptomator-cli ]; then
    echo "Binary creation with jpackage failed."
    exit 1
fi
