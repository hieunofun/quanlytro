# Backend và nghiệp vụ V13 — 30/10/2026

Triển khai trên Spring Boot/Java 21/PostgreSQL hiện tại. Giữ 49 bảng; V13 thêm hai liên kết truy vết, không chuyển sang backend của dự án khác. Mã nguồn V13 đi kèm tài liệu này; chưa nâng cấp database đang sử dụng của người dùng.

## Phần đã triển khai

| Nghiệp vụ | Hành vi đã bổ sung |
| --- | --- |
| Thu tiền cho hóa đơn | Lưu `cash_transactions.payment_document_id`; cùng mã phiếu không được chuyển sang hóa đơn khác. Tiền thực nhận và tiền phân bổ vẫn tách riêng; dư nằm ở ADVANCE |
| Gửi lại giao dịch | Kiểm tra số tiền, hóa đơn, phương thức, thời điểm và tham chiếu ngân hàng. Không gửi thời điểm ở lần thử lại thì dùng thời điểm đã lưu. Tham chiếu ngân hàng trùng không được ghi nhận hai lần |
| Giá trị tiền | Từ chối tiền thực nhận/phân bổ/hoàn có hơn hai chữ số thập phân hoặc vượt numeric(14,2), thay vì để DB làm tròn âm thầm |
| Hóa đơn tháng | Lập lại cùng hợp đồng/tháng trả chứng từ hiện có. Khóa hợp đồng trước khi đọc giá/kỳ; hai yêu cầu cùng lúc không tạo hai hóa đơn |
| Phát hành | Đọc lại trạng thái và tổng dòng sau khi khóa chứng từ; hai yêu cầu phát hành cùng lúc trả cùng kết quả, một sự kiện phát hành |
| Ngày tính tiền | Ngày phát hành/hạn trả cấu hình tới 31, tháng ngắn dùng ngày cuối tháng. API lịch và lập hóa đơn dùng cùng quy tắc. Chốt điện nước dùng ngày cấu hình gần nhất không sau ngày phát hành, tính cả ngày chốt |
| Điểm đo | Kiểm tra thời điểm trong tuổi thọ công tơ, không ở tương lai, chỉ số không giảm khi so cả điểm trước và sau. Ràng buộc kiểm tra cả ở service và PostgreSQL |
| Sửa điện đã phát hành | API chuyên biệt lập điều chỉnh theo đơn giá gốc. Chỉ số/hóa đơn đã phát hành giữ nguyên. Có bản điểm đo mới liên kết `meter_readings.correction_item_id`, dùng làm số đầu kỳ sau |
| Tiền đã phân bổ bị dư sau điều chỉnh | Giải phóng về đúng quỹ gốc theo từng khoản thanh toán; không tự tạo tiền nhận mới hoặc phiếu chi hoàn |
| Chuỗi kỳ đo đã phụ thuộc | Chặn sửa điểm đã là nguồn đầu kỳ kế tiếp; không âm thầm đẩy chênh lệch sang người/kỳ khác. Toàn bộ giao dịch bị rollback nếu bản sửa không hợp lệ |
| Giao/trả phòng | Khóa phòng/hợp đồng trước khi kiểm tra cọc giao phòng. Checkout cần hợp đồng ACTIVE có lần giao thực tế. Không được lùi ngày trả làm khoảng điện đã gán nằm ngoài trách nhiệm |
| Hồ sơ người thuê | Không nhận đồng thời `tenantId` và `tenant`. STAFF tiếp nhận khách mới trong phòng được giao; không dùng payload phụ để truy cập hồ sơ ngoài phạm vi |
| Quyết toán | API bảng chi tiết gom khoản nợ, điều chỉnh, phân bổ, tạm giữ, hoàn thực tế và khoản chờ xác minh. Chỉ ADMIN/STAFF đúng phạm vi |
| Ràng buộc nguồn | Chặn liên kết chứng từ/quyết toán/tạm giữ sang hợp đồng khác. Không sửa số tiền/căn cứ của tạm giữ đã ghi; giải phóng rồi tạo bản mới |

## CSDL và nguồn tham khảo

Metadata PostgreSQL thực thi: **49 bảng, 455 cột, 151 khóa ngoại, 132 CHECK, 7 EXCLUDE, 10 view**. DBML và sơ đồ được sinh lại từ metadata, không đếm theo mô tả README.

- [Migration V13](../src/main/resources/db/migration/V13__billing_integrity_and_receipt_intent.sql) nâng cấp từ V12; V1–V12 được giữ nguyên.
- [DDL tạo mới](quanlytro.sql) + [quy tắc hiện hành](database-rules.sql) dùng cho database trống. Không chạy DDL này đè database có dữ liệu; ứng dụng nâng cấp bằng Flyway.
- [DBML](quanlytro.dbml), [từ điển dữ liệu](TU-DIEN-DU-LIEU-49-BANG.md), [ERD SVG](diagrams/quanlytro-dbdiagram.svg), [ERD PDF](diagrams/quanlytro-dbdiagram.pdf).
- [API V13](API49.md); tài liệu V10 chỉ dùng đối chiếu lịch sử.

