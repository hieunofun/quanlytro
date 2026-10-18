# Kết quả kiểm tra bản bàn giao

> Kết quả lịch sử của prototype trước Flyway V10. Kết quả hiện hành nằm trong [backend-validation.json](backend-validation.json); kiểm chứng thiết kế bước 1–2 nằm trong [database-validation.json](database-validation.json).

Ngày kiểm tra: **02/10/2026**, múi giờ Asia/Ho_Chi_Minh. Môi trường: Windows, Java 21, Maven Wrapper, PostgreSQL 16.4, Node.js 24, Chrome headless. Database kiểm thử và demo riêng tại cổng 55432.

## Java / PostgreSQL

| Bộ kiểm thử | Số test | Lỗi |
|---|---:|---:|
| BusinessWorkflowTests | 30 | 0 |
| FeeCalculatorTests | 3 | 0 |
| HostelManagementApplicationTests | 1 | 0 |
| HttpSecurityTests | 4 | 0 |
| ImageWorkflowTests | 5 | 0 |
| PdfTests | 1 | 0 |
| Tổng các test hiện tại | **44** | **0** |

Lần chạy cuối `mvn verify` thành công: 44 test, không lỗi, không bỏ qua; frontend build xong trước Maven. Các báo cáo từng lớp nằm trong `docs/test-results`. Sau đó chỉ bổ sung cuộn về đầu khi mở chi tiết ở frontend và đóng gói lại JAR; backend không thay đổi.

Nội dung kiểm tra: snapshot đơn giá, công thức và chia theo ngày/ngày-người; chặn hóa đơn trùng; chỉ số giảm; sửa có lịch sử; cảnh báo chặn phát hành; TENANT không xem hợp đồng người khác; STAFF cần khu được giao; không thu vượt nợ; trả phòng/cấn cọc/mở phòng; ngày SQL trả đúng dạng ngày lịch; báo cáo quý/lịch sử; JWT lỗi/thiếu, refresh một lần, logout/đổi mật khẩu thu hồi, khóa sau 5 lần sai; PDF đọc lại được tiếng Việt.

## Luồng demo qua HTTP

`scripts/verify-demo.ps1` thành công trên JAR đang chạy:

- Chỉ số nước sửa 174 → 155, consumption 8 m³.
- Hóa đơn tổng 3.690.000 VND; thanh toán 2.000.000; còn 1.690.000.
- Ngày phát hành trả `2026-10-01`, không bị lệch múi giờ.
- Xuất `docs/demo-invoice.pdf` thành công.
- API doanh thu tháng/quý, công nợ, lấp đầy hiện tại/lịch sử và sự cố đều trả 200.

Database demo trên máy hiện tại đã ở trạng thái sau sửa chỉ số và thanh toán một phần. Khi nạp vào database trống bằng seed-demo.ps1, dữ liệu bắt đầu ở trạng thái cảnh báo 27 m³ để thực hiện kịch bản đầy đủ. Không ghi đè demo đã tồn tại.

## Giao diện trên JAR cuối

`npm run test:e2e` thành công trên http://localhost:8080:

- Mở 9 màn quản lý và form tạo phòng.
- Quản lý xem chi tiết hóa đơn; đăng xuất rồi đổi sang TENANT, không giữ chi tiết của phiên trước.
- TENANT tải lại trang vẫn ở cổng hóa đơn, không bị chuyển tới dashboard quản lý.
- TENANT không có nút tạo hóa đơn hoặc thu tiền.
- Ở 375 px, công thức nước hiện trực tiếp trong từng khoản thu; form sự cố và toàn trang không tràn ngang.
- Không có lỗi JavaScript; chụp 5 ảnh trong `docs/screenshots`.

Probe của skill UI/UX đã chạy trên trang thật ở 375 và 1440 px, không còn lỗi đo được. Ảnh được mở để kiểm tra thủ công: chữ tiếng Việt, ngày, số tiền, bố cục và công thức trên điện thoại.

## Giới hạn kiểm chứng

Docker Compose đã build và chạy bằng Docker Engine 29.1.3 / Compose 2.40.3 trong Ubuntu/WSL ở vòng kiểm tra trước. Kiểm chứng bằng database/volume riêng, app tại cổng 18080 và PostgreSQL tại 55433: health UP, seed-demo và verify-demo thành công qua HTTP; hóa đơn 3.690.000, thu 2.000.000, còn 1.690.000 và xuất PDF. Vòng sửa lỗi V9 kiểm chứng JAR trên Windows/PostgreSQL, chưa build lại Docker image. Chưa thực hiện kiểm thử tải lớn toàn hệ thống. Mobile, ngân hàng, AI/IoT ngoài V1.

