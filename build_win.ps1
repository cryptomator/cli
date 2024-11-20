"Building cryptomator cli..."

# Check if maven is installed
$commands = 'mvn'
foreach ($cmd in $commands) {
    Invoke-Expression -Command "${cmd} --version" -ErrorAction Stop
}

# Check if JAVA_HOME is set
if(-not $env:JAVA_HOME) {
    throw "Environment variable JAVA_HOME not defined"
}

# Check Java version
$minJavaVersion=$(mvn help:evaluate "-Dexpression=jdk.version" -q -DforceStdout)
$javaVersion = $(& "$env:JAVA_HOME\bin\java" --version) -split ' ' | Select-Object -Index 1
if( ($javaVersion.Split('.') | Select-Object -First 1) -ne "22") {
    throw "Java version $javaVersion is too old. Minimum required version is $minJavaVersion"
}

Write-Host "Building java app with maven..."
mvn -B clean package
Copy-Item ./LICENSE.txt -Destination ./target -ErrorAction Stop
Move-Item ./target/cryptomator-cli-*.jar ./target/mods -ErrorAction Stop

Write-Host "Creating JRE with jlink..."
& $env:JAVA_HOME/bin/jlink `
    `@./dist/jlink.args `
    --module-path "${env:JAVA_HOME}/jmods"

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path ./target/runtime))) {
    throw "JRE creation with jLink failed with exit code $LASTEXITCODE."
}

# jpackage
# app-version is hard coded, since the script is only for local test builds
Write-Host "Creating app binary with jpackage..."
& $env:JAVA_HOME/bin/jpackage `
    `@./dist/jpackage.args `
    --java-options "--enable-native-access=org.cryptomator.jfuse.win" `
    --win-console

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path ./target/cryptomator-cli))) {
    throw "Binary creation with jpackage failed with exit code $LASTEXITCODE."
}