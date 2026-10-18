# Hệ thống quản lý nhà trọ

Phân tích yêu cầu và thiết kế cơ sở dữ liệu — học phần Kỹ năng lập trình nâng cao, N03.

**Phạm vi:** Mô hình mục tiêu 17 bảng; một chủ nhà, nhân viên theo khu và người đại diện hợp đồng.

**Nhóm:** ………………………………

## Slide 01 — Hệ thống quản lý nhà trọ

- Phân tích yêu cầu và thiết kế cơ sở dữ liệu
- Hóa đơn minh bạch — theo dõi công nợ — cảnh báo điện nước
- Một chủ nhà, nhiều khu; ba tác nhân ADMIN / STAFF / TENANT

## Slide 02 — Bài toán và phạm vi

- Một chủ nhà quản lý nhiều khu và nhiều phòng.
- Theo dõi người thuê, hợp đồng, chỉ số và các khoản phải thu.
- Người thuê xem được căn cứ tính tiền và lịch sử thanh toán.
- MVP: cho thuê → tính tiền → thu tiền → quyết toán; kèm xử lý sự cố.
- Thanh toán ghi nhận thủ công; đặt phòng công khai, AI/IoT ngoài phạm vi.

## Slide 03 — Tác nhân và phân quyền

| Tác nhân | Được thực hiện | Giới hạn |
| --- | --- | --- |
| Chủ nhà / ADMIN | Quản lý tài khoản, phân công, khu/phòng và tài chính | Toàn bộ hệ thống của chủ nhà |
| Nhân viên / STAFF | Lập hợp đồng, ghi chỉ số, lập hóa đơn, thu tiền, xử lý sự cố | Khu được phân công; không quản trị tài khoản/quyền |
| Người thuê / TENANT | Xem hợp đồng, hóa đơn, giao dịch; gửi và theo dõi sự cố | Chỉ hợp đồng mình đại diện; không tự xác nhận thu tiền |

## Slide 04 — Chức năng quản lý và cho thuê

| Nhóm chức năng | Use Case chính | Tác nhân |
| --- | --- | --- |
| Tài khoản | Tạo tài khoản; cập nhật, khóa tài khoản | ADMIN |
| Phân công | Phân công nhân viên; thu hồi phân công | ADMIN |
| Khu và phòng | Tạo khu/phòng; cập nhật phòng; chuyển phòng bảo trì | ADMIN; STAFF theo quyền |
| Người thuê | Tạo, cập nhật hồ sơ; tra cứu người thuê | ADMIN; tra cứu có giới hạn |
| Hợp đồng | Lập, tra cứu hợp đồng; cập nhật thành viên cư trú | ADMIN / STAFF; TENANT tra cứu |

## Slide 05 — Chức năng tính tiền và vận hành

| Nhóm chức năng | Use Case chính |
| --- | --- |
| Dịch vụ | Thiết lập đơn giá và ngưỡng cảnh báo theo khu |
| Điện nước | Ghi chỉ số; sửa chỉ số cuối; kiểm tra cảnh báo |
| Hóa đơn | Lập nháp; xóa nháp; phát hành; tra cứu hóa đơn |
| Thanh toán | Ghi nhận thanh toán; tra cứu công nợ; tổng hợp tiền thu |
| Trả phòng | Chốt kỳ cuối; quyết toán; cấn cọc; xác định hoàn/thu thêm |
| Sự cố | Gửi yêu cầu; đính kèm ảnh; cập nhật và theo dõi tiến độ |

## Slide 06 — Luồng lập và phát hành hóa đơn

- Kiểm tra kỳ, đơn giá, chỉ số và hóa đơn trùng trước khi lập nháp.
- Có cảnh báo chờ kiểm tra thì chưa phát hành; nhập sai thì xóa nháp, sửa chỉ số và lập lại.
- Chỉ phát hành khi đến ngày; giữ nguyên giá snapshot và chỉ số được tham chiếu.

## Slide 07 — Luồng quyết toán trả phòng

