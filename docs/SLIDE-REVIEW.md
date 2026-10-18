# Rà soát slide báo cáo bước 1–2

Ngày rà soát: **18/10/2026**. Đọc và xem trực quan đủ 22 trang của PDF nhóm gửi (`thiet-ke-csdl-va-phan-tich-nghiep-vu 1.pdf`), đối chiếu với SQL, ERD chính thức và phân quyền API hiện hành. Bản sửa là [PowerPoint](thiet-ke-csdl-va-phan-tich-nghiep-vu.pptx) và [PDF](thiet-ke-csdl-va-phan-tich-nghiep-vu.pdf) trong repository; file PDF gốc nhóm gửi được giữ nguyên.

**Còn thiếu trước khi nộp:** trang 20 chưa có MSSV, phần việc, minh chứng và tỷ lệ đóng góp thực tế của sáu thành viên. Nhóm cần điền đủ và bảo đảm tổng tỷ lệ bằng 100%. Không tự gán công việc hoặc tỷ lệ khi chưa có thông tin thực tế.

## Kết quả theo từng phần

| Trang | Kết quả rà soát / điều chỉnh |
|---|---|
| 1 | Giữ tên nhóm gehihi và đủ sáu tên từ PDF; chia hai cột để đọc rõ. |
| 2 | Phạm vi một chủ nhà, nhiều khu; thanh toán thủ công và chức năng ngoài MVP được xác định rõ. |
| 3 | Ba tác nhân đúng thiết kế. Làm rõ TENANT chỉ xem hóa đơn đã phát hành của hợp đồng mình đại diện. |
| 4 | Làm rõ tạo/sửa khu và hồ sơ do ADMIN; STAFF sửa phòng và tra cứu trong khu được giao. Bổ sung ngừng khu, xóa phòng chưa có phụ thuộc. |
| 5 | Bổ sung tác nhân cho từng nhóm Use Case; TENANT tra cứu, ADMIN/STAFF ghi nhận thu tiền. |
| 6 | Sửa nhánh chỉ số sai: xóa hóa đơn nháp trước, sửa chỉ số cuối, đánh giá và lập lại. Xác nhận hết cảnh báo trước khi phát hành. |
| 7 | Làm rõ phải đủ hóa đơn ISSUED, khớp ngày/chỉ số; còn cảnh báo PENDING thì dừng. Lưu quyết toán, cấn cọc, kết thúc hợp đồng trong một transaction. |
| 8 | Khoảng cư trú nửa mở, giá đầu kỳ, giới hạn thu tiền và thứ tự cấn cọc đúng SQL/API. |
| 9 | Đủ 17 bảng, chia sáu nhóm đúng ERD. Giữ cách trình bày gọn của PDF nhóm gửi. |
| 10 | Đúng quan hệ khu–phòng–hợp đồng, người đại diện và bảng thành viên. Đổi câu “người dùng chung hồ sơ” thành “người thuê dùng lại hồ sơ” để tránh hiểu nhầm tài khoản. |
| 11 | Đúng quan hệ lịch sử giá, chỉ số và cảnh báo; một chỉ số có tối đa một cảnh báo. |
| 12 | Sửa nhãn quan hệ chỉ số–dòng hóa đơn thành 0..1–0..1; ghi đúng `meter_reading_id` và FK tùy chọn. |
| 13 | Kiểu tiền/chỉ số/ngày đúng thiết kế. Nêu ngoại lệ công nợ snapshot dùng NUMERIC(14,2), các khoản tiền/đơn giá thông thường dùng NUMERIC(12,2). |
| 14 | Làm rõ EXCLUDE thành viên chỉ ngăn chồng khoảng của cùng người trong cùng hợp đồng; không tuyên bố ngăn một người xuất hiện ở mọi hợp đồng khác nhau. |
| 15 | Phân biệt FK NULL + UNIQUE (tùy chọn hai phía) với FK NOT NULL + UNIQUE (bảng con bắt buộc có cha). Ba quan hệ N–N có bảng trung gian và lịch sử rõ ràng. |
| 16 | Phát biểu 2NF theo khóa ứng viên ghép; 3NF theo phụ thuộc bắc cầu, kèm ví dụ cụ thể. Không lấy cột id hoặc số bảng làm bằng chứng chuẩn hóa. |
| 17 | Khôi phục giải thích snapshot đã bị bỏ khỏi PDF: tên/đơn vị/giá tại sự kiện lập hóa đơn giữ nguyên khi danh mục đổi. |
| 18 | Các tình huống kiểm tra đối chiếu được với ca SQL và test backend; không gộp số ca SQL với số test Java. |
| 19 | Tính lại độc lập: tổng 3.680.000đ, nợ 1.680.000đ, hoàn 1.120.000đ. Bổ sung giả định trả ngày 01/10 và không có nợ kỳ khác. Đây là ví dụ số học, không phải số tiền của bộ demo khác. |
| 20 | Đổi từ bốn dòng trống sang sáu thành viên đúng PDF. MSSV, phần việc, minh chứng, tỷ lệ còn chờ nhóm điền. |
| 21 | Đúng mô hình yêu cầu sửa chữa–ảnh; người gửi, người được giao và người đính kèm tham chiếu users. |
| 22 | Đúng ảnh ERD chính thức 17 bảng. Thêm liên kết bấm mở sơ đồ để phóng to; dùng các trang ERD chi tiết khi trình bày từng nghiệp vụ. |

