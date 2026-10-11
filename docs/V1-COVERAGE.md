# Đối chiếu tài liệu V1

Đối chiếu này mô tả bản triển khai thực tế, không coi các mục mở rộng là đã hoàn thành.

| Nội dung V1 | Bản triển khai |
|---|---|
| Java/Spring/React/PostgreSQL/REST | Có; React build nhúng vào JAR |
| JWT, đăng nhập/đăng xuất, đổi mật khẩu, khóa sai nhiều lần, cấp lại token | Có; reset bằng mã một lần do ADMIN cấp, chưa có email |
| ADMIN/STAFF/TENANT, giới hạn backend | Có; STAFF phân công một/nhiều khu hoặc phòng; danh sách, chi tiết, báo cáo và ảnh kiểm tra cùng phạm vi |
| Khu/tầng/phòng, giá, sức chứa, diện tích/trạng thái | Có; bảo trì chặn hợp đồng mới, cập nhật giá có lịch sử |
| Tiện nghi, ảnh, ghi chú nội bộ | Có; ảnh tải trực tiếp JPEG/PNG hoặc URL, nhiều ảnh; ghi chú không trả cho TENANT |
| Người thuê, CCCD, ngày sinh, địa chỉ, lịch sử hợp đồng | Có; ngày sinh/địa chỉ API và form hồ sơ |
| Hợp đồng, cọc, thành viên, gia hạn, trả phòng | Có; ngày vào/rời của thành viên, cấn trừ cọc có giao dịch; chi tiết quyết toán/hoàn cọc hoặc thu thêm trên giao diện |
| Chống chồng hợp đồng | Có kiểm tra service và exclusion constraint |
| Điều khoản, vi phạm, PDF, sắp hết hạn | Có; vi phạm OPEN → HANDLING → RESOLVED/DISMISSED, lý do và ghi chú xử lý, nhật ký; chỉ quản lý xử lý |
| Điện/nước, giá theo thời gian, lịch sử sửa | Có; chỉ sửa kỳ cuối trước khi có hóa đơn |
| Ngưỡng khu/phòng, cảnh báo bất thường | Có; baseline cửa sổ tháng, ưu tiên ngưỡng phòng |
| Dịch vụ đầu người, vào/trả giữa tháng | Có; tính ngày-người và ngày ở thực tế; hai hợp đồng liên tiếp cùng phòng/tháng có chỉ số và hóa đơn riêng |
| Hóa đơn nháp/phát hành, chặn trùng, snapshot | Có; nháp xóa rồi lập lại, audit ghi trước khi xóa |
| PDF hóa đơn, thu nhiều lần, phương thức, công nợ | Có; thủ công CASH/BANK_TRANSFER_MANUAL/OTHER, DEPOSIT_OFFSET nội bộ |
| Phí phạt tùy chọn | Có cấu hình; tính từ nợ quá hạn ở hóa đơn kế tiếp |
| Kỳ, ngày chốt, ngày phát hành | Tháng dương lịch; áp dụng ngày chốt số, ngày phát hành tháng sau và hạn thu; xem lịch trước khi lập, chặn phát hành sớm; quyết toán dùng ngày trả phòng thực tế |
| Thông báo/đã đọc | Có; in-app, reminder theo giờ |
| Sự cố, ảnh, ưu tiên, hạn xử lý, giao nhân viên, chi phí | Có; ảnh tải trực tiếp hoặc URL, chuyển trạng thái có kiểm tra |
| Bảo trì định kỳ | Có lịch và nút hoàn tất kỳ để dịch ngày tiếp theo |
| Dashboard/doanh thu/công nợ/lấp đầy/sự cố | Có API/UI tháng/quý và lấp đầy cuối tháng; tiền thu theo ngày nhận, không tính cấn cọc là dòng tiền mới |
| Xuất báo cáo PDF hoặc Excel | Có PDF và CSV đọc bằng Excel; chưa có XLSX |
| JPA/Entity/Repository, OOP/DI/Transaction/AOP | Có; JPA đọc snapshot, JDBC phục vụ query/locking; AOP đo API, audit transaction |
| Dữ liệu demo/báo cáo/slide/đóng gói | Có script demo, tài liệu báo cáo và hồ sơ nộp |
| Mobile/ngân hàng/QR/AI/IoT/Push | Ngoài V1, chưa triển khai |

Trả phòng sau khi đã phát hành kỳ cuối yêu cầu ngày/chỉ số khớp hóa đơn; bản này không sửa hóa đơn phát hành hoặc tự tạo phiếu hoàn bù. Muốn trả giữa tháng, thực hiện trả phòng trước khi phát hành kỳ cuối để hệ thống tính đúng tỷ lệ ngày.
