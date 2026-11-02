<#
.SYNOPSIS
  Removes what this project's build and dev runs put on the machine, after saving the built jars to dist\.

.DESCRIPTION
  Version-aware: versions come from gradle.properties and gradle\wrapper\gradle-wrapper.properties.
  Default scope (safe for your other Fabric projects):
    project : build\, .gradle\, mantle\build\, tconstruct\build\, mantle\run\, tconstruct\run\
    global  : Loom's Minecraft <minecraft_version> files only:
              <gradle home>\caches\fabric-loom\<minecraft_version>
              <gradle home>\caches\fabric-loom\minecraftMaven\net\minecraft\*\<minecraft_version>
  -Deep also removes caches shared with every Gradle/Fabric project on this machine:
              <gradle home>\caches\fabric-loom\assets, <gradle home>\wrapper\dists\gradle-<gradle version>-*,
              <gradle home>\caches\modules-2 (they re-download when needed).
  Never touches: sources, .git, dist\, scripts\dev.local.*, your Minecraft launcher, JDKs.

.PARAMETER DryRun     Show what would be deleted, delete nothing.
.PARAMETER Yes        Do not ask for confirmation.
.PARAMETER Deep       Also remove the shared caches listed above.
.PARAMETER NoKeepJar  Do not copy the built jars to dist\ first.

.EXAMPLE
  scripts\env\clean_env.bat -DryRun
#>
[CmdletBinding()]
param(
    [switch]$DryRun,
    [switch]$Yes,
    [switch]$Deep,
    [switch]$NoKeepJar
)
$ErrorActionPreference = 'Stop'

$Project = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$GradleHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }

