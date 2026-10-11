-- ==============================================================================
-- V3: Tạo Exclusion Constraints, Partial Unique Indexes và Performance Indexes
-- ==============================================================================

-- 1. Exclusion Constraint: Chống 2 hợp đồng ACTIVE chồng lấn thời gian trên cùng 1 phòng
ALTER TABLE contracts
ADD CONSTRAINT exclude_overlapping_active_contracts
EXCLUDE USING gist (
    room_id WITH =,
    daterange(start_date, end_date, '[)') WITH &&
)
WHERE (status = 'ACTIVE');

-- 2. Partial Unique Indexes cho alert_thresholds
-- 2.1. Tối đa 1 cấu hình mặc định (property-level) cho mỗi loại đồng hồ
CREATE UNIQUE INDEX uq_alert_thresholds_property_default
ON alert_thresholds (property_id, meter_type)
WHERE room_id IS NULL;

-- 2.2. Tối đa 1 cấu hình override (room-level) cho mỗi loại đồng hồ
CREATE UNIQUE INDEX uq_alert_thresholds_room_override
ON alert_thresholds (room_id, meter_type)
WHERE room_id IS NOT NULL;

-- 3. Performance Indexes
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_phone ON users (phone);

CREATE INDEX idx_properties_code ON properties (property_code);

CREATE INDEX idx_floors_property_id ON floors (property_id);

CREATE INDEX idx_rooms_property ON rooms (property_id);
CREATE INDEX idx_rooms_status ON rooms (status);
CREATE INDEX idx_rooms_floor ON rooms (floor_id);

CREATE INDEX idx_emp_assign_user ON employee_assignments (user_id);
CREATE INDEX idx_emp_assign_prop ON employee_assignments (property_id);

CREATE INDEX idx_tenant_identity ON tenant_profiles (identity_number);
CREATE INDEX idx_tenant_phone ON tenant_profiles (phone);
CREATE INDEX idx_tenant_user_id ON tenant_profiles (user_id);

CREATE INDEX idx_contracts_room_status ON contracts (room_id, status);
CREATE INDEX idx_contracts_tenant ON contracts (primary_tenant_id);

CREATE INDEX idx_contract_members_contract ON contract_members (contract_id);

CREATE INDEX idx_service_items_type ON service_items (service_type);

CREATE INDEX idx_price_configs_search ON service_price_configs (property_id, service_item_id, effective_from DESC);

CREATE INDEX idx_threshold_lookup ON alert_thresholds (property_id, room_id, meter_type, is_active);

CREATE INDEX idx_meter_room_period ON meter_readings (room_id, billing_period);
CREATE INDEX idx_meter_contract ON meter_readings (contract_id);

CREATE INDEX idx_alerts_reading ON consumption_alerts (meter_reading_id);
CREATE INDEX idx_alerts_status ON consumption_alerts (status);

CREATE INDEX idx_invoices_contract_period ON invoices (contract_id, billing_period);
CREATE INDEX idx_invoices_pay_status ON invoices (payment_status);

CREATE INDEX idx_invoice_items_invoice ON invoice_items (invoice_id);

CREATE INDEX idx_pay_trans_invoice ON payment_transactions (invoice_id);
CREATE INDEX idx_pay_trans_date ON payment_transactions (payment_date);

CREATE INDEX idx_checkout_contract ON checkout_records (contract_id);
