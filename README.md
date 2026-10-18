# Hệ thống quản lý nhà trọ

## Báo cáo bước 1–2 — Nhóm gehihi

**[MỞ SƠ ĐỒ CƠ SỞ DỮ LIỆU TRÊN DBDIAGRAM.IO](https://dbdiagram.io/d/Quan-ly-nha-tro-ERD-chinh-thuc-6ad48e63e625619f4bbe6458)**

- [Slide báo cáo — PDF, 22 trang](docs/thiet-ke-csdl-va-phan-tich-nghiep-vu.pdf) · [PowerPoint chỉnh sửa được](docs/thiet-ke-csdl-va-phan-tich-nghiep-vu.pptx).
- [ERD xuất đầy đủ — PDF](docs/diagrams/quanlytro-dbdiagram-official.pdf) · [SVG](docs/diagrams/quanlytro-dbdiagram-official.svg) · [PNG](docs/diagrams/quanlytro-dbdiagram-official.png).
- [Phân tích yêu cầu, tác nhân và Use Case](docs/NOI-DUNG-THUYET-TRINH-BUOC-1-2.md) · [Thiết kế CSDL](docs/DATABASE.md) · [Chứng minh chuẩn hóa](docs/NORMALIZATION.md).
- [Kết quả rà soát slide](docs/SLIDE-REVIEW.md) · [DBML](docs/quanlytro.dbml) · [SQL bảng/khóa](docs/quanlytro.sql) · [SQL view/trigger](docs/database-rules.sql).

Sơ đồ trực tuyến xem được khi chưa đăng nhập (đã kiểm tra ngày 18/10/2026). Mô hình có 17 bảng nghiệp vụ; các ràng buộc EXCLUDE, chỉ mục có điều kiện và trigger xem thêm trong SQL. Trang 20 của slide còn cần nhóm bổ sung MSSV, phần việc, minh chứng và tỷ lệ đóng góp thực tế.

## Phiên bản triển khai

Backend Spring Boot và React sử dụng mô hình 17 bảng đã chốt: hóa đơn giữ snapshot đơn giá, tổng tiền/công nợ tính qua view, cảnh báo điện nước và phân quyền nhân viên theo khu nhà.

Flyway V10 chuyển prototype V9 và giữ 29 bảng cũ trong schema archive. Đọc [hướng dẫn chuyển đổi](docs/BACKEND-17-TABLES.md), [API hiện hành](docs/API.md) và [kết quả kiểm chứng backend](docs/backend-validation.json) trước khi nâng cấp database có dữ liệu.

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

Kiểm thử dùng PostgreSQL thật. Đặt DB_URL trỏ vào database kiểm thử riêng trước khi chạy `mvnw.cmd verify`. Phần lớn kiểm thử nghiệp vụ rollback dữ liệu; ca thu tiền đồng thời có ghi dữ liệu nên luôn dùng database kiểm thử riêng. Kiểm thử HTTP tạo và dọn các tài khoản riêng.

Phát triển giao diện: `cd frontend`, `npm ci`, `npm run dev`. Vite chuyển `/api` đến backend cổng 8080. `npm run build` xuất frontend vào `src/main/resources/static`; phải build frontend trước khi đóng gói JAR.

## Quy tắc nghiệp vụ

- ADMIN quản lý toàn bộ; STAFF chỉ thao tác khu được phân công; TENANT chỉ xem hợp đồng mình đại diện, hóa đơn đã phát hành và phòng tương ứng, đồng thời gửi sự cố.
- JWT truy cập 30 phút, refresh 7 ngày và xoay mỗi lần dùng. Đăng xuất/đổi mật khẩu/khóa tài khoản vô hiệu hóa phiên. Năm lần sai khóa tạm 15 phút. Phiên được giữ trong bộ nhớ của một instance; khởi động lại backend yêu cầu đăng nhập lại. Khôi phục mật khẩu bằng mã một lần nằm ngoài MVP 17 bảng.
- Kỳ hóa đơn là tháng dương lịch. Đơn giá áp dụng tại ngày đầu kỳ. Ngày kết thúc hợp đồng và ngày trả phòng là mốc **không bao gồm**: trả phòng ngày 16 nghĩa là tính ngày 1–15.
- Phòng và dịch vụ cố định chia theo số ngày thực tế. Dịch vụ đầu người tính tổng ngày-người của đại diện và các thành viên. Điện/nước tính chênh chỉ số, không chia thêm theo ngày.
- Chỉ sửa chỉ số cuối cùng, trước khi có hóa đơn kỳ đó. Lịch sử sửa chỉ số dạng bảng riêng thuộc prototype, chưa có trong MVP 17 bảng. Xóa hóa đơn nháp trước khi sửa chỉ số đã được đưa vào nháp. Hóa đơn đã phát hành giữ nguyên.
- Cảnh báo so với trung bình các kỳ trước trong cửa sổ cấu hình. Khi chưa đủ số tháng lịch sử cấu hình hoặc trung bình bằng 0, không đánh giá tăng theo phần trăm. Cảnh báo chờ kiểm tra chặn phát hành.
- Một hóa đơn/hợp đồng/kỳ; nhiều lần thanh toán, không được vượt công nợ. Dòng hóa đơn giữ snapshot tên, đơn vị và đơn giá; lượng dùng lấy qua chỉ số, thành tiền và công nợ tính qua view.
- Khi trả phòng phải có hóa đơn cho mọi tháng đã ở. Kỳ cuối chưa lập sẽ được chốt số và lập tự động. Nếu kỳ cuối đã phát hành, ngày trả và chỉ số cuối phải khớp dữ liệu đã tính, vì hóa đơn đã phát hành không được sửa.
- Tiền cọc cấn trừ thiệt hại và công nợ theo thứ tự kỳ. Mỗi lần cấn trừ tạo giao dịch `DEPOSIT_OFFSET`; số hoàn âm là số người thuê còn phải trả.
- MVP 17 bảng không tính phí chậm thanh toán; không có ảnh phòng, phân công riêng từng phòng, vi phạm hoặc thông báo. Các chức năng này của prototype không được tính là đã triển khai trên mô hình mới.

## Hồ sơ bàn giao

- [Hồ sơ bước 1–2 — thiết kế mục tiêu 17 bảng, ERD và kiểm tra SQL](docs/DATABASE.md)
- [Slide phân tích yêu cầu và thiết kế CSDL](docs/thiet-ke-csdl-va-phan-tich-nghiep-vu.pptx)
- [Kịch bản demo](docs/DEMO.md)
- [Tài liệu API](docs/API.md)
- [Backend 17 bảng và hướng dẫn chuyển đổi](docs/BACKEND-17-TABLES.md)
- [Kiểm chứng backend/API hiện hành](docs/backend-validation.json)
- [Kiểm chứng thiết kế DB](docs/database-validation.json)
- Tài liệu lịch sử của prototype trước V10: [báo cáo](docs/REPORT.md), [đối chiếu V1](docs/V1-COVERAGE.md), [kết quả kiểm tra cũ](docs/VALIDATION.md).

MVP hỗ trợ ảnh sự cố JPEG/PNG từ máy (6 MB/ảnh, tối đa 12 ảnh/yêu cầu), kiểm tra nội dung và quyền tải lại. Ảnh lưu tại UPLOAD_DIRECTORY (mặc định `.runtime/uploads`); khi backup phải lưu cả database và thư mục ảnh. Ghi chỉ số và phát hành hóa đơn là thao tác quản lý: ngày chốt/phát hành theo khu, hạn thu theo hợp đồng; tháng ngắn chốt vào ngày cuối tháng, quyết toán dùng ngày thực tế. Hệ thống không tự phát hành hóa đơn.

Giao diện mặc định tiếng Việt, nền sáng, bố cục menu trái và bảng/chi tiết; không có công tắc dark mode. Màu và font đổi tại `frontend/src/tokens.css` (`--primary`, `--app-font`). Dữ liệu demo là dữ liệu giả được ghi vào database riêng, không phải số liệu hardcode trên dashboard.

Kiểm tra trình duyệt sau khi nạp demo và chạy `scripts/verify-demo.ps1`: trong `frontend`, chạy `npm run test:e2e` (cần Chrome; có thể chỉ định CHROME_PATH hoặc BROWSER_CHANNEL). Đặt BASE_URL nếu backend chạy cổng khác 8080. Ảnh kiểm tra mặc định lưu trong `.runtime/mvp-screenshots`, đổi bằng SCREENSHOT_DIR. `frontend/tests/features.mjs` là bộ kiểm tra prototype cũ, không dùng để nghiệm thu MVP 17 bảng.
