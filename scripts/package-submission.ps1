$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$submission=Join-Path $projectRoot 'submission'
$stage=Join-Path $projectRoot ('.runtime/submission-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $submission,$stage | Out-Null
$releaseRoot=Join-Path $stage 'hostel-management-v1'
New-Item -ItemType Directory -Force $releaseRoot | Out-Null
foreach($entry in @('src','docs','scripts','.mvn','pom.xml','mvnw','mvnw.cmd','README.md','Dockerfile','compose.yml','.dockerignore','.gitignore','.env.example')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $entry) -Destination $releaseRoot -Recurse -Force
}
$frontend=Join-Path $releaseRoot 'frontend'
New-Item -ItemType Directory -Force $frontend | Out-Null
foreach($entry in @('src','public','tests','package.json','package-lock.json','index.html','vite.config.js')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "frontend/$entry") -Destination $frontend -Recurse -Force
}
New-Item -ItemType Directory -Force (Join-Path $releaseRoot 'target') | Out-Null
$builtJar=Join-Path $projectRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar'
Copy-Item -LiteralPath $builtJar -Destination (Join-Path $releaseRoot 'target/hostel-management-0.0.1-SNAPSHOT.jar') -Force
Copy-Item -LiteralPath $builtJar -Destination (Join-Path $submission 'hostel-management-v1.jar') -Force
Compress-Archive -LiteralPath $releaseRoot -DestinationPath (Join-Path $submission 'hostel-management-v1.zip') -Force
foreach($entry in @('bao-cao-do-an.docx','slide-bao-ve.pptx','demo-invoice.pdf')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "docs/$entry") -Destination $submission -Force
}
Get-ChildItem $submission -File | Where-Object Name -ne 'SHA256SUMS.txt' | Get-FileHash -Algorithm SHA256 | ForEach-Object { "$($_.Hash.ToLower())  $([IO.Path]::GetFileName($_.Path))" } | Set-Content -Encoding ascii (Join-Path $submission 'SHA256SUMS.txt')
Get-ChildItem $submission -File | Select-Object Name,Length
