# Hướng dẫn chạy local và xem luồng nghiệp vụ backend

Đã kiểm tra ngày **02/11/2026** trên dữ liệu giả hiện có. Backend V13/model49, 49 bảng; database demo `hostel_review49` tại `127.0.0.1:55449`.

## 1. Mở ứng dụng và Postman

- Ứng dụng: <http://127.0.0.1:8093>.
- Import [backend.postman_collection.json](backend.postman_collection.json) vào Postman, chạy **Đăng nhập** trước. Collection tự lưu Bearer token.
- `baseUrl` của collection là `http://127.0.0.1:8093`.
- Dữ liệu nghiệp vụ thuộc **tháng 01/2026**; khi xem báo cáo, chọn tháng này. Mã khu là `BE-3513448266`.

| Vai trò | Tên đăng nhập | Mật khẩu demo | Phạm vi |
| --- | --- | --- | --- |
| Quản trị | `admin` | `ReviewAdmin49#2026` | Toàn bộ dữ liệu và cấu hình |
| Nhân viên | `staff.demo` | `ReviewStaff49#2026` | Khu `BE-3513448266`: phòng 101/201, hợp đồng A/B/C; lập hóa đơn, ghi điện nước, thu tiền theo quyền nghiệp vụ |
| Người thuê | `tenant.demo` | `ReviewTenant49#2026` | Khách B phòng 101, hợp đồng `BE-3513448266-B`, hóa đơn `INV-7-2026-01-01` |

Hai tài khoản nhân viên/người thuê được tạo qua API ngày 02/11. Nhân viên đã được phân công khu ID `6`; tài khoản người thuê đã liên kết hồ sơ B ID `7`. Nhân viên chưa được cấp quyền bổ sung để duyệt điều chỉnh/quyết toán, cấn cọc hoặc chi hoàn; dùng ADMIN cho các thao tác đó. Người thuê chỉ thấy hợp đồng/hóa đơn B, không thấy A/C.

Để đổi vai trò, đăng xuất rồi đăng nhập lại; dùng các cửa sổ trình duyệt với hồ sơ riêng nếu cần mở nhiều vai trò cùng lúc. Postman hiện giữ tài khoản ADMIN trong request Đăng nhập; muốn kiểm tra vai trò khác, sao chép request rồi thay `username` và `password` tương ứng.

Collection dùng để xem dữ liệu đã tạo và gửi lại hai phiếu có sẵn. Nó không chứa toàn bộ thao tác tạo mới. [api-transcript.json](api-transcript.json) lưu 67 request nghiệp vụ đã thực hiện ngày 01/11, gồm payload và response.

