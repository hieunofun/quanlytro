CREATE TABLE room_employee_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(user_id,room_id)
);
CREATE TABLE contract_violations (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','HANDLING','RESOLVED','DISMISSED')),
    resolution_note TEXT,
    recorded_by BIGINT NOT NULL REFERENCES users(id),
    resolved_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE uploaded_images (
    id UUID PRIMARY KEY,
    room_id BIGINT REFERENCES rooms(id),
    maintenance_request_id BIGINT REFERENCES maintenance_requests(id),
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    extension VARCHAR(5) NOT NULL CHECK(extension IN ('png','jpg')),
    size_bytes BIGINT NOT NULL CHECK(size_bytes>0),
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK((room_id IS NOT NULL)::int+(maintenance_request_id IS NOT NULL)::int=1)
);
CREATE INDEX idx_room_assignment_user ON room_employee_assignments(user_id,is_active);
CREATE INDEX idx_violation_contract ON contract_violations(contract_id,status);
ALTER TABLE invoices ADD COLUMN is_settlement BOOLEAN NOT NULL DEFAULT FALSE;
