# Phương án CSDL 49 bảng — đã duyệt triển khai

Ngày lập: **19/10/2026**. Chủ nhiệm đồng ý triển khai ngày **26/10/2026**. Kết quả mã nguồn, sơ đồ và kiểm chứng: [Bản triển khai 49 bảng](TRIEN-KHAI-MO-HINH-49-BANG.md).

## 1. Trả lời trực tiếp

**Nếu chọn phạm vi khuyến nghị trong D01–D24, phương án cụ thể ở tài liệu này gồm 49 bảng ứng dụng.** Con số này gồm nghiệp vụ và phần hỗ trợ các chức năng V1 như tài khoản, mã khôi phục, tệp và nhật ký; không tính view, index, sequence, `flyway_schema_history` hoặc 29 bảng lưu trữ trong `prototype_v9`.

Phương án này đã được chọn để triển khai. 49 là số bảng của phạm vi đã chốt, không phải tiêu chí càng nhiều càng tốt. SQL, DBML, migration V11, backend và ERD local đã được cập nhật; slide và bản dbdiagram trực tuyến cũ chưa được cập nhật.

Nguồn phạm vi là [bản phân tích yêu cầu và rà thực tế](PHUONG-AN-PHAN-TICH-LAI-YEU-CAU-VA-CSDL.md), gồm 24 quyết định, 60 Use Case và 65 tình huống nghiệm thu. Tài liệu này chọn cách tổ chức dữ liệu cụ thể từ các ứng viên ở bản đó; một số tên bảng được gộp/đổi như giải thích dưới đây.

| Nhóm | Số bảng | Nội dung |
| --- | ---: | --- |
| A. Tài khoản, quyền, nhật ký | 6 | Đăng nhập, hồ sơ người, phạm vi nhân viên, ủy quyền, khôi phục và tra soát |
| B. Khu/phòng, tiện nghi và tài sản | 7 | Danh mục phòng, giá, khai thác và vị trí đồ vật theo thời gian |
| C. Giữ chỗ, hợp đồng, cư trú, bàn giao | 12 | Tách người ký/liên hệ/người ở, điều khoản có hiệu lực, trả phòng và quyết toán |
| D. Dịch vụ, công tơ, cảnh báo | 9 | Đăng ký, giá, điểm đo, khoảng tiêu thụ và căn cứ cảnh báo |
| E. Sửa chữa và bảo trì | 4 | Yêu cầu theo vị trí, lịch sử xử lý, chi phí và lịch định kỳ |
| F. Chứng từ, tiền và cọc | 7 | Các khoản phải thu/phải hoàn, tiền thật, quỹ tiền khách, phân bổ/cấn/chuyển |
| G. Thông báo | 2 | Nội dung và trạng thái từng người nhận |
| H. Tệp và ảnh | 2 | Một nguồn lưu tệp, liên kết đúng đối tượng nghiệp vụ |
| **Tổng** | **49** | **6 + 7 + 12 + 9 + 4 + 7 + 2 + 2** |

Cách đọc: mục 2 giải thích các khối chính; mục 3 là danh sách đủ 49 bảng và FK; mục 4–6 giải thích các quan hệ và ví dụ; mục 7 nêu những lựa chọn phải duyệt.

## 2. Cách tổ chức để tránh thêm bảng hoặc lưu trùng

### 2.1. Hợp đồng làm trung tâm trách nhiệm, vị trí làm trung tâm vận hành

Hợp đồng gắn một phòng cố định. Người ký/chịu trách nhiệm, người liên hệ và người ở thực tế là ba quan hệ riêng. Chuyển hẳn sang phòng khác hoặc thay bên thuê chịu trách nhiệm chính dùng hợp đồng kế tiếp có liên kết hợp đồng trước; chỉ đổi liên hệ hoặc gia hạn/giá thì không bắt lập hợp đồng mới.

Sự cố gắn **phòng, khu vực chung hoặc vị trí tài sản**, không bắt phải có hợp đồng. Khoản người thuê phải bồi thường mới gắn hợp đồng chịu trách nhiệm. Nhờ đó sửa phòng trống không cần tạo hợp đồng giả, và người báo hỏng không mặc nhiên là người phải trả tiền.

### 2.2. Các khoản phải thu dùng chung cấu trúc chứng từ

`billing_documents` + `billing_items` thay cho việc tạo riêng một cặp bảng cho từng loại tiền: hóa đơn tháng, phát sinh cuối kỳ, bồi thường, điều chỉnh và khoản phải hoàn được duyệt. Mỗi loại vẫn có quy tắc riêng và dữ liệu nguồn rõ ràng.

Một hóa đơn tháng/hợp đồng/kỳ vẫn chỉ có một bản chuẩn. Chứng từ điều chỉnh hoặc phát sinh phải có lý do/nguồn và tác động rõ; không dùng đổi tên chứng từ để thu lại cùng một khoản. Chi phí sửa thật ở `maintenance_costs`, còn bồi thường được duyệt ở dòng chứng từ; hai số này không dùng chung một cột.

### 2.3. Tiền thật khác việc sử dụng tiền

`cash_transactions` ghi tiền thực nhận/thực chi. `fund_accounts` chia rõ cọc, tiền trả trước, tiền giữ chỗ và tiền chưa phân bổ. `fund_entries` ghi từng tác động vào các quỹ hoặc khoản phải thu: nhận vào, phân loại, trả nợ, cấn cọc, chuyển cọc, hoàn và đảo sai.

Một lần chuyển 5 triệu có thể được chia cọc 2 triệu, trả nợ 2,7 triệu, dư 0,3 triệu; tiền thật vẫn chỉ là 5 triệu. Việc phân bổ/cấn/chuyển không tạo một lần nhận tiền mới.

### 2.4. Một hệ lưu tệp cho nhiều nghiệp vụ

`stored_files` lưu tệp và metadata một lần. `file_attachments` liên kết tệp với phòng, biên bản, điểm đo, sự cố hoặc chứng từ. Không cần một bảng ảnh riêng cho mỗi tính năng; ảnh giới thiệu phòng bị gỡ vẫn không xóa ảnh đã dùng làm căn cứ bàn giao.

### 2.5. Không tạo bảng cho dữ liệu đã tính được

Không có bảng `invoice_totals`, `debts`, `deposit_balances`, `room_occupancy_statistics` hoặc bảng riêng cho mỗi trạng thái. Tổng/nợ/số dư/trạng thái thanh toán/lấp đầy được tính qua view. Tầng vẫn là `rooms.floor_number` trong phương án này; chưa có nghiệp vụ độc lập cần bảng tầng.

## 3. Danh sách đủ 49 bảng và liên kết

Mỗi bảng có PK `id BIGINT`, trừ khóa ghép nếu lúc thiết kế vật lý quyết định phù hợp hơn. Dấu `?` nghĩa FK tùy chọn, **không có nghĩa được bỏ kiểm tra**. Các FK người thao tác như `created_by`, `approved_by`, `recorded_by` đều tới `users`; để dễ đọc, bảng dưới tập trung vào FK nghiệp vụ chính.

