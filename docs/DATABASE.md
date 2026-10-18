# Thiết kế cơ sở dữ liệu — hồ sơ bước 1–2

Thiết kế mục tiêu cho MVP quản lý nhà trọ: **17 bảng, 138 cột, 17 PK, 28 FK, 55 CHECK, 2 EXCLUDE và 6 view**. Phục vụ một chủ nhà, nhân viên được phân công theo khu và người đại diện hợp đồng. Thành viên cư trú dùng hồ sơ chung nhưng không mặc nhiên có tài khoản/quyền truy cập.

## Bộ hồ sơ thống nhất

- [Slide PowerPoint](thiet-ke-csdl-va-phan-tich-nghiep-vu.pptx).
- [Slide PDF](thiet-ke-csdl-va-phan-tich-nghiep-vu.pdf).
- [Nội dung slide và danh mục yêu cầu](NOI-DUNG-THUYET-TRINH-BUOC-1-2.md).
- [DBML để import vào dbdiagram.io](quanlytro.dbml).
- [SQL tạo bảng và khóa](quanlytro.sql).
- [SQL view và ràng buộc liên bảng](database-rules.sql).
- [Ca kiểm tra thực thi](database-cases.sql), [kết quả kiểm tra](database-validation.json).
- [Chứng minh chuẩn hóa và quyết định thiết kế](NORMALIZATION.md).
- [ERD đầy đủ — PDF](diagrams/quanlytro-dbdiagram.pdf), [PNG](diagrams/quanlytro-dbdiagram.png), [SVG](diagrams/quanlytro-dbdiagram.svg).

## Các thay đổi so với bản 16 bảng

| Nội dung | Thiết kế đã chốt |
|---|---|
| Nhiều URL trong một trường | Tách maintenance_images; mỗi dòng một ảnh, FK về yêu cầu sửa chữa |
| Họ tên/giấy tờ lặp trong thành viên | contract_members tham chiếu tenant_profiles. Dùng cùng hồ sơ qua nhiều lần cư trú |
| room_id và consumption trong chỉ số | Chỉ lưu contract_id; phòng truy ra qua hợp đồng, lượng dùng tính trong v_meter_consumption |
| meter_type, lượng dùng, phần trăm/severity lặp ở cảnh báo | Tham chiếu chỉ số duy nhất; giữ baseline/ngưỡng tại lần đánh giá; tính thông tin suy ra qua view |
| Tổng tiền, đã thu, công nợ và trạng thái thanh toán lưu cùng lúc | Tổng hợp từ dòng hóa đơn và giao dịch trong v_invoice_balances |
| Chỉ số cũ/mới và lượng dùng lặp trên dòng hóa đơn | Dòng đo đếm tham chiếu meter_readings đã khóa; không sao chép service_id và quantity |
| Công thức số hoàn cọc lưu dư | Chỉ giữ cọc/nợ snapshot và thiệt hại; số quyết toán tính trong v_checkout_totals |
| Trạng thái đang thuê lặp với hợp đồng | rooms lưu tình trạng khai thác; v_room_status suy ra tình trạng thuê |
| Sự cố lưu đồng thời khu/phòng/hợp đồng | Chỉ lưu contract_id; bổ sung submitted_by và assigned_to có FK |
| Trạng thái chỉ được ghi trong COMMENT | CHECK thực thi các tập trạng thái và các điều kiện số/ngày |

## Thực thi và kết quả

Trên database **trống, dành riêng cho thiết kế**, chạy theo thứ tự:

```powershell
psql -v ON_ERROR_STOP=1 -d design_check -f docs/quanlytro.sql
psql -v ON_ERROR_STOP=1 -d design_check -f docs/database-rules.sql
psql -v ON_ERROR_STOP=1 -d design_check -f docs/database-cases.sql
```

SQL ca kiểm tra mặc định rollback fixture. Bản kiểm chứng tự động tạo database tạm, chạy các ca và kiểm tra hai kết nối thu tiền đồng thời rồi xóa đúng database tạm:

```powershell
python scripts/verify-design.py --port 55436 --pg-bin "C:/Program Files/PostgreSQL/18/bin"
python scripts/build-design.py
node scripts/render-design.cjs
```

Cần PostgreSQL đang chạy ở cổng được chọn và có quyền tạo database/extension btree_gist. Các script không dùng database ứng dụng làm đích kiểm tra. `build-design.py` dùng metadata từ lần kiểm tra để sinh DBML; `render-design.cjs` dùng @dbml/core, @viz-js/viz trong `.runtime/dbml-tools` và Playwright của frontend.

Tạo lại slide và bản nội dung từ cùng một nguồn:

```powershell
python -m pip install -r scripts/requirements-presentation.txt
python scripts/create-class-presentation.py
python scripts/export-presentation-text.py
```

PowerPoint có 20 trang chính và một trang ERD phụ lục; văn bản, bảng và sơ đồ trên slide là đối tượng chỉnh sửa được, không có speaker notes. PDF là bản xuất để trình chiếu dự phòng.