## Kiểm tra bổ sung

- Quyền nhân viên theo phòng: danh sách, chi tiết, báo cáo, chỉnh sửa và tệp ảnh cùng phạm vi; thu hồi phân công có hiệu lực ngay.
- Vi phạm: tạo OPEN, chuyển HANDLING/RESOLVED/DISMISSED với ghi chú; người thuê không xử lý; không mở lại vi phạm đã đóng.
- Lịch: áp dụng ngày chốt/phát hành, tháng ngắn tự chốt ngày cuối; hạn thu dịch tháng nếu cần, chặn phát hành sớm; trả phòng giữa tháng dùng ngày thực tế.
- Báo cáo tiền thu theo tháng nhận tiền, loại trừ DEPOSIT_OFFSET khỏi tiền thu mới.
- Ảnh: kiểm thử HTTP với PNG hợp lệ, tệp giả PNG, quá 6 MB, không đăng nhập, ngoài phạm vi, nhân viên/phòng và người thuê/sự cố.
- `node tests/features.mjs` trên JAR cuối: upload có quyền, xử lý vi phạm đến RESOLVED, xem lịch và phân công một phòng trên giao diện; ADMIN/STAFF, không lỗi JavaScript, ảnh desktop và 375 px. Chạy tuần tự với browser.mjs vì đăng xuất thu hồi mọi phiên của tài khoản demo.

Báo cáo Word/slide hiện là bản nháp của lần bàn giao trước; người dùng yêu cầu làm báo cáo sau. Bộ code ZIP mới chỉ đóng gói mã nguồn, JAR và tài liệu vận hành/kiểm thử.

## Các lỗi phát hiện và đã sửa trong lần kiểm tra sâu

- Hai người thuê liên tiếp trong cùng phòng/tháng: ghi chỉ số và phát hành hóa đơn riêng cho hợp đồng mới; vẫn chặn chỉ số trùng trong cùng hợp đồng. Cảnh báo dùng tổng tiêu thụ từng tháng làm baseline.
- Thành viên đã rời phòng: không cho sửa lại ngày sang tháng khác, không ghi rời trên hợp đồng đã kết thúc; phí đầu người đã phát hành giữ nguyên.
- Quyết toán: điện nước kỳ cuối 110.000đ được lưu đúng; hoàn cọc không khấu trừ điện nước hai lần. V9 nâng database cũ và cập nhật bản quyết toán có sẵn đã xác nhận qua API.
- Sức chứa: bỏ người đã rời, kiểm tra cả người dự kiến vào ở theo ngày; các khoảng ở không chồng nhau không bị cộng sai.
- Tải ảnh đồng thời: giữ đủ hai ảnh; khi đã có 11 ảnh, hai yêu cầu đồng thời cho đúng một thành công và một lỗi 400, tổng vẫn 12 ảnh.
- `node tests/checkout.mjs`: trả phòng qua giao diện và hiện quyết toán ngay; hoàn 1.460.000đ, cần thu thêm 1.040.000đ, hoàn bằng 0; reload giữ kết quả; TENANT xem đúng quyết toán của mình. Desktop 1440 và mobile 375 px, không lỗi JavaScript/tràn ngang. Bộ này bắt buộc database riêng qua `E2E_ISOLATED_DATABASE=true`.
- Probe UI/UX trên trang quyết toán thật ở 375/1440 px: 0 mục xếp Hỏng; đã sửa giữ vị trí cuộn cũ khi mở chi tiết. Hai cảnh báo nhẹ còn lại là chênh 1 px giữa hàng thông tin và vòng focus của link logo; giữ vòng focus để người dùng bàn phím nhận biết vị trí, không ảnh hưởng nội dung/số tiền.
- `npm audit --json`: không có lỗ hổng được báo cho các dependency frontend tại thời điểm kiểm tra; không phải kết quả quét dependency Java hay pentest toàn hệ thống.

Chi tiết lỗi/cách sửa và danh sách nâng cấp đề xuất ở [CODE-REVIEW.md](CODE-REVIEW.md). Bằng chứng HTTP trước/sau: `test-results/edge-before.json` và `test-results/edge-after.json`.
