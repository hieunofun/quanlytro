# Từ điển dữ liệu — 49 bảng V13

Ngày cập nhật: 30/10/2026. Sinh từ metadata PostgreSQL đã thực thi; không tính bảng lịch sử Flyway. Xem [luồng triển khai và kiểm chứng](TRIEN-KHAI-MO-HINH-49-BANG.md).

Quy ước khoảng thời gian: `[từ, đến)`, ngày/thời điểm kết thúc không thuộc khoảng. Tất cả tiền dùng `numeric`; tiền giao dịch dương, chiều tiền nằm ở loại bút toán. Thời điểm dùng `timestamptz`, hiển thị theo Asia/Ho_Chi_Minh.

## 01. `users`

Tài khoản đăng nhập

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `username` | `character varying(50)` | Có | UNIQUE |
| `password_hash` | `character varying(255)` | Có | — |
| `role` | `character varying(20)` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 02. `tenant_profiles`

Hồ sơ người, độc lập với vai trò ký/liên hệ/cư trú

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `user_id` | `bigint` | Không | → `users.id`; UNIQUE |
| `full_name` | `character varying(150)` | Có | — |
| `identity_number` | `character varying(30)` | Không | UNIQUE |
| `phone` | `character varying(30)` | Không | — |
| `email` | `character varying(150)` | Không | — |
| `date_of_birth` | `date` | Không | — |
| `hometown` | `text` | Không | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 03. `properties`

Khu nhà; một chủ nhà trong phạm vi ứng dụng

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `property_code` | `character varying(30)` | Có | UNIQUE |
| `name` | `character varying(150)` | Có | — |
| `address` | `text` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `meter_cutoff_day` | `smallint` | Có | — |
| `invoice_issue_day` | `smallint` | Có | — |
| `collection_timing` | `character varying(20)` | Có | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 04. `rooms`

Phòng và sức chứa; giá/trạng thái được lấy từ nguồn theo thời gian

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `property_id` | `bigint` | Có | → `properties.id`; UNIQUE ghép |
| `floor_number` | `smallint` | Có | — |
| `room_code` | `character varying(30)` | Có | UNIQUE ghép |
| `max_occupants` | `smallint` | Có | — |
| `area_sqm` | `numeric(8,2)` | Không | — |
| `description` | `text` | Có | — |
| `internal_note` | `text` | Có | — |
| `opened_on` | `date` | Có | — |
| `retired_on` | `date` | Không | — |

## 05. `employee_assignments`

Phân công đúng khu hoặc đúng phòng, có hiệu lực

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `user_id` | `bigint` | Có | → `users.id` |
| `property_id` | `bigint` | Không | → `properties.id` |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `assigned_at` | `timestamp with time zone` | Có | — |
| `revoked_at` | `timestamp with time zone` | Không | — |

## 06. `staff_permission_grants`

Ủy quyền hành động và hạn mức trong phân công

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `assignment_id` | `bigint` | Có | UNIQUE ghép; → `employee_assignments.id` |
| `action_code` | `character varying(40)` | Có | UNIQUE ghép |
| `amount_limit` | `numeric(14,2)` | Không | — |
| `expires_at` | `timestamp with time zone` | Không | — |
| `granted_by` | `bigint` | Có | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 07. `password_reset_tokens`

Mã khôi phục băm, dùng một lần

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `user_id` | `bigint` | Có | → `users.id` |
| `token_hash` | `character varying(64)` | Có | UNIQUE |
| `issued_by` | `bigint` | Có | → `users.id` |
| `expires_at` | `timestamp with time zone` | Có | — |
| `used_at` | `timestamp with time zone` | Không | — |
| `attempts` | `smallint` | Có | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 08. `audit_events`

Lịch sử thao tác; không lưu thông tin xác thực

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `actor_user_id` | `bigint` | Không | → `users.id` |
| `actor_type` | `character varying(10)` | Có | — |
| `action` | `character varying(80)` | Có | — |
| `resource_type` | `character varying(80)` | Có | — |
| `resource_id` | `character varying(100)` | Không | — |
| `before_data` | `jsonb` | Không | — |
| `after_data` | `jsonb` | Không | — |
| `reason` | `text` | Không | — |
| `recorded_at` | `timestamp with time zone` | Có | — |

