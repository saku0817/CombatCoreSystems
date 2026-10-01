$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$env:JAVA_HOME='C:/Program Files/Java/jdk-25.0.4'
$stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
$backup=Join-Path $projectRoot "backups/v1.4.6/candidate-$stamp"
New-Item -ItemType Directory -Path $backup | Out-Null
Compress-Archive -LiteralPath @((Join-Path $projectRoot 'src'),(Join-Path $projectRoot 'pom.xml'),(Join-Path $projectRoot 'scripts'),(Join-Path $projectRoot 'docs')) -DestinationPath (Join-Path $backup 'source.zip')
& 'C:/Users/user/Downloads/apache-maven-3.9.11-bin/apache-maven-3.9.11/bin/mvn.cmd' -o '-Dmaven.repo.local=.m2/repository' '-DskipTests' package -q
if($LASTEXITCODE -ne 0){throw 'Candidate package failed'}
$jar=Join-Path $projectRoot 'target/CombatCoreSystems-1.4.6.jar'
Copy-Item -LiteralPath $jar -Destination $backup
$plugins=Join-Path $projectRoot 'server/plugins-v1.4.6-smoke'
$classes=Join-Path $projectRoot 'server/v146-smoke-classes'
New-Item -ItemType Directory -Force -Path $plugins,$classes | Out-Null
Copy-Item -LiteralPath $jar -Destination $plugins -Force
$dependencies=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.m2/repository') -Recurse -Filter '*.jar' | Where-Object FullName -NotMatch '26.2.build.119'
$classpath=(@((Join-Path $projectRoot 'target/classes'))+@($dependencies.FullName)) -join ';'
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -cp $classpath -d $classes scripts/v146-smoke/V146Smoke.java
if($LASTEXITCODE -ne 0){throw 'Smoke fixture compile failed'}
Copy-Item -LiteralPath scripts/v146-smoke/plugin.yml -Destination $classes -Force
& "$env:JAVA_HOME/bin/jar.exe" --create --file (Join-Path $plugins 'CCSV146Smoke.jar') -C $classes .
if($LASTEXITCODE -ne 0){throw 'Smoke fixture package failed'}
Copy-Item -LiteralPath (Join-Path $plugins 'CCSV146Smoke.jar') -Destination $backup
Write-Output "Candidate backed up: $backup"
Write-Output "Isolated plugins: $plugins"