### A. Tài khoản, quyền và nhật ký — 6 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 01 | `users` | Username, mật khẩu băm, vai trò, tình trạng tài khoản | Username UNIQUE; ADMIN/STAFF/TENANT. Không lưu hồ sơ cư trú tại đây. |
| 02 | `tenant_profiles` | Hồ sơ dùng chung của người ký, người ở hoặc người có quyền liên quan | `user_id? → users`; user_id UNIQUE nếu có. CCCD có giá trị kiểm tra UNIQUE, không gộp người chỉ vì trùng tên. |
| 03 | `employee_assignments` | Một lần phân công nhân viên cho khu **hoặc** phòng, từ–đến | `user_id → users`; đúng một trong `property_id? → properties`, `room_id? → rooms`. Người được giao phải là STAFF. |
| 04 | `staff_permission_grants` | Quyền hành động/hạn mức được giao trong một phân công | `assignment_id → employee_assignments`; mã hành động, hạn mức, thời hạn. Không cho STAFF tự cấp quyền hay nâng quyền ADMIN. |
| 05 | `password_reset_tokens` | Hash mã khôi phục, ngày cấp/hết hạn/dùng, giới hạn thử | `user_id → users`; người cấp nếu có → users. Không lưu mã thô/mật khẩu trong log. |
| 06 | `audit_events` | Người/thời gian/thao tác/đối tượng/lý do và phần trước/sau được phép lưu | `actor_user_id? → users`, phân biệt tác vụ SYSTEM. Định danh đối tượng lịch sử không bắt có FK sống tới nháp đã xóa; audit không làm nguồn thay cho sổ tiền. |

### B. Khu/phòng và tài sản — 7 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 07 | `properties` | Khu nhà, mã, địa chỉ, lịch chốt/thu và trạng thái khai thác | Mã khu UNIQUE; một chủ nhà trong phạm vi đồ án, chưa tạo mô hình nhiều chủ nhà. |
| 08 | `rooms` | Phòng, số tầng, diện tích, sức chứa, ghi chú nội bộ, ngày đưa vào/ngừng danh mục | `property_id → properties`; `(property_id, room_code)` UNIQUE. Giá hiện tại/trạng thái hiển thị lấy từ nguồn lịch sử và cư trú, không nhập hai nơi. |
| 09 | `room_price_history` | Giá niêm yết theo mốc hiệu lực | `room_id → rooms`; `(room_id, effective_from)` UNIQUE. Không dùng để tính lại giá hợp đồng đã ký. |
| 10 | `room_operating_periods` | Các khoảng READY/TURNOVER/MAINTENANCE/INACTIVE | `room_id → rooms`; khoảng khai thác không chồng. “Có người đang giữ phòng” lấy từ room_possessions, không trộn với trạng thái sẵn sàng. |
| 11 | `asset_types` | Danh mục điều hòa, giường, tủ, máy bơm, hạng mục tiện nghi | Mã loại UNIQUE; tên/đơn vị danh mục không chép vào mọi liên kết hiện tại. |
| 12 | `assets` | Một đồ có mã hoặc một nhóm đồ cùng loại/số lượng theo D04 | `asset_type_id → asset_types`; mã nội bộ UNIQUE; số lượng > 0. Không quản lý mua sắm/khấu hao trong phạm vi này. |
| 13 | `asset_placements` | Tài sản ở đâu trong từng khoảng thời gian | `asset_id → assets`; đúng một trong `room_id? → rooms`, `property_id? → properties` cho đồ chung. Một tài sản/nhóm không ở hai nơi cùng lúc. |

Danh sách tiện nghi phòng được JOIN `rooms → asset_placements → assets → asset_types`; không thêm `room_amenities` giữ một danh sách hiện tại thứ hai. Đồ giá trị cao có mã riêng; các đồ giống nhau có thể một nhóm số lượng. Chuyển một phần nhóm thì tách nhóm có căn cứ trước, chưa làm hệ quản lý kho tổng quát.

### C. Hợp đồng, cư trú và bàn giao — 12 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 14 | `reservations` | Giữ chỗ có thời hạn, người đề nghị, điều kiện hủy/hoàn, trạng thái | `room_id → rooms`; `tenant_id? → tenant_profiles`. Chưa xác minh người thì lưu thông tin liên hệ tối thiểu, không tạo hồ sơ giả. |
| 15 | `contracts` | Mã hợp đồng, phòng cố định, vòng đời, liên kết lần thuê trước | `room_id → rooms`; `reservation_id? → reservations`; `previous_contract_id? → contracts`. Không cho một giữ chỗ sinh hai hợp đồng thuê cùng phạm vi. |
| 16 | `contract_parties` | Người ký/có vai trò theo thỏa thuận, căn cứ và mốc trách nhiệm | `contract_id → contracts`; `tenant_id → tenant_profiles`. Bản cơ sở có một bên thuê chính chịu trách nhiệm; người bảo lãnh/chứng kiến không tự bị coi là cùng chịu mọi nợ. |
| 17 | `contract_contacts` | Người liên hệ chính/phụ, khoảng được giao, quyền được cấp như xem tài chính | `contract_id → contracts`; `tenant_id → tenant_profiles`. Tối đa một liên hệ chính tại một thời điểm; đổi liên hệ không đổi bên chịu nợ. |
| 18 | `contract_residencies` | Từng người thực ở, ngày vào/rời, căn cứ xác nhận | `contract_id → contracts`; `tenant_id → tenant_profiles`. Gồm cả người ký nếu thực ở; không cộng một đại diện ngầm nữa. |
| 19 | `contract_terms` | Bản ký/phụ lục, số phiên bản, ngày ký/hiệu lực, giá thuê, cọc cam kết, hạn thuê, lịch thanh toán và điều kiện | `contract_id → contracts`; `(contract_id, version_no)` UNIQUE; phiên bản đã xác nhận bất biến. Các đoạn áp dụng được xác định từ mốc hiệu lực đã duyệt. |
| 20 | `room_possessions` | Thời điểm thực nhận quyền sử dụng phòng và thực giao lại | `contract_id → contracts`. Phòng suy qua hợp đồng; không tự điền ngày trả từ hạn trên giấy. Lịch thuê tương lai được quản lý riêng với tình trạng đang giữ thực tế. |
| 21 | `move_out_notices` | Ngày báo, dự kiến trả, lý do, kết quả xem xét, thay/hủy thông báo | `contract_id → contracts`; `supersedes_notice_id? → move_out_notices`. Thông báo không tự đóng cư trú/hợp đồng hoặc hoàn tiền. |
| 22 | `handovers` | Biên bản nhận/trả/giao trách nhiệm, thời điểm, người giao/nhận, xác nhận | `contract_id → contracts`; `possession_id? → room_possessions`. Mã biên bản UNIQUE; bản xác nhận khóa nội dung. |
| 23 | `handover_items` | Từng hạng mục, số lượng, tình trạng ghi nhận, thiếu/hỏng và ý kiến | `handover_id → handovers`; `asset_placement_id? → asset_placements`. Snapshot tình trạng lúc giao, không lấy tình trạng hiện tại thay quá khứ. |
| 24 | `contract_violations` | Vi phạm, ngày, lý do, trạng thái xử lý và kết luận | `contract_id → contracts`. Thay đổi được audit; vi phạm không tự sinh phạt tiền nếu chưa có căn cứ/quyết định. |
| 25 | `checkout_records` | Quyết toán nháp/đã duyệt, ngày hết tính tiền theo căn cứ, snapshot trước cấn, mốc chốt, người duyệt | `contract_id → contracts`; `return_handover_id? → handovers`. Tối đa một quyết toán đã duyệt/hợp đồng; phát sinh sau duyệt dùng chứng từ bổ sung, không sửa snapshot cũ. |

