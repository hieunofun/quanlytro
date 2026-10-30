param([int]$Port=8091,[switch]$SkipBuild,[switch]$Restart,[string]$AdminPassword='ReviewAdmin49#2026',[string]$PostgresBin=$env:POSTGRES_BIN,[string]$JavaHome=$env:JAVA_HOME,[int]$DatabasePort=55449)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$reviewRoot=Join-Path $projectRoot '.runtime/implementation-49'
New-Item -ItemType Directory -Force $reviewRoot | Out-Null
$pg=$PostgresBin
if (!$pg) { $pg=@((Join-Path $projectRoot '.runtime/tools/postgresql-18.6/pgsql/bin'),'C:/Program Files/PostgreSQL/18/bin','C:/Program Files/PostgreSQL/16/bin') | Where-Object { Test-Path (Join-Path $_ 'pg_ctl.exe') } | Select-Object -First 1 }
if (!$pg -or !(Test-Path "$pg\pg_ctl.exe")) { throw 'PostgreSQL binaries required; set POSTGRES_BIN or pass -PostgresBin.' }
$pgData=Join-Path $reviewRoot 'pgdata'
if (!(Test-Path (Join-Path $pgData 'PG_VERSION'))) {
    & "$pg\initdb.exe" -D $pgData -U postgres -A trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'Review PostgreSQL initialization failed.' }
}
& "$pg\pg_ctl.exe" -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pg\pg_ctl.exe" -D $pgData -l (Join-Path $reviewRoot 'postgres.log') -o "-p $DatabasePort -h 127.0.0.1" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Review PostgreSQL start failed.' }
}
$exists=& "$pg\psql.exe" -X -h 127.0.0.1 -p $DatabasePort -U postgres -d postgres -Atc "select 1 from pg_database where datname='hostel_review49'"
if ($LASTEXITCODE -ne 0) { throw "Cannot connect to isolated review PostgreSQL on $DatabasePort." }
if (!$exists) { & "$pg\createdb.exe" -h 127.0.0.1 -p $DatabasePort -U postgres hostel_review49; if ($LASTEXITCODE -ne 0) { throw 'Review database creation failed.' } }
$javaRoot=$JavaHome
if (!$javaRoot) { $javaRoot=Get-ChildItem (Join-Path $projectRoot '.runtime/tools') -Directory -Filter 'jdk-21*' | Select-Object -First 1 -ExpandProperty FullName }
if (!$javaRoot -or !(Test-Path "$javaRoot\bin\java.exe")) { throw 'Java 21 required; set JAVA_HOME.' }
$env:JAVA_HOME=$javaRoot
Push-Location $projectRoot
try {
    if (!$SkipBuild) {
        Push-Location frontend
        try { if (!(Test-Path node_modules)) { npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' } }; npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' } } finally { Pop-Location }
        $nativeErrorPreference=$ErrorActionPreference
        try { $ErrorActionPreference='Continue'; .\mvnw.cmd -q -DskipTests package } finally { $ErrorActionPreference=$nativeErrorPreference }
        if ($LASTEXITCODE -ne 0) { throw 'Backend package failed.' }
    }
    $jar=Join-Path $reviewRoot 'model49-review.jar'
    $pidFile=Join-Path $reviewRoot 'review-app.pid'
    if ($Restart -and (Test-Path $pidFile)) {
        $reviewPid=[int](Get-Content $pidFile)
        $running=Get-CimInstance Win32_Process -Filter "ProcessId=$reviewPid"
        if ($running) { if (!$running.CommandLine.Contains($jar)) { throw 'PID belongs to another process; refusing to stop it.' }; Stop-Process -Id $reviewPid; Start-Sleep -Seconds 1 }
    }
    try { $ready=(Invoke-RestMethod "http://127.0.0.1:$Port/api/health").schemaVersion -eq 49 } catch { $ready=$false }
    if (!$ready) {
        Copy-Item -LiteralPath (Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar') -Destination $jar -Force
        $env:DB_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/hostel_review49";$env:DB_USERNAME='postgres';$env:DB_PASSWORD='postgres'
        $env:ADMIN_PASSWORD=$AdminPassword;$env:SPRING_PROFILES_ACTIVE='dev'
        $env:JWT_SECRET=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
        $env:UPLOAD_DIRECTORY=Join-Path $reviewRoot 'review-uploads'
        $process=Start-Process -FilePath "$javaRoot\bin\java.exe" -ArgumentList @('-jar',('"'+$jar+'"'),"--server.port=$Port",'--server.address=127.0.0.1','--logging.level.org.springframework.jdbc.core.JdbcTemplate=WARN','--logging.level.com.hostel=INFO') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $reviewRoot 'review-app.log') -RedirectStandardError (Join-Path $reviewRoot 'review-app-error.log') -PassThru
        $process.Id | Set-Content $pidFile
        for($attempt=0;$attempt -lt 50;$attempt++) { Start-Sleep -Seconds 1;try { $ready=(Invoke-RestMethod "http://127.0.0.1:$Port/api/health").schemaVersion -eq 49;if($ready){break} } catch {};if($process.HasExited){throw 'Review app exited; inspect .runtime/implementation-49/review-app.log.'} }
        if (!$ready) { throw 'Review startup timed out.' }
    }
    Write-Output "Review V13: http://127.0.0.1:$Port | isolated DB $DatabasePort/hostel_review49 | admin account"
} finally { Pop-Location }
