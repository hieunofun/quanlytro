# Chuyển backend sang mô hình 17 bảng

Backend dùng mô hình đã chốt tại `quanlytro.dbml`, gồm 17 bảng nghiệp vụ, 138 cột và 6 view. Flyway history là bảng kỹ thuật thứ 18 trong public. Hibernate `ddl-auto=validate` kiểm tra entity; không dùng Hibernate để tự sửa schema.

## Cơ chế V10

V1–V9 giữ nguyên checksum. `V10__normalize_to_approved_model.sql` chạy trong **một transaction PostgreSQL**:

1. Di chuyển 29 bảng prototype cùng sequence/index/FK sang `prototype_v9`; giữ Flyway history trong public.
2. Tạo 17 bảng theo thiết kế mục tiêu, chuyển dữ liệu quản lý khu/phòng, hợp đồng, dịch vụ, chỉ số, hóa đơn, giao dịch và quyết toán.
3. Dùng floor_number của bảng tầng cũ làm thuộc tính phòng. Trạng thái đang thuê được suy ra từ hợp đồng.
4. Tách thành viên thành tenant_profiles và contract_members. CCCD có giá trị dùng lại hồ sơ; bản ghi không CCCD giữ thành hồ sơ riêng. ID thành viên được giữ, sequence được đặt sau ID đã nhập.
5. Chuyển chỉ số sang service_item_id và DATE kỳ. Dòng điện nước tham chiếu meter_readings, các khoản tháng giữ số ngày/ngày-người; không giữ tổng tiền/công nợ trên bảng hóa đơn.
6. Tách từng URL sự cố vào maintenance_images. URL upload riêng cũ được bổ sung đuôi theo metadata uploaded_images để tiếp tục đọc cùng tệp gốc.
7. Cài view/trigger, đối chiếu từng subtotal và tổng/đã thu/còn nợ với sổ cũ. Có chênh lệch thì rollback toàn bộ V10; 29 bảng vẫn ở public.

Các khoản đã hủy, sự cố không gắn hợp đồng và các tính năng ngoài MVP chỉ giữ ở archive, không biến thành nghiệp vụ mới. Phân công từng phòng **không được tự nâng** thành quyền cả khu; ADMIN phải phân công lại nếu cần.

Cấu hình ngưỡng cũ phải thống nhất theo khu trước khi chuyển. Nếu một khu có nhiều ngưỡng/cửa sổ khác nhau, V10 dừng để quản lý xác định cấu hình. Giá trị ngày xác nhận cảnh báo cũ không được lưu trong prototype; checked_at của cảnh báo đã xử lý được ghi tại lúc chuyển đổi, không được coi là thời điểm xử lý lịch sử chính xác.

Dữ liệu không hợp lệ với chuẩn mới (CCCD trùng, hợp đồng lịch sử chồng nhau, khoản phí không biểu diễn được, dữ liệu quyết toán thiếu...) gây lỗi và rollback; không xóa hoặc tự sửa tiền cho qua migration.

## Chạy trên bản sao để nghiệm thu

Sao lưu database V9 và thư mục upload trước. Dùng database clone, kiểm tra đủ dữ liệu trước khi đổi cấu hình ứng dụng chính. Không chạy trực tiếp `docs/quanlytro.sql` lên database prototype.

```powershell
# Các biến PGHOST/PGPORT/PGUSER do môi trường cung cấp.
New-Item -ItemType Directory -Path .runtime/backups -Force | Out-Null
pg_dump -Fc --file .runtime/backups/prototype-v9.dump hostel_demo
createdb hostel_mvp_review
pg_restore --dbname hostel_mvp_review .runtime/backups/prototype-v9.dump

$env:DB_URL='jdbc:postgresql://127.0.0.1:5432/hostel_mvp_review'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='<mật khẩu database>'
$env:ADMIN_PASSWORD='<mật khẩu admin ít nhất 12 ký tự>'
$env:JWT_SECRET='<chuỗi ngẫu nhiên ít nhất 32 ký tự>'
./mvnw.cmd verify
java -jar target/hostel-management-0.0.1-SNAPSHOT.jar --server.address=127.0.0.1 --server.port=8081
```

Nếu database trống, Flyway vẫn chạy V1–V9 rồi V10, giữ được lịch sử nâng cấp. Không dùng `flyway clean`, `repair` hoặc sửa migration đã áp dụng để vượt lỗi. Sau khi ghi nghiệp vụ mới vào mô hình 17 bảng, archive không chứa các thay đổi mới; rollback vận hành phải dựa trên backup và kế hoạch đối soát, không chỉ đổi schema.

## Kiểm chứng

- BusinessWorkflowTests: công thức tháng/ngày/ngày-người, chỉ số đầu/chuỗi chỉ số, cảnh báo đủ lịch sử, bất biến hóa đơn, thanh toán, trả phòng, cấn cọc và phân quyền.
- HttpSecurityTests: đăng nhập, khóa tạm, refresh một lần, logout, đổi mật khẩu, validate HTTP, không lộ password_hash.
- ImageWorkflowTests: ảnh riêng theo khu/hợp đồng, tệp giả, quá dung lượng, hai yêu cầu upload đồng thời và giới hạn 12 ảnh.
- MigrationTests: dữ liệu V9 có sẵn, đối chiếu tiền, rollback khi lệch, không mở rộng quyền và bảo toàn liên kết tệp ảnh.
- Các test tính phí, PDF và khởi động Spring vẫn chạy cùng bộ.

Dùng PostgreSQL riêng cho test. MigrationTests tự tạo/xóa database có tên ngẫu nhiên `hostel_migration_*`; tài khoản test cần CREATEDB. Mọi kiểm chứng runtime trong lần triển khai này chạy trên PostgreSQL thử riêng; database demo cũ không bị ghi đè.

```powershell
$env:DB_URL='jdbc:postgresql://127.0.0.1:55437/hostel_cutover_tests'
$env:DB_USERNAME='postgres'
$env:DB_PASSWORD='postgres'
./mvnw.cmd verify
```

API và các body mới: [API.md](API.md). Số ca đạt và môi trường kiểm chứng thực tế: [backend-validation.json](backend-validation.json).