## 09. `room_price_history`

Giá niêm yết; không tự thay giá hợp đồng

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Có | UNIQUE ghép; → `rooms.id` |
| `effective_from` | `date` | Có | UNIQUE ghép |
| `amount` | `numeric(14,2)` | Có | — |
| `recorded_by` | `bigint` | Không | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 10. `room_operating_periods`

Khoảng sẵn sàng, chuẩn bị, bảo trì hoặc ngừng khai thác

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Có | → `rooms.id` |
| `valid_from` | `timestamp with time zone` | Có | — |
| `valid_to` | `timestamp with time zone` | Không | — |
| `status` | `character varying(20)` | Có | — |
| `recorded_by` | `bigint` | Không | → `users.id` |
| `note` | `text` | Không | — |

## 11. `asset_types`

Danh mục tiện nghi và hạng mục tài sản

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `code` | `character varying(40)` | Có | UNIQUE |
| `name` | `character varying(150)` | Có | — |
| `unit_name` | `character varying(30)` | Có | — |

## 12. `assets`

Tài sản có mã hoặc nhóm số lượng

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `asset_type_id` | `bigint` | Có | → `asset_types.id` |
| `asset_code` | `character varying(50)` | Có | UNIQUE |
| `quantity` | `numeric(12,2)` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `note` | `text` | Không | — |

## 13. `asset_placements`

Vị trí tài sản theo thời gian; phòng hoặc khu vực chung

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `asset_id` | `bigint` | Có | → `assets.id` |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `property_id` | `bigint` | Không | → `properties.id` |
| `valid_from` | `timestamp with time zone` | Có | — |
| `valid_to` | `timestamp with time zone` | Không | — |
| `recorded_by` | `bigint` | Không | → `users.id` |

## 14. `reservations`

Giữ chỗ có thời hạn và điều kiện hủy

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Có | → `rooms.id` |
| `tenant_id` | `bigint` | Không | → `tenant_profiles.id` |
| `reservation_code` | `character varying(60)` | Có | UNIQUE |
| `contact_name` | `character varying(150)` | Không | — |
| `contact_phone` | `character varying(30)` | Không | — |
| `start_date` | `date` | Có | — |
| `end_date` | `date` | Có | — |
| `expires_at` | `timestamp with time zone` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `conditions` | `text` | Có | — |
| `created_by` | `bigint` | Không | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 15. `contracts`

Một phòng cố định, liên kết hợp đồng kế tiếp khi chuyển phòng/thay bên thuê

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Có | → `rooms.id` |
| `reservation_id` | `bigint` | Không | → `reservations.id`; UNIQUE |
| `previous_contract_id` | `bigint` | Không | → `contracts.id` |
| `continuation_reason` | `character varying(30)` | Không | — |
| `contract_code` | `character varying(60)` | Có | UNIQUE |
| `start_date` | `date` | Có | — |
| `reconciliation_status` | `character varying(20)` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `created_by` | `bigint` | Không | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 16. `contract_parties`

