# Kiểm tra và sửa lỗi code — 02/10/2026

Đã tái hiện các lỗi dưới đây bằng HTTP trên JAR bàn giao trước, dùng database riêng `hostel_review_*`. Dữ liệu demo không dùng để tái hiện các ca trả phòng. Bản sửa thêm migration V9, không sửa checksum V1–V8.

| Lỗi đã xác nhận | Bằng chứng trước khi sửa | Cách sửa |
|---|---|---|
| Đổi người thuê trong cùng phòng/tháng bị chặn ghi chỉ số | Người cũ trả ngày 16/01; người mới nhận cùng ngày. Hai yêu cầu ghi chỉ số tháng 01 cho hợp đồng mới đều trả 409. | Unique theo hợp đồng/kỳ/loại. Mỗi hợp đồng có hóa đơn riêng; baseline cảnh báo cộng lượng tiêu thụ trong từng tháng trước khi lấy trung bình. |
| Có thể đổi lại ngày thành viên rời phòng sau khi tính tiền | Đã ghi rời 16/01 và phát hành phí gửi xe 46 ngày-người; đổi ngày rời sang 16/02 vẫn trả 200. | Chỉ ghi rời một lần trên hợp đồng ACTIVE; đọc trạng thái sau khi khóa hàng; vẫn chặn thay đổi ảnh hưởng hóa đơn. |
| Quyết toán thiếu tiền điện nước kỳ cuối và không hiện kết quả | Điện nước trên hóa đơn 110.000đ nhưng `final_utility_amount=0`; tiền hoàn 1.460.000đ không có trong trang chi tiết. | Lưu tổng điện nước từ snapshot hóa đơn; V9 cập nhật bản quyết toán cũ. Chi tiết hợp đồng trả `checkout`; giao diện mở ngay kết quả và xem lại sau reload, gồm tiền hoàn/cần thu thêm. |
| Sửa sức chứa đếm cả người đã rời phòng | Phòng còn một người; người thứ hai rời 16/09, nhưng giảm sức chứa xuống 1 bị 400. | Kiểm tra số người ở đồng thời từ hôm nay và những ngày thành viên dự kiến vào ở, bỏ người đã rời. |
| Tải hai ảnh sự cố đồng thời làm mất một ảnh khỏi danh sách | Hai upload đều 200, metadata lưu 2 ảnh, nhưng yêu cầu sửa chữa chỉ liên kết 1 ảnh. | Đọc lại toàn bộ bản ghi sau khi lấy khóa, rồi kiểm tra trạng thái/số ảnh và cập nhật; giới hạn 12 ảnh vẫn đúng khi tải đồng thời. |

Điện nước kỳ cuối là thông tin trong tổng hóa đơn. Tiền quyết toán = cọc − công nợ trước cấn cọc − thiệt hại; không trừ điện nước lần nữa. Số âm được hiển thị thành **Cần thu thêm** với số tiền dương.

Kiểm chứng chi tiết và kết quả cuối nằm trong [VALIDATION.md](VALIDATION.md). Bộ test thêm 8 ca nghiệp vụ và 2 ca HTTP tải đồng thời; trình duyệt kiểm tra tiền hoàn, thu thêm, hoàn bằng 0, xem lại sau reload và người thuê xem quyết toán của mình.

## Nâng cấp nên ưu tiên tiếp theo

| Ưu tiên | Nâng cấp | Lý do |
|---|---|---|
| 1 | Sao lưu database và thư mục ảnh theo lịch; thử phục hồi vào database riêng | Hiện có Docker volume nhưng chưa có quy trình tự động sao lưu/phục hồi. PostgreSQL khuyến nghị sao lưu dữ liệu định kỳ; xem [tài liệu chính thức](https://www.postgresql.org/docs/current/backup.html). |
| 2 | Bộ lọc theo khu/trạng thái/kỳ và phân trang phía server | Nhiều API danh sách hiện đọc toàn bộ dữ liệu; sẽ chậm khi có nhiều phòng, hóa đơn và thanh toán. |
| 3 | Phiếu quyết toán cọc/biên nhận PDF và xuất XLSX | Thuận tiện đối chiếu, ký nhận và trình bày khi bảo vệ; hiện có PDF hợp đồng/hóa đơn và CSV công nợ. |
| 4 | Ẩn thao tác không có quyền theo phân công từng phòng | Backend chặn đúng, nhưng một số nút tạo cấu hình chung vẫn hiện cho STAFF chỉ quản lý một phòng. |
| 5 | CI chạy kiểm thử với PostgreSQL riêng khi sửa code | Bảo vệ các luồng tính tiền, phân quyền và xử lý đồng thời trong những lần nâng cấp sau. |

Email/QR thanh toán/AI/IoT có thể làm ở phiên bản sau. Các mục trên là đề xuất, chưa triển khai trong bản sửa lỗi này. Chưa kiểm thử tải lớn; các test tải ảnh đồng thời không thay thế stress test toàn hệ thống.
