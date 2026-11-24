// ==============================================================================
// Script: seed-defense.mjs
// Dữ liệu mô phỏng V14 hoàn chỉnh phục vụ bảo vệ đồ án trước Hội đồng
// ==============================================================================

import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const BASE_URL = process.env.BASE_URL || 'http://127.0.0.1:8093/api';
let token = '';

async function call(name, path, body = null, method = null) {
  const m = method || (body ? 'POST' : 'GET');
  const headers = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (body) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${BASE_URL}${path}`, {
    method: m,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const errText = await res.text();
    console.error(`FAILED: ${name} [${m} ${path}] -> HTTP ${res.status}: ${errText}`);
    throw new Error(`${name} failed: HTTP ${res.status}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function attachOnce(kind, id, filename, bytes, contentType) {
  const path = `/files/${kind}/${id}`;
  const current = await call('Kiểm tra tệp minh chứng', path);
  if (current.some((file) => file.original_name === filename)) return;
  const data = new FormData();
  data.append('file', new Blob([bytes], { type: contentType }), filename);
  const result = await fetch(BASE_URL + path, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: data });
  assert.ok(result.ok, `Đính kèm ${filename} thất bại: ${await result.text()}`);
}

console.log('Seeding V14 Defense Demo to:', BASE_URL);

// 1. Đăng nhập Admin
const login = await call('Login Admin', '/auth/login', {
  username: 'admin',
  password: process.env.ADMIN_PASSWORD || 'ReviewAdmin49#2026',
});
token = login.accessToken;
console.log('✓ Admin authenticated');

// Kiểm tra khu nhà đã có chưa
const properties = await call('Kiểm tra khu nhà', '/properties');
let property = properties.find((p) => (p.property_code || p.code) === 'SKYHOME');
if (!property) {
  property = await call('Tạo Tòa nhà SkyHome Central', '/properties', {
    code: 'SKYHOME',
    name: 'Tòa nhà SkyHome Central',
    address: 'Số 123 Đường Nguyễn Trãi, Thanh Xuân, Hà Nội',
  });
  console.log('✓ Tạo khu nhà SkyHome:', property.id);
} else {
  console.log('✓ Dùng khu nhà SkyHome hiện hữu:', property.id);
}

await call('Cấu hình ngày chốt và phát hành', `/properties/${property.id}/billing-policy`, {
  meterCutoffDay: 31,
  invoiceIssueDay: 1,
}, 'PUT');

const accounts = await call('Lấy tài khoản quỹ', '/cash-accounts');
const cashAccount = accounts.find((a) => a.property_id === property.id && a.account_type === 'CASH') || accounts[0];
let bankAccount = accounts.find((a) => a.property_id === property.id && a.account_type === 'BANK');
if (!bankAccount) {
  bankAccount = await call('Tạo tài khoản ngân hàng khu nhà', '/catalog/cash-accounts', {
    propertyId: property.id,
    accountCode: 'BANK-SKYHOME',
    name: 'Tài khoản ngân hàng Tòa nhà SkyHome Central',
    accountType: 'BANK',
    isActive: true,
  });
}

// Thiết lập giá dịch vụ
const defaultServices = await call('Danh mục dịch vụ', '/catalog/services');
const elecService = defaultServices.find((s) => s.service_code === 'ELEC');
const waterService = defaultServices.find((s) => s.service_code === 'WATER');
assert.ok(elecService && waterService, 'Thiếu danh mục dịch vụ điện nước');