Vai trò bên ký; không tự coi người ký là người ở

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id`; UNIQUE ghép |
| `tenant_id` | `bigint` | Có | UNIQUE ghép; → `tenant_profiles.id` |
| `party_role` | `character varying(30)` | Có | UNIQUE ghép |
| `effective_from` | `date` | Có | UNIQUE ghép |
| `effective_to` | `date` | Không | — |
| `agreement_note` | `text` | Có | — |

## 17. `contract_contacts`

Liên hệ và quyền được cấp độc lập với bên ký

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `tenant_id` | `bigint` | Có | → `tenant_profiles.id` |
| `contact_role` | `character varying(20)` | Có | — |
| `effective_from` | `date` | Có | — |
| `effective_to` | `date` | Không | — |
| `can_view_billing` | `boolean` | Có | — |
| `authorized_by` | `bigint` | Không | → `users.id` |

## 18. `contract_residencies`

Tất cả người thực ở kể cả người ký nếu có ở

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `tenant_id` | `bigint` | Có | → `tenant_profiles.id` |
| `effective_from` | `date` | Có | — |
| `effective_to` | `date` | Không | — |
| `relationship` | `character varying(50)` | Không | — |
| `recorded_by` | `bigint` | Không | → `users.id` |

## 19. `contract_terms`

Bản điều kiện và phụ lục đã duyệt có hiệu lực, không sửa đè

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id`; UNIQUE ghép |
| `version_no` | `integer` | Có | UNIQUE ghép |
| `effective_from` | `date` | Có | — |
| `lease_end` | `date` | Có | — |
| `rental_price` | `numeric(14,2)` | Có | — |
| `deposit_required` | `numeric(14,2)` | Có | — |
| `payment_day` | `smallint` | Có | — |
| `collection_timing` | `character varying(20)` | Có | — |
| `terms` | `text` | Có | — |
| `signed_at` | `timestamp with time zone` | Không | — |
| `approved_by` | `bigint` | Không | → `users.id` |
| `origin` | `character varying(20)` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 20. `room_possessions`

Quyền sử dụng thực tế không tự kết thúc khi hết hạn giấy

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `taken_at` | `timestamp with time zone` | Có | — |
| `returned_at` | `timestamp with time zone` | Không | — |
| `recorded_by` | `bigint` | Không | → `users.id` |
| `note` | `text` | Không | — |

## 21. `move_out_notices`

Đề nghị trả, thay/hủy không tự giải phóng phòng

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `supersedes_notice_id` | `bigint` | Không | → `move_out_notices.id` |
| `requested_date` | `date` | Có | — |
| `reason` | `text` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `submitted_by` | `bigint` | Có | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 22. `handovers`

Biên bản nhận/trả/giao trách nhiệm

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `possession_id` | `bigint` | Không | → `room_possessions.id` |
| `handover_code` | `character varying(60)` | Có | UNIQUE |
| `handover_type` | `character varying(20)` | Có | — |
| `occurred_at` | `timestamp with time zone` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `given_by` | `bigint` | Không | → `users.id` |
| `received_by_tenant_id` | `bigint` | Không | → `tenant_profiles.id` |
| `confirmed_by` | `bigint` | Không | → `users.id` |
| `confirmed_at` | `timestamp with time zone` | Không | — |
| `note` | `text` | Không | — |

## 23. `handover_items`

Snapshot đồ vật và tình trạng lúc giao

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `handover_id` | `bigint` | Có | → `handovers.id`; UNIQUE ghép |
| `asset_placement_id` | `bigint` | Không | → `asset_placements.id` |
| `line_no` | `integer` | Có | UNIQUE ghép |
| `item_name_snapshot` | `character varying(150)` | Có | — |
| `quantity` | `numeric(12,2)` | Có | — |
| `condition_note` | `text` | Có | — |
| `note` | `text` | Không | — |

## 24. `contract_violations`

Vi phạm và kết quả xử lý, không tự sinh khoản phạt

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `violation_type` | `character varying(50)` | Có | — |
| `description` | `text` | Có | — |
| `occurred_on` | `date` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `resolution_note` | `text` | Không | — |
| `created_by` | `bigint` | Có | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 25. `checkout_records`

Snapshot quyết toán trước cấn, không coi tính ra là đã hoàn

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `return_handover_id` | `bigint` | Không | → `handovers.id` |
| `billing_end` | `date` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `deposit_snapshot` | `numeric(14,2)` | Có | — |
| `advance_snapshot` | `numeric(14,2)` | Có | — |
| `outstanding_snapshot` | `numeric(14,2)` | Có | — |
| `approved_by` | `bigint` | Không | → `users.id` |
| `approved_at` | `timestamp with time zone` | Không | — |
| `notes` | `text` | Không | — |

## 26. `service_items`

