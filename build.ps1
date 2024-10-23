"Building cryptomator cli..."

$commands = 'mvn'
$envVars = 'JAVA_HOME'
foreach ($cmd in $commands) {
    Invoke-Expression -Command "${cmd} --version" -ErrorAction Stop
}

(Get-ChildItem env:* | Where-Object { $envVars -contains $_.Name} | Measure-Object).Count

<#
foreach ($envVar in $envVars) {
    if( "$env:$envVar")
}
#>

mvn -B clean package
Copy-Item .\LICENSE.txt -Destination .\target
Move-Item .\target\cryptomator-cli-*.jar .\target\mods
## according to jdpes we only need java.base (and java.compiler due to dagger)
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

# jpackage
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
    --app-version "1.0.0.0" `
    --java-options "--enable-preview" `
    --java-options "--enable-native-access=org.cryptomator.jfuse.win" `
    --java-options "-Xss5m" `
    --java-options "-Xmx256m" `
    --java-options '-Dfile.encoding="utf-8"' `
    --win-console
    #--resource-dir dist/win/resources
    #--icon dist/win/resources/Cryptomator.ico