# Reads key=value lines from a .properties file.
function Read-Properties([string]$Path) {
    $map = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([^#=\s][^=]*?)\s*=\s*(.*)$') { $map[$Matches[1]] = $Matches[2].Trim() }
    }
    return $map
}

function Get-SizeMB([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return 0 }
    $sum = (Get-ChildItem -LiteralPath $Path -Recurse -Force -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($null -eq $sum) { return 0 }
    return [math]::Round($sum / 1MB, 1)
}

$props = Read-Properties (Join-Path $Project 'gradle.properties')
$mcVersion = $props['minecraft_version']
$wrapper = Read-Properties (Join-Path $Project 'gradle\wrapper\gradle-wrapper.properties')
$gradleVersion = if ($wrapper['distributionUrl'] -match 'gradle-([\d.]+)-bin\.zip') { $Matches[1] } else { $null }
if (-not $mcVersion -or -not $gradleVersion) { throw 'Could not read minecraft_version or the Gradle version from the project files.' }

$loom = Join-Path $GradleHome 'caches\fabric-loom'
$targets = @(
    @{ Label = 'root build output';               Path = Join-Path $Project 'build' },
    @{ Label = 'project Gradle/Loom cache';       Path = Join-Path $Project '.gradle' },
    @{ Label = 'mantle build output';             Path = Join-Path $Project 'mantle\build' },
    @{ Label = 'tconstruct build output (incl. test runs)'; Path = Join-Path $Project 'tconstruct\build' },
    @{ Label = 'mantle dev runs and worlds';      Path = Join-Path $Project 'mantle\run' },
    @{ Label = 'tconstruct dev runs and worlds';  Path = Join-Path $Project 'tconstruct\run' },
    @{ Label = "Loom Minecraft $mcVersion files"; Path = Join-Path $loom $mcVersion }
)
foreach ($dir in Get-ChildItem -Path (Join-Path $loom 'minecraftMaven\net\minecraft') -Directory -ErrorAction SilentlyContinue) {
    $targets += @{ Label = "Loom Minecraft $mcVersion jars ($($dir.Name))"; Path = Join-Path $dir.FullName $mcVersion }
}
if ($Deep) {
    $targets += @{ Label = 'Loom assets (shared by all MC versions)'; Path = Join-Path $loom 'assets' }
    foreach ($dist in Get-ChildItem -Path (Join-Path $GradleHome 'wrapper\dists') -Directory -Filter "gradle-$gradleVersion-*" -ErrorAction SilentlyContinue) {
        $targets += @{ Label = "Gradle $gradleVersion distribution (shared)"; Path = $dist.FullName }
    }
    $targets += @{ Label = 'Gradle dependency cache (shared)'; Path = Join-Path $GradleHome 'caches\modules-2' }
}

$present = @($targets | Where-Object { Test-Path -LiteralPath $_.Path })
Write-Host "Project   : $Project"
Write-Host "Gradle    : $GradleHome (Gradle $gradleVersion)"
Write-Host "Minecraft : $mcVersion"
Write-Host ''
if ($present.Count -eq 0) {
    Write-Host 'Nothing to clean: environment is already clean.'
    exit 0
}
$total = 0
foreach ($t in $present) {
    $mb = Get-SizeMB $t.Path
    $total += $mb
    Write-Host ('  {0,9} MB  {1}  [{2}]' -f $mb, $t.Path, $t.Label)
}
Write-Host ('  {0,9} MB  total' -f [math]::Round($total, 1))
Write-Host ''

if ($DryRun) {
    Write-Host 'Dry run: nothing was deleted.'
    exit 0
}
if (-not $Yes) {
    $answer = Read-Host 'Delete the items above? Type YES to continue'
    if ($answer -ne 'YES') { Write-Host 'Aborted.'; exit 1 }
}

# 1. Keep the built jars (build folders are about to disappear).
if (-not $NoKeepJar) {
    $jars = @('mantle', 'tconstruct') | ForEach-Object {
        Get-ChildItem -Path (Join-Path $Project "$_\build\libs") -Filter "$_-*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*-sources.jar' }
    }
    $jars = @($jars)
    if ($jars.Count -gt 0) {
        $dist = Join-Path $Project 'dist'
        New-Item -ItemType Directory -Force -Path $dist | Out-Null
        foreach ($j in $jars) {
            Copy-Item -LiteralPath $j.FullName -Destination $dist -Force
            Write-Host "Saved jar: $(Join-Path $dist $j.Name)"
        }
    } else {
        Write-Host 'No built jars found (nothing saved to dist\).'
    }
}

# 2. Stop what holds files open: this project's Gradle daemons and dev game processes.
# WHY: 'gradlew --stop' needs a JDK; reuse the one the dev scripts use when JAVA_HOME is unset
if ($env:TF_JAVA_HOME) { $env:JAVA_HOME = $env:TF_JAVA_HOME }
$localFile = Join-Path $Project 'scripts\dev.local.bat'
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath $localFile)) {
    $m = Select-String -LiteralPath $localFile -Pattern 'JAVA_HOME=([^"]+)' | Select-Object -First 1
    if ($m) { $env:JAVA_HOME = $m.Matches[0].Groups[1].Value.Trim() }
}
$gradlew = Join-Path $Project 'gradlew.bat'
if ((Test-Path -LiteralPath $gradlew) -and $env:JAVA_HOME) {
    Write-Host 'Stopping Gradle daemons'
    $oldErr = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    & $gradlew --stop 2>&1 | Out-Null
    $ErrorActionPreference = $oldErr
}
# WHY: match the project path too, so dev games of other projects keep running
$projectName = Split-Path $Project -Leaf
Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'Knot(Client|Server)' -and $_.CommandLine -like "*$projectName*" } |
    ForEach-Object { Write-Host "Stopping dev game process $($_.ProcessId)"; Stop-Process -Id $_.ProcessId -Force }
Start-Sleep -Seconds 2

# 3. Delete.
$failed = @()
foreach ($t in $present) {
    try {
        Remove-Item -LiteralPath $t.Path -Recurse -Force -ErrorAction Stop
        Write-Host "Deleted $($t.Path)"
    } catch {
        $failed += $t.Path
        Write-Warning "Could not delete $($t.Path): $($_.Exception.Message)"
    }
}
Write-Host ''
if ($failed.Count -gt 0) {
    Write-Host 'Some items could not be deleted (files in use?). Close Minecraft, your IDE and Gradle, then run again.'
    exit 2
}
Write-Host 'Environment cleaned. To build again run: scripts\env\restore_env.bat'
