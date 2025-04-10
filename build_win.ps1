"Building cryptomator cli..."

$appVersion='0.1.0-local'

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
$minJavaVersion=[int]$(mvn help:evaluate "-Dexpression=jdk.version" -q -DforceStdout)
$javaVersion = $(& "$env:JAVA_HOME\bin\java" --version) -split ' ' | Select-Object -Index 1
if( ([int] ($javaVersion.Split('.') | Select-Object -First 1)) -lt $minJavaVersion) {
    throw "Java version $javaVersion is too old. Minimum required version is $minJavaVersion"
}

Write-Host "Building java app with maven..."
mvn -B clean package
Copy-Item ./LICENSE.txt -Destination ./target -ErrorAction Stop
Move-Item ./target/cryptomator-cli-*.jar ./target/mods -ErrorAction Stop

Write-Host "Creating JRE with jlink..."
Get-Content -Path './dist/jlink.args' | ForEach-Object { $_.Replace('${JAVA_HOME}', "$env:JAVA_HOME")} | Out-File -FilePath './target/jlink.args'
& $env:JAVA_HOME/bin/jlink `@./target/jlink.args

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path ./target/runtime))) {
    throw "JRE creation with jLink failed with exit code $LASTEXITCODE."
}

## powershell does not have envsubst
$jpAppVersion='99.9.9'
Get-Content -Path './dist/jpackage.args' | ForEach-Object {
    $_.Replace('${APP_VERSION}', $appVersion).
        Replace('${JP_APP_VERSION}', $jpAppVersion).
        Replace('${NATIVE_ACCESS_PACKAGE}', 'org.cryptomator.jfuse.win')
} | Out-File -FilePath './target/jpackage.args'

# jpackage
Write-Host "Creating app binary with jpackage..."
& $env:JAVA_HOME/bin/jpackage `@./target/jpackage.args --win-console

if ( ($LASTEXITCODE -ne 0) -or (-not (Test-Path ./target/cryptomator-cli))) {
    throw "Binary creation with jpackage failed with exit code $LASTEXITCODE."
}