Giá thuê/cọc thỏa thuận nằm trong `contract_terms`. Không vừa cho sửa `contracts.rental_price/deposit_amount/end_date` vừa sửa phiên bản độc lập. Hạn/giá hiện hành là view từ phiên bản được duyệt; nếu sau này cần cache để tối ưu thì phải ghi rõ cache và cơ chế đồng bộ, không coi là nguồn nghiệp vụ thứ hai.

### D. Dịch vụ, công tơ và cảnh báo — 9 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 26 | `service_items` | Danh mục dịch vụ, đơn vị, cơ sở tính được hỗ trợ | Mã dịch vụ UNIQUE; phân biệt đo đếm/cả kỳ/chia ngày/đầu người/số lượng/một lần theo chính sách. |
| 27 | `service_price_configs` | Giá dịch vụ của khu theo hiệu lực | `property_id → properties`; `service_item_id → service_items`; khóa khu–dịch vụ–mốc hiệu lực. |
| 28 | `contract_service_subscriptions` | Dịch vụ hợp đồng dùng, từ–đến, số lượng/chế độ đã chọn | `contract_id → contracts`; `service_item_id → service_items`. Thay xe/số lượng đóng đoạn cũ/mở đoạn mới; có giá ở khu không tự nghĩa là hợp đồng đăng ký. |
| 29 | `meters` | Công tơ vật lý, mã, loại dịch vụ, ngày lắp/tháo | `room_id → rooms`; `service_item_id → service_items`. Một công tơ đang dùng/phòng/dịch vụ theo khoảng; không trừ chỉ số hai công tơ khác nhau. |
| 30 | `meter_readings` | Điểm đo, thời điểm thực đo, chỉ số, người ghi; phiên bản sửa được liên kết | `meter_id → meters`; `supersedes_reading_id? → meter_readings`; `handover_id? → handovers`. Sửa tạo phiên bản có lý do, không thay mất người/giá trị trước. |
| 31 | `consumption_segments` | Một khoảng tiêu thụ từ điểm đầu tới điểm cuối và bên chịu khoảng đó | `start_reading_id → meter_readings`; `end_reading_id → meter_readings`; `contract_id? → contracts`; `supersedes_segment_id? → consumption_segments`. Hai điểm cùng công tơ; contract NULL chỉ khi phần chủ nhà/phòng trống được xác định rõ. |
| 32 | `alert_policies` | Ngưỡng, cửa sổ, điều kiện so sánh/ngày tối thiểu theo khu hoặc phòng, theo dịch vụ | `service_item_id → service_items`; đúng một trong `property_id? → properties`, `room_id? → rooms`. Phòng ghi đè khu theo thời gian hiệu lực. |
| 33 | `consumption_alerts` | Lần đánh giá, phương pháp/baseline áp dụng, kết quả và người kiểm tra | `segment_id → consumption_segments`; `policy_id → alert_policies`; `supersedes_alert_id? → consumption_alerts`. Thiếu baseline khác thiếu số đo tính tiền. |
| 34 | `alert_baseline_sources` | Những khoảng lịch sử nào thực sự được dùng trong một đánh giá | `alert_id → consumption_alerts`; `source_segment_id → consumption_segments`; cặp này UNIQUE. Không chỉ lưu một số trung bình không truy được căn cứ. |

Điểm đo và đoạn đã dùng trong chứng từ được bảo toàn. Khi sửa điểm đo chưa khóa, các đoạn nháp phụ thuộc phải được kiểm tra/lập lại. Đã phát hành thì giữ bản cũ và tạo căn cứ/chứng từ điều chỉnh; không để phiên bản “mới nhất” làm đổi lượng trên hóa đơn cũ.

Phiên bản thay thế phải cùng điểm nghiệp vụ/khoảng nguồn thích hợp, có lý do và phê duyệt cần thiết; không chỉ thêm một dòng cùng giờ để né quy tắc. Cửa sổ so sánh tương lai dùng các phiên bản hợp lệ, trong khi chứng từ/cảnh báo cũ vẫn tham chiếu đúng nguồn đã khóa tại lần lập.

### E. Sửa chữa và bảo trì — 4 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 35 | `maintenance_requests` | Yêu cầu, vị trí, người báo/giao, loại/ưu tiên, hạn, trạng thái hiện tại | Đúng một đích: `room_id? → rooms`, `property_id? → properties` cho khu vực chung, hoặc `asset_placement_id? → asset_placements`; `schedule_id? → maintenance_schedules`. |
| 36 | `maintenance_events` | Từng lần tiếp nhận/giao việc/cập nhật/hoàn thành/tái phát | `request_id → maintenance_requests`; người thực hiện → users. Không chỉ giữ ghi chú cuối; không xóa lần hoàn thành cũ khi tái phát. |
| 37 | `maintenance_costs` | Các khoản chi phí sửa thực tế và chứng cứ/người ứng nếu có | `request_id → maintenance_requests`; `advanced_by_tenant_id? → tenant_profiles`. Chi phí không tự trở thành bồi thường hoặc tiền người thuê đã nộp. |
| 38 | `maintenance_schedules` | Lịch bảo trì định kỳ cho thiết bị hoặc vị trí, chu kỳ và hạn | Đúng một đích: `asset_id? → assets`, `room_id? → rooms`, `property_id? → properties`. Mỗi lần đến hạn sinh request có `scheduled_for`; UNIQUE lịch–kỳ công việc để không tạo lặp. |

Không có `maintenance_requests.contract_id` bắt buộc. Quyền người thuê báo dựa vào vị trí và cư trú tại thời điểm báo; quyền xem yêu cầu cũ còn kiểm tra người gửi/quyền liên quan. Người thuê phải bồi thường thì dòng chứng từ gắn hợp đồng và tham chiếu căn cứ sửa chữa/bàn giao.

