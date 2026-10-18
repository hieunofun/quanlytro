# API MVP — mô hình 17 bảng (Flyway V10)

Base URL `/api`, JSON UTF-8. Gửi `Authorization: Bearer <accessToken>` trừ health/login/refresh. Lỗi JSON `{ "message": "..." }`: 400 dữ liệu/quy tắc, 401 chưa xác thực, 403 ngoài quyền, 404 không tìm thấy, 409 vi phạm ràng buộc, 410 chức năng prototype đã ngừng.

ADMIN quản lý toàn hệ thống. STAFF chỉ quản lý khu có `employee_assignments.is_active=true`. TENANT chỉ đọc hợp đồng mình đại diện, hóa đơn đã phát hành và sự cố thuộc hợp đồng đó. Hồ sơ thành viên không tự cấp quyền đăng nhập.

| Nhóm | Endpoint | Quyền / nội dung |
|---|---|---|
| Trạng thái | GET /health | `status=UP`, `schemaVersion=17` |
| Đăng nhập | POST /auth/login | username, password; trả accessToken, refreshToken, expiresIn, user |
| Phiên | POST /auth/refresh; GET /auth/me; POST /auth/logout | Refresh xoay một lần; logout thu hồi mọi phiên của tài khoản |
| Mật khẩu | POST /auth/change-password | oldPassword, newPassword (12–72 ký tự) |
| Tài khoản | GET/POST /users; PUT /users/{id}/status | ADMIN; tạo bằng username, password, role; status ACTIVE/LOCKED/INACTIVE |
| Phân công | GET/POST /assignments; DELETE /assignments/{id} | ADMIN; userId, propertyId; DELETE thu hồi phân công |
| Khu | GET/POST /properties; PUT /properties/{id}; PUT /properties/{id}/status | ADMIN ghi, STAFF đọc khu được giao; code, name, address |
| Chính sách | PUT /properties/{id}/billing-policy | ADMIN: meterCutoffDay (1–31), invoiceIssueDay (1–28) |
| Phòng | GET/POST /rooms; GET/PUT/DELETE /rooms/{id} | Tạo/xóa ADMIN; sửa ADMIN/STAFF; propertyId, floorNumber, code, price, maxOccupants, area |
| Khai thác | PUT /rooms/{id}/status | status AVAILABLE/MAINTENANCE; trạng thái RENTED được tính từ hợp đồng |
| Tầng | GET /floors?propertyId=... | Danh sách suy ra từ floor_number; không có bảng tầng / API tạo tầng |
| Người thuê | GET/POST /tenants; PUT /tenants/{id}; PUT /tenants/{id}/profile | ADMIN tiếp nhận/sửa hồ sơ; STAFF đọc hồ sơ gắn khu; TENANT đọc hồ sơ của mình |
| Hồ sơ | POST/PUT /tenants | fullName; userId, identityNumber, phone, email tùy chọn; profile: dateOfBirth, hometown |
| Hợp đồng | GET/POST /contracts; GET /contracts/{id} | ADMIN/STAFF ghi; code, roomId, tenantId, startDate, endDate, rentalPrice, deposit, paymentDay |
| Thành viên | POST /contracts/{id}/members | tenantId (hồ sơ đã có), relationship, effectiveFrom; không gửi bản sao họ tên/CCCD |
| Rời phòng | POST /contracts/{id}/members/{memberId}/depart | effectiveTo; chỉ một lần, không ảnh hưởng hóa đơn đã lập |
| Gia hạn | POST /contracts/{id}/renew | endDate; chỉ hợp đồng chưa có hóa đơn phát hành vì điều khoản đã chốt được bảo vệ |
| Điều khoản | PUT /contracts/{id}/notes | terms |
| Lịch tính tiền | GET /contracts/{id}/billing-calendar?period=YYYY-MM | cutoffDate, issueDate, dueDate |
| Trả phòng | POST /contracts/{id}/checkout; GET /contracts/{id}/checkout | date, electricIndex, waterIndex, damage, notes |
| Dịch vụ | GET/POST /services | ADMIN ghi: code, name, calculationType, unitName; danh mục điện ELEC, nước WATER |
| Giá | GET/POST /prices | ADMIN ghi: propertyId, serviceId, unitPrice, effectiveFrom; STAFF chỉ đọc khu được giao |
| Ngưỡng | GET/POST /thresholds | ADMIN ghi: propertyId, percentage, months (1–12); một cấu hình chung mỗi khu |
| Chỉ số | GET/POST /meter-readings | ADMIN/STAFF: contractId, period YYYY-MM, meterType ELECTRICITY/WATER, newIndex, readingDate, oldIndex |
| Chốt sớm | POST /meter-readings/settlement | Cùng body ghi chỉ số; dùng để chốt trước ngày chốt tháng khi trả phòng |
| Sửa chỉ số | PUT /meter-readings/{id} | newIndex, reason; chỉ bản ghi cuối chưa gắn hóa đơn |
| Cảnh báo | GET /alerts; PUT /alerts/{id} | ADMIN/STAFF: status CONFIRMED/DISMISSED, note |
| Hóa đơn | GET/POST /invoices; GET/DELETE /invoices/{id} | Tạo DRAFT bằng contractId, period; issueDate/dueDate nếu gửi phải đúng lịch; chỉ xóa nháp |
| Phát hành | POST /invoices/{id}/issue | Đúng ngày, đủ dòng/đơn giá, không còn cảnh báo PENDING |
| Thanh toán | POST /invoices/{id}/payments; GET /payments | ADMIN/STAFF: code, amount > 0, method CASH/BANK_TRANSFER_MANUAL/OTHER, notes |
| Sự cố | GET/POST /maintenance-requests; GET /maintenance-requests/{id} | contractId, title, description, priority URGENT/NORMAL/LOW, imageUrls[] tùy chọn |
| Tiến độ | PUT /maintenance-requests/{id} | ADMIN/STAFF: status, assignedTo, expectedDate, actualCost, note |
| Ảnh sự cố | POST /maintenance-requests/{id}/images | JSON `{ "url": "https://..." }` hoặc multipart field file; tối đa 12 ảnh |
| Xóa ảnh | DELETE /maintenance-requests/{id}/images/{imageId} | Đúng phạm vi hợp đồng/khu; gỡ bản ghi ảnh |
| Tải ảnh | GET /uploads/{uuid}.png hoặc .jpg | Có xác thực và quyền trên sự cố; không công khai thư mục upload |
| Người xử lý | GET /rooms/{id}/assignees; GET /properties/{id}/assignees | ADMIN/STAFF, danh sách theo khu được giao |
| Báo cáo | GET /dashboard?period=YYYY-MM; GET /reports/revenue; GET /reports/debts | ADMIN/STAFF theo khu; công nợ từ view, tiền mới thu không gồm cấn cọc |
| Báo cáo | GET /reports/revenue-quarterly; /reports/occupancy; /reports/occupancy-history; /reports/maintenance | ADMIN/STAFF theo khu |
| Xuất | GET /invoices/{id}/pdf; GET /contracts/{id}/pdf; GET /reports/pdf; GET /reports/debts.csv | Kiểm tra quyền trước khi xuất |

