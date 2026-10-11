# Kịch bản bảo vệ

Nạp dữ liệu: `scripts/seed-demo.ps1 -AdminPassword 'mật-khẩu-admin'`. Dữ liệu là dữ liệu giả, cố định theo tháng 6–9/2026; chọn kỳ **2026-09** ở dashboard. Mật khẩu và danh sách tài khoản demo nằm trong README.

1. Chủ nhà đăng nhập, mở Phòng: 203 đang thuê, 204 trống. Mở hợp đồng HD-DEMO-203 để xem người đại diện, tiền cọc và thời hạn.
2. Điện nước: tháng 6/7/8 dùng 8/9/10 m³, tháng 9 dùng 27 m³. Trung bình 9 m³, tăng 200%, vượt ngưỡng 50%.
3. Mở chỉ số nước tháng 9 → Sửa chỉ số: đổi 174 thành **155**, lý do “Kiểm tra công tơ, nhập nhầm”. Tiêu thụ còn 8 m³ và cảnh báo được tính lại. API lịch sử giữ 174 → 155, người sửa và thời điểm.
4. Hóa đơn → Tạo hóa đơn, hợp đồng phòng 203, kỳ 2026-09, ngày phát hành 2026-10-01, hạn 2026-10-05. Mở nháp: tiền phòng 3.000.000; điện 100 × 3.500 = 350.000; nước 8 × 20.000 = 160.000; Wifi 100.000; rác 30.000; xe 50.000. Tổng **3.690.000 VND**.
5. Phát hành và tải PDF. Tạo lại hóa đơn cùng kỳ để chứng minh hệ thống từ chối. Thử sửa chỉ số kỳ đã phát hành: bị chặn.
6. Đăng nhập tenant_demo, mở hóa đơn và từng công thức. Người thuê không có nút tạo hóa đơn hay thu tiền.
7. Chủ nhà/nhân viên ghi nhận thanh toán 2.000.000. Hóa đơn chuyển “Thanh toán một phần”, còn **1.690.000 VND**. Người thuê xem được lịch sử giao dịch.
8. Thêm giá nước 25.000 hiệu lực 2026-10-01. Mở lại hóa đơn tháng 9: vẫn 160.000 tiền nước, tổng vẫn 3.690.000.
9. Người thuê tạo sự cố. Nhân viên mở và chuyển Mới tạo → Đã tiếp nhận → Đang xử lý → Hoàn thành, ghi chi phí. Người thuê nhận thông báo trạng thái.
10. Chủ nhà tạo khu khác, không phân công staff_demo. Đăng nhập staff_demo để chứng minh chỉ thấy An Phú; API trực tiếp ngoài khu cũng bị từ chối.
11. Xem dashboard để phân biệt đã lập hóa đơn với đã thu, công nợ và sự cố. Tải công nợ CSV hoặc báo cáo PDF.

## Demo trả phòng ngắn

Tạo hợp đồng riêng ở phòng 204 với ngày bắt đầu 2026-09-01, kết thúc 2026-10-01; nhập giá điện/nước cho khu đã có. Trả phòng ngày 2026-09-16 với chỉ số cuối. Hệ thống lập kỳ cuối cho 15/30 ngày, chặn nếu còn cảnh báo chưa kiểm tra, cấn trừ cọc và mở phòng trống. Trang hợp đồng hiện điện nước kỳ cuối, công nợ, thiệt hại và tiền hoàn cọc/cần thu thêm. Dùng ngày đã qua để có thể phát hành hóa đơn quyết toán ngay. Không dùng hợp đồng demo 203 để bỏ qua các kỳ tháng 6–8 chưa lập hóa đơn; hệ thống yêu cầu đủ các kỳ đã ở.

## Câu hỏi thường gặp

- Không dùng AI: cảnh báo dựa vào trung bình và ngưỡng phần trăm, dễ giải thích và kiểm chứng.
- Đơn giá mới không đổi hóa đơn cũ vì invoice_items lưu snapshot.
- Chống thanh toán đồng thời bằng transaction và khóa hàng hóa đơn `FOR UPDATE`.
- Chống hợp đồng chồng lấn bằng kiểm tra phòng và exclusion constraint PostgreSQL.
- STAFF/TENANT bị kiểm tra ở backend, không phụ thuộc nút bị ẩn trên frontend.

## Chức năng bổ sung của V1

- Phòng → Xem → Chọn ảnh từ máy; JPEG/PNG. Người thuê chỉ tải được ảnh phòng của mình. Sự cố có vùng đính kèm tương tự; thử tệp văn bản giả đuôi PNG để thấy lỗi kiểm tra.
- Thiết lập → Phân công → chọn khu và phòng 204. Tài khoản mới chỉ thấy phòng 204, không thấy 203 hoặc doanh thu/hợp đồng của 203. Để trống phòng nghĩa là giao toàn khu; phân công toàn khu còn hiệu lực vẫn có quyền cả khu.
- Hợp đồng → Ghi nhận vi phạm → nhập lý do → Đang xử lý → Đã giải quyết hoặc Loại bỏ, ghi nội dung xử lý. Người thuê xem được kết quả, không xử lý thay quản lý.
- Thiết lập → Chính sách hóa đơn: thử chốt ngày 31/phát hành ngày 3. Lịch trong form chỉ số/hóa đơn thay theo khu; tháng 2 chốt ngày cuối tháng. Không sửa ngày hóa đơn đã phát hành.
- Báo cáo tháng/quý hiện tiền đã thu ở tháng nhận thực tế. Demo thu ngày 02/10/2026 cho hóa đơn 09/2026: khoản thu xuất hiện ở tháng 10, trong khi công nợ gắn kỳ tháng 9.
