// ==============================================================================
// Script: seed-defense.mjs
// Dữ liệu mô phỏng V13 hoàn chỉnh phục vụ bảo vệ đồ án trước Hội đồng
// ==============================================================================

import assert from 'node:assert/strict';

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

console.log('Seeding V13 Defense Demo to:', BASE_URL);

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

// Cấu hình chốt số & chính sách cảnh báo 50% sau 3 tháng
try {
  await call('Cấu hình ngày chốt & phát hành HĐ', `/properties/${property.id}/billing-policy`, {
    meterCutoffDay: 31,
    invoiceIssueDay: 1,
  }, 'PUT');
} catch (e) {
  console.log('Bỏ qua cập nhật chính sách ngày chốt:', e.message);
}

const accounts = await call('Lấy tài khoản quỹ', '/cash-accounts');
const cashAccount = accounts.find((a) => a.property_id === property.id) || accounts[0];

// Thiết lập giá dịch vụ
const defaultServices = await call('Danh mục dịch vụ', '/catalog/services');
const elecService = defaultServices.find((s) => (s.service_code || s.code) === 'ELEC') || defaultServices[0];
const waterService = defaultServices.find((s) => (s.service_code || s.code) === 'WATER') || defaultServices[1];

const existingPrices = await call('Biểu giá hiện có', '/catalog/prices');
async function ensurePrice(serviceItemId, unitPrice) {
  const match = existingPrices.find(
    (p) => p.property_id === property.id && p.service_item_id === serviceItemId && p.effective_from === '2025-01-01'
  );
  if (!match) {
    try {
      await call(`Cập nhật biểu giá ${unitPrice}`, '/catalog/prices', {
        propertyId: property.id,
        serviceItemId,
        effectiveFrom: '2025-01-01',
        unitPrice,
      });
    } catch (e) {
      console.log('Bỏ qua tạo biểu giá:', e.message);
    }
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

try {
  await call('Chính sách cảnh báo điện nước 50% sau 3 tháng', '/thresholds', {
    propertyId: property.id,
    percentage: 50,
    months: 3,
  });
} catch (e) {
  console.log('Bỏ qua cập nhật threshold:', e.message);
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
const atHeater = await ensureAssetType('WATER_HEATER', 'Bình nước nóng Ariston', 'Bình');

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
const r202 = await ensureRoom('P202', 2, 2800000, 25, 2); // Phòng trống
console.log('✓ Phòng trọ đã sẵn sàng: P101, P102, P201, P202');

// Bố trí đồ đạc cho P101
const p101Assets = await call(`Kiểm tra đồ P101`, `/rooms/${r101.id}/assets`);
if (!p101Assets || p101Assets.length === 0) {
  await call(`Gán Điều hòa vào P101`, `/rooms/${r101.id}/assets`, {
    assetTypeId: atAC.id,
    assetCode: 'AST-101-AC',
    quantity: 1,
    note: 'Daikin 9000 BTU, mới 100%',
  });
  await call(`Gán Tủ lạnh vào P101`, `/rooms/${r101.id}/assets`, {
    assetTypeId: atFridge.id,
    assetCode: 'AST-101-FRIDGE',
    quantity: 1,
    note: 'Toshiba 180L, chạy êm',
  });
  await call(`Gán Giường vào P101`, `/rooms/${r101.id}/assets`, {
    assetTypeId: atBed.id,
    assetCode: 'AST-101-BED',
    quantity: 1,
    note: 'Giường gỗ sồi kèm đệm cao su',
  });
  await call(`Gán Tủ áo vào P101`, `/rooms/${r101.id}/assets`, {
    assetTypeId: atWardrobe.id,
    assetCode: 'AST-101-WARDROBE',
    quantity: 1,
    note: 'Tủ 3 cánh MDF chống ẩm',
  });
}
console.log('✓ Đồ đạc nội thất P101 đã bố trí đầy đủ');

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
      terms: 'Người thuê thanh toán đúng hạn trước ngày ' + payDay + ' hàng tháng. Giữ gìn an ninh trật tự, vệ sinh chung và bảo quản trang thiết bị phòng.',
    });
    // Ký duyệt
    await call(`Ký hợp đồng ${code}`, `/contracts/${c.id}/activate`, {
      signedAt: `${start}T09:00:00+07:00`,
    });
    // Thu tiền cọc
    await call(`Thu cọc ${code}`, '/cash-transactions', {
      cashAccountId: cashAccount.id,
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
    // Tạo biên bản bàn giao nhận phòng (MOVE_IN) tự động nạp đồ
    await call(`Biên bản nhận phòng ${code}`, `/contracts/${c.id}/handovers`, {
      handoverType: 'MOVE_IN',
      handoverCode: `HO-IN-${code}`,
      occurredAt: `${start}T10:00:00+07:00`,
      autoPopulateItems: true,
      note: 'Bàn giao phòng và trang thiết bị đầy đủ theo thỏa thuận',
    });
    // Xác nhận biên bản
    const freshDetail = await call(`Chi tiết HĐ ${c.id}`, `/contracts/${c.id}`);
    const ho = freshDetail.handovers?.find((h) => h.handover_type === 'MOVE_IN');
    if (ho && ho.status === 'DRAFT') {
      await call(`Xác nhận biên bản bàn giao ${ho.id}`, `/handovers/${ho.id}/confirm`, {
        note: 'Hai bên đã ký nhận hiện trạng đầy đủ',
      });
    }
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
  try {
    await call('Gia hạn HĐ P101 (Phụ lục số 2)', `/contracts/${c101.id}/renew`, {
      effectiveFrom: '2026-06-01',
      endDate: '2027-06-01',
      rentalPrice: 3600000,
      deposit: 3500000,
      signedAt: '2026-01-10T14:00:00+07:00',
      reason: 'Ký gia hạn thêm 1 năm theo nguyện vọng của khách thuê',
    });
    console.log('✓ Đã ký phụ lục gia hạn HĐ P101 (Phiên bản số 2)');
  } catch (e) {
    console.log('Bỏ qua phụ lục gia hạn:', e.message);
  }
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
// P101: 3 tháng đầu TB ~82 kWh -> Tháng 01/2026 đột biến lên 195 kWh (>130% tăng, vượt ngưỡng 50%)!
const readings = [
  // Tháng 10/2025
  { m: mElec101, c: c101, start: '2025-10-01T00:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 100, newI: 180 }, // 80 kWh
  { m: mWater101, c: c101, start: '2025-10-01T00:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 50, newI: 60 },  // 10 m3
  { m: mElec102, c: c102, start: '2025-10-01T00:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 200, newI: 270 }, // 70 kWh
  { m: mWater102, c: c102, start: '2025-10-01T00:00:00+07:00', end: '2025-10-31T23:59:59+07:00', oldI: 80, newI: 89 },  // 9 m3

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

  // Tháng 01/2026: ĐỘT BIẾN ĐIỆN TẠI P101 (195 kWh so với TB 82.3 kWh -> +137%)
  { m: mElec101, c: c101, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 347, newI: 542 }, // 195 kWh
  { m: mWater101, c: c101, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 81, newI: 93 },  // 12 m3
  { m: mElec102, c: c102, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 423, newI: 501 }, // 78 kWh
  { m: mWater102, c: c102, start: '2026-01-01T00:00:00+07:00', end: '2026-01-31T23:59:59+07:00', oldI: 109, newI: 120 }, // 11 m3
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
    try {
      await call(`Ghi chỉ số ${rd.m.meter_code} (${rd.oldI}->${rd.newI})`, '/meter-readings', {
        meterId: rd.m.id,
        contractId: rd.c.id,
        startAt: rd.start,
        endAt: rd.end,
        oldIndex: rd.oldI,
        newIndex: rd.newI,
      });
    } catch (e) {
      console.log(`Bỏ qua chỉ số ${rd.m.meter_code}:`, e.message);
    }
  }
}
console.log('✓ Đã ghi nhận lịch sử 4 tháng điện nước và kích hoạt cảnh báo đột biến');

// 6. Lập Hóa đơn định kỳ & Phân bổ thanh toán (Trước hạn, Đúng hạn, Sau hạn, Còn nợ)
const existingInvoices = (await call('Danh sách hóa đơn', '/invoices')) || [];

async function ensureMonthlyInvoice(contract, period, issueDate, dueDate, payCase) {
  let inv = existingInvoices.find((i) => i.contract_id === contract.id && i.billing_period === `${period}-01`);
  if (!inv) {
    try {
      inv = await call(`Lập HĐ ${contract.contract_code} kỳ ${period}`, '/invoices', {
        contractId: contract.id,
        period,
        issueDate,
        dueDate,
      });
    } catch (e) {
      console.log(`Lập HĐ ${period} thủ công:`, e.message);
      return null;
    }
    // Phát hành
    if (inv && inv.status === 'DRAFT') {
      inv = await call(`Phát hành HĐ ${inv.invoice_code}`, `/invoices/${inv.id}/issue`, {});
    }

    if (!inv) return null;
    const total = Number(inv.total_amount || 0);

    if (payCase === 'EARLY') {
      // Nộp trước hạn 2 ngày
      const payDate = `${period}-03T10:00:00+07:00`;
      await call(`Thanh toán TRƯỚC HẠN ${inv.invoice_code}`, `/invoices/${inv.id}/payments`, {
        code: `PAY-EARLY-${inv.id}`,
        amount: total,
        cashAccountId: cashAccount.id,
        occurredAt: payDate,
        method: 'BANK_TRANSFER_MANUAL',
        notes: 'Khách nộp sớm trước ngày đến hạn',
      });
    } else if (payCase === 'ON_TIME') {
      // Nộp chuẩn ngày đến hạn (mùng 5)
      const payDate = `${dueDate}T14:00:00+07:00`;
      await call(`Thanh toán ĐÚNG HẠN ${inv.invoice_code}`, `/invoices/${inv.id}/payments`, {
        code: `PAY-ONTIME-${inv.id}`,
        amount: total,
        cashAccountId: cashAccount.id,
        occurredAt: payDate,
        method: 'CASH',
        notes: 'Nộp tiền mặt đúng ngày đến hạn',
      });
    } else if (payCase === 'LATE') {
      // Nộp muộn sau ngày hạn 3 ngày
      const payDate = `${period}-08T15:00:00+07:00`;
      await call(`Thanh toán SAU HẠN ${inv.invoice_code}`, `/invoices/${inv.id}/payments`, {
        code: `PAY-LATE-${inv.id}`,
        amount: total,
        cashAccountId: cashAccount.id,
        occurredAt: payDate,
        method: 'BANK_TRANSFER_MANUAL',
        notes: 'Khách báo bận nên nộp trễ hạn 3 ngày',
      });
    } else if (payCase === 'PARTIAL') {
      // Trả một phần, còn nợ một phần quá hạn
      const payDate = `${dueDate}T10:00:00+07:00`;
      await call(`Thanh toán MỘT PHẦN ${inv.invoice_code}`, `/invoices/${inv.id}/payments`, {
        code: `PAY-PART-${inv.id}`,
        amount: 2000000,
        cashAccountId: cashAccount.id,
        occurredAt: payDate,
        method: 'CASH',
        notes: 'Nộp tạm 2 triệu, xin nợ lại phần còn lại',
      });
    }
  }
  return inv;
}

// Tạo chuỗi hóa đơn cho P101 (Trần Văn An)
await ensureMonthlyInvoice(c101, '2025-10', '2025-10-01', '2025-10-05', 'EARLY');
await ensureMonthlyInvoice(c101, '2025-11', '2025-11-01', '2025-11-05', 'ON_TIME');
await ensureMonthlyInvoice(c101, '2025-12', '2025-12-01', '2025-12-05', 'LATE');
await ensureMonthlyInvoice(c101, '2026-01', '2026-01-01', '2026-01-05', 'UNPAID');

// Tạo chuỗi hóa đơn cho P102 (Lê Thị Bích) -> Sinh nợ quá hạn
await ensureMonthlyInvoice(c102, '2025-10', '2025-10-01', '2025-10-05', 'ON_TIME');
await ensureMonthlyInvoice(c102, '2025-11', '2025-11-01', '2025-11-05', 'ON_TIME');
await ensureMonthlyInvoice(c102, '2025-12', '2025-12-01', '2025-12-05', 'PARTIAL'); // Nợ 1 phần, quá hạn
await ensureMonthlyInvoice(c102, '2026-01', '2026-01-01', '2026-01-05', 'UNPAID');  // Chưa trả đồng nào, quá hạn
console.log('✓ Đã phát hành hóa đơn 4 tháng với đầy đủ các ca: Trước hạn, Đúng hạn, Sau hạn, Chưa thu & Quá hạn');

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

const pastDetail = await call(`Kiểm tra HĐ cũ ${cPast.id}`, `/contracts/${cPast.id}`);
let hoOut = pastDetail.handovers?.find((h) => h.handover_type === 'MOVE_OUT');
if (!hoOut) {
  const updatedContract = await call(`Tạo biên bản trả phòng P201`, `/contracts/${cPast.id}/handovers`, {
    handoverType: 'MOVE_OUT',
    handoverCode: `HO-OUT-${cPast.id}`,
    occurredAt: '2025-11-01T15:00:00+07:00',
    autoPopulateItems: false,
    note: 'Kiểm kê bàn giao trả phòng khi kết thúc hợp đồng',
  });
  hoOut = updatedContract.handovers?.find((h) => h.handover_type === 'MOVE_OUT');
}
if (hoOut && hoOut.status === 'DRAFT') {
  const hi1 = await call('Ghi nhận Điều hòa hỏng', `/handovers/${hoOut.id}/items`, {
    itemName: 'Điều hòa Daikin 9000 BTU',
    quantity: 1,
    conditionNote: 'Hư cánh vẫy gió tự động và mất điều khiển từ xa',
    note: 'Cần sửa chữa thay thế linh kiện',
  });
  await call('Ghi nhận Giường ngủ', `/handovers/${hoOut.id}/items`, {
    itemName: 'Giường gỗ sồi 1.8m',
    quantity: 1,
    conditionNote: 'Bình thường, trầy xước nhẹ',
  });
  // Xác nhận biên bản trả phòng
  await call(`Xác nhận biên bản trả phòng ${hoOut.id}`, `/handovers/${hoOut.id}/confirm`, {
    note: 'Khách thuê đã đồng ý với biên bản kiểm kê',
  });
  // Lập hóa đơn bồi thường thiệt hại (DAMAGE)
  await call('Phát sinh chứng từ bồi thường hư hỏng', `/handovers/${hoOut.id}/damage-compensation`, {
    handoverItemId: hi1.id,
    amount: 350000,
    issueDate: '2025-11-01',
    dueDate: '2025-11-03',
    note: 'Chi phí mua điều khiển mới và sửa cánh vẫy điều hòa',
  });
  console.log('✓ Đã lập biên bản trả phòng và phát sinh hóa đơn bồi thường DAMAGE gắn mã đồ vật');
}

// 8. KIỂM TRA & XÁC THỰC CÁC CHỈ SỐ THEO YÊU CẦU CỦA THẦY
console.log('\n========================================');
console.log('XÁC THỰC KẾT QUẢ ĐỒ ÁN THEO GÓP Ý CỦA THẦY');
console.log('========================================');

// 1. Dashboard Tháng 12/2025
const dashDec = await call('Dashboard Tháng 12/2025', '/dashboard?period=2025-12');
console.log('\n[1] Dashboard Tháng 12/2025:');
console.log('- Tổng lập HĐ (billed):', dashDec.billed);
console.log('- Thu trước hạn (paid_early):', dashDec.paid_early);
console.log('- Thu đúng hạn (paid_on_time):', dashDec.paid_on_time);
console.log('- Thu sau hạn (paid_late):', dashDec.paid_late);
console.log('- Chưa thu (unpaid):', dashDec.unpaid);
console.log('- Tiền điện (electricity):', dashDec.electricity);
console.log('- Tiền nước (water):', dashDec.water);

// 2. Dashboard Quý 4/2025
const dashQ4 = await call('Dashboard Quý 4/2025', '/dashboard?viewType=quarter&quarter=2025-Q4');
console.log('\n[2] Dashboard Quý 4/2025 (Theo Quý):');
console.log('- Kỳ nhãn:', dashQ4.period);
console.log('- Tổng lập HĐ:', dashQ4.billed);
console.log('- Thực tế đã thu:', dashQ4.collected);
console.log('- Dư nợ kỳ:', dashQ4.outstanding);

// 3. Dư nợ khách hàng (Debts)
const debtorTenants = await call('Báo cáo Dư nợ khách hàng', '/reports/tenant-debts');
console.log('\n[3] Danh sách Dư nợ khách hàng:');
for (const t of debtorTenants || []) {
  console.log(`- Khách: ${t.full_name} (${t.phone}) | Dư nợ: ${t.total_debt} VNĐ | Số HĐ nợ: ${t.unpaid_invoice_count} | Quá hạn: ${t.max_overdue_days} ngày`);
}

// 4. Điện nước bất thường (Anomaly)
const meterReadings = await call('Danh sách chỉ số điện nước', '/meter-readings');
const topReading = meterReadings[0];
console.log('\n[4] Danh sách Điện nước (Mặc định đẩy bất thường lên đầu):');
console.log(`- Bản ghi đầu tiên: Phòng ${topReading.room_code} | Kỳ: ${topReading.billing_period} | Tiêu thụ: ${topReading.consumption} | Cảnh báo: ${topReading.has_anomaly} | Tăng: +${topReading.percentage_increase}%`);
assert.equal(topReading.has_anomaly, true, 'Bản ghi bất thường phải được đẩy lên đầu danh sách');

// 5. Đồ đạc và Bàn giao
const room101Info = await call('Chi tiết phòng 101 kèm đồ đạc', `/rooms/${r101.id}`);
console.log('\n[5] Đồ đạc trong phòng 101:');
console.log(`- Số lượng đồ đạc bố trí: ${room101Info.assets?.length || 0} món`);
for (const a of room101Info.assets || []) {
  console.log(`  + ${a.asset_type_name} (${a.asset_code}): Số lượng ${a.quantity} ${a.unit_name} [${a.status}]`);
}

console.log('\n========================================');
console.log('✓ HOÀN TẤT SEED DỮ LIỆU DEMO V13 CHUẨN BẢO VỆ!');
console.log('========================================\n');
