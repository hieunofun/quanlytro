# Sơ đồ CSDL hiện hành — model49 / V13

Ngày đối chiếu: **01/11/2026**. Nguồn cấu trúc là `pg_catalog` của PostgreSQL demo `hostel_review49` tại localhost:55449 sau migration V1–V13. Chỉ đọc metadata; không sửa database.

**Để trình bày, dùng [8 sơ đồ nghiệp vụ tách riêng](modules/index.html), mỗi phần một file PDF và DBML. [Tải cả 8 PDF](modules/8-so-do-nghiep-vu.zip).** Bản tổng bên dưới dành để tra cứu toàn hệ thống. Các bản tách dùng cùng nguồn DBML/schema; PDF/SVG tách dựng bằng Graphviz để kiểm soát bố cục, không ghi nhận là xuất từ dịch vụ dbdiagram.io. Xem [quy ước bản tách](modules/README.md).

- **[Mở sơ đồ, phóng to và tìm bảng](index.html)** — mở file bằng Chrome/Edge; dùng ngoại tuyến.
- [PDF một trang](quanlytro-database.pdf), [PNG đầy đủ](quanlytro-database.png).
- [SVG nguyên bản xuất từ dbdiagram.io](quanlytro-dbdiagram.svg).
- **[DBML để mở/chỉnh sửa trong dbdiagram.io](../quanlytro.dbml)** — thay toàn bộ ví dụ mặc định trong trình soạn thảo bằng nội dung file này.

Các định dạng cùng thể hiện **một sơ đồ**. PDF/PNG có thêm tiêu đề nhóm và chú giải bên ngoài SVG gốc. Không thêm bảng/cột/quan hệ để trang trí. Không cần tài khoản để xem bản cục bộ hoặc mở liên kết embed có trong [dbdiagram-link.txt](dbdiagram-link.txt); chưa tạo diagram lưu trong tài khoản trên dịch vụ.

## Cấu trúc đã giữ đúng

| Nội dung | Đã kiểm tra |
| --- | ---: |
| Bảng nghiệp vụ / khóa chính | 49 / 49 |
| Cột, đúng thứ tự vật lý trong database | 455 |
| Khóa ngoại, đúng cột nguồn và cột đích | 151 |
| CHECK, giữ nguyên biểu thức | 132 |
| EXCLUDE, ghi nguyên định nghĩa SQL trong Note | 7 |
| Chỉ mục có WHERE, ghi nguyên SQL trong Note | 14 |
| Nhóm trình bày | 8 |

Đã đối chiếu từng kiểu dữ liệu và độ chính xác `numeric`, NULL/NOT NULL, identity, mặc định, PK/UNIQUE, chỉ mục, phía FK, một–một/một–nhiều, quan hệ tùy chọn, ON DELETE/ON UPDATE. `varchar` và `timestamptz` là tên viết gọn của kiểu PostgreSQL tương ứng. Không chuyển CHECK trạng thái thành kiểu ENUM chưa tồn tại.

Tất cả 151 quan hệ đều xuất phát từ FK thật; không đoán quan hệ bằng hậu tố `_id`. Những cột truy vết có FK vẫn được vẽ đầy đủ, gồm `cash_transactions.payment_document_id` và `meter_readings.correction_item_id` của V13. Phiên đăng nhập đang nằm trong bộ nhớ backend nên không vẽ thêm bảng phiên.

Không đưa `flyway_schema_history` vào 49 bảng nghiệp vụ. **10 SQL view hiện có được liệt kê trong Project Note, không vẽ thành bảng vật lý**: `v_billing_lines`, `v_contract_current`, `v_document_balances`, `v_fund_balances`, `v_fund_movements`, `v_item_applications`, `v_item_balances`, `v_maintenance`, `v_room_status`, `v_segments`. Định nghĩa thật của chúng nằm trong snapshot. Trigger/function vẫn theo migration; DBML không thay thế toàn bộ DDL PostgreSQL.

Thứ tự hai cột bổ sung V13 trong bản DDL tổng hợp khác database đi lên bằng `ALTER TABLE`; tên, kiểu và ràng buộc khớp. Bản vẽ lấy thứ tự database đang chạy. Khác biệt thứ tự được lưu cụ thể trong `source.column_order_differences` của snapshot.

## Cách sắp xếp

