# Triển khai mô hình 49 bảng

> Bản hiện hành đã nâng cấp qua V13: xem [biên bản backend V13](TRIEN-KHAI-BACKEND-V13.md), [API](API49.md), [CSDL](DATABASE.md) và tệp kết quả kiểm chứng mới. Các số liệu kiểm thử, số cột và mô tả V11 bên dưới là hồ sơ đợt 26/10, không phải số liệu mới nhất.

Ngày: **26/10/2026**. Chủ nhiệm đã đồng ý triển khai phương án 49 bảng qua yêu cầu “ok triển khai nhé”. Đây là chấp thuận của chủ nhiệm trong phiên làm việc, không phải xác nhận của giảng viên.

Đã triển khai mã nguồn, migration, SQL, DBML, ERD và các luồng backend chính. Kiểm chứng trên PostgreSQL riêng ở `127.0.0.1:55449`. **Chưa chuyển dữ liệu đang dùng ở cổng 5432 sang V11.**

## 1. Hồ sơ hiện hành

- [Phương án nghiệp vụ đã duyệt](PHUONG-AN-PHAN-TICH-LAI-YEU-CAU-VA-CSDL.md), [49 bảng và quan hệ](PHUONG-AN-CSDL-49-BANG.md).
- [Từ điển đủ các trường](TU-DIEN-DU-LIEU-49-BANG.md).
- [SQL tạo bảng](quanlytro.sql), [view và trigger](database-rules49.sql), [DBML](quanlytro.dbml).
- ERD tổng thể: [SVG](diagrams/quanlytro-dbdiagram.svg), [PNG](diagrams/quanlytro-dbdiagram.png), [PDF](diagrams/quanlytro-dbdiagram.pdf).
- ERD từng phần: [cư trú](diagrams/erd-cu-tru.svg), [điện nước](diagrams/erd-dien-nuoc.svg), [tài chính](diagrams/erd-tai-chinh.svg), [sự cố](diagrams/erd-su-co.svg).
- [Kiểm chứng SQL](database-validation.json), [kiểm chứng Java/HTTP/migration](implementation-validation49.json).
- Nguồn cũ giữ ở [archive/v10](archive/v10/DATABASE.md). Slide và ERD mang hậu tố `official` từ dbdiagram cũ vẫn thuộc mô hình 17 bảng; chưa công bố lại trên dbdiagram hoặc cập nhật slide.

## 2. Mô hình vật lý

**49 bảng, 453 cột, 49 PK, 149 FK, 132 CHECK, 7 EXCLUDE, 10 view** theo metadata PostgreSQL 18.4 đã chạy. Bảng `flyway_schema_history` và các schema lưu trữ không được cộng vào 49 bảng nghiệp vụ.

Các tổng tiền, số dư, tiền đã phân bổ và công nợ được tính từ chứng từ/bút toán. Không có cột `balance` cho phép người dùng sửa trực tiếp trên quỹ.

Các tệp sinh từ nguồn:

```powershell
python scripts/build-schema49.py
python scripts/verify-model49.py --port 55449
python scripts/build-model49-artifacts.py --database hostel_test49 --port 55449
node scripts/render-design.cjs
python scripts/build-model49-docs.py
```

`build-schema49.py` dùng khi đang phát triển V11 chưa phát hành. Sau khi V11 được dùng trên dữ liệu thật, phải đóng băng checksum; thay đổi tiếp theo tạo V12, không sinh lại nội dung V11 đã áp dụng.

## 3. Chuyển dữ liệu V10 → V11

Migration mới: `src/main/resources/db/migration/V11__approved_49_table_model.sql`. V1–V10 giữ nguyên nội dung.

1. Flyway chạy V11 trong giao dịch PostgreSQL. 17 bảng V10 được chuyển sang `model_v10`, giữ ID và lịch sử nguồn.
2. Tạo 49 bảng mới, nhập hồ sơ, hợp đồng, người ở đã ghi nhận, hóa đơn, chỉ số, giao dịch, sự cố và liên kết ảnh.
3. Đối chiếu tiền từng dòng, tổng hóa đơn, tiền đã phân bổ, nợ và tiền thu thực tế. Sai lệch hoặc lỗi FK làm rollback toàn bộ V11.
4. Khóa ghi bảng trong `model_v10`. `prototype_v9` có sẵn vẫn được giữ riêng.

Những gì **không suy diễn thành sự thật mới**:

