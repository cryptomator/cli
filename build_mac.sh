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
    --app-version "99.9.9" \
    --java-options "-Dorg.cryptomator.cli.version=0.0.1-local" \
    --java-options "--enable-preview" \
    --java-options "--enable-native-access=org.cryptomator.jfuse.mac" \
    --java-options "-Xss5m" \
    --java-options "-Xmx256m" \
    --java-options "-Dfile.encoding=utf-8" \

if [ $? -ne 0 ] || [ ! -d ./target/cryptomator-cli.app ]; then
    echo "Binary creation with jpackage failed."
    exit 1
fi