- Kiểm tra quyền, ngày trả và việc quyết toán trùng.
- Chốt chỉ số, hoàn tất hóa đơn mọi tháng đã ở; hóa đơn đã phát hành phải khớp ngày/chỉ số.
- Số quyết toán = cọc − thiệt hại − công nợ; công nợ đã gồm điện nước kỳ cuối.
- Lưu kết quả, tạo giao dịch cấn cọc và kết thúc hợp đồng trong một giao dịch.

## Slide 08 — Quy tắc nghiệp vụ chính

- Khoảng cư trú [ngày vào, ngày ra): trả ngày 16 thì tính đến hết ngày 15.
- Phí tháng tính theo ngày ở/ngày-người; điện nước tính chênh chỉ số.
- Đơn giá lấy tại đầu kỳ; một hợp đồng có tối đa một hóa đơn mỗi kỳ.
- Hóa đơn đã phát hành giữ nguyên; mỗi lần thu phải dương và không vượt nợ.
- Cọc trừ thiệt hại, sau đó cấn công nợ; không trừ điện nước hai lần.

## Slide 09 — Mô hình dữ liệu: 17 bảng

| Nhóm dữ liệu | Các bảng |
| --- | --- |
| Tài khoản và phân công | users, employee_assignments |
| Khu và phòng | properties, rooms |
| Người thuê và hợp đồng | tenant_profiles, contracts, contract_members |
| Dịch vụ và điện nước | service_items, service_price_configs, meter_readings, consumption_alerts |
| Hóa đơn và quyết toán | invoices, invoice_items, payment_transactions, checkout_records |
| Sự cố và ảnh | maintenance_requests, maintenance_images |

## Slide 10 — ERD — khu nhà, người thuê và hợp đồng

- properties 1–N rooms; rooms 1–N contracts; tenant_profiles 1–N contracts.
- contracts N–N tenant_profiles qua contract_members đối với các lần ở cùng.
- Mã phòng duy nhất trong khu; khoảng thuê không được chồng nhau.

## Slide 11 — ERD — đơn giá, chỉ số và cảnh báo

- Đơn giá riêng cho từng khu–dịch vụ–ngày hiệu lực.
- Chỉ số chỉ lưu hợp đồng, kỳ và dịch vụ đo đếm; phòng và lượng dùng truy ra bằng view.
- Một chỉ số có tối đa một cảnh báo; thông tin đánh giá và người kiểm tra có căn cứ.

## Slide 12 — ERD — hóa đơn, giao dịch và quyết toán

- Một hóa đơn có nhiều dòng và nhiều giao dịch.
- Mỗi hợp đồng có tối đa một quyết toán; dòng đo đếm tham chiếu chỉ số cùng hợp đồng/kỳ.
- Chỉ lưu nguồn dữ liệu và snapshot cần bảo toàn; không lưu nhiều bản sao của tổng tiền.

## Slide 13 — Thuộc tính và kiểu dữ liệu

| Dữ liệu | Kiểu lựa chọn | Căn cứ |
| --- | --- | --- |
| Khóa nội bộ | BIGINT IDENTITY | Liên kết ổn định bằng PK/FK |
| Tiền, đơn giá | NUMERIC(12,2) | Thập phân chính xác; làm tròn thành tiền |
| Chỉ số điện nước | NUMERIC(10,2) | Có thể có phần thập phân; mới ≥ cũ |
| Điện thoại, giấy tờ | VARCHAR | Giữ số 0 đầu; không dùng làm phép tính |
| Ngày / thời điểm | DATE / TIMESTAMPTZ | Phân biệt ngày nghiệp vụ và thời điểm |
| Kỳ hóa đơn | DATE ngày đầu tháng | Kiểu ngày hợp lệ; CHECK ngày = 1 |

## Slide 14 — Khóa và ràng buộc toàn vẹn