Đã đối chiếu schema OpenKOS về [phân bổ thanh toán](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/database/migrations/2026_07_11_133727_create_payment_allocations_table.php), [chuỗi chỉ số và nguồn điều chỉnh](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/database/migrations/2026_09_09_094617_create_utility_readings_table.php), [quyết toán cọc](https://github.com/senatroxx/OpenKos/blob/aeb354411dad78de3f13e7856d43c849c65b389e/database/migrations/2026_09_11_064836_create_deposit_settlements_table.php). Học việc tách nguồn và trạng thái; giữ mô hình sổ quỹ của bài để ghi đủ tiền thu dư và cọc thực nhận. Chính sách tiền dư/cấn quỹ đối chiếu [ADR-011 của LeaseBook](https://github.com/jwh3times/LeaseBook/blob/64a10ddefffbc2b0c02beb6bfbf7eb04518a5354/docs/adr/ADR-011-deposit-prepayment-over-application-policy.md). [Resident](https://resident.vn/) cung cấp đối chiếu quy trình chốt số → hóa đơn → thu/gạch nợ, chưa truy cập backend của nhà cung cấp.

## Kiểm chứng và chạy lại

Kết quả thực thi nằm ở [kiểm chứng Java/HTTP/migration](implementation-validation49.json) và [kiểm chứng schema/SQL](database-validation.json). Chỉ đọc trạng thái `all_passed` cùng phạm vi trong hai tệp này; không lấy số test cũ làm kết quả mới.

Lượt hoàn chỉnh ngày 30/10: **123 kiểm thử đạt**, gồm 61 ca ứng dụng/API V13, 8 ca migration và 54 ca hồi quy V10/đơn vị; không lỗi, không bỏ qua. Bộ schema độc lập đạt **20 ca SQL**. `mvn verify` đóng gói JAR thành công. JAR đã khởi động trên database mới `hostel_review13_20261030`, Flyway tới V13; health UP, đăng nhập và đọc tài khoản ADMIN thành công tại **http://127.0.0.1:8093**. Tài khoản xem thử `admin`, mật khẩu `ReviewAdmin49#2026`, chỉ dùng database thử riêng cổng 55450.

Chạy Java/HTTP/hồi quy trên các database mới riêng cho mỗi lượt:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test-backend-business.ps1 -DatabasePort 55451
```

Script tìm JDK 21 và PostgreSQL từ `JAVA_HOME`, `POSTGRES_BIN` hoặc `.runtime/tools`; có thể truyền `-JavaHome` và `-PostgresBin`. Database được giữ lại để tra soát; tên nằm trong output. Script không dùng database demo hoặc database thật. Khi đã có cluster kiểm thử đang chạy, truyền đúng cổng cluster đó.

Kiểm tra bộ SQL tạo mới:

```powershell
python scripts/verify-model49.py --port 55450 --pg-bin .runtime/tools/postgresql-18.6/pgsql/bin
```

Script SQL tạo database ngẫu nhiên, thực thi DDL/rule/test rồi xóa đúng database của lượt đó. `BusinessMigrationTests` kiểm tra dữ liệu V10 đi qua V11/V12/V13, tiền nhận/nợ/lưu trữ không đổi và không tự tạo ngày ký/cọc/giao phòng.

Các tình huống tiêu biểu trong `BusinessIntegrityTests`:

- Nhận 4.000.000 cho hóa đơn 3.620.000; gửi lại cùng mã vẫn một phiếu, dư 380.000.
- Đã thu 175.000 cho 50 kWh × 3.500; sửa còn 40 kWh tạo giảm 35.000, trả phần phân bổ về quỹ, chưa chi hoàn. Kỳ sau bắt đầu từ 40, không từ 50.
- Tiền thanh toán đến từ cả cọc và trả trước; khi giảm tiền, trả về đúng từng nguồn, không chuyển hết sang quỹ khác.
- Hai yêu cầu lập/phát hành hóa đơn hoặc hoàn tiền cùng lúc không tạo bản trùng/chi vượt số dư.
- Cọc thực nhận 1.500.000 trên cam kết 2.000.000; STAFF cần quyền thiếu cọc 500.000 mới giao phòng.
- Thiệt hại 2.000.000, cọc 1.500.000; cấn cọc xong vẫn nợ 500.000. Khoản tranh chấp không được phát hành/cấn như khoản đã xác minh.

## Phạm vi còn giới hạn

- Chưa có quy trình tự động đối soát dây chuyền khi sửa điểm đo mà các kỳ kế tiếp đã phát sinh. API chặn thay đổi đó; cần kiểm tra từng kỳ liên quan trước khi triển khai luồng mở rộng.
- Điều chỉnh điện sau phát hành và bảng quyết toán chi tiết đã có API; chưa bổ sung màn hình chuyên dụng trong lượt backend này.
- Giá dịch vụ đổi trong kỳ cần tách khoảng hoặc lập điều chỉnh có căn cứ; không tự chia sản lượng điện nước khi thiếu điểm đo ranh giới.
- Quyết toán điều chỉnh tự động tiền phòng; dịch vụ đã phát hành vẫn cần chứng từ điều chỉnh theo thỏa thuận.
- Chưa kết nối xác minh ngân hàng tự động. `BANK_TRANSFER_MANUAL` là người vận hành xác minh; `paid=true` khi hoàn là ghi nhận người dùng xác nhận đã chi, không phải lệnh chuyển tiền ngân hàng.
- Không suy ra tuân thủ pháp luật, khả năng chịu tải, phục hồi backup hoặc toàn bộ chính sách chủ trọ từ các ca đã chạy. Lượt kiểm chứng chỉ dùng database thử; chưa thay dữ liệu thật.