Danh mục dịch vụ; không thay đăng ký dịch vụ

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `service_code` | `character varying(30)` | Có | UNIQUE |
| `name` | `character varying(100)` | Có | — |
| `unit_name` | `character varying(30)` | Có | — |
| `calculation_type` | `character varying(20)` | Có | — |
| `is_mandatory` | `boolean` | Có | — |
| `is_active` | `boolean` | Có | — |

## 27. `service_price_configs`

Lịch sử giá dịch vụ tại khu

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `property_id` | `bigint` | Có | → `properties.id`; UNIQUE ghép |
| `service_item_id` | `bigint` | Có | UNIQUE ghép; → `service_items.id` |
| `effective_from` | `date` | Có | UNIQUE ghép |
| `unit_price` | `numeric(14,2)` | Có | — |
| `recorded_by` | `bigint` | Không | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 28. `contract_service_subscriptions`

Đăng ký và số lượng theo khoảng, chế độ cả kỳ/chia ngày

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Có | → `contracts.id` |
| `service_item_id` | `bigint` | Có | → `service_items.id` |
| `effective_from` | `date` | Có | — |
| `effective_to` | `date` | Không | — |
| `quantity` | `integer` | Có | — |
| `charge_mode` | `character varying(20)` | Có | — |
| `recorded_by` | `bigint` | Không | → `users.id` |

## 29. `meters`

Công tơ vật lý; thay tạo mã mới

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Có | → `rooms.id` |
| `service_item_id` | `bigint` | Có | → `service_items.id` |
| `meter_code` | `character varying(60)` | Có | UNIQUE |
| `installed_at` | `timestamp with time zone` | Có | — |
| `retired_at` | `timestamp with time zone` | Không | — |
| `recorded_by` | `bigint` | Không | → `users.id` |

## 30. `meter_readings`

Điểm đo thực và chuỗi phiên bản sửa

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `correction_item_id` | `bigint` | Không | → `billing_items.id`; UNIQUE |
| `meter_id` | `bigint` | Có | → `meters.id`; UNIQUE ghép |
| `observed_at` | `timestamp with time zone` | Có | UNIQUE ghép |
| `reading_value` | `numeric(14,3)` | Có | — |
| `time_precision` | `character varying(30)` | Có | — |
| `revision_no` | `integer` | Có | UNIQUE ghép |
| `supersedes_reading_id` | `bigint` | Không | → `meter_readings.id`; UNIQUE |
| `handover_id` | `bigint` | Không | → `handovers.id` |
| `recorded_by` | `bigint` | Có | → `users.id` |
| `reason` | `text` | Không | — |
| `recorded_at` | `timestamp with time zone` | Có | — |

## 31. `consumption_segments`

Khoảng đo giao trách nhiệm cho hợp đồng hoặc chủ nhà

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `start_reading_id` | `bigint` | Có | UNIQUE ghép; → `meter_readings.id` |
| `end_reading_id` | `bigint` | Có | → `meter_readings.id`; UNIQUE ghép |
| `contract_id` | `bigint` | Không | → `contracts.id` |
| `responsibility` | `character varying(10)` | Có | — |
| `supersedes_segment_id` | `bigint` | Không | → `consumption_segments.id`; UNIQUE |
| `recorded_by` | `bigint` | Không | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 32. `alert_policies`

Ngưỡng theo khu/phòng và dịch vụ, có hiệu lực

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `service_item_id` | `bigint` | Có | → `service_items.id` |
| `property_id` | `bigint` | Không | → `properties.id` |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `effective_from` | `date` | Có | — |
| `history_months` | `smallint` | Có | — |
| `minimum_days` | `integer` | Có | — |
| `threshold_percentage` | `numeric(9,2)` | Có | — |
| `absolute_daily_threshold` | `numeric(14,3)` | Không | — |

## 33. `consumption_alerts`

