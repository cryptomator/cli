"Building cryptomator cli..."

$commands = 'mvn'
foreach ($cmd in $commands) {
    Invoke-Expression -Command "${cmd} --version" -ErrorAction Stop
}

if(-not $env:JAVA_HOME) {
    throw "Environment variable JAVA_HOME not defined"
}

Write-Host "Building java app with maven..."
mvn -B clean package
Copy-Item .\LICENSE.txt -Destination .\target
Move-Item .\target\cryptomator-cli-*.jar .\target\mods

Write-Host "Creating JRE with jlink..."
& $env:JAVA_HOME\bin\jlink `
--verbose `
--output target\runtime `
--module-path "${env:JAVA_HOME}\jmods" `
--add-modules java.base,java.compiler,java.naming,java.xml `
--strip-native-commands `
--no-header-files `
--no-man-pages `
--strip-debug `
--compress zip-0

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path .\target\runtime))) {
   throw "JRE creation with jLink failed with exit code $LASTEXITCODE."
}

# jpackage
# app-version is hard coded, since the script is only for local test builds
Write-Host "Creating app binary with jpackage..."
& $env:JAVA_HOME\bin\jpackage `
    --verbose `
    --type app-image `
    --runtime-image target/runtime `
    --input target/libs `
    --module-path target/mods `
    --module org.cryptomator.cli/org.cryptomator.cli.CryptomatorCli `
    --dest target `
    --name cryptomator-cli `
    --vendor "Skymatic GmbH" `
    --copyright "(C) 2016 - 2024 Skymatic GmbH" `
    --app-version "0.0.1.0" `
    --java-options "-Dorg.cryptomator.cli.version=0.0.1-local" `
    --java-options "--enable-preview" `
    --java-options "--enable-native-access=org.cryptomator.jfuse.win" `
    --java-options "-Xss5m" `
    --java-options "-Xmx256m" `
    --java-options '-Dfile.encoding="utf-8"' `
    --win-console

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path .\target\cryptomator-cli))) {
    throw "Binary creation with jpackage failed with exit code $LASTEXITCODE."
}