### F. Chứng từ, tiền và cọc — 7 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 39 | `billing_documents` | Đầu chứng từ: loại, mã, kỳ nhãn, hạn, nháp/phát hành, căn cứ/duyệt | Đúng một trong `contract_id? → contracts`, `reservation_id? → reservations`; `checkout_id? → checkout_records`; `related_document_id? → billing_documents`. Hóa đơn MONTHLY bắt buộc có hợp đồng/kỳ. |
| 40 | `billing_items` | Từng khoản thu/giảm/phải hoàn, kỳ dịch vụ, cơ sở tính, giá/tên/đơn vị áp dụng và nguồn | `document_id → billing_documents`; `adjusts_item_id? → billing_items`; các FK căn cứ có điều kiện tới terms/subscription/segment/handover/cost/violation, giải thích ở mục 5. |
| 41 | `cash_accounts` | Quỹ tiền mặt hoặc tài khoản nhận/chi của chủ nhà, phạm vi vận hành | `property_id? → properties` khi thuộc một khu; NULL là quỹ chung do ADMIN/quyền thích hợp quản lý. Không phải ví tiền khách. |
| 42 | `cash_transactions` | Phiếu thu/chi tiền thật, số tiền, phương thức, tài khoản, ngày thật/ngày nhập, xác minh | `cash_account_id → cash_accounts`; `payer_or_payee_id? → tenant_profiles`. Chưa rõ người vẫn ghi được tiền đã xác minh; IN/OUT đều số dương. |
| 43 | `fund_accounts` | Quỹ tiền khách theo mục đích: DEPOSIT/ADVANCE/RESERVATION/UNAPPLIED, bên được hưởng và phạm vi | FK có điều kiện: `contract_id? → contracts`, `reservation_id? → reservations`, `unapplied_receipt_id? → cash_transactions`; `beneficiary_tenant_id? → tenant_profiles`. Không lưu cột balance chỉnh tay. |
| 44 | `fund_entries` | Mỗi lần tiền vào quỹ, áp vào nợ, phân loại/chuyển, hoàn hoặc đảo có nguồn | FK có điều kiện tới `cash_transactions`, `fund_accounts`, `billing_items`, `fund_entries` gốc. Kiểu bút toán quyết định trường nguồn/đích nào bắt buộc; chi tiết tại mục 5. |
| 45 | `fund_holds` | Khoản tạm giữ có căn cứ, số tiền, lý do, người duyệt, ngày rà lại/giải phóng | `fund_account_id → fund_accounts`; `related_billing_item_id? → billing_items`. Không tự biến tạm giữ thành bồi thường đã thu hoặc giữ vô thời hạn thiếu căn cứ. |

Đây là sổ quản lý tiền/đối soát phục vụ bài toán thuê trọ, không tuyên bố là hệ kế toán doanh nghiệp đầy đủ. Không có một bảng `compensation_cases` giữ tổng thứ hai: chứng từ COMPENSATION có các dòng DAMAGE chính là hồ sơ bồi thường có phê duyệt/trạng thái tranh chấp/căn cứ.

### G. Thông báo — 2 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 46 | `notifications` | Loại, nội dung, thời gian gửi, nguồn nghiệp vụ và khóa chống gửi lặp | Nguồn có thể FK tùy chọn tới `billing_documents`, `contracts`, `maintenance_requests`, `consumption_alerts`; thông báo chung không bắt nguồn. Có nguồn thì đúng loại/đúng phạm vi. |
| 47 | `notification_recipients` | Tài khoản nào nhận, thời điểm đọc | `notification_id → notifications`; `user_id → users`; cặp UNIQUE. `read_at IS NULL` là chưa đọc, không cần boolean riêng phải đồng bộ. |

### H. Tệp và ảnh — 2 bảng

| STT | Bảng | Lưu dữ kiện gì | FK chính / ràng buộc |
| --- | --- | --- | --- |
| 48 | `stored_files` | Mã lưu tệp, MIME, dung lượng, hash, người/thời điểm tải | `uploaded_by → users`; storage_key UNIQUE. Nội dung tệp đã dùng làm căn cứ phải bất biến; không thay file dưới cùng key. |
| 49 | `file_attachments` | Tệp được dùng ở đối tượng nào, chú thích, thứ tự và mức hiển thị | `file_id → stored_files`; đúng một FK đích trong danh sách ở mục 4.4. Một tệp có thể có nhiều liên kết, mỗi liên kết đúng quyền. |

## 4. Các liên kết chính được hiểu như thế nào?

### 4.1. Khu/phòng và hợp đồng

| Quan hệ | Lực lượng | Ý nghĩa |
| --- | --- | --- |
| properties — rooms | 1–N | Một khu nhiều phòng, một phòng thuộc một khu. |
| rooms — contracts | 1–N theo thời gian | Một phòng có nhiều lần thuê; lịch đã xác nhận không được chồng. |
| contracts — contract_terms | 1–N | Bản ký ban đầu và các phụ lục/điều kiện có hiệu lực, không sửa đè quá khứ. |
| contracts — tenant_profiles qua contract_parties | N–N có vai trò | Ai ký/giữ vai trò nào được ghi rõ, không dùng thay số cư trú. |
| contracts — tenant_profiles qua contract_residencies | N–N theo thời gian | Ai thực ở, vào/rời lúc nào; một người có thể có nhiều lần ở. |
| contracts — tenant_profiles qua contract_contacts | N–N có thời hạn/quyền | Ai là liên hệ hoặc được giao quyền; không tự đổi bên chịu nợ. |
| contracts — checkout_records đã duyệt | 1–0..1 | Có thể chưa trả/chưa chốt; bản chốt phải thuộc đúng hợp đồng. |
| users — tenant_profiles | 0..1–0..1 | Hồ sơ không bắt phải có đăng nhập; tài khoản quản lý có thể không có hồ sơ thuê. |

Tối đa một bản con qua UNIQUE không có nghĩa mọi cha bắt buộc có con. Nếu đã ký hợp đồng phải có bên thuê chính và bản terms ban đầu thì đó là điều kiện vòng đời phải kiểm tra cùng transaction.

### 4.2. Điện/nước và hóa đơn

| Quan hệ | Lực lượng | Ý nghĩa |
| --- | --- | --- |
| rooms — meters — meter_readings | Hai quan hệ 1–N | Công tơ thuộc phòng; mỗi công tơ có nhiều điểm đo. Thay công tơ tạo công tơ mới. |
| meter_readings — consumption_segments | 1–N qua hai vai trò đầu/cuối | Một điểm có thể là cuối đoạn cũ, đầu đoạn mới. |
| contracts — consumption_segments | 1–N, hợp đồng tùy chọn ở phía đoạn | Đoạn khách chịu gắn hợp đồng; đoạn phòng trống có căn cứ thuộc chủ nhà. |
| consumption_segments — billing_items gốc | 1–0..1 | Lượng của một đoạn chỉ được ghi thành khoản thu gốc một lần. Điều chỉnh tham chiếu dòng gốc/căn cứ riêng, không tạo lại khoản gốc thứ hai. |
| consumption_alerts — consumption_segments lịch sử qua alert_baseline_sources | N–N | Biết các đoạn nào dùng làm baseline, tái kiểm tra được lý do đánh giá. |
| contracts — service_items qua subscriptions | N–N theo thời gian | Giá khu có thể tồn tại dù hợp đồng không đăng ký dịch vụ tùy chọn. |
| billing_documents — billing_items | 1–N | Một chứng từ nhiều khoản; từng khoản có kỳ dịch vụ riêng. |

### 4.3. Tiền và công nợ