| Cơ chế | Quy tắc được bảo vệ |
| --- | --- |
| PK + FK | Mọi bảng có định danh; không tham chiếu bản ghi không tồn tại |
| UNIQUE | Không trùng mã phòng trong khu, hóa đơn/kỳ hoặc mã giao dịch |
| CHECK | Tiền hợp lệ; mới ≥ cũ; ngày hợp lệ; trạng thái đúng tập giá trị |
| EXCLUDE | Không chồng khoảng thuê phòng hoặc khoảng cư trú của cùng thành viên |
| Trigger + khóa hàng | Không thu vượt nợ; không sửa hóa đơn phát hành; kiểm tra liên bảng |

## Slide 15 — Quan hệ và bảng trung gian

| Quan hệ | Ví dụ | Cách biểu diễn |
| --- | --- | --- |
| 1–0..1 | Tài khoản–hồ sơ; hợp đồng–quyết toán | UNIQUE trên FK; liên kết tùy chọn |
| 1–N | Khu–phòng; hợp đồng–hóa đơn; hóa đơn–giao dịch | FK ở phía nhiều |
| N–N | Nhân viên–khu nhà | employee_assignments |
| N–N theo thời gian | Người thuê–hợp đồng ở cùng | contract_members + khoảng cư trú |
| N–N có lịch sử giá | Khu nhà–dịch vụ | service_price_configs + ngày hiệu lực |

## Slide 16 — Chuẩn hóa dữ liệu

| Mức | Vấn đề cần tránh | Cách xử lý trong mô hình |
| --- | --- | --- |
| 1NF | Nhiều ảnh trong một cột | maintenance_images: mỗi dòng một ảnh |
| 2NF | Thông tin chỉ phụ thuộc một phần khóa ghép | Không lưu họ tên ở thành viên; không lưu room_id ở chỉ số |
| 3NF | Thông tin danh mục lặp qua bảng quan hệ | Hồ sơ người và danh mục dịch vụ lưu tại thực thể riêng |
| Dữ liệu suy ra | Nhiều cột tổng hợp có thể cập nhật lệch nhau | Tính lượng dùng, thành tiền, công nợ và trạng thái qua view |

## Slide 17 — Phụ thuộc dữ liệu và bảo toàn lịch sử

| Phụ thuộc | Quyết định thiết kế |
| --- | --- |
| tenant_id → thông tin cá nhân | Bảng thành viên chỉ tham chiếu hồ sơ người thuê |
| contract_id → room_id → property_id | Chỉ số và sự cố truy ra khu/phòng qua hợp đồng |
| Chỉ số mới − chỉ số cũ → lượng dùng | Tính lượng dùng; không nhập lại tại cảnh báo/hóa đơn |
| Dòng hóa đơn + giao dịch → công nợ | Một view tính tổng, đã thu và số còn nợ |

## Slide 18 — Đối chiếu nghiệp vụ và kiểm tra dữ liệu

| Nghiệp vụ | Dữ liệu chính | Tình huống đã kiểm tra |
| --- | --- | --- |
| Lập hợp đồng | rooms, contracts, contract_members | Chồng thời gian; phòng bảo trì; vượt sức chứa |
| Lập/phát hành hóa đơn | meter_readings, invoices, invoice_items | Trùng kỳ; cảnh báo PENDING; sai chỉ số tham chiếu |
| Thu tiền | invoices, payment_transactions | Tiền âm; trùng giao dịch; thu vượt công nợ |
| Trả phòng | checkout_records và giao dịch cấn cọc | Snapshot trước cấn; không tính điện nước hai lần |
| Gửi sự cố | maintenance_requests, maintenance_images | Sai người gửi; sai người đính kèm; ảnh rỗng |

## Slide 19 — Ví dụ hóa đơn và quyết toán

- Dữ liệu minh họa; các khoản nợ khác giả định bằng 0.
- Cấn cọc 1.680.000 tạo DEPOSIT_OFFSET; hóa đơn hết nợ nhưng tiền mới nhận vẫn là 2.000.000.

| Khoản thu — ở đủ tháng 09/2026 | Công thức | Thành tiền |
| --- | --- | --- |
| Tiền phòng | 3.000.000 × 30/30 | 3.000.000 đ |
| Điện | (1.120 − 1.000) × 3.500 | 420.000 đ |
| Nước | (128 − 120) × 20.000 | 160.000 đ |
| Internet | 100.000 × 30/30 | 100.000 đ |
| Tổng hóa đơn |  | 3.680.000 đ |