Nếu cần chạy lại server, đứng tại `E:\qlynhatro\quanlytro`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-model49-review.ps1 -SkipBuild -Port 8093
```

`-SkipBuild` chạy JAR đã đóng gói. Sau khi sửa mã nguồn, bỏ `-SkipBuild` và thêm `-Restart` để build/chạy bản mới.

## 2. Luồng nghiệp vụ từ đầu đến cuối

Mọi endpoint dưới đây có tiền tố `/api`, dùng Bearer token sau đăng nhập.

| Bước | Thao tác/API chính | Ý nghĩa nghiệp vụ |
| --- | --- | --- |
| 1 | Tạo khu `/properties`, phòng `/rooms`, giá dịch vụ `/catalog/prices`, công tơ `/catalog/meters` | Chuẩn bị phòng, giá có ngày hiệu lực và công tơ vật lý |
| 2 | `POST /contracts`, sau đó `POST /contracts/{id}/activate` với `signedAt` | Lập nháp rồi ghi nhận đã ký. Cọc cam kết và quỹ được tạo chưa có nghĩa đã nhận tiền |
| 3 | `POST /cash-transactions` với `contractId`, `fundType=DEPOSIT`, `confirmed=true` | Ghi tiền cọc thực nhận; phiếu chưa xác minh chưa tăng số dư quỹ |
| 4 | `POST /contracts/{id}/members`, đăng ký dịch vụ `/catalog/subscriptions`, giao phòng `/contracts/{id}/possessions` | Người ký, người thực ở và thời điểm nhận phòng được lưu riêng. Dịch vụ theo người dựa trên cư trú thực tế |
| 5 | `POST /meter-readings` với công tơ, khoảng thời gian, chỉ số đầu/cuối | Chốt sản lượng theo người chịu trách nhiệm. Bỏ `contractId` khi chủ nhà chịu điện lúc phòng trống |
| 6 | `POST /invoices` với `contractId`, `period`; sau đó `/invoices/{id}/issue` | Lập nháp để kiểm tra rồi phát hành. Tiền phòng/dịch vụ PRORATED chia theo số ngày trong tháng |
| 7 | `POST /invoices/{id}/payments` với tiền thực nhận và mã phiếu | Nhận đầy đủ tiền khách trả, phân bổ vào công nợ; phần vượt nợ giữ ở ADVANCE |
| 8 | `POST /contracts/{id}/checkout` với `billingEnd`, `returnedAt`; sau đó `/checkout/approve` | Ghi trả phòng vật lý rồi duyệt tài chính. Chốt điện cuối, tính khoản còn thiếu, điều chỉnh tiền phòng/dịch vụ PRORATED nếu trả sớm; chọn `applyAdvance`, `useDeposit` khi cấn nợ |
| 9 | `POST /fund-accounts/{id}/refund` với `paid=true`, số tiền, thời điểm và căn cứ | Ghi tiền đã chi hoàn thực tế. Duyệt quyết toán chưa tự tạo phiếu chi |
| 10 | Kết thúc kỳ vận hành TURNOVER, mở kỳ READY | Kiểm tra/vệ sinh phòng trước khi giao cho khách kế tiếp |

`billingEnd` là ngày kết thúc **không tính vào tiền phòng**. Ví dụ `2026-01-16` nghĩa là tính đến hết 15/01. Trách nhiệm điện nước còn căn cứ giờ nhận/trả thực tế.

Nếu chưa chốt đủ điện nước, cần khoản tạm giữ có căn cứ/ngày rà soát trước khi duyệt. FULL_PERIOD/ONE_TIME giữ quy tắc thu đã chọn; không tự chia ngày như PRORATED.

## 3. Xem ca A trả phòng, phòng trống, rồi B vào

Phòng `101`, ID `6`, giá **3.100.000đ/tháng**. Điện **3.500đ/kWh**; internet **310.000đ/tháng**; dịch vụ theo người **31.000đ/người/tháng**. Nước không phát sinh, giá rác bằng 0 trong dữ liệu giả này.

| Khoảng | Ngày tính tiền phòng | Chỉ số điện | Bên chịu |
| --- | --- | --- | --- |
| A | 01–15/01: 15 ngày | 100 → 130: 30 kWh | A |
| Phòng trống | 16/01: 1 ngày | 130 → 132: 2 kWh | Chủ nhà |
| B | 17–31/01: 15 ngày | 132 → 160: 28 kWh | B |

**A — Hợp đồng → `BE-3513448266-A` → Xem → Quyết toán trả phòng.**

- ID hợp đồng `6`, trạng thái ENDED, quyết toán APPROVED.
- Tiền phòng 1.500.000 + điện 105.000 + internet 150.000 + theo người 15.000 = **1.770.000đ**.
- Cọc thực nhận **2.000.000đ**, dùng cấn **1.770.000đ**, đã chi hoàn **230.000đ**, nợ còn **0đ**.
- Postman: **Hợp đồng A**, **Quyết toán A**; API `GET /contracts/6/settlement`.
- `actual_refund_amount=230000`. `refund_due=0` hiện tại vì đã hoàn xong, không phải chưa từng hoàn.

**Điện nước — xem các khoảng đo của phòng 101.**

- 2 kWh trong lúc phòng trống có `responsibility=OWNER`, không đưa vào nợ A/B.
- Postman có thể thêm `GET /meter-readings` để xem `contract_id`, chỉ số và khoảng thời gian. Chọn các dòng `room_id=6`.

**B — Hóa đơn → `INV-7-2026-01-01` → Xem.**

- Hợp đồng `7`, hóa đơn `2`, trạng thái ISSUED.
- Tiền phòng 1.500.000 + điện 98.000 + internet 150.000 + theo người 15.000 = **1.763.000đ**.
- Thực nhận **1.800.000đ**, công nợ **0đ**, giữ trả trước **37.000đ**. Cọc B **2.000.000đ** được quản lý riêng.
- Postman: **Hóa đơn B**, **Tiền dư B**; API `GET /invoices/2`, `GET /fund-accounts/13`.
- Muốn xem tiền trả trước bằng giao diện: **Hợp đồng → `BE-3513448266-B` → Xem → Ký kết, bàn giao và tiền đang giữ**.

## 4. Xem ca sửa điện sau khi đã thu tiền

Vào **Hóa đơn → `BE-3513448266-ELEC-C` → Xem → Chứng từ điều chỉnh liên quan**.

1. Hóa đơn điện ban đầu: **50 kWh × 3.500 = 175.000đ**, đã thu đủ.
2. Chỉ số cuối đúng là 40; API chuyên biệt `POST /billing-items/{id}/meter-corrections` nhận `code`, `correctedClosingIndex=40`, `reason`.
3. Backend giữ lịch sử gốc, tạo chứng từ giảm **35.000đ**; khoản phải thu sau điều chỉnh còn **140.000đ**.
4. **35.000đ** phân bổ dư trở về ADVANCE của C. Đây là tiền còn giữ, chưa chi hoàn cho khách.

Postman: **Hóa đơn điện C sau điều chỉnh** (`GET /invoices/3`), **Điều chỉnh đã lập** (`GET /billing-documents/4`), **Tiền phân bổ dư C** (`GET /fund-accounts/17`).

Ca C đã sửa xong. Để xem kết quả, dùng các GET trên. Thao tác “Sửa chỉ số” ở màn hình Điện nước dùng luồng sửa khoảng đo; dòng đã phát hành cần API điều chỉnh chuyên biệt. Khi kỳ sau đã phụ thuộc điểm đo, backend chặn sửa để yêu cầu đối soát chuỗi.

## 5. Demo chống gửi lại giao dịch

Sau khi đăng nhập Postman:

1. Chạy **Gửi lại phiếu thu B**: HTTP 200, trả giao dịch đã có. B vẫn dư **37.000đ**.
2. Chạy **Gửi lại phiếu hoàn A**: HTTP 200, vẫn chỉ hoàn **230.000đ** một lần.
3. Sao chép request thu B, giữ nguyên mã `BE-3513448266-PAY-B`, đổi `amount` thành `1800001`: HTTP **400**, vì cùng mã nhưng nội dung khác.
4. Gọi `GET /invoices/2` khi bỏ Bearer token: HTTP **401**.

Chỉ dùng lại đúng mã/nội dung của phiếu cũ để kiểm tra gửi lại. Tạo mã mới là yêu cầu ghi giao dịch mới.

## 6. Đối chiếu dữ liệu và kết quả local ngày 02/11

SQL chỉ đọc: [backend-readonly.sql](backend-readonly.sql), chạy trên `127.0.0.1:55449/hostel_review49`, user `postgres` của database demo local.

Đã thực thi trong lượt chạy này:

- Health HTTP 200: `status=UP`, `schemaVersion=49`; migration cuối V13 thành công.
- Đăng nhập ADMIN thành công; API quyết toán A, hóa đơn B/C và quỹ trả trước trả đúng số tiền nêu trên.
- Hai phiếu gửi lại trả HTTP 200; thay số tiền cùng mã bị HTTP 400. Danh sách, số tiền và trạng thái phiếu thu/chi của khu không đổi sau kiểm tra gửi lại.
- Trình duyệt đăng nhập và mở ba màn hình quyết toán A, hóa đơn B, điều chỉnh C thành công, không có lỗi JavaScript trong các ca này.
- Hai tài khoản STAFF/TENANT đăng nhập qua API và trình duyệt, mở hóa đơn B đúng 1.763.000đ. STAFF bị chặn hợp đồng ngoài khu; TENANT chỉ được liệt kê hợp đồng/hóa đơn B và bị chặn hóa đơn C. Cả hai bị chặn API quản lý tài khoản.
- Tổng tiền thực vào trừ thực chi của khu thử, gồm cả cọc và ca C: **7.745.000đ**. Đây không phải lợi nhuận hay doanh thu tiền thuê.

Bằng chứng mới được lưu riêng trong `.runtime/local-demo/`: `api-smoke-2026-11-02.json`, `replay-smoke-2026-11-02.json`, `browser-smoke-2026-11-02.json` và ảnh chụp. Giữ nguyên bộ bằng chứng ngày 01/11. Lượt này không chạy lại toàn bộ Maven/SQL test suite và không tạo mới toàn bộ vòng đời qua UI.

Bằng chứng tài khoản/phân quyền: `.runtime/local-demo/role-accounts-2026-11-02.json`, `role-browser-2026-11-02.json` và ảnh `staff-demo-2026-11-02.png`, `tenant-demo-2026-11-02.png` cùng thư mục.