| Quan hệ | Lực lượng | Ý nghĩa |
| --- | --- | --- |
| cash_accounts — cash_transactions | 1–N | Quỹ/tài khoản chủ nhà nhận/chi nhiều lần. |
| cash_transactions — fund_entries nhận/hoàn | 1–N | Một lần thu/chi có thể chia nhiều mục đích, tổng phần ghi quỹ phải khớp tiền thật đã xác nhận. |
| fund_accounts — fund_entries | 1–N theo vai trò nguồn/đích | Nhiều tác động tạo số dư; chuyển nội bộ nối hai quỹ. |
| fund_accounts — billing_items qua fund_entries loại PAYMENT | N–N | Một quỹ trả nhiều khoản, một khoản nhận nhiều lần từ nhiều nguồn. |
| fund_accounts — fund_holds | 1–N | Giữ một phần tiền có căn cứ, không trừ nó như đã chi/cấn. |
| billing_items — billing_items qua adjusts_item_id | 1–N | Một khoản gốc có thể có nhiều điều chỉnh được duyệt, không sửa/xóa bản gốc. |

### 4.4. Ảnh/tệp liên kết bằng FK thật

`file_attachments` có các FK đích tùy chọn: `room_id → rooms`, `asset_id → assets`, `handover_item_id → handover_items`, `meter_reading_id → meter_readings`, `maintenance_request_id → maintenance_requests`, `maintenance_cost_id → maintenance_costs`, `contract_term_id → contract_terms`, `billing_document_id → billing_documents`, `violation_id → contract_violations`, `fund_entry_id → fund_entries`.

**Mỗi dòng chỉ được điền một FK đích**, dùng CHECK đếm đúng một giá trị không NULL. Không dùng cặp chuỗi `entity_type/entity_id` không có FK rồi khẳng định đã chống mồ côi. Một file dùng ở phòng và bàn giao là hai dòng liên kết tới cùng stored_file, không sao metadata/nội dung tệp.

Liên kết tệp có thể bị gỡ khỏi ảnh giới thiệu phòng, nhưng liên kết chứng cứ đã chốt không được gỡ tùy tiện. Xóa vật lý tệp chỉ khi không còn liên kết/căn cứ cần giữ. Quyền tải phải kiểm tra đối tượng gắn tệp, không công khai mọi file theo đường dẫn.

## 5. Chốt rõ nhóm tài chính để không thu/hoàn hai lần

### 5.1. `billing_documents` và `billing_items` có những loại nào?

| Loại chứng từ | Ví dụ | Liên kết/cách bảo toàn |
| --- | --- | --- |
| MONTHLY | Thu tiền phòng tháng 10 và điện/nước của cửa sổ đã chốt | `contract_id` + kỳ nhãn; UNIQUE có điều kiện cho đúng loại MONTHLY. Từng dòng có khoảng dịch vụ riêng. |
| FINAL_CHARGE | Điện/nước kỳ cuối chưa nằm trên hóa đơn tháng | Hợp đồng + quyết toán nếu có; dòng tham chiếu đoạn đo chưa thu. Không thu lại đoạn đã có ở chứng từ khác. |
| COMPENSATION | Giường hỏng do lỗi người thuê, phần được xác nhận 500.000 | Hợp đồng chịu trách nhiệm, từng dòng DAMAGE có căn cứ bàn giao/sửa chữa, phê duyệt và tình trạng tranh chấp. |
| ADJUSTMENT | Giảm tiền phòng đã thu trước do hai bên chốt trả sớm | `related_document_id` và `adjusts_item_id` trỏ bản/dòng gốc; tăng/giảm có chiều riêng, tiền tuyệt đối không âm. |
| REIMBURSEMENT | Người thuê ứng sửa 400.000 được chủ nhà xác nhận phải hoàn | Dòng CREDIT có nguồn maintenance_cost; chưa phải đã chi/đã bù trừ. |

Đây là **cùng cấu trúc đầu–chi tiết**, không phải trộn mọi nghiệp vụ mà bỏ quy tắc. Chứng từ nào bắt buộc có hợp đồng, nguồn, người duyệt hoặc liên kết gốc phải kiểm tra theo loại. Chứng từ trước khi có hợp đồng chỉ được gắn giữ chỗ có thật; không tạo hợp đồng giả để giữ tiền.

Các thuộc tính cốt lõi của dòng: `item_kind`, `direction` DEBIT/CREDIT, tên/đơn vị/đơn giá áp dụng, cơ sở tính, lượng tính phí khi cần, khoảng/kỳ dịch vụ, căn cứ và trạng thái xác minh/tranh chấp. Tổng tiền được tính, không cho vừa nhập tổng vừa nhập các nguồn độc lập.

- Tiền phòng: nguồn `contract_term_id → contract_terms`; có thể nhiều dòng cho nhiều đoạn giá/ngày.
- Dịch vụ đăng ký: `subscription_id → contract_service_subscriptions`; giá tham chiếu cấu hình đã áp dụng nếu cần, không tự lấy danh mục hiện tại để đổi chứng từ cũ.
- Điện/nước thực đo: `segment_id → consumption_segments`; lượng lấy từ hai điểm đo đã khóa. Không lưu thêm lượng dùng nhập tay cạnh nguồn đo.
- Khoản hỏng đồ: `handover_item_id? → handover_items`, `maintenance_cost_id? → maintenance_costs` khi là căn cứ; việc giữ cả hai căn cứ được phép nếu hai vai trò khác nhau, không coi chi phí sửa là tự động quyết định bồi thường.
- Phạt/miễn giảm có căn cứ: `violation_id? → contract_violations` nếu liên quan; vẫn cần quyết định/thỏa thuận, không cứ có vi phạm là có quyền thu.
- Ước tính do thiếu số đo: dòng được đánh dấu ESTIMATED, ghi phương pháp, cửa sổ, người/căn cứ duyệt và lượng ước tính; **không tạo điểm đo thực giả**. Khi có số thực, điều chỉnh có liên kết, không thu cả ước tính và toàn bộ lượng thật như hai khoản độc lập.

Lượng phí tháng có thể là snapshot ngày/ngày-người/xe-ngày tại lúc phát hành. **Mẫu số lấy từ kỳ tính của dòng**, không máy móc lấy kỳ nhãn hóa đơn: phí tháng 09 có 30 ngày dù được thu trên chứng từ kỳ 10 có 31 ngày. Khoản một lần/giảm/bồi thường không bị chia ngày.

Mỗi dòng gốc cần khóa nghiệp vụ chống tạo lặp theo nguồn cụ thể. Ví dụ đoạn điện chỉ tạo một dòng thu gốc; một sự cố có nhiều khoản khác nhau thì phân biệt từng khoản, không UNIQUE cả request để làm mất các khoản hợp lệ. Gửi lại cùng đề nghị/mã thao tác không tạo thêm chứng từ bồi thường hoặc điều chỉnh.

### 5.2. Các loại quỹ tiền khách trong `fund_accounts`

| Loại | Phạm vi bắt buộc | Ý nghĩa |
| --- | --- | --- |
| DEPOSIT | Hợp đồng, bên có quyền nhận lại được xác định | Cọc bảo đảm đang giữ; khác số cọc cam kết trong terms. |
| ADVANCE | Hợp đồng, bên có quyền được xác định | Tiền trả trước/tiền dư đã được xác minh thuộc lần thuê này. |
| RESERVATION | Giữ chỗ | Tiền giữ phòng trước hợp đồng; chuyển sang cọc bằng bút toán, không thu lần hai. |
| UNAPPLIED | Phiếu thu IN đã xác minh, `unapplied_receipt_id` UNIQUE | Tiền chưa rõ người/đích hoặc đang chờ phân loại; có thể để beneficiary NULL. Không tự trở thành cọc hoặc tự giảm nợ một phòng đoán theo số tiền. |

