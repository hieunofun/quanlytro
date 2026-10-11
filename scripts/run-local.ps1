param([int]$Port=8080,[string]$AdminPassword='DemoAdmin#2026',[switch]$NoSeed)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$runtime=Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null
$javaHomeCandidate=Get-ChildItem 'D:\tools' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if ($javaHomeCandidate) { $env:JAVA_HOME=$javaHomeCandidate }
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
if (!(Test-Path $java)) { throw 'Java 21 is required; set JAVA_HOME.' }
$pg='D:\tools\pgsql\bin'
if (!(Test-Path "$pg\pg_ctl.exe")) { throw 'PostgreSQL binaries not found. Use Docker or configure PostgreSQL manually per README.' }
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
$env:DB_USERNAME='postgres';$env:DB_PASSWORD='postgres';$env:ADMIN_PASSWORD=$AdminPassword
$env:SPRING_PROFILES_ACTIVE='dev'
$jar=Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Build the JAR with scripts/build.ps1 first.' }
try { $ready=(Invoke-RestMethod "http://localhost:$Port/api/health").status -eq 'UP' } catch { $ready=$false }
if (!$ready) {
    $runtimeJar=Join-Path $runtime 'app.jar'
    Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force
    $env:JWT_SECRET=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
    $process=Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$runtimeJar+'"'),"--server.port=$Port") -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtime 'app.log') -RedirectStandardError (Join-Path $runtime 'app-error.log') -PassThru
    $process.Id | Set-Content (Join-Path $runtime 'app.pid')
    for($attempt=0;$attempt -lt 60;$attempt++) {
        Start-Sleep -Seconds 1
        try { $ready=(Invoke-RestMethod "http://localhost:$Port/api/health").status -eq 'UP'; if($ready){break} } catch {}
        if($process.HasExited){throw 'Application exited; check .runtime/app.log and app-error.log.'}
    }
    if (!$ready) { throw 'Startup timed out; inspect .runtime/app.log.' }
}
if (!$NoSeed) { & (Join-Path $PSScriptRoot 'seed-demo.ps1') -BaseUrl "http://localhost:$Port" -AdminPassword $AdminPassword }
Write-Output "Ready: http://localhost:$Port"