## Slide 20 — Phân công thực hiện

| Họ tên — MSSV | Phần việc thực hiện | Minh chứng | Đóng góp |
| --- | --- | --- | --- |
| …………………… | ……………………………… | Tài liệu / commit | …… % |
| …………………… | ……………………………… | Tài liệu / commit | …… % |
| …………………… | ……………………………… | Tài liệu / commit | …… % |
| …………………… | ……………………………… | Tài liệu / commit | …… % |

## Slide 21 — ERD — yêu cầu sửa chữa và ảnh

- Sự cố gắn hợp đồng; khu/phòng truy ra qua hợp đồng.
- Người gửi, người được giao và người đính kèm đều tham chiếu users.
- Một yêu cầu có nhiều ảnh; mỗi dòng một URL.

## Slide 22 — ERD tổng thể — 17 bảng

- 17 bảng, 138 cột và 28 khóa ngoại; cùng mô hình với các ERD chi tiết.
- Bản xuất chính thức từ dbdiagram.io: diagrams/quanlytro-dbdiagram-official.svg.

# Phụ lục — danh mục Use Case

**A:** ADMIN. **S:** STAFF theo khu được giao. **T:** TENANT theo hợp đồng mình đại diện. Hồ sơ chưa gắn hợp đồng do ADMIN tiếp nhận; STAFF tra cứu hồ sơ có căn cứ trong khu được giao.

| Mã | Chức năng | Tác nhân | Dữ liệu chính |
| --- | --- | --- | --- |
| UC01 | Đăng nhập hệ thống | A/S/T | users |
| UC02 | Đổi mật khẩu | A/S/T | users |
| UC03 | Tạo tài khoản | A | users |
| UC04 | Cập nhật, khóa tài khoản | A | users |
| UC05 | Phân công, thu hồi phân công nhân viên | A | employee_assignments |
| UC06 | Tạo khu nhà | A | properties |
| UC07 | Cập nhật khu nhà | A | properties |
| UC08 | Ngừng khai thác khu nhà | A | properties |
| UC09 | Tạo phòng | A | rooms |
| UC10 | Cập nhật phòng | A/S | rooms |
| UC11 | Chuyển phòng sang bảo trì | A/S | rooms |
| UC12 | Xóa phòng chưa có phụ thuộc | A | rooms |
| UC13 | Tạo hồ sơ người thuê | A | tenant_profiles |
| UC14 | Cập nhật hồ sơ người thuê | A | tenant_profiles |
| UC15 | Tra cứu hồ sơ người thuê | A/S/T | tenant_profiles, contracts |
| UC16 | Lập hợp đồng thuê | A/S | contracts, rooms, tenant_profiles |
| UC17 | Tra cứu hợp đồng | A/S/T | contracts |
| UC18 | Cập nhật thành viên cư trú | A/S | contract_members, tenant_profiles |
| UC19 | Thiết lập dịch vụ và đơn giá | A | service_items, service_price_configs |
| UC20 | Ghi chỉ số điện nước | A/S | meter_readings |
| UC21 | Sửa chỉ số cuối chưa dùng trong hóa đơn | A/S | meter_readings |
| UC22 | Kiểm tra, xác nhận cảnh báo | A/S | consumption_alerts |
| UC23 | Lập hóa đơn nháp | A/S | invoices, invoice_items |
| UC24 | Xóa hóa đơn nháp | A/S | invoices, invoice_items |
| UC25 | Phát hành hóa đơn | A/S | invoices, consumption_alerts |
| UC26 | Tra cứu hóa đơn đã phát hành | A/S/T | v_invoice_lines, v_invoice_balances |
| UC27 | Ghi nhận thanh toán | A/S | payment_transactions |
| UC28 | Tra cứu công nợ | A/S/T | v_invoice_balances |
| UC29 | Quyết toán trả phòng | A/S | contracts, checkout_records, payment_transactions |
| UC30 | Gửi yêu cầu sửa chữa | T | maintenance_requests |
| UC31 | Cập nhật tiến độ sửa chữa | A/S | maintenance_requests |
| UC32 | Theo dõi yêu cầu sửa chữa | A/S/T | maintenance_requests |
| UC33 | Tổng hợp tiền thu | A/S | payment_transactions, v_invoice_balances |
| UC34 | Đính kèm ảnh sự cố | A/S/T | maintenance_images |
| UC35 | Thiết lập ngưỡng cảnh báo theo khu | A | properties |

