param([string]$BaseUrl='http://localhost:8080',[string]$AdminPassword='DemoAdmin#2026')
$ErrorActionPreference='Stop'
$auth=Invoke-RestMethod "$BaseUrl/api/auth/login" -Method Post -ContentType 'application/json' -Body (@{username='admin';password=$AdminPassword}|ConvertTo-Json)
$headers=@{Authorization="Bearer $($auth.accessToken)"}
function Request([string]$Path,[hashtable]$Body,[string]$Method='POST') {
    Invoke-RestMethod -Method $Method -Uri "$BaseUrl/api$Path" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 10)))
}
$c=(Invoke-RestMethod "$BaseUrl/api/contracts" -Headers $headers) | Where-Object contract_code -eq 'HD-DEMO-203'
if (!$c) {throw 'Run seed-demo.ps1 first.'}
$existing=(Invoke-RestMethod "$BaseUrl/api/invoices" -Headers $headers) | Where-Object { $_.contract_id -eq $c.id -and $_.billing_period -eq '2026-09-01' }
if (!$existing) {
    $m=(Invoke-RestMethod "$BaseUrl/api/meter-readings" -Headers $headers) | Where-Object { $_.contract_id -eq $c.id -and $_.billing_period -eq '2026-09-01' -and $_.meter_type -eq 'WATER' }
    if ([decimal]$m.new_index -ne 155) { $null=Request "/meter-readings/$($m.id)" @{newIndex=155;reason='Kiểm tra đồng hồ, sửa chỉ số nhập nhầm'} 'PUT' }
    $draft=Request '/invoices' @{contractId=$c.id;period='2026-09';issueDate='2026-10-01';dueDate='2026-10-05'}
    $existing=Request "/invoices/$($draft.id)/issue" @{}
}
if ($existing.status -eq 'DRAFT') { $existing=Request "/invoices/$($existing.id)/issue" @{} }
if ([decimal]$existing.total_amount -ne 3690000) {throw "Unexpected invoice total: $($existing.total_amount)"}
if ([decimal]$existing.paid_amount -eq 0) { $existing=Request "/invoices/$($existing.id)/payments" @{code='DEMO-PAY-203';amount=2000000;method='BANK_TRANSFER_MANUAL';notes='Thanh toán một phần trong demo'} }
if ([decimal]$existing.balance_due -ne 1690000) {throw "Unexpected outstanding balance: $($existing.balance_due)"}
$pdf=Join-Path (Split-Path $PSScriptRoot -Parent) '.runtime/demo-invoice-mvp.pdf'
Invoke-WebRequest "$BaseUrl/api/invoices/$($existing.id)/pdf" -Headers $headers -OutFile $pdf
Write-Output 'PASS: corrected water = 8 m3; invoice = 3,690,000; payment = 2,000,000; balance = 1,690,000; PDF exported.'