Tách 8 nhóm theo các bảng đang có: tài khoản, nhà trọ/tài sản, hợp đồng/cư trú, dịch vụ/điện nước, tài chính, sự cố, thông báo và tệp. Các bảng hợp đồng, đo đếm và tài chính đặt gần nhau; bảng phụ đặt cùng vùng nghiệp vụ. Giữ đủ cột, không rút gọn bảng để làm hình có vẻ đơn giản hơn. Sơ đồ nhiều bảng cần phóng to để đọc.

Màu và `TableGroup` là metadata trình bày, không tạo thêm SQL schema. Theo tài liệu dbdiagram, khung nhóm và màu tùy chỉnh trên dịch vụ thuộc gói Pro; bản render không đăng nhập dùng giao diện mà dịch vụ cho phép. Tiêu đề vùng trên PDF/PNG được thêm bên ngoài bản xuất và không che tên bảng.

**Tọa độ không nằm trong DBML.** [layout.json](layout.json) lưu vị trí đã sắp. PDF/SVG và bản xem cục bộ giữ bố cục này. Liên kết embed không giữ tọa độ tùy chỉnh nên dịch vụ sẽ tự xếp lại. Không khai báo tọa độ bằng cú pháp DBML không có thật.

## Nguồn phương pháp đã đối chiếu

1. [DBML — cú pháp cấu trúc](https://dbml.dbdiagram.io/docs/): kiểu dữ liệu, PK/UNIQUE, default, chỉ mục, CHECK, Ref, nullable và chiều FK của quan hệ một–một.
2. [DBML — ghi chú và trình bày](https://dbml.dbdiagram.io/syntax/enrichment-visualization/): TableGroup, màu, Note; các mục này không có nghĩa là thêm dữ liệu nghiệp vụ.
3. [dbdiagram — thao tác soạn và xuất sơ đồ](https://docs.dbdiagram.io/basic-editing-experience/), [Table Groups](https://docs.dbdiagram.io/table-groups/), [Diagram Views](https://docs.dbdiagram.io/diagram-views/): phân vùng để đọc mô hình lớn và giới hạn tính năng theo gói.
4. [dbdiagram — DBML trong liên kết, không cần tài khoản](https://docs.dbdiagram.io/dbml-in-link-diagram/): DBML nén deflate/base64 ở fragment của URL embed; không tạo bản lưu trên tài khoản.
5. TECH SCHOOL: [video thiết kế DB bằng dbdiagram.io](https://www.youtube.com/watch?v=rx6CPDK_5mU) và [bài hướng dẫn đầy đủ của chính tác giả](https://dev.to/techschoolguru/design-db-schema-and-generate-sql-code-with-dbdiagram-io-4ko5). Đã đọc bài viết có ví dụ và hình minh họa; công cụ không phát được trang YouTube nên không ghi nhận là đã xem toàn bộ video. Chỉ tham khảo cách biểu diễn/xuất hình, không lấy schema ngân hàng của bài để thêm vào hệ thống nhà trọ.

## Bằng chứng và tái tạo

- [schema-snapshot.json](schema-snapshot.json): metadata PostgreSQL, lịch sử migration và định nghĩa 10 view; không chứa dữ liệu khách hoặc token đăng nhập.
- [validation.json](validation.json): phân tích bằng thư viện chính thức `@dbml/core` 10.3.1 và so từng thành phần với snapshot.
- [render-validation.json](render-validation.json): kiểm tra bản xuất thật, vị trí, SVG, PDF và trình xem.

Chạy từ thư mục gốc repo; các dependency đã có trong môi trường hiện tại:

```powershell
python scripts/export-dbml.py --port 55449 --database hostel_review49
node scripts/verify-dbdiagram.cjs
node scripts/render-dbdiagram.cjs
python scripts/verify-dbdiagram-output.py
python scripts/build-dbdiagram-viewer.py
node scripts/verify-dbdiagram-viewer.cjs
```

Exporter tự chọn `psql` đi kèm trong `.runtime` nếu có; có thể truyền `--psql`. Parser nằm ở `.runtime/dbml-tools/node_modules/@dbml/core`; renderer dùng Playwright của frontend và Chrome. Kiểm tra PDF dùng PyMuPDF trong `.runtime/presentation-tools`. Không đổi dependency của ứng dụng backend/frontend để vẽ sơ đồ.
