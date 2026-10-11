param([int]$Port=8080, [string]$AdminPassword='DemoAdmin#2026')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$runtime=Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null

$javaHomeCandidate=Get-ChildItem 'D:\tools' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if ($javaHomeCandidate) { $env:JAVA_HOME=$javaHomeCandidate }
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
if (!(Test-Path $java)) { throw 'Java 21 is required; set JAVA_HOME.' }

$pg='D:\tools\pgsql\bin'
if (!(Test-Path "$pg\pg_ctl.exe")) { throw 'PostgreSQL binaries not found in D:\tools\pgsql\bin' }
$pgData=Join-Path $runtime 'pgdata'
if (!(Test-Path (Join-Path $pgData 'PG_VERSION'))) {
    & "$pg\initdb.exe" -D $pgData -U postgres -A trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL init failed' }
}
& "$pg\pg_ctl.exe" -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pg\pg_ctl.exe" -D $pgData -l (Join-Path $runtime 'postgres.log') -o '-p 55432 -h 127.0.0.1' -w start
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed' }
}

$exists=& "$pg\psql.exe" -h 127.0.0.1 -p 55432 -U postgres -d postgres -tAc "select 1 from pg_database where datname='hostel_demo'"
if (!$exists) { & "$pg\createdb.exe" -h 127.0.0.1 -p 55432 -U postgres hostel_demo }

$env:DB_URL='jdbc:postgresql://127.0.0.1:55432/hostel_demo'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='postgres'
$env:ADMIN_PASSWORD=$AdminPassword
$env:SPRING_PROFILES_ACTIVE='dev'
$env:JWT_SECRET=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')

$jar=Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Build the JAR with scripts/build.ps1 first.' }
$runtimeJar=Join-Path $runtime 'app.jar'
Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force

# Auto-seed when ready in background
Start-Job -ScriptBlock {
    param($root, $p, $pwd)
    for ($i=0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 1
        try {
            if ((Invoke-RestMethod "http://localhost:$p/api/health").status -eq 'UP') {
                & (Join-Path $root 'scripts\seed-demo.ps1') -BaseUrl "http://localhost:$p" -AdminPassword $pwd
                break
            }
        } catch {}
    }
} -ArgumentList $projectRoot, $Port, $AdminPassword | Out-Null

Write-Host "Hostel Management server starting on port $Port..."
& $java -jar $runtimeJar "--server.port=$Port"