## Đối chiếu tiêu chí chấm

| Tiêu chí | Tỷ trọng | Minh chứng |
|---|---:|---|
| Tác nhân và quyền hạn | 10% | Trang 3–5; Access và các Controller kiểm tra phạm vi khu/hợp đồng. |
| Use Case / User Story | 15% | Trang 4–5; danh mục UC01–UC35 trong [tài liệu nội dung](NOI-DUNG-THUYET-TRINH-BUOC-1-2.md). |
| Luồng nghiệp vụ và nhánh lỗi | 15% | Trang 6–8; hai sơ đồ hóa đơn và trả phòng. |
| Thực thể và thuộc tính | 20% | Trang 9–13, 21–22; [DBML](quanlytro.dbml) và [SQL](quanlytro.sql). |
| PK / FK | 15% | Trang 10–15; cả 17 bảng có PK, tổng 28 FK. |
| Quan hệ và xử lý N–N | 15% | Trang 10–12, 15, 21; employee_assignments, contract_members, service_price_configs. |
| Chuẩn hóa | 10% | Trang 16–17 và [phân tích phụ thuộc dữ liệu](NORMALIZATION.md). |

Đối chiếu này đánh giá nội dung và minh chứng; điểm thực tế còn phụ thuộc phần trình bày, trả lời câu hỏi và phân công của nhóm.

## Kiểm chứng kỹ thuật

- Đối chiếu metadata đã kiểm chứng: **17 bảng, 138 cột, 17 PK, 28 FK, 55 CHECK, 2 EXCLUDE, 6 view**. DBML và ERD SVG có đủ 17 tên bảng. Không thay đổi cấu trúc DB trong lần rà soát này, không cần vẽ lại dbdiagram.
- Xuất PowerPoint bằng PowerPoint trên máy, kiểm tra hình học đối tượng và text trong bảng; xem trực quan lại toàn bộ trang. Số trang, tràn chữ, đối tượng ngoài khung và mã băm file được lưu trong [presentation-validation.json](presentation-validation.json).
- Xác nhận liên kết dbdiagram trả HTTP 200, đúng tên sơ đồ, hiển thị nội dung và ERD trong một trình duyệt mới **chưa đăng nhập**. Không cần tài khoản để thầy xem theo link này tại thời điểm kiểm tra.
- Kết quả test đã có, được đọc và đối chiếu trong lần rà soát: [40 ca SQL và thu tiền đồng thời](database-validation.json), [58 test Java cùng kiểm tra HTTP/trình duyệt](backend-validation.json). Không chạy lại test backend cho các thay đổi tài liệu/slide này.
- README đặt liên kết dbdiagram và bộ hồ sơ báo cáo ngay đầu trang. Các mô tả prototype cũ về phân công theo phòng, phí chậm trả, ảnh phòng và mã khôi phục mật khẩu đã được phân biệt với MVP 17 bảng; tài liệu V1 có nhãn lịch sử.

Nhóm điền thông tin trong [presentation-team.json](presentation-team.json) rồi tạo lại slide, hoặc sửa trực tiếp trang 20 trong PowerPoint và xuất lại PDF. Không nộp bản PDF cũ thay cho bản đã rà soát.
