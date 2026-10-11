-- ==============================================================================
-- V4: Seed danh mục dịch vụ tham chiếu chuẩn (Không seed tài khoản Admin)
-- ==============================================================================

INSERT INTO service_items (service_code, name, service_type, calculation_type, unit_name, is_mandatory) VALUES
('ELEC', 'Điện sinh hoạt', 'ELECTRICITY', 'METER_BASED', 'kWh', true),
('WATER', 'Nước sinh hoạt', 'WATER', 'METER_BASED', 'm3', true),
('WIFI', 'Internet Wifi', 'FIXED_SERVICE', 'PER_ROOM', 'Tháng', false),
('TRASH', 'Thu gom rác sinh hoạt', 'FIXED_SERVICE', 'PER_ROOM', 'Tháng', true),
('PARKING', 'Gửi xe máy', 'FIXED_SERVICE', 'PER_PERSON', 'Chiếc/Tháng', false)
ON CONFLICT (service_code) DO NOTHING;
