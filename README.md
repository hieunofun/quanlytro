# Hệ thống quản lý nhà trọ

Backend Java 21 / Spring Boot, PostgreSQL và frontend React. Phiên bản hiện hành: **V13, mô hình 49 bảng**; profile `dev` và `prod` kích hoạt `model49`.

## Tài liệu hiện hành

- [Thay đổi backend, nghiệp vụ, nguồn tham khảo và giới hạn V13](docs/TRIEN-KHAI-BACKEND-V13.md).
- [API V13](docs/API49.md), [CSDL](docs/DATABASE.md), [từ điển dữ liệu](docs/TU-DIEN-DU-LIEU-49-BANG.md).
- [DBML](docs/quanlytro.dbml), [ERD PDF](docs/diagrams/quanlytro-dbdiagram.pdf), [ERD SVG](docs/diagrams/quanlytro-dbdiagram.svg).
- [Kết quả Java/HTTP/migration](docs/implementation-validation49.json), [kết quả SQL](docs/database-validation.json).
- [Đối chiếu OpenKOS, LeaseBook và sản phẩm Việt Nam](docs/KHUYEN-NGHI-BACKEND-NGHIEP-VU-2026-10-30.md).

Các slide và liên kết dbdiagram mang hậu tố `official` thuộc mô hình 17 bảng trước đây. Tài liệu API V10 và `docs/archive/v10` giữ để tra cứu lịch sử; không dùng làm hợp đồng API hiện hành.

## Quy tắc nghiệp vụ chính

- Người ký, người thực ở, thời điểm giao phòng và ngày hết tính tiền được ghi riêng. Hết hạn hợp đồng chưa đồng nghĩa khách đã trả phòng.
- Cọc cam kết không tự tạo tiền. Phiếu thu chưa xác minh chưa tăng quỹ; ghi nhận tiền thực nhận, phân loại cọc/trả trước rồi phân bổ nợ.
- Nhận dư giữ đầy đủ tiền vào, phần vượt nợ nằm ở trả trước. Mã giao dịch và tham chiếu ngân hàng bảo vệ thao tác gửi lại.
- Hóa đơn giữ nguồn, đơn giá và khoảng tính. Đã phát hành không sửa/xóa; sửa sai bằng chứng từ điều chỉnh có liên kết.
- Sửa chỉ số đã tính tiền tạo điều chỉnh theo giá gốc và bản điểm đo mới cho kỳ tiếp theo. Khi đã có kỳ phụ thuộc, hệ thống yêu cầu đối soát chuỗi trước.
- Điện nước thuộc người thuê phải nằm trong thời gian nhận/trả thực tế. Công tơ có tuổi thọ, chuỗi số đo và lịch sử thay thế.
- Phòng/dịch vụ chia ngày theo tháng dương lịch; dịch vụ đầu người dựa trên người thực ở. Ngày kết thúc là mốc không bao gồm. Ngày cấu hình 29–31 dùng ngày cuối tháng khi tháng ngắn.
- Duyệt quyết toán khác với chi hoàn. Tạm giữ có căn cứ/ngày rà lại; nợ vượt cọc vẫn được theo dõi. Chi phí sửa chữa không tự trở thành nợ người thuê.
- STAFF chỉ thao tác phòng/khu được giao; cấn cọc, hoàn, chuyển quỹ và điều chỉnh cần quyền theo hạn mức. TENANT chỉ truy cập dữ liệu được cấp quyền.

## Build và kiểm thử backend

Yêu cầu JDK 21 và PostgreSQL; kiểm thử nghiệp vụ dùng PostgreSQL thật. Maven Wrapper có sẵn. Script dưới tạo database riêng từng lượt, giữ lại dữ liệu để tra soát, không dùng database thật:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-backend-business.ps1 -DatabasePort 55451
```

Đặt `JAVA_HOME` và `POSTGRES_BIN`, hoặc truyền `-JavaHome` / `-PostgresBin`. Script cũng tìm công cụ trong `.runtime/tools`. Các nhóm hồi quy V10, mô hình hiện hành và migration có database riêng.

Kiểm tra DDL/SQL độc lập:

```powershell
python scripts/verify-model49.py --port 55450 --pg-bin .runtime/tools/postgresql-18.6/pgsql/bin
```

## Chạy bản xem riêng

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-model49-review.ps1
```

Mặc định http://127.0.0.1:8091, database review `hostel_review49` trên cổng 55449. Script nhận `-JavaHome`, `-PostgresBin`, `-DatabasePort`, `-Port` và `-Restart`. Đây là database xem thử; thông tin tài khoản khởi tạo xem trong script. `seed-demo.ps1` và `verify-demo.ps1` là công cụ V10, không dùng để nạp V13.

## Docker

Sao chép `.env.example` thành `.env`, điền `DB_PASSWORD`, `ADMIN_PASSWORD` (tối thiểu 12 ký tự) và `JWT_SECRET` ngẫu nhiên (tối thiểu 32 byte), rồi:

```sh
docker compose up --build -d
```

Mở http://localhost:8080. Flyway thực hiện nâng cấp; trước khi nâng cấp dữ liệu thật cần sao lưu database và thư mục `UPLOAD_DIRECTORY`. Docker chưa được chạy trong lượt kiểm chứng V13 này.

## Frontend và giới hạn

Trong `frontend`: `npm ci`, `npm run dev`; `npm run build` xuất giao diện vào `src/main/resources/static` trước khi đóng gói JAR. Các API tài chính nâng cao có thể chưa có màn hình chuyên dụng.

Phạm vi hiện tại phục vụ một chủ nhà. Xác minh chuyển khoản thủ công; thông báo nội bộ; chưa có ngân hàng tự động hoặc thao tác tự động sửa dây chuyền nhiều kỳ. Kết quả kiểm thử không thay cho nghiệm thu toàn bộ chính sách vận hành thực tế. Xem giới hạn chi tiết trong tài liệu V13.
