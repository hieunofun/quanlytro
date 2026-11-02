-- Chỉ đọc. Chạy trên database thử hostel_review49 (127.0.0.1:55449).
-- Các khu BE-* là dữ liệu giả do verify-backend-demo.mjs tạo bằng API thật.

-- 1. Phiên bản migration đang dùng.
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 2. Công nợ và tổng tiền sau điều chỉnh của các hợp đồng thử.
SELECT p.property_code, c.contract_code, d.document_code, d.document_type,
       d.status, d.total_amount, d.paid_amount, d.balance_due
FROM v_document_balances d
JOIN contracts c ON c.id=d.contract_id
JOIN rooms r ON r.id=c.room_id
JOIN properties p ON p.id=r.property_id
WHERE p.property_code LIKE 'BE-%'
ORDER BY p.id DESC, c.id, d.id;

-- 3. Tiền cọc/trả trước còn giữ. Không lưu balance chỉnh tay.
SELECT p.property_code, c.contract_code, f.fund_type,
       f.balance, f.held_amount, f.available
FROM v_fund_balances f JOIN contracts c ON c.id=f.contract_id
JOIN rooms r ON r.id=c.room_id JOIN properties p ON p.id=r.property_id
WHERE p.property_code LIKE 'BE-%' ORDER BY p.id DESC, c.id, f.id;

-- 4. Chia điện theo người chịu trách nhiệm; khoảng phòng trống thuộc OWNER.
SELECT p.property_code, r.room_code, s.contract_id, s.responsibility,
       s.service_start, s.service_end, s.old_index, s.new_index, s.consumption
FROM v_segments s JOIN rooms r ON r.id=s.room_id JOIN properties p ON p.id=r.property_id
WHERE p.property_code LIKE 'BE-%' ORDER BY p.id DESC, r.id, s.service_start;

-- 5. Phiếu tiền thật, phân biệt IN/OUT và mã tham chiếu.
SELECT p.property_code, tx.transaction_code, tx.direction, tx.amount,
       tx.status, tx.payment_document_id, tx.external_reference, tx.occurred_at
FROM cash_transactions tx JOIN cash_accounts a ON a.id=tx.cash_account_id
JOIN properties p ON p.id=a.property_id
WHERE p.property_code LIKE 'BE-%' ORDER BY p.id DESC, tx.id;

-- 6. Bản chỉ số sửa có liên kết, bản chỉ số gốc còn nguyên.
SELECT p.property_code, m.meter_code, q.id, q.observed_at, q.reading_value,
       q.revision_no, q.supersedes_reading_id, q.correction_item_id
FROM meter_readings q JOIN meters m ON m.id=q.meter_id
JOIN rooms r ON r.id=m.room_id JOIN properties p ON p.id=r.property_id
WHERE p.property_code LIKE 'BE-%' ORDER BY p.id DESC, m.id, q.observed_at, q.revision_no;
