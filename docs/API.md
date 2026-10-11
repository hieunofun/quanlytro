# API V1

URL `/api`; JSON UTF-8. Ngoại trừ health/login/refresh/reset-password, gửi `Authorization: Bearer <accessToken>`. Lỗi trả JSON `{ "message": "..." }`: 400 sai dữ liệu/quy tắc, 401 chưa xác thực, 403 ngoài quyền, 404 không tồn tại, 409 trùng/ràng buộc database.

`GET /contracts/{id}` có thêm `checkout` (null nếu chưa trả phòng). Quyết toán chứa cọc ban đầu, công nợ trước cấn cọc, thiệt hại, chỉ số cuối, điện nước kỳ cuối và số hoàn cọc. `actual_refund_amount<0` nghĩa là cần thu thêm. Điện nước đã nằm trong công nợ hóa đơn, không khấu trừ lần nữa.

| Nhóm | Endpoint | Quyền / chức năng |
|---|---|---|
| Auth | POST /auth/login | username, password; trả accessToken, refreshToken, user |
| Auth | POST /auth/refresh | refreshToken; xoay token, dùng một lần |
| Auth | GET /auth/me; POST /auth/logout | thông tin / thu hồi các phiên của tài khoản |
| Auth | POST /auth/change-password | oldPassword, newPassword |
| Auth | POST /users/{id}/reset-code | ADMIN cấp mã sau khi xác minh |
| Auth | POST /auth/reset-password | code, newPassword |
| Tài khoản | GET/POST /users; PUT /users/{id}/status | ADMIN; status ACTIVE/LOCKED/INACTIVE |
| Phân công | GET/POST /assignments | ADMIN; userId, propertyId, roomId tùy chọn (null = toàn khu), active |
| Khu | GET/POST /properties; PUT /properties/{id} | STAFF đọc khu chứa phòng được giao, ADMIN tạo/sửa |
| Chính sách | PUT /properties/{id}/billing-policy | ADMIN: meterCutoffDay, invoiceIssueDay, lateFeeEnabled, lateFeePercentage |
| Nhân viên | GET /properties/{id}/assignees; GET /rooms/{id}/assignees | người xử lý được phép tại khu/phòng |
| Tầng | GET/POST /floors | propertyId, number, name |
| Phòng | GET/POST /rooms; PUT /rooms/{id} | propertyId, floorId, code, price, maxOccupants, area |
| Phòng | PUT /rooms/{id}/details | status, amenities, internalNote, imageUrls[] |
| Phòng | GET /rooms/{id}/images; GET /rooms/{id}/price-history | ảnh / lịch sử giá; TENANT không được xem ghi chú/lịch sử giá |
| Người thuê | GET/POST /tenants; PUT /tenants/{id} | userId tùy chọn, fullName, identityNumber, phone, email |
| Hồ sơ | PUT /tenants/{id}/profile | dateOfBirth, hometown |
| Hợp đồng | GET/POST /contracts; GET /contracts/{id} | code, roomId, tenantId, startDate, endDate, rentalPrice, deposit, paymentDay, electricIndex, waterIndex |
| Thành viên | POST /contracts/{id}/members | fullName, identityNumber, phone, relationship, effectiveFrom |
| Rời phòng | POST /contracts/{id}/members/{memberId}/depart | effectiveTo; hợp đồng ACTIVE, chỉ ghi nhận một lần; không thay dữ liệu đã tính tiền |
| Gia hạn | POST /contracts/{id}/renew | endDate |
| Điều khoản | PUT /contracts/{id}/notes | terms, violationNote |
| Trả phòng | POST /contracts/{id}/checkout; GET /contracts/{id}/checkout | date, electricIndex, waterIndex, damage, notes |
| Dịch vụ | GET/POST /services; GET/POST /prices | thêm dịch vụ: ADMIN, code/name/calculationType PER_ROOM hoặc PER_PERSON/unitName; giá: propertyId, serviceId, unitPrice, effectiveFrom |
| Ngưỡng | GET/POST /thresholds | propertyId, roomId tùy chọn, meterType, percentage, months |
| Chỉ số | GET/POST /meter-readings | contractId, period YYYY-MM, meterType ELECTRICITY/WATER, newIndex, readingDate; duy nhất theo hợp đồng/kỳ/loại |
| Sửa chỉ số | PUT /meter-readings/{id}; GET /meter-readings/{id}/history | newIndex, reason |
| Cảnh báo | GET /alerts; PUT /alerts/{id} | status CONFIRMED/DISMISSED, note |
| Hóa đơn | GET/POST /invoices; GET /invoices/{id}; DELETE /invoices/{id} | tạo nháp: contractId, period; issueDate/dueDate tùy chọn phải khớp lịch; chỉ xóa nháp |
| Phát hành | POST /invoices/{id}/issue | chặn khi có cảnh báo chờ hoặc chưa đến issue_date |
| Thanh toán | POST /invoices/{id}/payments; GET /payments | code, amount, method CASH/BANK_TRANSFER_MANUAL/OTHER, notes |
| Sự cố | GET/POST /maintenance-requests | contractId, title, description, category, priority, imageUrls[] |
| Xử lý | PUT /maintenance-requests/{id} | status, assignedTo, expectedDate, actualCost, note |
| Bảo trì | GET/POST /maintenance-schedules; POST /maintenance-schedules/{id}/complete | propertyId, title, nextDate, intervalDays, notes |
| Thông báo | GET /notifications; PUT /notifications/{id}/read | dữ liệu riêng người đăng nhập |
| Thông báo chung | POST /notifications/announce | ADMIN; message |
| Dashboard | GET /dashboard?period=YYYY-MM | số phòng, hóa đơn, tiền đã thu, nợ, sự cố, hợp đồng sắp hết hạn |
| Báo cáo | GET /reports/revenue, /reports/revenue-quarterly, /reports/debts, /reports/occupancy, /reports/occupancy-history, /reports/maintenance | STAFF chỉ khu/phòng được giao; occupancy-history đo cuối tháng |
| Xuất | GET /invoices/{id}/pdf, /contracts/{id}/pdf, /reports/pdf, /reports/debts.csv | PDF tiếng Việt / CSV mở bằng Excel |
| Nhật ký | GET /audit-logs | ADMIN; 500 thao tác gần nhất |