| Dữ liệu cũ | Cách chuyển |
| --- | --- |
| Cọc ghi trên hợp đồng | Cam kết cọc; quỹ cọc `PENDING`, không tự tạo phiếu thu |
| Cấn cọc đã ghi sổ | Khôi phục đúng khoản cấn từ nguồn mở sổ có nhãn lịch sử; không tạo tiền mặt mới |
| Tiền cọc còn lại | Chủ nhà xác minh số dư và ghi căn cứ trước khi được sử dụng |
| Hợp đồng có tên người đại diện | Giữ người đứng tên; không tự ghi họ đang cư trú |
| Hợp đồng ACTIVE | Cần đối soát hồ sơ; không tự tạo lần nhận phòng vật lý |
| Không có ngày ký | `origin=LEGACY_V10`; không tạo ngày ký giả |
| Chỉ số chỉ có ngày | Gắn `LEGACY_DAY_BOUNDARY`; thời điểm quy đổi không được coi là giờ đo chính xác |
| Phiếu thu không ghi tài khoản nhận | Giữ tiền đã ghi, dùng tài khoản lịch sử ngừng nhận tiền mới |
| Quyết toán ghi số phải hoàn | Giữ lịch sử `LEGACY_RECORDED`; không tạo phiếu chi đã hoàn |
| Ảnh có đường dẫn nhưng không có byte | Giữ liên kết cũ; cần khôi phục thư mục upload tương ứng |

Phòng thuộc hợp đồng ACTIVE cũ được đánh dấu chờ rà soát bàn giao. Việc đối soát hợp đồng không tự mở khóa quỹ cọc, tạo người ở hoặc tạo lần giao phòng.

## 4. Các luồng đã nối backend

| Nghiệp vụ | Hành vi hiện tại |
| --- | --- |
| Hợp đồng | Nháp → ghi nhận đã ký; người đứng tên, liên hệ và cư trú tách riêng |
| Gia hạn | Thêm bản điều kiện có hiệu lực; không sửa bản đã ký, không thay dòng đã phát hành |
| Giao / trả phòng | Ghi thời điểm thực tế; hết hạn giấy không tự coi phòng trống; trả phòng chuyển sang chờ chuẩn bị |
| Người ở | Ghi vào/rời theo khoảng; kiểm tra sức chứa, không mặc định cộng người ký |
| Hóa đơn | Dòng có nguồn, giá/đơn vị snapshot, khoảng tính và công thức; nháp được bỏ, đã phát hành bị khóa |
| Dịch vụ | Theo đăng ký từng hợp đồng, thời hạn, số lượng và cách tính; theo người dùng số ngày cư trú thực |
| Điện nước | Công tơ vật lý → điểm đo → khoảng tiêu thụ; thay công tơ tạo thiết bị mới; sửa điểm chưa tính tiền tạo phiên bản |
| Cảnh báo | So mức dùng/ngày với lịch sử có nguồn; phân biệt thiếu lịch sử/khác nhóm thuê/cần kiểm tra; cảnh báo chưa giải quyết chặn phát hành |
| Thu tiền | Phiếu chờ xác minh chưa có số dư; xác nhận tạo tiền chưa phân loại, sau đó chuyển vào cọc/trả trước |
| Thu dư | Phần dùng trả nợ là PAYMENT; phần dư tiếp tục nằm trong quỹ khách |
| Cấn / chuyển / đảo | Bút toán có nguồn, cấp quyền và khóa giao dịch; không sửa/xóa bút toán đã ghi |
| Trả phòng | Kết thúc sử dụng trước; duyệt tài chính riêng, lập tiền phòng còn thiếu, điều chỉnh tiền phòng đã thu trước và lập điện nước chưa tính |
| Điện nước cuối chưa rõ | Phải ghi căn cứ, tạm giữ có ngày rà lại; không cho duyệt như đã rõ hết |
| Hoàn tiền | Chỉ tạo OUT khi ghi nhận tiền đã chi; tạm giữ làm giảm số có thể hoàn; gửi lại mã phiếu không chi thêm lần nữa |
| Sự cố | Theo phòng trống/phòng có người/khu chung/tài sản; lưu từng bước và từng chi phí |
| Bảo trì | Lịch tạo phiếu cho lần đến hạn; thao tác này chưa có nghĩa việc sửa chữa đã hoàn thành |
| Phân quyền | Nhân viên đúng phòng/khu; hoàn/cấn cọc/chuyển/điều chỉnh cần quyền bổ sung theo hạn mức |
| Tệp | JPEG/PNG/PDF có byte, hash, metadata và FK đối tượng; tải qua API có kiểm tra quyền |
| Khôi phục | Chủ nhà cấp mã một lần sau xác minh; chỉ lưu hash, hết hạn 15 phút, tối đa 5 lần nhập sai |
| Thông báo | Lưu trong ứng dụng và trạng thái đọc từng người; chưa tích hợp SMS/email |
| Báo cáo | Tách tiền thật IN/OUT theo ngày thu/chi với tiền đã lập chứng từ, tiền phân bổ và nợ |