const existingPrices = await call('Biểu giá hiện có', '/catalog/prices');
async function ensurePrice(serviceItemId, unitPrice) {
  const match = existingPrices.find(
    (p) => p.property_id === property.id && p.service_item_id === serviceItemId && p.effective_from === '2025-01-01'
  );
  if (!match) {
    await call(`Cập nhật biểu giá ${unitPrice}`, '/catalog/prices', {
      propertyId: property.id,
      serviceItemId,
      effectiveFrom: '2025-01-01',
      unitPrice,
    });
  }
}
for (const s of defaultServices) {
  let defaultPrice = 50000;
  const sc = s.service_code || s.code;
  if (sc === 'ELEC') defaultPrice = 3500;
  else if (sc === 'WATER') defaultPrice = 25000;
  else if (sc === 'TRASH') defaultPrice = 50000;
  else if (sc === 'WIFI') defaultPrice = 100000;
  else if (sc === 'PARKING') defaultPrice = 100000;
  await ensurePrice(s.id, defaultPrice);
}

const policies = await call('Danh sách ngưỡng cảnh báo', '/catalog/thresholds');
for (const service of [elecService, waterService]) {
  if (!policies.some(p => p.property_id === property.id && p.service_item_id === service.id && p.effective_from === '2025-10-01')) {
    await call('Cấu hình ngưỡng so với ba tháng trước', '/catalog/thresholds', {
      propertyId: property.id,
      serviceItemId: service.id,
      effectiveFrom: '2025-10-01',
      thresholdPercentage: 50,
      historyMonths: 3,
      minimumDays: 7,
    });
  }
}

// 2. Tạo Loại tài sản & Danh mục đồ đạc
const assetTypes = await call('Danh mục loại tài sản', '/catalog/asset-types');
async function ensureAssetType(code, name, unit) {
  let at = assetTypes.find((x) => (x.code || x.asset_type_code) === code);
  if (!at) {
    at = await call(`Tạo loại tài sản ${name}`, '/catalog/asset-types', {
      code,
      name,
      unitName: unit,
    });
  }
  return at;
}
const atAC = await ensureAssetType('AC_INVERTER', 'Điều hòa Inverter Daikin', 'Bộ');
const atFridge = await ensureAssetType('FRIDGE_2D', 'Tủ lạnh Toshiba 180L', 'Chiếc');
const atBed = await ensureAssetType('WOOD_BED', 'Giường ngủ gỗ Sồi 1.8m', 'Chiếc');
const atWardrobe = await ensureAssetType('WARDROBE_3D', 'Tủ quần áo 3 cánh', 'Chiếc');

// 3. Tạo các Phòng trọ
const rooms = await call('Danh sách phòng', '/rooms');
async function ensureRoom(code, floor, price, area, occupants) {
  let r = rooms.find((x) => (x.room_code || x.code) === code && x.property_id === property.id);
  if (!r) {
    r = await call(`Tạo phòng ${code}`, '/rooms', {
      propertyId: property.id,
      floorNumber: floor,
      code,
      price,
      maxOccupants: occupants,
      area,
      openedOn: '2025-08-01',
    });
  }
  return r;
}
const r101 = await ensureRoom('P101', 1, 3500000, 30, 2);
const r102 = await ensureRoom('P102', 1, 3200000, 28, 2);
const r201 = await ensureRoom('P201', 2, 4000000, 35, 3);
await ensureRoom('P202', 2, 2800000, 25, 2);
console.log('✓ Phòng trọ đã sẵn sàng: P101, P102, P201, P202');

async function ensureRoomAssets(room, definitions) {
  const placed = await call(`Đồ đạc phòng ${room.room_code}`, `/rooms/${room.id}/assets`);
  for (const [type, code, quantity, note] of definitions) {
    if (!placed.some(asset => asset.asset_code === code)) {
      await call(`Bố trí ${code}`, `/rooms/${room.id}/assets`, {
        assetTypeId: type.id, assetCode: code, quantity, note,
        validFrom: '2025-08-01T08:00:00+07:00',
      });
    }
  }
}
await ensureRoomAssets(r101, [
  [atAC, 'AST-101-AC', 1, 'Daikin 9000 BTU'],
  [atFridge, 'AST-101-FRIDGE', 1, 'Toshiba 180 L'],
  [atBed, 'AST-101-BED', 1, 'Kèm đệm cao su'],
  [atWardrobe, 'AST-101-WARDROBE', 1, 'Gỗ MDF chống ẩm'],
]);
await ensureRoomAssets(r201, [
  [atAC, 'AST-201-AC', 1, 'Kèm điều khiển'],
  [atBed, 'AST-201-BED', 1, 'Kèm đệm'],
]);
console.log('✓ Đã bố trí tài sản phòng P101 và P201');

