-- ==============================================================================
-- V2: Tạo 17 bảng Core V1 theo thứ tự khóa ngoại hợp lệ
-- ==============================================================================

-- 1. users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100) NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'STAFF', 'TENANT')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. properties
CREATE TABLE properties (
    id BIGSERIAL PRIMARY KEY,
    property_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. floors
CREATE TABLE floors (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    floor_number INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_floors_property_number UNIQUE (property_id, floor_number),
    CONSTRAINT uq_floors_id_property_id UNIQUE (id, property_id)
);

-- 4. rooms
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    floor_id BIGINT NOT NULL,
    room_code VARCHAR(30) NOT NULL,
    base_price NUMERIC(12, 2) NOT NULL CHECK (base_price >= 0),
    max_occupants INT NOT NULL DEFAULT 2 CHECK (max_occupants > 0),
    area_sqm NUMERIC(6, 2) NULL CHECK (area_sqm > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rooms_property_room_code UNIQUE (property_id, room_code),
    CONSTRAINT fk_rooms_floor_property FOREIGN KEY (floor_id, property_id) REFERENCES floors(id, property_id) ON DELETE RESTRICT
);

-- 5. employee_assignments
CREATE TABLE employee_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_employee_assignments UNIQUE (user_id, property_id)
);

-- 6. tenant_profiles
CREATE TABLE tenant_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    full_name VARCHAR(100) NOT NULL,
    identity_number VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NULL,
    date_of_birth DATE NULL,
    hometown VARCHAR(150) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. contracts
CREATE TABLE contracts (
    id BIGSERIAL PRIMARY KEY,
    contract_code VARCHAR(50) NOT NULL UNIQUE,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    primary_tenant_id BIGINT NOT NULL REFERENCES tenant_profiles(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rental_price NUMERIC(12, 2) NOT NULL CHECK (rental_price > 0),
    deposit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (deposit_amount >= 0),
    payment_cycle_day INT NOT NULL DEFAULT 5 CHECK (payment_cycle_day BETWEEN 1 AND 28),
    initial_electric_index NUMERIC(10, 2) NOT NULL CHECK (initial_electric_index >= 0),
    initial_water_index NUMERIC(10, 2) NOT NULL CHECK (initial_water_index >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'TERMINATED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contracts_dates CHECK (start_date < end_date)
);

-- 8. contract_members
CREATE TABLE contract_members (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    identity_number VARCHAR(20) NULL,
    phone VARCHAR(20) NULL,
    relationship VARCHAR(50) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. service_items
CREATE TABLE service_items (
    id BIGSERIAL PRIMARY KEY,
    service_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    service_type VARCHAR(30) NOT NULL CHECK (service_type IN ('ELECTRICITY', 'WATER', 'FIXED_SERVICE')),
    calculation_type VARCHAR(30) NOT NULL CHECK (calculation_type IN ('METER_BASED', 'PER_PERSON', 'PER_ROOM')),
    unit_name VARCHAR(30) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE
);

-- 10. service_price_configs
CREATE TABLE service_price_configs (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    service_item_id BIGINT NOT NULL REFERENCES service_items(id) ON DELETE RESTRICT,
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    effective_from DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_service_price_configs UNIQUE (property_id, service_item_id, effective_from)
);

-- 11. alert_thresholds
CREATE TABLE alert_thresholds (
    id BIGSERIAL PRIMARY KEY,
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    room_id BIGINT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    meter_type VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    threshold_percentage NUMERIC(5, 2) NOT NULL DEFAULT 50.00 CHECK (threshold_percentage > 0),
    calculation_window_months INT NOT NULL DEFAULT 3 CHECK (calculation_window_months BETWEEN 1 AND 12),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. meter_readings
CREATE TABLE meter_readings (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE RESTRICT,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE RESTRICT,
    billing_period VARCHAR(7) NOT NULL CHECK (billing_period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    meter_type VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    old_index NUMERIC(10, 2) NOT NULL CHECK (old_index >= 0),
    new_index NUMERIC(10, 2) NOT NULL CHECK (new_index >= 0),
    consumption NUMERIC(10, 2) NOT NULL CHECK (consumption >= 0),
    reading_date DATE NOT NULL,
    recorded_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    has_anomaly BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_meter_index_order CHECK (new_index >= old_index),
    CONSTRAINT chk_meter_consumption_match CHECK (consumption = (new_index - old_index)),
    CONSTRAINT uq_meter_reading_record UNIQUE (room_id, billing_period, meter_type)
);

-- 13. consumption_alerts
CREATE TABLE consumption_alerts (
    id BIGSERIAL PRIMARY KEY,
    meter_reading_id BIGINT NOT NULL REFERENCES meter_readings(id) ON DELETE CASCADE,
    meter_type VARCHAR(20) NOT NULL CHECK (meter_type IN ('ELECTRICITY', 'WATER')),
    baseline_average NUMERIC(10, 2) NOT NULL,
    current_consumption NUMERIC(10, 2) NOT NULL,
    percentage_increase NUMERIC(6, 2) NOT NULL,
    applied_threshold_percentage NUMERIC(5, 2) NOT NULL,
    alert_severity VARCHAR(20) NOT NULL DEFAULT 'WARNING' CHECK (alert_severity IN ('WARNING', 'CRITICAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'DISMISSED')),
    confirmed_by BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. invoices
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_code VARCHAR(50) NOT NULL UNIQUE,
    contract_id BIGINT NOT NULL REFERENCES contracts(id) ON DELETE RESTRICT,
    billing_period VARCHAR(7) NOT NULL CHECK (billing_period ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    balance_due NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (balance_due >= 0),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ISSUED', 'CANCELLED')),
    has_anomaly_alert BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_contract_period UNIQUE (contract_id, billing_period),
    CONSTRAINT chk_invoices_paid_limit CHECK (paid_amount <= total_amount),
    CONSTRAINT chk_invoices_balance_match CHECK (balance_due = (total_amount - paid_amount)),
    CONSTRAINT chk_invoices_dates CHECK (issue_date <= due_date)
);

-- 15. invoice_items
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    service_item_id BIGINT NULL REFERENCES service_items(id) ON DELETE SET NULL,
    item_name_snapshot VARCHAR(100) NOT NULL,
    unit_price_snapshot NUMERIC(12, 2) NOT NULL CHECK (unit_price_snapshot >= 0),
    quantity NUMERIC(10, 2) NOT NULL CHECK (quantity >= 0),
    old_index_snapshot NUMERIC(10, 2) NULL,
    new_index_snapshot NUMERIC(10, 2) NULL,
    subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    formula_description VARCHAR(255) NULL
);

-- 16. payment_transactions
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    transaction_code VARCHAR(50) NOT NULL UNIQUE,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'BANK_TRANSFER_MANUAL')),
    payment_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes TEXT NULL
);

-- 17. checkout_records
CREATE TABLE checkout_records (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL UNIQUE REFERENCES contracts(id) ON DELETE RESTRICT,
    checkout_date DATE NOT NULL,
    final_electric_index NUMERIC(10, 2) NOT NULL,
    final_water_index NUMERIC(10, 2) NOT NULL,
    final_utility_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    damage_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (damage_amount >= 0),
    unpaid_invoices_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (unpaid_invoices_amount >= 0),
    initial_deposit NUMERIC(12, 2) NOT NULL CHECK (initial_deposit >= 0),
    actual_refund_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED')),
    settled_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
