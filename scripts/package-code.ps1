$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$stage=Join-Path $projectRoot ('.runtime/code-submission-'+[Guid]::NewGuid().ToString('N'))
$releaseRoot=Join-Path $stage 'hostel-management-v1'
$submission=Join-Path $projectRoot 'submission'
New-Item -ItemType Directory -Force $releaseRoot,$submission | Out-Null
foreach($entry in @('src','scripts','.mvn','pom.xml','mvnw','mvnw.cmd','README.md','Dockerfile','compose.yml','.dockerignore','.gitignore','.env.example')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $entry) -Destination $releaseRoot -Recurse -Force
}
$frontend=Join-Path $releaseRoot 'frontend'
New-Item -ItemType Directory -Force $frontend | Out-Null
foreach($entry in @('src','public','tests','package.json','package-lock.json','index.html','vite.config.js')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "frontend/$entry") -Destination $frontend -Recurse -Force
}
$docs=Join-Path $releaseRoot 'docs'
New-Item -ItemType Directory -Force $docs | Out-Null
foreach($entry in @('API.md','DEMO.md','V1-COVERAGE.md','VALIDATION.md','CODE-REVIEW.md','REPORT.md','test-results','screenshots','diagrams')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "docs/$entry") -Destination $docs -Recurse -Force
}
New-Item -ItemType Directory -Force (Join-Path $releaseRoot 'target') | Out-Null
$jar=Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar'
Copy-Item -LiteralPath $jar -Destination (Join-Path $releaseRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar') -Force
Copy-Item -LiteralPath $jar -Destination (Join-Path $submission 'hostel-management-v1.jar') -Force
Compress-Archive -LiteralPath $releaseRoot -DestinationPath (Join-Path $submission 'hostel-management-code-v1.zip') -Force
Get-FileHash -Algorithm SHA256 (Join-Path $submission 'hostel-management-code-v1.zip'),(Join-Path $submission 'hostel-management-v1.jar') | ForEach-Object { "$($_.Hash.ToLower())  $([IO.Path]::GetFileName($_.Path))" } | Set-Content -Encoding ascii (Join-Path $submission 'CODE-SHA256SUMS.txt')
Get-Item (Join-Path $submission 'hostel-management-code-v1.zip'),(Join-Path $submission 'hostel-management-v1.jar') | Select-Object Name,Length