// Công tơ điện nước
const metersList = await call('Danh sách công tơ', '/catalog/meters');
async function ensureMeter(roomId, serviceId, code) {
  let m = metersList.find((x) => (x.room_id === roomId && x.service_item_id === serviceId) || (x.meter_code || x.code) === code);
  if (!m) {
    m = await call(`Lắp công tơ ${code}`, '/catalog/meters', {
      roomId,
      serviceItemId: serviceId,
      meterCode: code,
      installedAt: '2025-08-01T00:00:00+07:00',
    });
  }
  return m;
}
const mElec101 = await ensureMeter(r101.id, elecService.id, 'EM-P101');
const mWater101 = await ensureMeter(r101.id, waterService.id, 'WM-P101');
const mElec102 = await ensureMeter(r102.id, elecService.id, 'EM-P102');
const mWater102 = await ensureMeter(r102.id, waterService.id, 'WM-P102');

// 4. Khách thuê & Hợp đồng
const contracts = await call('Danh sách hợp đồng', '/contracts');
async function ensureContract(code, roomId, fullName, phone, cccd, rent, dep, start, end, payDay = 5, timing = 'IN_ADVANCE') {
  let c = contracts.find((x) => (x.contract_code || x.code) === code);
  if (!c) {
    c = await call(`Tạo hợp đồng ${code}`, '/contracts', {
      code,
      roomId,
      tenant: {
        fullName,
        phone,
        identityNumber: cccd,
        email: `${phone}@example.com`,
      },
      startDate: start,
      endDate: end,
      rentalPrice: rent,
      deposit: dep,
      paymentDay: payDay,
      collectionTiming: timing,
      primaryResides: true,
      lessorName: 'Nguyễn Minh Hà',
      lessorPhone: '0900000000',
      lessorAddress: '123 Nguyễn Trãi, Thanh Xuân, Hà Nội',
      tenantAddress: 'Địa chỉ liên hệ ghi trong hồ sơ demo',
      terms: 'Người thuê thanh toán đúng hạn trước ngày ' + payDay + ' hàng tháng. Giữ gìn an ninh trật tự, vệ sinh chung và bảo quản trang thiết bị phòng.',
    });
    // Ký duyệt
    await call(`Ký hợp đồng ${code}`, `/contracts/${c.id}/activate`, {
      signedAt: `${start}T09:00:00+07:00`,
    });
    // Thu tiền cọc
    await call(`Thu cọc ${code}`, '/cash-transactions', {
      cashAccountId: bankAccount.id,
      contractId: c.id,
      fundType: 'DEPOSIT',
      code: `DEP-${code}`,
      amount: dep,
      method: 'BANK_TRANSFER_MANUAL',
      occurredAt: `${start}T09:30:00+07:00`,
      confirmed: true,
      note: 'Nhận cọc chuyển khoản ngân hàng',
    });
    // Giao phòng thực tế
    await call(`Giao phòng ${code}`, `/contracts/${c.id}/possessions`, {
      takenAt: `${start}T10:00:00+07:00`,
      reason: 'Đã nhận đủ cọc và bàn giao chìa khóa',
    });
  }
  let detail = await call(`Chi tiết HĐ ${c.id}`, `/contracts/${c.id}`);
  let handover = detail.handovers.find(h => h.handover_type === 'MOVE_IN');
  if (!handover) {
    detail = await call(`Biên bản nhận phòng ${code}`, `/contracts/${c.id}/handovers`, {
      handoverType: 'MOVE_IN',
      handoverCode: `HO-IN-${code}`,
      occurredAt: `${start}T10:00:00+07:00`,
      autoPopulateItems: true,
      note: 'Bàn giao phòng, chìa khóa và trang thiết bị',
    });
    handover = detail.handovers.find(h => h.handover_type === 'MOVE_IN');
  }
  if (handover.status === 'DRAFT') {
    for (const item of handover.items) {
      await call('Ghi hiện trạng khi nhận', `/handovers/${handover.id}/items/${item.id}`, {
        quantity: item.quantity,
        conditionNote: 'Hoạt động tốt, đầy đủ phụ kiện',
      }, 'PUT');
    }
    await call('Xác nhận biên bản nhận phòng', `/handovers/${handover.id}/confirm`, {
      note: 'Đã đối chiếu hiện trạng cùng người thuê',
    });
  }
  return call(`Lấy chi tiết ${c.id}`, `/contracts/${c.id}`);
}