DEPOSIT/ADVANCE không đồng thời gắn reservation/phiếu chưa phân bổ như một phạm vi khác. RESERVATION chỉ gắn giữ chỗ; UNAPPLIED gắn phiếu nguồn. Phân loại quỹ và các FK tương ứng có CHECK, không cho một quỹ vừa thuộc hai hợp đồng hoặc tự đổi chủ bằng UPDATE.

Một hợp đồng có thể có cả quỹ DEPOSIT và ADVANCE, với quyền thụ hưởng được xác minh. Người nộp hộ không tự trở thành người nhận cọc; chủ quỹ/quyền nhận được ghi riêng có căn cứ. Phương án cơ sở chuyển giữa hợp đồng của cùng bên có quyền; chuyển quyền tiền sang một người khác cần thỏa thuận/phê duyệt riêng, không tự suy từ việc họ ở lại.

### 5.3. Các FK có điều kiện của `fund_entries`

Các cột liên kết ứng viên: `cash_transaction_id`, `from_fund_id`, `to_fund_id`, `debit_item_id`, `credit_item_id`, `original_entry_id`. Tất cả đều là FK thật tới bảng tương ứng; không phải chỉ lưu một mã loại và ID tùy ý.

| Loại bút toán | Nguồn → đích bắt buộc | Tác động cần kiểm soát |
| --- | --- | --- |
| RECEIVE | cash transaction IN → quỹ | Tiền đã xác minh vào quỹ. Một phiếu có nhiều RECEIVE nhưng tổng bằng số thực nhận khi chốt ghi sổ. |
| PAYMENT | quỹ → dòng DEBIT đã phát hành/đủ điều kiện | Giảm quỹ và giảm nợ đúng dòng; không vượt số dư khả dụng/nợ. Dùng DEPOSIT là cấn cọc, không là thu tiền mới. |
| TRANSFER | quỹ nguồn → quỹ đích | Một số tiền duy nhất, giảm nguồn/tăng đích cùng transaction; dùng cho phân loại tiền, giữ chỗ sang cọc hoặc chuyển cọc được duyệt. |
| REFUND | quỹ → cash transaction OUT | Giảm tiền đang giữ chỉ khi thực chi đã xác nhận. Một lần hoàn có thể lấy từ cọc và trả trước, tổng các phần bằng tiền chi thật. |
| CREDIT_TO_FUND | dòng CREDIT độc lập được duyệt → quỹ | Ghi nhận khoản phải hoàn như người thuê ứng sửa đã được xác minh. Không phải dòng tiền nhận thật; không cấp quá phần credit chưa dùng. |
| ADJUSTMENT_RELEASE | dòng CREDIT điều chỉnh + PAYMENT gốc → đúng quỹ nguồn của PAYMENT đó | Khôi phục phần đã trả vượt sau khi giảm khoản gốc; không khôi phục vượt phần đủ điều kiện/đã áp dụng. |
| REVERSAL | original_entry_id → bút toán trước | Đảo phần sai có liên kết, tiền dương; không chép lại nguồn/đích như dữ kiện tùy sửa. Không đảo quá phần chưa đảo hoặc làm âm nguồn liên quan. |
| OPENING_BALANCE | căn cứ số dư được đối soát → quỹ | Chỉ phục vụ mở sổ dữ liệu cũ được duyệt. Không tạo phiếu thu tiền ngày migration và không tính thành tiền mới nhận. |

Mỗi loại chỉ điền các FK của vai trò cần thiết; trường không dùng phải NULL. Với REVERSAL, chiều và đối tượng chịu tác động suy từ bút toán gốc; bút toán đảo không được trỏ chính nó hoặc tạo vòng tham chiếu.

Các bất biến phải thi hành khi ghi sổ:

1. Tiền giao dịch/bút toán đều > 0; chiều thu/chi/tăng/giảm nằm ở loại, không sửa thành khoản thu âm để giấu dấu.
2. RECEIVE/REFUND chỉ liên kết phiếu cùng chiều/trạng thái; tổng các phần khớp số phiếu đã xác nhận.
3. Số dư quỹ = tổng tăng − tổng giảm có hiệu lực; khả dụng = số dư − tạm giữ hợp lệ. Không lưu thêm balance chỉnh tay.
4. PAYMENT không vượt cả tiền nguồn lẫn nợ đích. Hai lần chuyển ngân hàng thật vẫn phải ghi đủ, dù phần chưa dùng trở thành tiền dư.
5. Mã yêu cầu chống bấm lại; tham chiếu tài khoản + giao dịch ngân hàng khi có chống hai nhân viên ghi cùng khoản bằng hai mã nội bộ. Hai khoản thật cùng số tiền/ngày không bị coi là trùng chỉ vì số tiền giống nhau.
6. Đảo phân bổ nhầm giữ phiếu tiền thật nguyên, không tạo thu/chi lại. Nếu phiếu được xác nhận nhầm dù tiền chưa nhận, cần quy trình hủy sai có căn cứ và đảo các phần liên quan; tiền thật đã nhận rồi hoàn phải có OUT thật.
7. TRANSFER giữa hai hợp đồng kiểm tra quyền cả nguồn/đích và bên hưởng; một đầu lỗi rollback cả hai, không tự chuyển nợ cùng với cọc.
8. Giảm giá một khoản chưa trả làm giảm nợ; giảm khoản đã trả có thể khôi phục phần tiền nguồn phải hoàn. Không vừa dùng một credit để giảm nợ rồi cấp cùng toàn bộ credit thành tiền quỹ lần nữa.

Ví dụ điều chỉnh: khoản thuê 3.000.000 đã trả đủ, được giảm 500.000 → khoản đúng 2.500.000. ADJUSTMENT_RELEASE khôi phục 500.000 từ phần PAYMENT đã dùng về đúng nguồn, khoản gốc còn được thanh toán ròng 2.500.000. Nếu nguồn là cọc thì cọc được phục hồi; nếu là trả trước thì trả trước được phục hồi. Đến khi có OUT/REFUND thì mới ghi đã hoàn tiền thật.

## 6. Đọc liên kết qua các tình huống thực tế

### 6.1. Khách mới → ký → nhận tiền → nhận phòng

Tạo hồ sơ tại `tenant_profiles`, hợp đồng ở `contracts`, người ký ở `contract_parties`, người liên hệ ở `contract_contacts`, bản ký ở `contract_terms`. Khi thực ở mới có `contract_residencies` và nhận quyền phòng ở `room_possessions`.

Nhận một lần 5.000.000: một `cash_transactions` IN; RECEIVE vào UNAPPLIED 5.000.000; TRANSFER 2.000.000 vào DEPOSIT và 3.000.000 vào ADVANCE theo xác nhận. Sau đó PAYMENT trả dòng tiền phòng lấy từ ADVANCE. Nguồn tiền thật vẫn chỉ 5.000.000.

