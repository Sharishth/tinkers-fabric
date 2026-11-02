<#
.SYNOPSIS
  Rebuilds the dev environment from nothing: finds the right JDK, fetches Gradle, builds and tests the mods.

.DESCRIPTION
  Version-aware: every requirement is read from the project, nothing is hard-coded here.
    Java      : java_version in gradle.properties
    Minecraft : minecraft_version, loader_version, fabric_api_version, loom_version in gradle.properties
    Gradle    : distributionUrl in gradle\wrapper\gradle-wrapper.properties (checksum-pinned, verified by the wrapper)
  Steps:
    1. Find a JDK of the required version: -JavaHome, TF_JAVA_HOME, scripts\dev.local.bat, JAVA_HOME,
       then common install folders under Program Files and LocalAppData.
    2. Save it to scripts\dev.local.bat (git-ignored) if that file does not exist, so scripts\dev.bat works.
    3. 'gradlew --version' downloads and verifies Gradle; 'gradlew build' compiles, runs unit tests and server
       GameTests (the first run downloads Minecraft and Fabric API, several minutes).
    4. With -ClientTest, also runs the client game test (opens a game window, saves screenshots).

.PARAMETER JavaHome    Path to a JDK home of the required version.
.PARAMETER NoBuild     Only check and restore tools; skip the build.
.PARAMETER ClientTest  Also run the client game test after the build.

.EXAMPLE
  scripts\env\restore_env.bat -ClientTest
#>
[CmdletBinding()]
param(
    [string]$JavaHome,
    [switch]$NoBuild,
    [switch]$ClientTest
)
$ErrorActionPreference = 'Stop'

$Project = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Project

# Reads key=value lines from a .properties file.
function Read-Properties([string]$Path) {
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([^#=\s][^=]*?)\s*=\s*(.*)$') { $map[$Matches[1]] = $Matches[2].Trim() }
    }
    return $map
}

# Returns the first line of 'java -version' for a JDK home, or $null.
function Get-JavaVersionLine([string]$JdkHome) {
    $javaExe = Join-Path $JdkHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe)) { return $null }
    $oldErr = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    $line = (& $javaExe -version 2>&1 | Select-Object -First 1) | Out-String
    $ErrorActionPreference = $oldErr
    return $line.Trim()
}

# Runs the Gradle wrapper and stops the script when it fails.
function Invoke-Gradle([string[]]$Arguments) {
    & (Join-Path $Project 'gradlew.bat') @Arguments
    if ($LASTEXITCODE -ne 0) { throw "gradlew $($Arguments -join ' ') failed with exit code $LASTEXITCODE" }
}

# 0. Requirements, read from the project
$props = Read-Properties (Join-Path $Project 'gradle.properties')
$wrapper = Read-Properties (Join-Path $Project 'gradle\wrapper\gradle-wrapper.properties')
$requiredJava = $props['java_version']
$gradleVersion = if ($wrapper['distributionUrl'] -match 'gradle-([\d.]+)-bin\.zip') { $Matches[1] } else { '?' }
if (-not $requiredJava) { throw 'java_version is missing from gradle.properties' }
# WHY: the daemon JDK (gradle-daemon-jvm.properties) and compile target must not drift apart
$daemonJava = (Read-Properties (Join-Path $Project 'gradle\gradle-daemon-jvm.properties'))['toolchainVersion']
if ($daemonJava -ne $requiredJava) {
    throw "gradle\gradle-daemon-jvm.properties toolchainVersion=$daemonJava but gradle.properties java_version=$requiredJava; make them equal."
}
Write-Host 'Requirements (from gradle.properties and the Gradle wrapper):'
Write-Host "  Java $requiredJava, Gradle $gradleVersion, Loom $($props['loom_version'])"
Write-Host "  Minecraft $($props['minecraft_version']), Fabric Loader $($props['loader_version']), Fabric API $($props['fabric_api_version'])"
Write-Host ''

# 1. JDK of the required version
$localFile = Join-Path $Project 'scripts\dev.local.bat'
$localJava = $null
if (Test-Path -LiteralPath $localFile) {
    $m = Select-String -LiteralPath $localFile -Pattern 'JAVA_HOME=([^"]+)' | Select-Object -First 1
    if ($m) { $localJava = $m.Matches[0].Groups[1].Value.Trim() }
}
$candidates = @($JavaHome, $env:TF_JAVA_HOME, $localJava, $env:JAVA_HOME) | Where-Object { $_ }
foreach ($root in @($env:ProgramFiles, (Join-Path $env:LOCALAPPDATA 'Programs'))) {
    if (-not $root) { continue }
    $candidates += @(Get-ChildItem -Path (Join-Path $root "*\jdk-$requiredJava*") -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName })
}
$jdk = $null
foreach ($c in $candidates) {
    $ver = Get-JavaVersionLine $c
    if (-not $ver) { continue }
    if ($ver -match "`"$requiredJava(\.|`")") { $jdk = $c; break }
    Write-Warning "Skipping $c (not Java $requiredJava): $ver"
}
if (-not $jdk) {
    throw "No JDK $requiredJava found. Install one (for example Microsoft Build of OpenJDK $requiredJava) and pass -JavaHome <path>."
}
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
Write-Host "JDK $requiredJava : $jdk"

# 2. Remember the JDK for scripts\dev.bat
if (-not (Test-Path -LiteralPath $localFile)) {
    Set-Content -LiteralPath $localFile -Encoding ASCII -Value @('@rem Local machine settings (git-ignored).', "set `"JAVA_HOME=$jdk`"")
    Write-Host "Saved    : $localFile"
}
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Write-Warning 'git not found on PATH (only needed to commit).' }

# 3. Gradle, build, tests
Write-Host "Gradle   : fetching $gradleVersion through the wrapper (checksum verified)"
Invoke-Gradle @('--version', '--console=plain')
if (-not $NoBuild) {
    Write-Host 'Building: compile, unit tests, server GameTests (first run downloads Minecraft)...'
    Invoke-Gradle @('build', '--console=plain')
}
if ($ClientTest) {
    Write-Host 'Client game test: a game window opens, boots to title, loads a world...'
    Invoke-Gradle @(':tconstruct:runClientGameTest', '--console=plain')
    Write-Host "Screenshots: $(Join-Path $Project 'tconstruct\build\run\clientGameTest\screenshots')"
}

# 4. Summary
Write-Host ''
Write-Host 'Environment restored.'
foreach ($mod in @('mantle', 'tconstruct')) {
    Get-ChildItem -Path (Join-Path $Project "$mod\build\libs") -Filter "$mod-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*-sources.jar' } | ForEach-Object { Write-Host "Jar      : $($_.FullName)" }
}
Write-Host ''
Write-Host 'Next: scripts\dev.bat help   (build, test, client-test, client, server)'
