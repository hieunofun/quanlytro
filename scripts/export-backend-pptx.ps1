param([string]$InputFile='docs/backend-review/backend-he-thong-nha-tro.pptx')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$slidePath=[IO.Path]::GetFullPath((Join-Path $projectRoot $InputFile))
$exportRoot=Join-Path $projectRoot ('.runtime/backend-review-pptx-'+[Guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Force -Path $exportRoot | Out-Null
$slideApp=$null;$slideDeck=$null;$initialDeckCount=0
try {
    $slideApp=New-Object -ComObject PowerPoint.Application
    $initialDeckCount=$slideApp.Presentations.Count
    $slideDeck=$slideApp.Presentations.Open($slidePath,$true,$false,$false)
    $slideDeck.Export($exportRoot,'PNG',1280,720)
    $slideDeck.SaveAs((Join-Path $exportRoot 'powerpoint-export.pdf'),32)
    Copy-Item -LiteralPath (Join-Path $exportRoot 'powerpoint-export.pdf') -Destination ([IO.Path]::ChangeExtension($slidePath,'.pdf')) -Force
    [pscustomobject]@{slides=$slideDeck.Slides.Count;pdf=[IO.Path]::ChangeExtension($slidePath,'.pdf');previews=$exportRoot} | ConvertTo-Json
} finally {
    if($slideDeck) { $slideDeck.Close() }
    if($slideApp -and $initialDeckCount -eq 0 -and $slideApp.Presentations.Count -eq 0) { $slideApp.Quit() }
    if($slideDeck) { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($slideDeck) | Out-Null }
    if($slideApp) { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($slideApp) | Out-Null }
}