// Khách 1: Trần Văn An (P101) - Thuê từ 2025-10-01
const c101 = await ensureContract(
  'HD-P101-AN',
  r101.id,
  'Trần Văn An',
  '0912345678',
  '001200000001',
  3500000,
  3500000,
  '2025-10-01',
  '2026-06-01',
  5,
  'IN_ADVANCE'
);
console.log('✓ Hợp đồng P101 (Trần Văn An) đã tạo & xác nhận bàn giao đồ đạc');

// Gia hạn thêm phụ lục hợp đồng bản 2 cho P101
if (c101.versions?.length === 1) {
  await call('Ký phụ lục hợp đồng P101', `/contracts/${c101.id}/renew`, {
    effectiveFrom: '2026-06-01',
    endDate: '2027-06-01',
    rentalPrice: 3600000,
    deposit: 3500000,
    paymentDay: 8,
    collectionTiming: 'IN_ARREARS',
    terms: 'Từ ngày hiệu lực phụ lục, tiền thuê được thanh toán vào ngày 8. Các nội dung bàn giao tiếp tục theo biên bản nhận phòng.',
    signedAt: '2026-01-10T14:00:00+07:00',
    reason: 'Gia hạn thêm một năm theo thỏa thuận',
  });
}

// Khách 2: Lê Thị Bích (P102) - Khách CÓ DƯ NỢ & QUÁ HẠN
const c102 = await ensureContract(
  'HD-P102-BICH',
  r102.id,
  'Lê Thị Bích',
  '0987654321',
  '001200000002',
  3200000,
  3200000,
  '2025-10-01',
  '2026-10-01',
  5,
  'IN_ADVANCE'
);
console.log('✓ Hợp đồng P102 (Lê Thị Bích) đã sẵn sàng');