Lần đánh giá, baseline và xác nhận có căn cứ

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `segment_id` | `bigint` | Có | → `consumption_segments.id`; UNIQUE ghép |
| `policy_id` | `bigint` | Có | → `alert_policies.id`; UNIQUE ghép |
| `supersedes_alert_id` | `bigint` | Không | → `consumption_alerts.id`; UNIQUE |
| `baseline_daily_average` | `numeric(18,6)` | Không | — |
| `applied_threshold` | `numeric(9,2)` | Có | — |
| `method` | `character varying(40)` | Có | — |
| `status` | `character varying(30)` | Có | — |
| `context_note` | `text` | Không | — |
| `checked_by` | `bigint` | Không | → `users.id` |
| `checked_at` | `timestamp with time zone` | Không | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 34. `alert_baseline_sources`

Khoảng nào đã được dùng làm baseline

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `alert_id` | `bigint` | Có | → `consumption_alerts.id`; UNIQUE ghép |
| `source_segment_id` | `bigint` | Có | UNIQUE ghép; → `consumption_segments.id` |

## 35. `maintenance_schedules`

Lịch định kỳ, không ghi đè lịch sử lần đã làm

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `asset_id` | `bigint` | Không | → `assets.id` |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `property_id` | `bigint` | Không | → `properties.id` |
| `title` | `character varying(150)` | Có | — |
| `interval_days` | `integer` | Có | — |
| `next_due` | `date` | Có | — |
| `is_active` | `boolean` | Có | — |
| `created_by` | `bigint` | Không | → `users.id` |

## 36. `maintenance_requests`

Sự cố theo vị trí, không bắt có hợp đồng

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `property_id` | `bigint` | Không | → `properties.id` |
| `asset_placement_id` | `bigint` | Không | → `asset_placements.id` |
| `schedule_id` | `bigint` | Không | → `maintenance_schedules.id`; UNIQUE ghép |
| `scheduled_for` | `date` | Không | UNIQUE ghép |
| `submitted_by` | `bigint` | Có | → `users.id` |
| `assigned_to` | `bigint` | Không | → `users.id` |
| `title` | `character varying(150)` | Có | — |
| `description` | `text` | Có | — |
| `priority` | `character varying(20)` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `incident_type` | `character varying(50)` | Có | — |
| `expected_date` | `date` | Có | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 37. `maintenance_events`

Từng lần xử lý/giao việc/hoàn thành/tái phát

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `request_id` | `bigint` | Có | → `maintenance_requests.id` |
| `action` | `character varying(30)` | Có | — |
| `actor_id` | `bigint` | Có | → `users.id` |
| `note` | `text` | Có | — |
| `recorded_at` | `timestamp with time zone` | Có | — |

## 38. `maintenance_costs`

Chi phí thực tế, không đồng nhất bồi thường

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `request_id` | `bigint` | Có | → `maintenance_requests.id` |
| `description` | `text` | Có | — |
| `amount` | `numeric(14,2)` | Có | — |
| `incurred_on` | `date` | Có | — |
| `advanced_by_tenant_id` | `bigint` | Không | → `tenant_profiles.id` |
| `recorded_by` | `bigint` | Có | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 39. `billing_documents`

Đầu chứng từ tháng/phát sinh/bồi thường/điều chỉnh/phải hoàn

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `contract_id` | `bigint` | Không | → `contracts.id` |
| `reservation_id` | `bigint` | Không | → `reservations.id` |
| `checkout_id` | `bigint` | Không | → `checkout_records.id` |
| `related_document_id` | `bigint` | Không | → `billing_documents.id` |
| `document_code` | `character varying(80)` | Có | UNIQUE |
| `document_type` | `character varying(30)` | Có | — |
| `billing_period` | `date` | Không | — |
| `issue_date` | `date` | Có | — |
| `due_date` | `date` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `reason` | `text` | Có | — |
| `created_by` | `bigint` | Không | → `users.id` |
| `issued_by` | `bigint` | Không | → `users.id` |
| `issued_at` | `timestamp with time zone` | Không | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 40. `billing_items`