Giao diện đã có các thao tác ký/giao phòng, thu cọc/trả trước, cư trú, gia hạn, ghi trả phòng, duyệt quyết toán, hoàn tiền và tạm giữ. Mục **Vận hành** bổ sung giữ chỗ, tài sản, công tơ, đăng ký dịch vụ, bàn giao, lịch bảo trì và ủy quyền. Các thao tác tài chính nâng cao như điều chỉnh từng dòng/chuyển cọc/đảo phân bổ dùng API có kiểm soát; chưa có màn hình chuyên dụng cho toàn bộ biến thể.

## 5. Chạy bản xem riêng

Tại thư mục `quanlytro`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-model49-review.ps1
```

Mở **http://127.0.0.1:8091**. Tài khoản thử `admin`; mật khẩu khởi tạo mặc định của script là `ReviewAdmin49#2026`, có thể truyền `-AdminPassword`. Script dùng riêng `55449/hostel_review49`, bind localhost, không nạp fixture V10 qua script seed cũ. Để chạy lại JAR mới: thêm `-Restart`.

Kiểm thử trình duyệt: tại `frontend`, chạy `npm.cmd run test:e2e` khi bản xem riêng đang chạy. Lượt kiểm tra tạo dữ liệu có nhãn BROWSER49 trên database thử riêng. `seed-demo.ps1`, `verify-demo.ps1` và `test:e2e:legacy` giữ cho V10; hai script PowerShell dừng trước khi ghi nếu server đang dùng V11.

Quy trình nhập mới: tạo khu/phòng → đặt giá dịch vụ bắt buộc → lập hợp đồng nháp → ghi đã ký → thu cọc xác minh → ghi người thực ở và giao phòng → ghi điểm đo/đăng ký dịch vụ → lập, kiểm tra và phát hành hóa đơn → ghi thu tiền hoặc phân bổ tiền đã giữ.

Kiểm thử cần hai database dùng riêng:

```powershell
$env:DB_URL='jdbc:postgresql://127.0.0.1:55449/hostel_test17'
$env:DB49_URL='jdbc:postgresql://127.0.0.1:55449/hostel_test49'
.\mvnw.cmd verify
```

Các test V10 dùng `target=10`, test `Model49*` dùng `target=11`. Không trỏ cùng một database cho cả hai. Không dùng database thật làm database kiểm thử.

## 6. Giới hạn đã biết để đọc đúng kết quả

Kết quả hiện tại: **85 kiểm thử Java** (27 V11 và 58 hồi quy V10), **14 ca SQL**, DBML khớp 49 bảng/149 FK. Trình duyệt đã kiểm tra đăng nhập, lập nháp, ghi ngày ký, thu cọc thực tế, giao phòng, không tự tính người ký là cư dân, chín màn hình quản lý và viewport điện thoại. Ảnh minh chứng ở `.runtime/implementation-49/browser/`.

- Chưa có căn cứ tuyên bố đã nghiệm thu đủ 65 tình huống đề xuất; kết quả có thật nằm ở hai tệp kiểm chứng nêu trên.
- Giá dịch vụ đổi giữa một khoảng tính cần tách khoảng/đăng ký hoặc lập dòng có căn cứ. Điện nước qua mốc đổi giá cần điểm đo ranh giới; chương trình không tự chia sản lượng giả định.
- Quyết toán tự điều chỉnh tiền phòng chia ngày. Dịch vụ đã phát hành cần điều chỉnh riêng theo thỏa thuận nếu ngày trả thực tế làm thay đổi quyền thu.
- Khôi phục dữ liệu thật cần cả database và thư mục tệp; chưa thử phục hồi bản sao dữ liệu đang dùng của anh.
- Mã khôi phục do chủ nhà chuyển riêng; cảnh báo/nhắc việc và thông báo hiện dùng trong ứng dụng, chưa có nhà cung cấp gửi ngoài.
- Mô hình này phục vụ một chủ nhà. Không có phân chia dữ liệu giữa nhiều chủ nhà độc lập.
- Bản trình chiếu và liên kết dbdiagram cũ chưa được cập nhật/công bố. Sơ đồ đúng V11 là các tệp local được liên kết ở mục 1.
