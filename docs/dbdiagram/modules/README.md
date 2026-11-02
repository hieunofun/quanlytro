# 8 sơ đồ CSDL theo nghiệp vụ

**[Mở bộ sơ đồ](index.html)** bằng Chrome/Edge. **[Tải đúng 8 file PDF](8-so-do-nghiep-vu.zip)**. Mỗi phần có một PDF một trang, SVG và DBML để chỉnh sửa. Đây là các phần của cùng CSDL `model49 / V13`, không phải 8 database khác nhau.

| Phần | Bảng chính đầy đủ | Tham chiếu rút gọn | FK thật | File |
| --- | ---: | ---: | ---: | --- |
| 01. Tài khoản và phân quyền | 6 | 2 | 9 | [PDF](01-tai-khoan-phan-quyen.pdf) · [DBML](01-tai-khoan-phan-quyen.dbml) |
| 02. Nhà trọ và tài sản | 7 | 1 | 10 | [PDF](02-nha-tro-tai-san.pdf) · [DBML](02-nha-tro-tai-san.dbml) |
| 03. Hợp đồng và cư trú | 12 | 4 | 34 | [PDF](03-hop-dong-cu-tru.pdf) · [DBML](03-hop-dong-cu-tru.dbml) |
| 04. Dịch vụ và điện nước | 9 | 6 | 28 | [PDF](04-dich-vu-dien-nuoc.pdf) · [DBML](04-dich-vu-dien-nuoc.dbml) |
| 05. Hóa đơn và dòng tiền | 7 | 13 | 35 | [PDF](05-hoa-don-dong-tien.pdf) · [DBML](05-hoa-don-dong-tien.dbml) |
| 06. Sự cố và bảo trì | 4 | 6 | 15 | [PDF](06-su-co-bao-tri.pdf) · [DBML](06-su-co-bao-tri.dbml) |
| 07. Thông báo | 2 | 5 | 7 | [PDF](07-thong-bao.pdf) · [DBML](07-thong-bao.dbml) |
| 08. Tệp đính kèm | 2 | 11 | 13 | [PDF](08-tep-dinh-kem.pdf) · [DBML](08-tep-dinh-kem.dbml) |
| **Tổng, không cộng bản tham chiếu** | **49** | — | **151** | **455 cột chính** |

## Quy ước để vừa đọc được vừa đúng CSDL

1. **Bảng chính** có màu của nhóm, giữ đầy đủ tên cột, thứ tự, kiểu dữ liệu và NULL/NOT NULL. Trong DBML, định nghĩa bảng chính giữ nguyên từ [file chuẩn](../../quanlytro.dbml), gồm default, identity, index, CHECK và ghi chú EXCLUDE/partial index.
2. **Bảng xám** là bảng thật ở nhóm khác, chỉ lặp lại PK/cột đích FK cùng một mã hoặc tên có thật để nhận diện. Trên bảng ghi rõ “THAM CHIẾU · rút gọn x/y cột”. Không cộng các bản lặp này thành bảng mới. Muốn xem đầy đủ, mở phần sở hữu bảng hoặc bản tổng.
3. **FK thuộc phần có bảng nguồn**. Ví dụ `billing_items.segment_id → consumption_segments.id` nằm trong phần 05 vì cột FK ở `billing_items`; phần 05 hiển thị `consumption_segments` dưới dạng tham chiếu. Không mở rộng tiếp các FK xuất phát từ bảng xám, để mỗi hình có phạm vi dừng rõ ràng.
4. Mọi khóa ngoại được giữ đúng cột nguồn/đích. Tất cả 151 FK xuất hiện đúng một lần trong bộ 8 hình. Không thêm liên kết chỉ vì tên cột có hậu tố `_id`, không bỏ liên kết `created_by`/`recorded_by` đang có trong CSDL.
5. PDF biểu diễn PK/FK/UNIQUE một cột và tính bắt buộc của cột. UNIQUE ghép, CHECK, trigger và các quy tắc khác cần đọc DBML/migration; không thể suy ra toàn bộ nghiệp vụ chỉ từ đường nối ERD.

Cardinality trong hình: một giá trị FK phải trỏ đến một bản ghi cha nếu FK bắt buộc, hoặc 0–1 nếu FK được NULL. Từ phía cha có thể không có bản ghi con; số con tối đa là 1 khi FK có UNIQUE không điều kiện, ngược lại là nhiều. Vạch/vòng tròn/chân quạ biểu diễn các giới hạn đó.

## Cách dùng công cụ

**Đề xuất cho repo này: một schema chuẩn → một bản tổng để tra cứu → tám bản theo nghiệp vụ để trình bày.** Không vẽ lại 49 bảng bằng tay trên nhiều phần mềm rồi duy trì từng bản riêng.

| Công cụ | Phù hợp với việc gì | Nguồn |
| --- | --- | --- |
| DBeaver | Mở PostgreSQL thật, đối chiếu cấu trúc và tạo Custom ERD chứa các bảng có sẵn | [Custom diagrams](https://dbeaver.com/docs/dbeaver/Custom-Diagrams/), [ER diagrams](https://dbeaver.com/docs/dbeaver/ER-Diagrams/) |
| dbdiagram.io | Chỉnh sửa/chia sẻ DBML. Có thể mở từng file trong bộ này; nhiều Diagram Views trong cùng một diagram cần gói trả phí | [Diagram Views](https://docs.dbdiagram.io/diagram-views/) |
| Graphviz | Tự tạo bố cục, màu và các bản PDF/SVG lặp lại được từ cùng metadata. Đây là bộ dựng hình được dùng cho 8 bản PDF mới | [Layout engines](https://graphviz.org/docs/layouts/), [ký hiệu đầu nối](https://graphviz.org/docs/attr-types/arrowType/) |

Chưa cài hoặc tạo dự án DBeaver mới trong lượt này. Các file DBML tương thích dbdiagram.io; tọa độ bố cục PDF không nằm trong DBML. Bản tổng xuất web dbdiagram.io trước đó vẫn nằm ở thư mục cha. Các PDF tách được dựng cục bộ bằng Graphviz và xuất PDF qua Chromium; không phải ảnh cắt từ PDF tổng.

## Kiểm chứng và tái tạo

- [manifest.json](manifest.json): bảng thuộc từng phần, các cột tham chiếu được chọn, tên từng FK và hash nguồn.
- [validation.json](validation.json): parser chính thức DBML so từng định nghĩa bảng/cột/FK với bản chuẩn; đủ 49 bảng chính, 455 cột và 151 FK, không trùng quyền sở hữu.
- [render-validation.json](render-validation.json): kiểm tra SVG/PDF thực tế, tên cột, kiểu, NULL, cột nguồn/đích, số trang và checksum PDF.

```powershell
python scripts/build-dbdiagram-modules.py
node scripts/verify-dbdiagram-modules.cjs
node scripts/render-dbdiagram-modules.cjs
python scripts/verify-dbdiagram-module-output.py
python scripts/build-dbdiagram-module-viewer.py
node scripts/verify-dbdiagram-module-viewer.cjs
```

Chạy tại thư mục gốc repo. Tất cả lấy nguồn từ `docs/quanlytro.dbml` và snapshot PostgreSQL đã kiểm chứng ngày 01/11/2026. Các thao tác này chỉ sinh tài liệu, không thay bảng hay dữ liệu ứng dụng.
