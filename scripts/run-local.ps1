param(
    [int]$Port=8080,
    [string]$AdminPassword='DemoAdmin#2026',
    [switch]$NoSeed,
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$PostgresBin=$env:POSTGRES_BIN,
    [int]$DatabasePort=55434
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$runtime=Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Force $runtime | Out-Null
if (!$JavaHome) {
    $JavaHome=Get-ChildItem (Join-Path $runtime 'tools'),'C:\Program Files\Eclipse Adoptium','C:\Program Files\Java','D:\tools' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if (!$JavaHome -or !(Test-Path (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Java 21 is required; set JAVA_HOME or pass -JavaHome.' }
$env:JAVA_HOME=$JavaHome
$java=Join-Path $JavaHome 'bin/java.exe'
if (!$PostgresBin) {
    $pgCandidates=@('D:\tools\pgsql\bin')
    $pgCandidates+=Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue | Sort-Object { [int]$_.Name } -Descending | ForEach-Object { Join-Path $_.FullName 'bin' }
    $PostgresBin=$pgCandidates | Where-Object { Test-Path -LiteralPath ($_+'\pg_ctl.exe') -ErrorAction SilentlyContinue } | Select-Object -First 1
}
if (!$PostgresBin -or !(Test-Path (Join-Path $PostgresBin 'pg_ctl.exe'))) { throw 'PostgreSQL binaries not found; set POSTGRES_BIN or pass -PostgresBin.' }
$pg=$PostgresBin
$pgData=Join-Path $runtime 'pgdata'
if (!(Test-Path (Join-Path $pgData 'PG_VERSION'))) {
    & "$pg\initdb.exe" -D $pgData -U postgres -A trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL init failed' }
}
& "$pg\pg_ctl.exe" -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pg\pg_ctl.exe" -D $pgData -l (Join-Path $runtime 'postgres.log') -o "-p $DatabasePort -h 127.0.0.1" -w start
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed' }
}
$exists=& "$pg\psql.exe" -h 127.0.0.1 -p $DatabasePort -U postgres -d postgres -tAc "select 1 from pg_database where datname='hostel_demo'"
if ($LASTEXITCODE -ne 0) { throw 'Unable to connect to the local demo PostgreSQL server.' }
if (!$exists) {
    & "$pg\createdb.exe" -h 127.0.0.1 -p $DatabasePort -U postgres hostel_demo
    if ($LASTEXITCODE -ne 0) { throw 'Demo database creation failed.' }
}
$env:DB_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/hostel_demo"
$env:DB_USERNAME='postgres';$env:DB_PASSWORD='postgres';$env:ADMIN_PASSWORD=$AdminPassword
$env:SPRING_PROFILES_ACTIVE='dev'
$jar=Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Build the JAR with scripts/build.ps1 first.' }
try { $ready=(Invoke-RestMethod "http://localhost:$Port/api/health").status -eq 'UP' } catch { $ready=$false }
if (!$ready) {
    $runtimeJar=Join-Path $runtime 'app.jar'
    Copy-Item -LiteralPath $jar -Destination $runtimeJar -Force
    $env:JWT_SECRET=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
    $process=Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$runtimeJar+'"'),"--server.port=$Port",'--server.address=127.0.0.1') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtime 'app.log') -RedirectStandardError (Join-Path $runtime 'app-error.log') -PassThru
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
