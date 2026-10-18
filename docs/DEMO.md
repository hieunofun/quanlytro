# Demo MVP 17 bảng

Chạy JAR trên database thử có Flyway V10; dùng [hướng dẫn chuyển đổi](BACKEND-17-TABLES.md). Đăng nhập ADMIN đã khởi tạo bằng ADMIN_PASSWORD. Script `seed-demo.ps1` tạo dữ liệu qua API, không chèn thẳng vào bảng nghiệp vụ.

```powershell
./scripts/seed-demo.ps1 -BaseUrl http://localhost:8081 -AdminPassword '<mật khẩu admin>'
./scripts/verify-demo.ps1 -BaseUrl http://localhost:8081 -AdminPassword '<mật khẩu admin>'
```

Script seed dừng nếu khu DEMO-AN-PHU đã có. Dữ liệu minh họa không phải dữ liệu vận hành.

1. Chủ nhà tạo khu, phòng (nhập số tầng), tài khoản STAFF/TENANT, hồ sơ người thuê và phân công theo khu.
2. Lập hợp đồng, đặt giá điện/nước và dịch vụ theo ngày hiệu lực. Giá trị chỉ số ban đầu không nằm trên hợp đồng; nhập oldIndex trong lần ghi đầu của từng đồng hồ.
3. Xem chỉ số nước tháng 6–8, ghi tháng 9 tăng mạnh. Có đủ ba tháng thì hiện cảnh báo; kiểm tra rồi xác nhận hoặc sửa chỉ số trước khi lập/phát hành hóa đơn.
4. Tạo hóa đơn 09/2026 với ngày phát hành 01/10/2026, hạn 05/10/2026. Sau khi sửa nước về 155, tổng tiền 3.690.000, thu 2.000.000, còn nợ 1.690.000. Xem cách tính và tải PDF.
5. Thêm hồ sơ thành viên bằng ADMIN trước, rồi chọn hồ sơ đó vào hợp đồng. Ghi ngày vào/rời để kiểm chứng phí theo ngày-người và sức chứa.
6. Đăng nhập TENANT để đọc hóa đơn/hợp đồng của mình, gửi sự cố và ảnh PNG/JPEG. ADMIN/STAFF chuyển NEW → ACCEPTED → IN_PROGRESS → COMPLETED.
7. Đăng nhập STAFF, xác nhận chỉ thấy dữ liệu khu được giao. Tài khoản TENANT không thấy hóa đơn nháp hoặc ghi chú nội bộ phòng.
8. Với hợp đồng thử riêng, ghi chỉ số cuối bằng tùy chọn chốt trả phòng; kiểm tra cảnh báo trước khi quyết toán. Hoàn tất tất cả các tháng đã ở. Tiền cọc trừ thiệt hại rồi cấn nợ; cấn cọc không làm tăng báo cáo tiền mới thu.

Các chức năng ngoài MVP như ảnh phòng, phân công riêng phòng, vi phạm và hộp thông báo đã được gỡ khỏi luồng giao diện. Xem [API.md](API.md) để biết các route prototype trả 410.