Đã thực thi **40 ca trên PostgreSQL 18.4**, cùng một ca thu tiền đồng thời: hóa đơn 1.000, hai kết nối cùng thu 600; một kết nối thành công, một bị từ chối, còn nợ 400. Chi tiết và số lượng đối tượng nằm trong `database-validation.json`.

## Các quyết định nghiệp vụ

- Khoảng cư trú là `[ngày vào, ngày ra)`. Trả ngày 16 thì tính đến hết ngày 15. EXCLUDE giữ cả lịch sử hợp đồng đã kết thúc; hai khoảng nối nhau được phép, chồng nhau bị từ chối.
- Thành viên ở cùng được tách khỏi người đại diện để không tính trùng. Ngày-người là số ngày của đại diện cộng giao khoảng cư trú của từng thành viên với kỳ.
- Kỳ hóa đơn là DATE ngày đầu tháng, không phải chuỗi tùy ý. Một hợp đồng/kỳ có đúng tối đa một hóa đơn.
- Dòng tiền phòng/phí tháng lưu số ngày hoặc ngày-người. Thành tiền = đơn giá × số ngày/ngày-người ÷ số ngày tháng. Không lưu tỷ lệ tháng đã làm tròn gây sai số tiền.
- Dòng đo đếm chỉ tham chiếu chỉ số cùng hợp đồng/kỳ. Lượng dùng = mới − cũ; không chia thêm theo số ngày ở.
- Giá áp dụng là mốc gần nhất không sau ngày đầu kỳ. Giữ tên, đơn vị và đơn giá đã áp dụng trên từng dòng hóa đơn.
- Hóa đơn đã phát hành và chỉ số được sử dụng không được sửa/xóa. Giao dịch thu tiền chỉ được thêm mới; thu vượt nợ bị từ chối trong khóa hàng của hóa đơn.
- Cảnh báo có baseline dương, ngưỡng và cửa sổ đã áp dụng. Khi không đủ lịch sử hoặc baseline = 0, tầng nghiệp vụ không tạo đánh giá phần trăm. Cấu hình ngưỡng hiện nằm tại properties.
- Quyết toán giữ cọc và công nợ trước cấn tại thời điểm chốt. Sau cấn, snapshot không đổi; công nợ hiện tại trong view giảm theo giao dịch. Số quyết toán âm được phép.
- DEPOSIT_OFFSET giảm công nợ nhưng không cộng vào tiền mới nhận. Điện nước kỳ cuối đã nằm trong nợ hóa đơn, không bị trừ thêm lần nữa.
- Xóa vật lý chỉ dành cho nháp hoặc bản ghi chưa có phụ thuộc. Không đưa hủy/thay thế hóa đơn phát hành vào MVP.
- Tầng API phải xác thực người thao tác và kiểm tra quyền đọc. Các FK/trigger không thay thế cơ chế xác thực hoặc row-level security. Bộ kiểm tra này kiểm chứng thiết kế DB, không phải bộ kiểm thử API.

## Quan hệ với backend hiện có

Hồ sơ bước 1–2 dùng **một mô hình mục tiêu duy nhất là 17 bảng**. Backend React/Spring Boot trong repo là **prototype schema 29 bảng V1–V9**, được giữ để tiếp tục phát triển ở bước 3. Đây là hai phiên bản có tên và vai trò rõ ràng, không phải hai ERD cùng mô tả một phiên bản chạy.

`quanlytro.sql` không được dùng để ghi đè database prototype. Chưa có migration dữ liệu hay sửa API để chạy với mô hình 17 bảng. Khi bước 3 chuyển sang mô hình mục tiêu, phải tạo migration mới, chuyển thành viên/ảnh sang bảng liên kết, đổi truy vấn tổng tiền sang view và sửa entity/repository tương ứng. Không sửa nội dung các migration V1–V9 đã áp dụng.

## Cập nhật dbdiagram.io

Mở sơ đồ → thay nội dung bằng `docs/quanlytro.dbml` → kiểm tra 17 bảng và 28 quan hệ → lưu sơ đồ mới. Các quan hệ có FK cho phép NULL dùng ký hiệu `>?` hoặc `?-` để khớp với tính bắt buộc của cột và tránh cảnh báo trên dbdiagram.

[Ảnh bố cục đã sắp xếp thủ công trên dbdiagram.io](diagrams/quanlytro-dbdiagram-manual.png): nhà/phòng và cư trú bên trái, hợp đồng ở giữa, điện nước và hóa đơn bên phải, tài khoản và sự cố phía dưới. DBML lưu cấu trúc, không chứa tọa độ kéo thả; cần đăng nhập và lưu sơ đồ trên dbdiagram để giữ bố cục. Ảnh là bản dự phòng của lần sắp xếp này.

EXCLUDE, chỉ mục có điều kiện và trigger phải xem thêm SQL; DBML ghi chú các chi tiết PostgreSQL không biểu diễn trực tiếp.

Tham khảo cú pháp: [PostgreSQL — Constraints](https://www.postgresql.org/docs/18/ddl-constraints.html), [Trigger functions](https://www.postgresql.org/docs/18/plpgsql-trigger.html), [Explicit locking](https://www.postgresql.org/docs/18/explicit-locking.html).