Các endpoint ghi dữ liệu nghiệp vụ dùng ADMIN hoặc STAFF trong khu/phòng được giao; TENANT được tạo sự cố và đọc dữ liệu của mình. Tài khoản, phân công, chính sách và thông báo chung chỉ ADMIN. Giá trị id trong API là số, tiền là số thập phân; frontend dùng danh sách chọn theo tên.

Các API bổ sung:

| Nhóm | Endpoint | Nội dung |
|---|---|---|
| Lịch | GET /contracts/{id}/billing-calendar?period=YYYY-MM | cutoffDate, issueDate, dueDate; đã kiểm tra quyền hợp đồng |
| Vi phạm | POST /contracts/{id}/violations | reason; tạo OPEN |
| Xử lý vi phạm | PUT /contracts/{id}/violations/{violationId} | status HANDLING/RESOLVED/DISMISSED, note bắt buộc; vi phạm đã đóng không mở lại |
| Ảnh phòng | POST /rooms/{id}/images | multipart/form-data, trường file; ADMIN/STAFF đúng phòng |
| Gỡ ảnh phòng | DELETE /rooms/{id}/images/{imageId} | đúng phòng, ghi audit |
| Ảnh sự cố | POST /maintenance-requests/{id}/images | multipart file; người gửi hoặc quản lý đúng phạm vi, trước khi hoàn thành |
| Đọc ảnh | GET /uploads/{uuid} | cần Bearer JWT, quyền đúng tài nguyên; cache no-store |

Chỉ số thường phải có readingDate trong tháng và từ cutoffDate. Chính sách chốt ngày 31 ở tháng 2 tự chuyển thành ngày cuối tháng. Hóa đơn thường phát hành ngày invoiceIssueDay của tháng kế tiếp; hạn thu theo payment_cycle_day, dịch thêm tháng nếu hạn trước ngày phát hành. Quyết toán trả phòng chốt và phát hành theo ngày thực tế; hóa đơn cũ giữ ngày đã lưu. STAFF chỉ một phòng được đọc đơn giá/chính sách dùng chung, không sửa cấu hình toàn khu.
