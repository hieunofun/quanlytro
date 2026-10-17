# Hệ thống quản lý nhà trọ

Bản Java/Web theo tài liệu `v1.docx`: React → REST API Spring Boot → PostgreSQL. Điểm chính là hóa đơn lưu đơn giá/công thức, cảnh báo điện nước và phân quyền theo khu nhà hoặc phòng cụ thể.

## Chạy bản đã đóng gói trên máy này

Ứng dụng demo: **http://localhost:8080**. Nếu ứng dụng đã dừng:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local.ps1
```

Script sử dụng JDK 21 và PostgreSQL có sẵn trong `D:\tools`, database riêng trong `.runtime`, cổng PostgreSQL `55432`. Database khác trên máy không bị chỉnh sửa. Tài khoản demo được tạo bởi `seed-demo.ps1`:

| Vai trò | Tài khoản | Mật khẩu demo |
|---|---|---|
| Chủ nhà | admin | DemoAdmin#2026 |
| Nhân viên | staff_demo | DemoStaff#2026 |
| Người thuê | tenant_demo | DemoTenant#2026 |

Các mật khẩu trên chỉ dành cho database demo. Tài khoản admin trên database có sẵn không bị ghi đè.

## Chạy trên máy khác bằng Docker

Yêu cầu Docker Compose. Sao chép `.env.example` thành `.env`, điền mật khẩu DB, ADMIN_PASSWORD (ít nhất 12 ký tự), JWT_SECRET ngẫu nhiên (ít nhất 32 byte), rồi:

```sh
docker compose up --build -d
```

Mở http://localhost:8080. Tài khoản `admin` dùng ADMIN_PASSWORD vừa cấu hình. Muốn nạp dữ liệu demo, chạy `scripts/seed-demo.ps1 -AdminPassword 'mật-khẩu-admin'`. Không chạy dữ liệu demo trên database đang sử dụng thực tế.

## Build và kiểm thử từ mã nguồn

Yêu cầu Java 21, Node.js 22+, PostgreSQL 16+. Maven Wrapper có sẵn. Tạo database trống và cấu hình DB_URL, DB_USERNAME, DB_PASSWORD; Flyway tự áp dụng migration.

Trong IntelliJ IDEA, mở `pom.xml` dưới dạng Maven project, chọn Project SDK/Java 21 và chạy `com.hostel.HostelManagementApplication` với các biến môi trường ở trên. Công nghệ chính vẫn là Java (JDK ≥17, bản này dùng 21) và PostgreSQL; Spring Boot phục vụ API, React phục vụ giao diện web.

```powershell
$env:JAVA_HOME = 'đường-dẫn-JDK-21'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/hostel_db'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'mật-khẩu-db'
$env:ADMIN_PASSWORD = 'mật-khẩu-admin-ít-nhất-12-ký-tự'
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1
& "$env:JAVA_HOME/bin/java.exe" -jar .\target\hostel-management-0.0.1-SNAPSHOT.jar
```

Kiểm thử dùng PostgreSQL thật. Đặt DB_URL trỏ vào database kiểm thử riêng trước khi chạy `mvnw.cmd verify`. Các kiểm thử nghiệp vụ rollback dữ liệu; kiểm thử HTTP tạo và dọn các tài khoản riêng.

Phát triển giao diện: `cd frontend`, `npm ci`, `npm run dev`. Vite chuyển `/api` đến backend cổng 8080. `npm run build` xuất frontend vào `src/main/resources/static`; phải build frontend trước khi đóng gói JAR.

## Quy tắc nghiệp vụ

- ADMIN quản lý toàn bộ; STAFF chỉ thao tác khu hoặc phòng cụ thể được phân công; TENANT chỉ xem hợp đồng/hóa đơn/phòng của mình và gửi sự cố.
- JWT truy cập 30 phút, refresh 7 ngày và xoay mỗi lần dùng. Đăng xuất/đổi mật khẩu/khóa tài khoản vô hiệu hóa phiên. Năm lần sai khóa tạm 15 phút. Đặt lại mật khẩu bằng mã một lần do chủ nhà cấp sau khi xác minh người dùng.
- Kỳ hóa đơn là tháng dương lịch. Đơn giá áp dụng tại ngày đầu kỳ. Ngày kết thúc hợp đồng và ngày trả phòng là mốc **không bao gồm**: trả phòng ngày 16 nghĩa là tính ngày 1–15.
- Phòng và dịch vụ cố định chia theo số ngày thực tế. Dịch vụ đầu người tính tổng ngày-người của đại diện và các thành viên. Điện/nước tính chênh chỉ số, không chia thêm theo ngày.
- Chỉ sửa chỉ số cuối cùng, trước khi có hóa đơn kỳ đó; lưu lịch sử trước/sau và lý do. Xóa hóa đơn nháp trước khi sửa chỉ số đã được đưa vào nháp. Hóa đơn đã phát hành giữ nguyên.
- Cảnh báo so với trung bình các kỳ trước trong cửa sổ cấu hình. Khi chưa có lịch sử hoặc trung bình bằng 0, không đánh giá tăng theo phần trăm. Cảnh báo chờ kiểm tra chặn phát hành.
- Một hóa đơn/hợp đồng/kỳ; nhiều lần thanh toán, không được vượt công nợ. Snapshot đơn giá, lượng dùng, công thức được lưu riêng.
- Khi trả phòng phải có hóa đơn cho mọi tháng đã ở. Kỳ cuối chưa lập sẽ được chốt số và lập tự động. Nếu kỳ cuối đã phát hành, ngày trả và chỉ số cuối phải khớp dữ liệu đã tính, vì hóa đơn đã phát hành không được sửa.
- Tiền cọc cấn trừ thiệt hại và công nợ theo thứ tự kỳ. Mỗi lần cấn trừ tạo giao dịch `DEPOSIT_OFFSET`; số hoàn âm là số người thuê còn phải trả.
- Phí chậm thanh toán mặc định tắt. Khi bật, hệ thống thêm snapshot phí từ công nợ đã quá hạn tại ngày lập hóa đơn kế tiếp; mỗi kỳ chỉ có một hóa đơn.

## Hồ sơ bàn giao

- [Thiết kế CSDL trên dbdiagram.io — 16 bảng, SQL, DBML, PDF và PNG](docs/DATABASE.md)
- [Kịch bản demo](docs/DEMO.md)
- [Tài liệu API](docs/API.md)
- [Báo cáo kỹ thuật](docs/REPORT.md)
- [Đối chiếu V1 và giới hạn](docs/V1-COVERAGE.md)
- [Kết quả kiểm tra](docs/VALIDATION.md)

Bản này hỗ trợ tải ảnh JPEG/PNG từ máy (6 MB/ảnh, 12 ảnh/phòng hoặc sự cố), kiểm tra nội dung ảnh, lưu trên đĩa và kiểm tra quyền khi tải lại. Đường dẫn lưu mặc định `.runtime/uploads`, cấu hình bằng UPLOAD_DIRECTORY; khi backup phải lưu cả database và thư mục ảnh. Vẫn hỗ trợ URL HTTP/HTTPS. Khôi phục mật khẩu qua mã do chủ nhà cấp, chưa tích hợp SMTP. Ghi chỉ số và phát hành hóa đơn thủ công; ngày chốt/phát hành được áp dụng ở backend: chốt số từ ngày đã đặt, phát hành vào ngày cấu hình của tháng sau, hạn thu theo hợp đồng. Không phát hành trước ngày dự kiến. Tháng ngắn chốt vào ngày cuối tháng; quyết toán trả phòng dùng ngày thực tế. Không tự phát hành hóa đơn khi chưa có thao tác quản lý. Mobile/ngân hàng/AI/IoT nằm ngoài V1. Xem bảng đối chiếu để biết chính xác các giới hạn.

Giao diện mặc định tiếng Việt, nền sáng, bố cục menu trái và bảng/chi tiết; không có công tắc dark mode. Màu và font đổi tại `frontend/src/tokens.css` (`--primary`, `--app-font`). Dữ liệu demo là dữ liệu giả được ghi vào database riêng, không phải số liệu hardcode trên dashboard.

Kiểm tra trình duyệt sau khi nạp demo và chạy `scripts/verify-demo.ps1`: trong `frontend`, chạy `npm run test:e2e` (cần Chrome; có thể chỉ định CHROME_PATH). Ảnh kiểm tra lưu trong `docs/screenshots`. Chạy thêm `node tests/features.mjs` để kiểm tra upload, vi phạm, lịch và phân công từng phòng trên giao diện. Test này thêm một vi phạm demo đã giải quyết, tạo tài khoản test rồi khóa/thu hồi và gỡ ảnh fixture.
