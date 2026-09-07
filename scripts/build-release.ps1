param(
    [string]$Maven = "C:\Users\user\Downloads\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd",
    [string]$JavaHome = "C:\Program Files\Java\jdk-25.0.4"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$pomPath = Join-Path $projectRoot "pom.xml"
[xml]$pom = Get-Content -LiteralPath $pomPath -Raw
$version = $pom.project.version
if (Test-Path -LiteralPath $JavaHome) {
    $env:JAVA_HOME = $JavaHome
}

if (-not (Test-Path -LiteralPath $Maven)) {
    $Maven = "mvn"
}

Push-Location $projectRoot
try {
    $localRepository = Join-Path $projectRoot ".m2\repository"
    & $Maven "-Dmaven.repo.local=$localRepository" clean verify
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE" }

    $jarName = "CombatCoreSystems-$version.jar"
    $sourceJar = Join-Path $projectRoot "target\$jarName"
    if (-not (Test-Path -LiteralPath $sourceJar)) { throw "Built JAR was not found: $sourceJar" }

    $releaseDir = Join-Path $projectRoot "releases\v$version"
    $backupDir = Join-Path $projectRoot "backups\v$version"
    New-Item -ItemType Directory -Force -Path $releaseDir, $backupDir | Out-Null

    Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $releaseDir $jarName) -Force
    $stamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
    Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $backupDir "CombatCoreSystems-$version-$stamp.jar")

    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceJar).Hash.ToLowerInvariant()
    Set-Content -LiteralPath (Join-Path $releaseDir "$jarName.sha256") -Value "$hash  $jarName" -Encoding utf8
    Write-Host "Release: $releaseDir\$jarName"
    Write-Host "Backup : $backupDir"
    Write-Host "SHA-256: $hash"
}
finally {
    Pop-Location
}