## Các hợp đồng dữ liệu cần chú ý

- Request kỳ dùng YYYY-MM; response `billing_period` là ngày đầu tháng YYYY-MM-01. Không lưu kỳ dạng chuỗi trong DB.
- Lần ghi đầu tiên của từng hợp đồng/đồng hồ bắt buộc gửi `oldIndex` từ biên bản bàn giao. Các lần sau suy ra từ chỉ số trước; nếu gửi lại phải khớp. Không giả định chỉ số ban đầu bằng 0.
- Hóa đơn lấy tổng tiền/công nợ từ `v_invoice_lines`, `v_invoice_balances`. Các field hiển thị `subtotal`, `payment_status`, `quantity` trên API là dữ liệu tính toán, không phải các cột lưu lặp.
- Cảnh báo chỉ tạo khi đủ N tháng lịch sử và trung bình > 0. Ngưỡng và trung bình đã áp dụng được giữ tại lần đánh giá.
- Chốt sớm: ghi/chỉnh chỉ số kỳ cuối → kiểm tra cảnh báo → phát hành/quyết toán. Giao dịch trả phòng rollback toàn bộ nếu còn cảnh báo hoặc thiếu hóa đơn tháng trước.
- Quyết toán: `settlement_amount = deposit_snapshot - damage_amount - outstanding_snapshot`. Số âm là cần thu thêm. `DEPOSIT_OFFSET` chỉ được tạo bên trong luồng quyết toán, không nhận từ endpoint thu tiền thông thường.
- `GET /contracts/{id}` trả `members`, `checkout`; các alias `initial_deposit`, `unpaid_invoices_amount`, `actual_refund_amount` được tính để giữ tương thích phần hiển thị. Chỉ số cuối truy ra từ meter_readings; không lưu lại trong checkout_records.
- Dịch vụ tháng dùng ngày/ngày-người nguyên; giá áp dụng là mốc gần nhất không sau ngày đầu kỳ. Giữ snapshot tên, đơn vị, đơn giá; không lưu số tiền tổng hợp.
- Tên hiển thị của tài khoản quản lý là username; thông tin cá nhân người thuê nằm trong tenant_profiles.

## Phiên và phạm vi MVP

Refresh token, danh sách thu hồi phiên và đếm lần đăng nhập sai nằm trong bộ nhớ của **một tiến trình**. Khởi động lại server yêu cầu đăng nhập lại; mật khẩu thay đổi và tài khoản khóa vô hiệu phiên cũ. Sau 5 lần sai, khóa đăng nhập tạm thời 15 phút. Triển khai nhiều instance cần kho phiên dùng chung; bản này không tuyên bố hỗ trợ cluster.

Các API prototype về phân công từng phòng, ảnh phòng, vi phạm hợp đồng, phí chậm trả, lịch bảo trì định kỳ, hộp thông báo, nhật ký DB và reset-code nằm ngoài MVP. Route cũ tương ứng trả 410. Dữ liệu trước chuyển đổi vẫn có trong schema prototype_v9; thao tác nghiệp vụ mới ghi log mã thao tác vào log ứng dụng, không ghi payload/mật khẩu.
