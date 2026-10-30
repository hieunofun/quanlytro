param(
    [string]$PostgresBin = $env:POSTGRES_BIN,
    [string]$JavaHome = $env:JAVA_HOME,
    [int]$DatabasePort = 55450,
    [string]$Tests = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$JavaHome) {
    $JavaHome = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.runtime/tools') |
        Where-Object { $_.PSIsContainer -and $_.Name -like 'jdk-21*' } | Select-Object -First 1 -ExpandProperty FullName
}
if (!$PostgresBin) {
    $candidates = @(
        (Join-Path $projectRoot '.runtime/tools/postgresql-18.6/pgsql/bin'),
        'C:/Program Files/PostgreSQL/18/bin', 'C:/Program Files/PostgreSQL/16/bin'
    )
    $PostgresBin = $candidates | Where-Object { Test-Path (Join-Path $_ 'pg_ctl.exe') } | Select-Object -First 1
}
if (!$JavaHome -or !(Test-Path (Join-Path $JavaHome 'bin/java.exe'))) { throw 'Java 21 is required; pass -JavaHome.' }
if (!$PostgresBin -or !(Test-Path (Join-Path $PostgresBin 'initdb.exe'))) { throw 'PostgreSQL binaries required; pass -PostgresBin.' }

$workRoot = Join-Path $projectRoot '.runtime/backend-business-tests'
$pgData = Join-Path $workRoot 'pgdata'
New-Item -ItemType Directory -Force -Path $workRoot | Out-Null
if (!(Test-Path (Join-Path $pgData 'PG_VERSION'))) {
    & (Join-Path $PostgresBin 'initdb.exe') -D $pgData -U postgres -A trust --encoding=UTF8 --locale=C > (Join-Path $workRoot 'initdb.log') 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Cannot initialize isolated PostgreSQL; see initdb.log.' }
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $pgData status *> $null
if ($LASTEXITCODE -ne 0) {
    & (Join-Path $PostgresBin 'pg_isready.exe') -h 127.0.0.1 -p $DatabasePort *> $null
    if ($LASTEXITCODE -eq 0) { throw "Port $DatabasePort is in use. Choose a different -DatabasePort for the isolated test cluster." }
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $pgData -l (Join-Path $workRoot 'postgres.log') -o "-p $DatabasePort -h 127.0.0.1" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Cannot start isolated PostgreSQL.' }
}
$connectedDirectory = & (Join-Path $PostgresBin 'psql.exe') -X -h 127.0.0.1 -p $DatabasePort -U postgres -d postgres -Atc 'show data_directory'
if ($LASTEXITCODE -ne 0 -or !$connectedDirectory) { throw 'Cannot verify the isolated PostgreSQL data directory.' }
$actualDirectory = [IO.Path]::GetFullPath($connectedDirectory.Trim()).TrimEnd('\','/')
$expectedDirectory = [IO.Path]::GetFullPath($pgData).TrimEnd('\','/')
if (![String]::Equals($actualDirectory,$expectedDirectory,[StringComparison]::OrdinalIgnoreCase)) {
    throw "Port $DatabasePort points to another PostgreSQL cluster. Pass the port of $expectedDirectory; no test database was created."
}
$runId = [Guid]::NewGuid().ToString('N').Substring(0,10)
$databases = @("hostel_biz17_$runId", "hostel_biz49_$runId", "hostel_bizrules_$runId")
foreach ($database in $databases) {
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $DatabasePort -U postgres $database
    if ($LASTEXITCODE -ne 0) { throw "Cannot create isolated test database $database." }
}
$original = @{}
$settings = @{
    JAVA_HOME=$JavaHome; DB_USERNAME='postgres'; DB_PASSWORD='postgres';
    DB_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/$($databases[0])";
    DB49_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/$($databases[1])";
    OPENKOS_DB_URL="jdbc:postgresql://127.0.0.1:$DatabasePort/$($databases[2])"
}
foreach ($key in $settings.Keys) { $original[$key]=[Environment]::GetEnvironmentVariable($key,'Process'); [Environment]::SetEnvironmentVariable($key,$settings[$key],'Process') }
Push-Location $projectRoot
try {
    $mavenArgs = @('-B','-ntp')
    if ($Tests) { $mavenArgs += "-Dtest=$Tests" }
    $mavenArgs += 'verify'
    $logPath = Join-Path $workRoot "verify-$runId.log"
    # Windows PowerShell treats Java warnings on stderr as NativeCommandError.
    # Maven's exit code, rather than a JVM warning, decides whether verification failed.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & .\mvnw.cmd @mavenArgs > $logPath 2>&1
        $testExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    Get-Content -LiteralPath $logPath -Tail 22
    Write-Output "Log: $logPath"
    Write-Output "Test databases retained for inspection: $($databases -join ', ') on 127.0.0.1:$DatabasePort"
    if ($testExit -ne 0) { throw "Backend verification failed (exit $testExit). See the log." }
} finally {
    Pop-Location
    foreach ($key in $settings.Keys) { [Environment]::SetEnvironmentVariable($key,$original[$key],'Process') }
}