Biên bản nhận phòng ở `handovers`, đồ/tình trạng ở `handover_items`, ảnh ở `stored_files`/`file_attachments`, điểm chỉ số đầu ở `meter_readings`. Cọc cam kết 3.000.000 nhưng mới phân bổ thực vào DEPOSIT 2.000.000 thì còn thiếu cam kết 1.000.000; nhận phòng thiếu cọc cần phê duyệt, không tự có thêm tiền.

### 6.2. Đổi người liên hệ hoặc người ở cùng

Chỉ đổi liên hệ: kết thúc khoảng của A/mở B ở `contract_contacts`, audit quyền thay đổi; `contracts`, nợ, cọc và chỉ số không bị chốt lại.

Người ở rời hoặc thêm người: ghi đúng khoảng ở `contract_residencies`; số ngày-người tính từ các khoảng này. Người ký không ở không cộng một người ngầm. Khách tạm về quê vẫn giữ phòng không tự thành đã trả phòng/được giảm tiền thuê.

Thay **bên thuê chịu trách nhiệm chính**: dùng hợp đồng kế tiếp theo phương án cơ sở, liên kết `previous_contract_id`; nợ trước mốc ở hợp đồng cũ. Người ở lại không tự gánh nợ/cọc chỉ vì được chọn làm liên hệ mới.

### 6.3. Gia hạn kèm tăng giá

Hợp đồng C01 giá 3.000.000 đến hết 31/10. Ký phụ lục từ 01/11 giá 3.300.000, kéo hạn đến hết 30/11: thêm bản `contract_terms` cho C01; không UPDATE giá tháng 10 và không bắt thêm C02 chỉ vì có giá mới.

Hóa đơn tháng 10 tham chiếu căn cứ/giá đã khóa; khoản tháng 11 dùng phiên bản mới. Nếu cọc cam kết tăng 500.000 thì nghĩa vụ bổ sung thay đổi, số tiền quỹ DEPOSIT chưa tăng cho đến khi thực có nguồn tiền/phân bổ hợp lệ.

Hạn được kéo dài vẫn phải kiểm tra phòng có hợp đồng/giữ chỗ tương lai không. Dù hạn/giá được suy từ terms, trigger/service phải khóa phòng và xét các điều kiện đã duyệt; không tuyên bố một UNIQUE hoặc EXCLUDE trên contracts tự kiểm tra được mọi JOIN sang terms.

### 6.4. Đổi khách giữa tháng và thay công tơ

Điểm R1=1000 đầu kỳ, R2=1050 khi A trả, R3=1120 lúc chốt của B. Hai `consumption_segments`: R1→R2 gắn hợp đồng A, R2→R3 gắn B. Hai bên cùng dùng R2 làm mốc, không nhập hai chỉ số bàn giao khác nhau. Nếu có thời gian phòng trống, đoạn đó thuộc chủ nhà, không chuyển sang B.

Khi thay công tơ: chốt cũ 1000→1080, mở mới 5→35. Hai công tơ/hai đoạn; lượng dùng 80+30=110. Ảnh/sự kiện điểm đo chứng minh mốc cũ/mới, không tính 35−1000.

Lịch sử cảnh báo ở `consumption_alerts` và nguồn tại `alert_baseline_sources`. Nhóm thuê/số người khác trước phải được ghi nhận khi đánh giá; thiếu baseline không chặn thu tiền nếu lượng/giá đủ căn cứ.

### 6.5. Sửa phòng trống hoặc đồ chung

`maintenance_requests` trỏ phòng trống hoặc asset_placement của máy bơm chung; tiến độ ở `maintenance_events`, khoản chi ở `maintenance_costs`. Không cần contract_id để tạo yêu cầu.

Nếu xác định người thuê phải chịu 500.000 trong khi chi sửa 800.000: tạo COMPENSATION có dòng DAMAGE 500.000 thuộc hợp đồng chịu trách nhiệm. Khoản phải thu không tự bằng 800.000; chưa đủ căn cứ/tranh chấp thì theo dõi chờ xử lý. Thu/cấn 500.000 bằng PAYMENT vào dòng đó, không chép thêm 500.000 ở checkout để thu lần nữa.

### 6.6. Trả giữa tháng sau khi đã thu tiền phòng

Đã thu 3.000.000 tiền phòng tháng 09; hai bên xác nhận hết tính từ 16/09, điện cuối 200.000. Tiền phòng đúng 1.500.000 → ADJUSTMENT giảm 1.500.000; FINAL_CHARGE điện 200.000 có nguồn đoạn đo cuối.

Phần tiền đã dùng vượt được khôi phục về nguồn theo mục 5.3, áp 200.000 vào điện nếu được phép, còn 1.300.000 phải hoàn/giữ trả trước theo xác nhận. Bản tháng 09 gốc không bị sửa/xóa. `checkout_records` ghi kết quả chốt; `cash_transactions` OUT và REFUND mới chứng minh đã hoàn thật.

### 6.7. Chuyển phòng và chuyển cọc

Hợp đồng C01 phòng 203 kết thúc quyền sử dụng theo mốc chuyển; C02 phòng 305 có `previous_contract_id=C01`, lý do nối là chuyển phòng. Chốt cũ/nhận mới bằng các biên bản, số đo và khoảng sử dụng đúng từng nơi.

Chuyển 800.000 cọc được duyệt: một TRANSFER trong `fund_entries` từ DEPOSIT C01 tới DEPOSIT C02. C01 giảm 800.000, C02 tăng 800.000, thu/chi thật mới bằng 0. Nợ C01 không tự thành nợ C02.

### 6.8. Hết hạn nhưng chưa bàn giao

Terms hết hạn không tự đóng `room_possessions`. `move_out_notices` chỉ là kế hoạch, không chứng minh chìa khóa/quyền sử dụng đã trả.

Khi khách cũ còn giữ phòng, khách mới chưa được kích hoạt nhận phòng dù lịch dự kiến đã đến; phải báo xung đột để người có quyền xử lý. Phòng đã trả nhưng còn dọn/sửa có thể ở TURNOVER/MAINTENANCE trong `room_operating_periods`, chưa phải READY.

## 7. Những quy tắc bắt buộc để 49 bảng không chỉ là danh sách

