param([string]$BaseUrl='http://localhost:8080',[Parameter(Mandatory=$true)][string]$AdminPassword)
$ErrorActionPreference='Stop'
$login=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' -Body (@{username='admin';password=$AdminPassword}|ConvertTo-Json)
$headers=@{Authorization="Bearer $($login.accessToken)"}
function Request([string]$Path,[hashtable]$Body,[string]$Method='POST') {
    Invoke-RestMethod -Method $Method -Uri "$BaseUrl/api$Path" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 10)))
}
$properties=Invoke-RestMethod "$BaseUrl/api/properties" -Headers $headers
if ($properties | Where-Object property_code -eq 'DEMO-AN-PHU') { Write-Output 'Demo already exists. No data changed.'; return }
$p=Request '/properties' @{code='DEMO-AN-PHU';name='Nhà trọ An Phú';address='Thành phố Hồ Chí Minh'}
$floor=Request '/floors' @{propertyId=$p.id;number=2;name='Tầng 2'}
$room=Request '/rooms' @{propertyId=$p.id;floorId=$floor.id;code='203';price=3000000;maxOccupants=3;area=25}
$null=Request "/rooms/$($room.id)/details" @{status='AVAILABLE';amenities='Điều hòa, nóng lạnh, giường, tủ';internalNote='Phòng demo để bảo vệ đồ án';imageUrls=@()} 'PUT'
$null=Request '/rooms' @{propertyId=$p.id;floorId=$floor.id;code='204';price=2800000;maxOccupants=2;area=22}
$user=Request '/users' @{username='tenant_demo';password='DemoTenant#2026';fullName='Nguyễn Minh Anh';phone='0900000203';email='tenant@example.test';role='TENANT'}
$staff=Request '/users' @{username='staff_demo';password='DemoStaff#2026';fullName='Trần Hoàng Nam';phone='0900000002';email='staff@example.test';role='STAFF'}
$null=Request '/assignments' @{userId=$staff.id;propertyId=$p.id;active=$true}
$t=Request '/tenants' @{userId=$user.id;fullName='Nguyễn Minh Anh';identityNumber='079000000203';phone='0900000203';email='tenant@example.test'}
$c=Request '/contracts' @{code='HD-DEMO-203';roomId=$room.id;tenantId=$t.id;startDate='2026-06-01';endDate='2027-06-01';rentalPrice=3000000;deposit=3000000;paymentDay=5;electricIndex=100;waterIndex=120}
$services=Invoke-RestMethod "$BaseUrl/api/services" -Headers $headers
foreach($s in $services) {
    $price=switch($s.service_code){ELEC{3500} WATER{20000} WIFI{100000} TRASH{30000} PARKING{50000}}
    $null=Request '/prices' @{propertyId=$p.id;serviceId=$s.id;unitPrice=$price;effectiveFrom='2026-06-01'}
}
$null=Request '/thresholds' @{propertyId=$p.id;meterType='WATER';percentage=50;months=3}
foreach($entry in @(@('2026-06',128,'2026-06-30'),@('2026-07',137,'2026-07-31'),@('2026-08',147,'2026-08-31'),@('2026-09',174,'2026-09-30'))) {
    $null=Request '/meter-readings' @{contractId=$c.id;period=$entry[0];meterType='WATER';newIndex=$entry[1];readingDate=$entry[2]}
}
$null=Request '/meter-readings' @{contractId=$c.id;period='2026-09';meterType='ELECTRICITY';newIndex=200;readingDate='2026-09-30'}
$null=Request '/maintenance-requests' @{contractId=$c.id;title='Điều hòa không lạnh';description='Máy chạy nhưng không làm lạnh, cần kiểm tra.';category='AIR_CONDITIONER';priority='NORMAL';imageUrls=@()}
Write-Output 'Demo ready: tenant_demo / DemoTenant#2026; staff_demo / DemoStaff#2026. Select September 2026 for the demo.'
