# Kiểm tra local — 11/10/2026

Repo: `https://github.com/hieunofun/quanlytro.git`, clone tại `E:\qlynhatro\quanlytro`.

Kết luận: đáp ứng phần yêu cầu công nghệ đọc được trong ảnh: Java, JDK ≥17 và một trong các cơ sở dữ liệu được phép. Backend dùng Java 21/Spring Boot 4.1.1; frontend React; database PostgreSQL 18.4. Chưa thể xác nhận đáp ứng toàn bộ đề bài vì repo không có tài liệu gốc `v1.docx` được README nhắc đến.

## Đã sửa để chạy trên máy này

- `scripts/run-local.ps1`: tìm JDK/PostgreSQL qua biến môi trường hoặc thư mục cài đặt, hỗ trợ `-JavaHome`, `-PostgresBin`, `-DatabasePort`; kiểm tra lỗi kết nối/tạo database.
- PostgreSQL demo tại `127.0.0.1:55434`, thư mục `.runtime/pgdata`. Cổng 55432 thuộc một dự án khác nên không dùng lại.
- Ứng dụng chỉ lắng nghe tại `127.0.0.1:8080`. README cập nhật cách chạy.
- JDK 21 portable tại `.runtime/tools/jdk-21.0.12.1+1`; archive tải từ Adoptium đã đối chiếu SHA-256.

## Kết quả kiểm chứng

| Kiểm tra | Kết quả |
|---|---|
| `npm ci` | Thành công, audit báo 0 lỗ hổng frontend tại thời điểm chạy |
| `npm run build` | Thành công |
| `mvnw.cmd -B -ntp verify` | BUILD SUCCESS; 44 test, 0 lỗi, 0 bỏ qua |
| Database test | `hostel_test`, riêng với `hostel_demo` |
| API `/api/health` | `{"status":"UP"}` |
| `scripts/verify-demo.ps1` | Hóa đơn 3.690.000đ; đã thu 2.000.000đ; còn 1.690.000đ; xuất PDF thành công |
| `npm run test:e2e` | 9 màn hình; ADMIN/TENANT; desktop 1440px/mobile 375px; không lỗi JavaScript hoặc tràn ngang trong các ca đã kiểm tra |
| `node tests/features.mjs` | Upload ảnh, xử lý vi phạm, xem lịch hóa đơn, phân công STAFF theo phòng đều qua |

Đã mở ảnh dashboard và hóa đơn mobile để kiểm tra hiển thị. Log Maven ở `.runtime/verify.log`, ảnh kiểm tra ở `.runtime/screenshots`, PDF demo ở `.runtime/demo-invoice.pdf`; báo cáo từng lớp ở `target/surefire-reports`.

## Mở demo

URL: **http://localhost:8080**. Chọn kỳ **2026-09** để xem hóa đơn mẫu; khoản thanh toán nhận ngày 11/10/2026 nên tiền thực thu được thống kê ở tháng 10.

| Vai trò | Tài khoản | Mật khẩu demo |
|---|---|---|
| Chủ nhà | admin | DemoAdmin#2026 |
| Nhân viên | staff_demo | DemoStaff#2026 |
| Người thuê | tenant_demo | DemoTenant#2026 |

Chạy lại trên máy này:

```powershell
cd E:\qlynhatro\quanlytro
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -JavaHome 'E:\qlynhatro\quanlytro\.runtime\tools\jdk-21.0.12.1+1'
```

Các test đã chạy không chứng minh mọi tình huống nghiệp vụ hoặc khả năng chịu tải. Theo tài liệu và mã hiện tại, reset mật khẩu dùng mã do ADMIN cấp; chưa có email SMTP, xuất Excel dùng CSV và chưa có quy trình backup tự động. Docker chưa được chạy trong lần kiểm tra này.