// 5. Ghi nhận chỉ số Điện & Nước 4 tháng (T10, T11, T12/2025 & T01/2026)
// P102 tăng điện và nước trong tháng 01/2026 sau ba tháng ổn định.
const readings = [
  // Tháng 10/2025
  { m: mElec101, c: c101, start: '2025-10-01T10:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 100, newI: 180 }, // 80 kWh
  { m: mWater101, c: c101, start: '2025-10-01T10:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 50, newI: 60 },  // 10 m3
  { m: mElec102, c: c102, start: '2025-10-01T10:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 200, newI: 270 }, // 70 kWh
  { m: mWater102, c: c102, start: '2025-10-01T10:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 80, newI: 89 },  // 9 m3

  // Tháng 11/2025
  { m: mElec101, c: c101, start: '2025-11-01T00:00:00+07:00', end: '2025-11-30T23:59:59+07:00', oldI: 180, newI: 265 }, // 85 kWh
  { m: mWater101, c: c101, start: '2025-11-01T00:00:00+07:00', end: '2025-11-30T23:59:59+07:00', oldI: 60, newI: 71 },  // 11 m3
  { m: mElec102, c: c102, start: '2025-11-01T00:00:00+07:00', end: '2025-11-30T23:59:59+07:00', oldI: 270, newI: 345 }, // 75 kWh
  { m: mWater102, c: c102, start: '2025-11-01T00:00:00+07:00', end: '2025-11-30T23:59:59+07:00', oldI: 89, newI: 99 },  // 10 m3

  // Tháng 12/2025
  { m: mElec101, c: c101, start: '2025-12-01T00:00:00+07:00', end: '2025-12-31T23:59:59+07:00', oldI: 265, newI: 347 }, // 82 kWh
  { m: mWater101, c: c101, start: '2025-12-01T00:00:00+07:00', end: '2025-12-31T23:59:59+07:00', oldI: 71, newI: 81 },  // 10 m3
  { m: mElec102, c: c102, start: '2025-12-01T00:00:00+07:00', end: '2025-12-31T23:59:59+07:00', oldI: 345, newI: 423 }, // 78 kWh
  { m: mWater102, c: c102, start: '2025-12-01T00:00:00+07:00', end: '2025-12-31T23:59:59+07:00', oldI: 99, newI: 109 }, // 10 m3

  // Tháng 01/2026: P102 tăng điện và nước vượt ngưỡng.
  { m: mElec101, c: c101, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 347, newI: 436 }, // 89 kWh
  { m: mWater101, c: c101, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 81, newI: 93 },  // 12 m3
  { m: mElec102, c: c102, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 423, newI: 651 }, // 228 kWh
  { m: mWater102, c: c102, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 109, newI: 142 }, // 33 m3
];

const existingSegments = (await call('Đọc chỉ số điện nước', '/meter-readings')) || [];
for (const rd of readings) {
  const match = existingSegments.find(
    (s) =>
      s.meter_id === rd.m.id &&
      (Number(s.old_index) === rd.oldI || Number(s.start_reading) === rd.oldI) &&
      (Number(s.new_index) === rd.newI || Number(s.end_reading) === rd.newI)
  );
  if (!match) {
    await call(`Ghi chỉ số ${rd.m.meter_code} (${rd.oldI}->${rd.newI})`, '/meter-readings', {
      meterId: rd.m.id,
      contractId: rd.c.id,
      startAt: rd.start,
      endAt: rd.end,
      oldIndex: rd.oldI,
      newIndex: rd.newI,
    });
  }
}
console.log('✓ Đã ghi lịch sử điện nước bốn tháng');

// 6. Lập Hóa đơn định kỳ & Phân bổ thanh toán (Trước hạn, Đúng hạn, Sau hạn, Còn nợ)
const existingInvoices = (await call('Danh sách hóa đơn', '/invoices')) || [];

async function ensureMonthlyInvoice(contract, period, payCase) {
  const dueDate = period + '-05';
  let invoice = existingInvoices.find(i => i.contract_id === contract.id && i.billing_period === period + '-01');
  if (!invoice) {
    invoice = await call(`Lập HĐ ${contract.contract_code} kỳ ${period}`, '/invoices', {
      contractId: contract.id, period, issueDate: period + '-01', dueDate,
    });
  }
  if (invoice.status === 'DRAFT') {
    invoice = await call('Phát hành hóa đơn', `/invoices/${invoice.id}/issue`, {});
  }
  const total = Number(invoice.total_amount);
  const part = Math.floor(total / 4);
  const payments = {
    EARLY: [['EARLY', '03', total]],
    ON_TIME: [['ONTIME', '05', total]],
    LATE: [['LATE', '08', total]],
    PARTIAL: [['PART', '05', 2000000]],
    SPLIT: [['EARLY', '03', part], ['ONTIME', '05', part], ['LATE', '08', part]],
    UNPAID: [],
  };
  for (const [label, date, amount] of payments[payCase]) {
    const isBank = label === 'EARLY' || label === 'LATE';
    await call(`Thanh toán ${label} ${invoice.invoice_code}`, `/invoices/${invoice.id}/payments`, {
      code: `PAY-${label}-${invoice.id}`,
      amount,
      cashAccountId: isBank ? bankAccount.id : cashAccount.id,
      occurredAt: `${period}-${date}T10:00:00+07:00`,
      method: isBank ? 'BANK_TRANSFER_MANUAL' : 'CASH',
    });
  }
  return invoice;
}

await ensureMonthlyInvoice(c101, '2025-10', 'EARLY');
await ensureMonthlyInvoice(c101, '2025-11', 'ON_TIME');
await ensureMonthlyInvoice(c101, '2025-12', 'LATE');
await ensureMonthlyInvoice(c101, '2026-01', 'SPLIT');
await ensureMonthlyInvoice(c102, '2025-10', 'ON_TIME');
await ensureMonthlyInvoice(c102, '2025-11', 'ON_TIME');
await ensureMonthlyInvoice(c102, '2025-12', 'PARTIAL');
// Tháng 01 của P102 chờ kiểm tra cảnh báo trước khi phát hành.
console.log('✓ Hóa đơn demo có thu trước hạn, đúng hạn, sau hạn và nợ một phần');

// 7. Hợp đồng trả phòng & Phát sinh bồi thường đồ đạc hư hỏng
const cPast = await ensureContract(
  'HD-P201-CUONG',
  r201.id,
  'Vũ Đình Cường',
  '0905123456',
  '001200000004',
  3800000,
  3800000,
  '2025-08-01',
  '2025-11-01',
  5,
  'IN_ADVANCE'
);

const evidenceImage = await readFile(new URL('../frontend/tests/fixtures/photo.png', import.meta.url));
let pastDetail = await call('Chi tiết hợp đồng đã trả phòng', `/contracts/${cPast.id}`);
let returnHandover = pastDetail.handovers.find(h => h.handover_type === 'MOVE_OUT');
const returnedAt = '2025-11-01T15:00:00+07:00';
if (!returnHandover) {
  pastDetail = await call('Tạo biên bản trả phòng P201', `/contracts/${cPast.id}/handovers`, {
    handoverType: 'MOVE_OUT',
    handoverCode: `HO-OUT-${cPast.id}`,
    occurredAt: returnedAt,
    autoPopulateItems: true,
    note: 'Đối chiếu trang thiết bị đã nhận',
  });
  returnHandover = pastDetail.handovers.find(h => h.handover_type === 'MOVE_OUT');
}
const aircon = returnHandover.items.find(item => item.asset_code === 'AST-201-AC');
assert.ok(aircon?.original_item_id, 'Thiếu vật dụng gốc để đối chiếu khi trả');
if (returnHandover.status === 'DRAFT') {
  for (const item of returnHandover.items) {
    await call('Ghi hiện trạng lúc trả', `/handovers/${returnHandover.id}/items/${item.id}`, {
      quantity: item.quantity,
      conditionNote: item.id === aircon.id ? 'Hư cánh vẫy, thiếu điều khiển' : 'Đủ số lượng, trầy xước nhẹ',
    }, 'PUT');
  }
  await call('Xác nhận biên bản trả phòng', `/handovers/${returnHandover.id}/confirm`, {
    note: 'Đã đối chiếu với biên bản nhận phòng',
  });
}
await attachOnce('handover-item', aircon.id, 'hien-trang-tra-phong-mo-phong.png', evidenceImage, 'image/png');
const receivedAircon = pastDetail.handovers.find(h => h.handover_type === 'MOVE_IN').items
  .find(item => item.id === aircon.original_item_id);
await attachOnce('handover-item', receivedAircon.id, 'hien-trang-nhan-phong-mo-phong.png', evidenceImage, 'image/png');
if (!pastDetail.checkout) {
  await call('Ghi nhận trả phòng thực tế', `/contracts/${cPast.id}/checkout`, {
    billingEnd: '2025-11-01', returnedAt, returnHandoverId: returnHandover.id,
    notes: 'Đã nhận chìa khóa, đối chiếu đồ đạc trước khi quyết toán',
  });
}
const compensation = await call('Lập chứng từ bồi thường', `/handovers/${returnHandover.id}/damage-compensation`, {
  handoverItemId: aircon.id,
  amount: 350000,
  issueDate: '2025-11-01',
  dueDate: '2025-11-03',
  note: 'Chi phí thay điều khiển và sửa cánh vẫy điều hòa',
});
assert.equal(compensation.document_type, 'COMPENSATION');
assert.equal(compensation.status, 'ISSUED');
assert.equal(compensation.total_amount, 350000);

const contract101 = await call('Kiểm tra phụ lục P101', `/contracts/${c101.id}`);
const annex = contract101.versions.find(v => v.version_no === 2);
assert.ok(annex);
const pdf = await fetch(`${BASE_URL}/contracts/${c101.id}/pdf?version=2`, {
  headers: { Authorization: `Bearer ${token}` },
});
assert.ok(pdf.ok);
const pdfBytes = Buffer.from(await pdf.arrayBuffer());
assert.equal(pdfBytes.subarray(0, 5).toString(), '%PDF-');
await attachOnce('contract-term', annex.id, 'phu-luc-p101-mau.pdf', pdfBytes, 'application/pdf');

const dashboard = await call('Dashboard kỳ hóa đơn mới nhất', '/dashboard');
assert.equal(dashboard.period, '2026-01');
for (const field of ['paid_early', 'paid_on_time', 'paid_late', 'unpaid', 'electricity', 'water']) {
  assert.ok(Number(dashboard[field]) > 0, `Ô ${field} cần có số liệu demo`);
}
assert.equal(
  Math.round((dashboard.paid_early + dashboard.paid_on_time + dashboard.paid_late + dashboard.unpaid) * 100),
  Math.round(dashboard.billed * 100),
  'Tổng các ô thu và chưa thu phải bằng tiền lập hóa đơn',
);
assert.equal(
  Math.round((dashboard.room_rent + dashboard.electricity + dashboard.water + dashboard.services_other) * 100),
  Math.round(dashboard.billed * 100),
  'Tổng tiền phòng, điện, nước và dịch vụ khác phải khớp tổng lập hóa đơn',
);
assert.equal(contract101.payment_cycle_day, 8, 'Phụ lục P101 phải có ngày thanh toán kỳ 8');
const quarter = await call('Dashboard quý 4/2025', '/dashboard?quarter=2025-Q4');
assert.equal(quarter.period, '2025-Q4');
assert.ok(quarter.billed > 0);

const debt = (await call('Công nợ theo khách', '/reports/tenant-debts')).find(t => t.phone === '0987654321');
assert.equal(debt.unpaid_invoice_count, debt.invoices.length);
assert.equal(debt.earliest_due_date, debt.invoices[0].due_date);
assert.ok(debt.total_debt > 0 && debt.total_paid > 0 && debt.max_overdue_days > 0);

const alerts = await call('Cảnh báo đang chờ', '/alerts');
for (const type of ['ELECTRICITY', 'WATER']) {
  assert.ok(alerts.some(a => a.room_code === 'P102' && a.meter_type === type && a.status === 'PENDING'),
    `Thiếu cảnh báo ${type} của P102`);
}
const sortedReadings = await call('Danh sách điện nước', '/meter-readings');
assert.equal(sortedReadings[0].has_anomaly, true);

const returned = await call('Kiểm tra quyết toán P201', `/contracts/${cPast.id}/checkout`);
assert.equal(returned.return_handover_id, returnHandover.id);
assert.equal(compensation.checkout_id, returned.id);
console.log('✓ Demo đầy đủ: tháng/quý, sáu chỉ số, công nợ, hai cảnh báo, phụ lục PDF và bàn giao có bồi thường.');