## Quy tắc dữ liệu

- Một hồ sơ dùng chung cho các lần cư trú. Người đại diện không được thêm lần nữa như thành viên của chính hợp đồng.
- Mã phòng duy nhất trong khu; không cho thuê chồng khoảng thời gian; không vượt sức chứa.
- Ngày ra không tính tiền. Tiền phòng/phí tháng chia theo ngày hoặc ngày-người; điện nước dùng chênh chỉ số.
- Kỳ là ngày đầu tháng. Một hợp đồng/kỳ có tối đa một hóa đơn; không phát hành khi còn cảnh báo PENDING.
- Hóa đơn phát hành và chỉ số đã sử dụng không được sửa; giao dịch thu tiền chỉ được thêm mới.
- Giữ tên, đơn vị, đơn giá tại lúc tính tiền. Tổng tiền và công nợ tính qua view, không lưu lặp.
- Quyết toán giữ cọc và nợ snapshot trước cấn. Cấn cọc không phải dòng tiền thu mới; số quyết toán có thể âm.
- Cùng một transaction phải giữ hợp đồng kết thúc, bản quyết toán và giao dịch cấn cọc nhất quán.

## Đặc tả Use Case chính

| Use Case | Tiền điều kiện | Luồng chính | Nhánh thay thế | Hậu điều kiện |
| --- | --- | --- | --- | --- |
| Phát hành hóa đơn | Đúng quyền; hóa đơn DRAFT | Kiểm tra ngày, giá, lượng tính phí và cảnh báo → ISSUED | Thiếu dữ liệu hoặc còn PENDING thì giữ nháp; sai chỉ số thì xóa nháp và lập lại | Snapshot và dữ liệu tham chiếu được bảo vệ |
| Quyết toán trả phòng | Hợp đồng/ngày hợp lệ; chưa quyết toán | Hoàn tất mọi kỳ → chốt cọc/nợ/thiệt hại → cấn cọc → lưu kết quả | Thiếu hóa đơn thì bổ sung; dữ liệu phát hành không khớp thì dừng; cọc thiếu thì ghi phải thu thêm | Một bản quyết toán; công nợ và giao dịch cấn cọc đối chiếu được |

## Hồ sơ dữ liệu

- [Thiết kế và ràng buộc](DATABASE.md).
- [Chứng minh chuẩn hóa](NORMALIZATION.md).
- [DBML](quanlytro.dbml), [SQL](quanlytro.sql), [view và trigger](database-rules.sql).
- [ERD chính thức — SVG](diagrams/quanlytro-dbdiagram-official.svg), [PNG](diagrams/quanlytro-dbdiagram-official.png), [PDF](diagrams/quanlytro-dbdiagram-official.pdf).
- [Ca kiểm tra DB](database-cases.sql), [kết quả thực thi](database-validation.json).
- [Luồng hóa đơn](diagrams/thuyet-trinh-luong-hoa-don.svg), [luồng trả phòng](diagrams/thuyet-trinh-luong-tra-phong.svg).

## Phiên bản triển khai

Đây là mô hình mục tiêu của bước 1–2. Backend 29 bảng V1–V9 là prototype riêng; chưa có migration/API chạy trên mô hình 17 bảng. Kết quả 40 ca và một ca đồng thời là kiểm tra SQL thiết kế trên database tạm, không phải kết quả kiểm thử API hoặc điểm chấm của giảng viên.
