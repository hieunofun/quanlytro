# Chuẩn hóa mô hình quản lý nhà trọ

Phạm vi là 17 bảng trong `quanlytro.sql`. Các view là dữ liệu suy ra, không thêm bản sao có thể cập nhật độc lập. Phân tích chuẩn hóa xét cả khóa chính và khóa ứng viên nghiệp vụ, không lấy sự tồn tại của cột id làm bằng chứng.

## 1NF — Loại bỏ nhóm giá trị lặp

- Mỗi `maintenance_images` lưu một URL. Không có cột chứa một danh sách ảnh ghép bằng dấu phẩy/xuống dòng.
- Một dòng `contract_members` lưu một người trong một khoảng cư trú. Một dòng `employee_assignments` lưu một cặp nhân viên–khu.
- Mô tả phòng và ghi chú là văn bản tự do, không được dùng như danh sách mã tiện nghi để lọc/đếm. Danh mục tiện nghi độc lập nằm ngoài MVP.

## 2NF — Phụ thuộc đầy đủ vào mọi khóa ứng viên

| Bảng / khóa ứng viên nghiệp vụ | Phụ thuộc có ý nghĩa |
|---|---|
| employee_assignments: (user_id, property_id) | Ngày và trạng thái của chính cặp phân công |
| rooms: (property_id, room_code) | Thuộc tính của phòng cụ thể trong khu |
| contract_members: (contract_id, tenant_id, effective_from) | Quan hệ và ngày rời của lần cư trú đó |
| service_price_configs: (property_id, service_item_id, effective_from) | Giá của dịch vụ tại khu và mốc hiệu lực cụ thể |
| meter_readings: (contract_id, billing_period, service_item_id) | Chỉ số, ngày ghi và người ghi của đúng kỳ/loại dịch vụ |
| invoices: (contract_id, billing_period) | Ngày phát hành, hạn thu và trạng thái của hóa đơn kỳ đó |
| maintenance_images: (maintenance_request_id, url) | Thông tin ảnh thuộc yêu cầu cụ thể |

`contract_id → room_id` nên đã bỏ room_id khỏi meter_readings; `tenant_id → full_name, identity_number, phone...` nên đã bỏ thông tin cá nhân khỏi contract_members. Bảng chỉ số không chứa tên/đơn vị dịch vụ vì chúng thuộc service_items. Dòng đo đếm trên hóa đơn không lặp service_item_id hoặc quantity đã được xác định qua meter_reading_id.

## 3NF — Không lặp thuộc tính phụ thuộc qua thực thể khác

- `users` chỉ chứa dữ liệu đăng nhập. Thông tin cá nhân của người cư trú nằm tại tenant_profiles; các lần cư trú tham chiếu cùng hồ sơ.
- `maintenance_requests.contract_id → contracts.room_id → rooms.property_id`: chỉ lưu contract_id ở yêu cầu. Khu và phòng được truy ra bằng JOIN, không có ba FK độc lập có thể ghép sai.
- Tên và cách tính dịch vụ nằm tại service_items; lịch sử giá riêng tại service_price_configs.
- Cảnh báo tham chiếu duy nhất một bản ghi chỉ số. Loại dịch vụ, phòng, lượng dùng và phần trăm tăng không được nhập lại trong cảnh báo.
- Lượng dùng, thành tiền, tổng hóa đơn, đã thu, công nợ và trạng thái thanh toán được tính trong view. Không có nhiều cột tổng hợp phải tự đồng bộ sau mỗi giao dịch.
- Trạng thái phòng đang thuê được tính theo thời gian hợp đồng; rooms chỉ lưu tình trạng khai thác thực tế.

Các thuộc tính phi khóa của bảng quan hệ mô tả sự kiện do khóa xác định. Không có phụ thuộc xác định tên người, tên dịch vụ hoặc thông tin khu qua một FK rồi lưu lại như thuộc tính danh mục hiện tại trong cùng bảng quan hệ.

## Snapshot lịch sử và lượng tính phí

`item_name_snapshot`, `unit_name_snapshot`, `unit_price_snapshot` là tên/đơn vị/giá đã áp dụng cho dòng hóa đơn, không phải bản sao có nghĩa “giá hiện tại”. Chúng được giữ khi danh mục đổi. Thuộc tính snapshot mô tả sự kiện giao dịch theo thời điểm, không được coi là phụ thuộc vào giá hiện tại của service_items.

Dòng đo đếm dùng chỉ số đã khóa để lấy lượng dùng; không có thêm cột chênh chỉ số có thể sai. Dòng tiền phòng/phí tháng giữ lượng tính phí là ngày/ngày-người của kỳ. Mẫu số số ngày tháng lấy từ kỳ của hóa đơn qua view, không lưu lại trên từng dòng; kết quả thành tiền làm tròn đến hai chữ số thập phân.

`checkout_records.contract_id` là khóa ứng viên UNIQUE. Cọc và công nợ snapshot mô tả trạng thái trước cấn tại lần quyết toán đó; chúng không bị tính lại từ công nợ hiện tại sau cấn. Số quyết toán tính từ ba đầu vào trong view. Vì vậy giữ được lịch sử mà không lưu thêm bản sao của kết quả phép trừ.

## Các quy tắc ngoài phạm vi chuẩn hóa

Chuẩn hóa không tự bảo đảm tiền thu không vượt nợ, người dùng có quyền truy cập, khoảng thuê không chồng lấn hoặc thao tác đồng thời an toàn. Các quy tắc này được tách rõ thành CHECK, FK, UNIQUE, EXCLUDE, trigger hoặc kiểm tra tại tầng API. Không dùng việc có 17 bảng hay 55 CHECK để thay cho chứng minh phụ thuộc dữ liệu.

## Đối chiếu prototype

Tài liệu này đánh giá mô hình mục tiêu 17 bảng. Không khẳng định backend prototype 29 bảng V1–V9 tự đạt các tính chất trên; việc chuyển dữ liệu và API sang mô hình này là công việc của bước triển khai tiếp theo.