1. **Phân quyền theo cả phạm vi và hành động.** Assignment giao một phòng không mở cả khu; grant không vượt quyền ADMIN. Xem/tải tệp, xuất PDF, ghi tiền và thao tác nguồn–đích đều kiểm tra backend.
2. **Thời gian có nghĩa rõ.** Ngày ký/hiệu lực/thu thật/nhập dữ liệu/dự kiến trả/bàn giao/hết tính tiền khác nhau. Khoảng ngày dùng [từ, đến không tính); “thuê đến hết 30/09” quy đổi mốc kết thúc không tính 01/10, không đánh rơi một ngày.
3. **Khóa phù hợp với từng quan hệ.** Số phiên bản theo hợp đồng, phòng trong khu, dòng theo chứng từ, lịch bảo trì/kỳ, thông báo/người nhận có khóa nghiệp vụ; không coi cột id là đủ.
4. **Chống chồng ở đúng phạm vi.** Có thể nhận lịch tương lai, nhưng không bàn giao mới khi phòng chưa sẵn sàng/còn người giữ. Kiểm tra terms/đặt chỗ bằng khóa phòng và quy tắc liên bảng; không dùng CHECK giả cho tổng hoặc JOIN.
5. **Tách dữ kiện hiện tại và quá khứ.** Đổi vị trí tài sản, người liên hệ, giá/điều khoản tạo bản có hiệu lực; không sửa ngược ảnh, đoạn đo hoặc chứng từ đã phát hành.
6. **Dữ kiện qua JOIN không lưu thêm tùy tiện.** contract→room→property; placement→room/property; segment→reading→meter. Không nhập cả chuỗi FK độc lập trên từng bản ghi.
7. **Snapshot có mục đích.** Tên/đơn vị/giá/cơ sở tính chứng từ và tình trạng bàn giao là sự kiện đã chốt. Nếu có FK phiên bản bất biến quyết định cùng dữ kiện thì phải chọn nguồn hoặc ghi rõ snapshot dư thừa có kiểm soát; chưa kết luận tự động mọi bảng đã 3NF.
8. **Không làm trùng tiền.** Tổng tiền thật lấy cash_transactions, số dư lấy fund_entries, nợ lấy billing_items và các phân bổ/điều chỉnh. Không cộng cả ba thành doanh thu/thu tiền.
9. **Khóa đồng thời thống nhất.** Thay lịch khóa phòng trước; tiếp theo các hợp đồng, quỹ/phiếu nguồn và đích nợ theo thứ tự ổn định. Lấy số dư lại sau khóa. Chuyển/cấn/hoàn không dùng số trên màn hình đã cũ để ghi sổ.
10. **Xóa có giới hạn.** Nháp có thể xóa theo chính sách và giữ audit; bản ký, chứng từ phát hành, bút toán ghi sổ và chứng cứ được giữ. FK RESTRICT bảo vệ nguồn lịch sử; CASCADE chỉ dùng cho phần nháp thật sự được phép xóa cùng nhau.

Tiền dùng NUMERIC chính xác; ngày dùng DATE, thời điểm dùng TIMESTAMPTZ. Tên/mã trạng thái là CHECK/enum khi phù hợp, không lập bảng danh mục cho mỗi trạng thái chỉ để tăng số bảng. Bộ khóa/FK/CHECK/trigger cụ thể và kiểm chứng phụ thuộc từng bảng sẽ làm sau khi chốt phương án.

## 8. Những gì phương án này đang chọn và còn cần anh duyệt

### 8.1. Lựa chọn để có con số 49 rõ ràng

- Có giữ chỗ/hủy có điều kiện; có phiếu thu–quỹ–phân bổ, tiền trả trước/thừa; chuyển cọc được duyệt; thay công tơ thủ công; điều chỉnh sau phát hành; bản ký/phụ lục; tệp chứng cứ và thông báo.
- Có số lượng tiện nghi/đồ với mã khi cần, vị trí theo thời gian và bàn giao; chưa làm mua sắm, khấu hao hoặc kho tổng quát.
- Hợp đồng một phòng cố định; chuyển phòng/thay bên thuê chính bằng hợp đồng kế tiếp. Gia hạn/đổi giá/liên hệ không ép tạo hợp đồng mới.
- Mỗi hợp đồng có một bên thuê chính chịu trách nhiệm trong bản cơ sở. Có thể lưu các người ký khác đúng vai trò, nhưng chưa tự hỗ trợ phân chia nghĩa vụ độc lập giữa nhiều bên theo mọi cách.
- Có quỹ chung hoặc quỹ theo khu để ghi tiền thật thủ công; chưa kết nối ngân hàng, chưa kế toán đầy đủ, chưa thuế/hóa đơn điện tử, chưa thuê từng giường hoặc chia công tơ tổng nhiều phòng.
- Phí chậm trả đang đề xuất để sau theo D15; tầng chỉ là số tầng theo D16. Hai lựa chọn này chưa được coi là anh đã đồng ý giảm phạm vi V1.

Nếu bỏ hẳn giữ chỗ thì bỏ `reservations` và các FK/loại quỹ tương ứng, còn **48 bảng** trong cách tổ chức này. Nếu muốn quản lý tầng độc lập thì thêm `floors`, thành **50 bảng** khi các phần còn lại giữ nguyên. Những thay đổi khác phải tính lại theo dữ liệu thật sự cần, không cộng trừ theo số chức năng trên giao diện.

### 8.2. Không phải 17 bảng cũ cộng 32 bảng mới

Một số bảng cũ được thay trách nhiệm hoặc hợp nhất:

| Hiện hành | Trong phương án này |
| --- | --- |
| contracts + đại diện ở primary_tenant_id + contract_members | contracts + parties + contacts + residencies + terms; không còn đếm đại diện ngầm là một cư dân. |
| meter_readings theo hợp đồng/kỳ | meters + điểm đo vật lý có phiên bản + consumption_segments giao trách nhiệm. |
| invoices + invoice_items | billing_documents + billing_items, thêm loại chứng từ có điều kiện và kỳ dịch vụ từng dòng. |
| payment_transactions vừa thu tiền vừa cấn | cash_transactions + fund_accounts + fund_entries: tiền thật, mục đích giữ và phân bổ là ba trách nhiệm rõ. |
| maintenance_requests bắt buộc hợp đồng | maintenance_requests theo vị trí + events + costs + schedules; bồi thường nằm ở chứng từ. |
| maintenance_images và các ảnh phòng/bàn giao mới | stored_files + file_attachments với FK đích thật, không nhân bản một bảng ảnh cho từng tính năng. |

### 8.3. Trạng thái thực hiện sau khi duyệt

Đã hoàn thiện thuộc tính, từ điển, ràng buộc, ERD local, DBML/SQL và migration V11; đã nối các luồng backend và biểu mẫu chính. Kết quả kiểm chứng, phần chưa nghiệm thu và việc chưa chuyển database đang dùng được ghi tại [bản triển khai](TRIEN-KHAI-MO-HINH-49-BANG.md). Slide chưa cập nhật.

Schema V11 đã được thực thi trên database thử nghiệm riêng: 49 bảng, 453 cột, 149 FK, 132 CHECK, 7 EXCLUDE và 10 view. Xem [metadata và kết quả SQL](database-validation.json), [kiểm thử ứng dụng](implementation-validation49.json) và [từ điển trường](TU-DIEN-DU-LIEU-49-BANG.md). Dữ liệu cọc cũ thiếu chứng từ vẫn phải được xác minh; số cọc trên hợp đồng không tự thành tiền đã thu. Database đang dùng chưa được chuyển đổi trong đợt này.

**Xác nhận phạm vi:** chủ nhiệm đã chọn phương án 49 bảng và đồng ý triển khai ngày 26/10/2026. Thay đổi phạm vi tiếp theo cần cập nhật tài liệu và migration mới phù hợp; việc đồng ý triển khai không thay cho nghiệm thu các tình huống thực tế.