Dòng tiền có nguồn, kỳ riêng và snapshot cần giữ

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `document_id` | `bigint` | Có | → `billing_documents.id`; UNIQUE ghép |
| `line_no` | `integer` | Có | UNIQUE ghép |
| `item_kind` | `character varying(30)` | Có | — |
| `direction` | `character varying(10)` | Có | — |
| `adjusts_item_id` | `bigint` | Không | → `billing_items.id` |
| `contract_term_id` | `bigint` | Không | → `contract_terms.id` |
| `subscription_id` | `bigint` | Không | → `contract_service_subscriptions.id` |
| `segment_id` | `bigint` | Không | → `consumption_segments.id` |
| `service_price_config_id` | `bigint` | Không | → `service_price_configs.id` |
| `handover_item_id` | `bigint` | Không | → `handover_items.id` |
| `maintenance_cost_id` | `bigint` | Không | → `maintenance_costs.id` |
| `violation_id` | `bigint` | Không | → `contract_violations.id` |
| `item_name_snapshot` | `character varying(150)` | Có | — |
| `unit_name_snapshot` | `character varying(40)` | Có | — |
| `unit_price_snapshot` | `numeric(14,4)` | Có | — |
| `calculation_basis` | `character varying(20)` | Có | — |
| `quantity_snapshot` | `numeric(18,6)` | Không | — |
| `calculation_period` | `date` | Không | — |
| `service_from` | `date` | Có | — |
| `service_to` | `date` | Có | — |
| `verification_status` | `character varying(20)` | Có | — |
| `basis_note` | `text` | Có | — |
| `source_key` | `character varying(120)` | Không | UNIQUE |

## 41. `cash_accounts`

Quỹ/tài khoản chủ nhà, không phải quỹ tiền khách

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `property_id` | `bigint` | Không | → `properties.id` |
| `account_code` | `character varying(60)` | Có | UNIQUE |
| `name` | `character varying(150)` | Có | — |
| `account_type` | `character varying(20)` | Có | — |
| `is_active` | `boolean` | Có | — |

## 42. `cash_transactions`

Phiếu thu/chi tiền thật, chưa xác minh không ghi sổ

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `payment_document_id` | `bigint` | Không | → `billing_documents.id` |
| `cash_account_id` | `bigint` | Có | → `cash_accounts.id` |
| `payer_or_payee_id` | `bigint` | Không | → `tenant_profiles.id` |
| `transaction_code` | `character varying(80)` | Có | UNIQUE |
| `direction` | `character varying(5)` | Có | — |
| `amount` | `numeric(14,2)` | Có | — |
| `method` | `character varying(30)` | Có | — |
| `occurred_at` | `timestamp with time zone` | Có | — |
| `recorded_at` | `timestamp with time zone` | Có | — |
| `status` | `character varying(20)` | Có | — |
| `external_reference` | `character varying(150)` | Không | — |
| `counterparty_note` | `text` | Không | — |
| `reason` | `text` | Không | — |
| `recorded_by` | `bigint` | Có | → `users.id` |
| `confirmed_by` | `bigint` | Không | → `users.id` |
| `confirmed_at` | `timestamp with time zone` | Không | — |

## 43. `fund_accounts`

Cọc/trả trước/giữ chỗ/chưa phân bổ, không lưu balance chỉnh tay

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `fund_code` | `character varying(100)` | Có | UNIQUE |
| `fund_type` | `character varying(20)` | Có | — |
| `reconciliation_status` | `character varying(20)` | Có | — |
| `contract_id` | `bigint` | Không | → `contracts.id` |
| `reservation_id` | `bigint` | Không | → `reservations.id` |
| `unapplied_receipt_id` | `bigint` | Không | → `cash_transactions.id`; UNIQUE |
| `beneficiary_tenant_id` | `bigint` | Không | → `tenant_profiles.id` |
| `created_at` | `timestamp with time zone` | Có | — |

## 44. `fund_entries`

Sổ tác động quỹ và phân bổ, số tiền luôn dương

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `entry_code` | `character varying(120)` | Có | UNIQUE |
| `entry_type` | `character varying(30)` | Có | — |
| `cash_transaction_id` | `bigint` | Không | → `cash_transactions.id` |
| `from_fund_id` | `bigint` | Không | → `fund_accounts.id` |
| `to_fund_id` | `bigint` | Không | → `fund_accounts.id` |
| `debit_item_id` | `bigint` | Không | → `billing_items.id` |
| `credit_item_id` | `bigint` | Không | → `billing_items.id` |
| `original_entry_id` | `bigint` | Không | → `fund_entries.id` |
| `amount` | `numeric(14,2)` | Có | — |
| `reason` | `text` | Có | — |
| `posted_by` | `bigint` | Có | → `users.id` |
| `posted_at` | `timestamp with time zone` | Có | — |

## 45. `fund_holds`

Tạm giữ có căn cứ và mốc rà lại, không là khoản đã cấn

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `fund_account_id` | `bigint` | Có | → `fund_accounts.id` |
| `related_billing_item_id` | `bigint` | Không | → `billing_items.id` |
| `amount` | `numeric(14,2)` | Có | — |
| `reason` | `text` | Có | — |
| `review_on` | `date` | Có | — |
| `created_by` | `bigint` | Có | → `users.id` |
| `released_at` | `timestamp with time zone` | Không | — |
| `created_at` | `timestamp with time zone` | Có | — |

## 46. `notifications`

Sự kiện thông báo có nguồn và khóa chống lặp

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `notification_type` | `character varying(40)` | Có | — |
| `title` | `character varying(200)` | Có | — |
| `body` | `text` | Có | — |
| `billing_document_id` | `bigint` | Không | → `billing_documents.id` |
| `contract_id` | `bigint` | Không | → `contracts.id` |
| `maintenance_request_id` | `bigint` | Không | → `maintenance_requests.id` |
| `consumption_alert_id` | `bigint` | Không | → `consumption_alerts.id` |
| `deduplication_key` | `character varying(180)` | Có | UNIQUE |
| `created_by` | `bigint` | Không | → `users.id` |
| `sent_at` | `timestamp with time zone` | Có | — |

## 47. `notification_recipients`

Trạng thái đọc riêng từng tài khoản

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `notification_id` | `bigint` | Có | → `notifications.id`; UNIQUE ghép |
| `user_id` | `bigint` | Có | UNIQUE ghép; → `users.id` |
| `read_at` | `timestamp with time zone` | Không | — |

## 48. `stored_files`

Tệp và metadata, không chứa file thay đổi dưới cùng key

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `storage_key` | `character varying(255)` | Có | UNIQUE |
| `original_name` | `character varying(255)` | Có | — |
| `content_type` | `character varying(100)` | Có | — |
| `size_bytes` | `bigint` | Không | — |
| `sha256` | `character varying(64)` | Không | — |
| `external_url` | `text` | Không | — |
| `uploaded_by` | `bigint` | Có | → `users.id` |
| `uploaded_at` | `timestamp with time zone` | Có | — |

## 49. `file_attachments`

Một liên kết tới một đối tượng bằng FK thật

| Trường | Kiểu | Bắt buộc | Khóa / tham chiếu |
| --- | --- | --- | --- |
| `id` | `bigint` | Có | PK |
| `file_id` | `bigint` | Có | → `stored_files.id` |
| `room_id` | `bigint` | Không | → `rooms.id` |
| `asset_id` | `bigint` | Không | → `assets.id` |
| `handover_item_id` | `bigint` | Không | → `handover_items.id` |
| `meter_reading_id` | `bigint` | Không | → `meter_readings.id` |
| `maintenance_request_id` | `bigint` | Không | → `maintenance_requests.id` |
| `maintenance_cost_id` | `bigint` | Không | → `maintenance_costs.id` |
| `contract_term_id` | `bigint` | Không | → `contract_terms.id` |
| `billing_document_id` | `bigint` | Không | → `billing_documents.id` |
| `violation_id` | `bigint` | Không | → `contract_violations.id` |
| `fund_entry_id` | `bigint` | Không | → `fund_entries.id` |
| `caption` | `text` | Có | — |
| `sort_order` | `integer` | Có | — |
| `attached_by` | `bigint` | Có | → `users.id` |
| `created_at` | `timestamp with time zone` | Có | — |
