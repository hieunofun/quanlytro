# Backend — kết quả kiểm tra ngày 01/11/2026

Phạm vi đã hoàn thiện: hợp đồng/cư trú, đổi khách giữa tháng, điện nước theo trách nhiệm, hóa đơn, thu dư, điều chỉnh sau thu và quyết toán/hoàn tiền. Không dùng cụm “hoàn chỉnh 100%” cho các nghiệp vụ chưa được kiểm chứng.

## Thay đổi sau bản V13 ngày 30/10

- Khi trả sớm, tự lập điều chỉnh cho **dịch vụ PRORATED** đã phát hành, gồm dịch vụ theo phòng/số lượng và ngày-người. FULL_PERIOD/ONE_TIME giữ chế độ thu đã chọn.
- Phần phân bổ dư sau quyết toán được trả về đúng từng nguồn, có tính các lần giải phóng/đảo trước đó.
- Hoàn tiền nhận `externalReference`; cùng mã phiếu phải khớp phương thức/thời điểm/tham chiếu. Cùng tham chiếu tiền đã chi không được ghi hai lần bằng hai mã khác nhau.
- Bổ sung test đổi khách cùng phòng từ đầu đến cuối, kiểm tra cả tiền phòng, dịch vụ, điện khi phòng trống và phương trình tiền.
- Không đổi schema hoặc sửa Flyway V1–V13 đã công bố; thay đổi thuộc service/API và kiểm thử.

## Kết quả đã thực thi

| Phạm vi | Kết quả |
| --- | --- |
| Java qua `mvn verify` | **126 test đạt**, không lỗi, không bỏ qua |
| Trong đó ứng dụng/API V13 | 64 test |
| Migration | 8 test |
| Hồi quy V10 và đơn vị | 54 test |
| Schema PostgreSQL và quy tắc SQL | **20 ca đạt** |
| Dữ liệu minh chứng qua HTTP | **67 request nghiệp vụ đã lưu** (không gồm đăng nhập); các assertion trong chương trình đều đạt |
| Trình duyệt | Đăng nhập, mở quyết toán A/hóa đơn B/điều chỉnh C; không lỗi JavaScript trong các ca này |

Nguồn: [Java](implementation-validation49.json), [SQL](database-validation.json), [API thực thi](backend-review/api-transcript.json), [số liệu đối chiếu](backend-review/demo-evidence.json), [trình duyệt](backend-review/browser-validation.json).

Trình duyệt dùng dữ liệu do API tạo. Đây không phải bằng chứng toàn bộ luồng tạo/sửa đã thao tác thuần bằng giao diện.

## Số liệu minh chứng

| Nội dung | Khách A | Phòng trống | Khách B |
| --- | --- | --- | --- |
| Ngày tính tiền phòng tháng 1 | 1–15: 15 ngày | 16: 1 ngày | 17–31: 15 ngày |
| Tiền phòng | 1.500.000 | 0 | 1.500.000 |
| Điện | 30 kWh | 2 kWh chủ nhà | 28 kWh |
| Tổng hóa đơn | 1.770.000 | Không đưa vào nợ khách | 1.763.000 |

Đơn giá phòng 3.100.000/tháng, điện 3.500/kWh; internet 310.000/tháng, dịch vụ theo người 31.000/người/tháng. Dữ liệu phụ: nước không phát sinh và giá rác bằng 0 trong bộ thử. A đã nhận cọc 2.000.000, cấn 1.770.000, chi hoàn 230.000. B nộp 1.800.000, hết nợ và còn trả trước 37.000.

Ca điều chỉnh riêng C: 50 kWh × 3.500 = 175.000; số đúng 40 kWh nên còn 140.000. Chứng từ giảm 35.000 và trả phần phân bổ dư về quỹ; không tự tạo phiếu chi hoàn.

## Tệp sử dụng

- [Slide PowerPoint sửa được](backend-review/backend-he-thong-nha-tro.pptx), [PDF](backend-review/backend-he-thong-nha-tro.pdf), [HTML offline](backend-review/backend-he-thong-nha-tro.html): 15 trang, không có ghi chú/lời thoại thuyết trình.
- [Bộ request Postman](backend-review/backend.postman_collection.json), [SQL chỉ đọc](backend-review/backend-readonly.sql).
- Backend thử tại **http://127.0.0.1:8093**, database `hostel_review49` cổng 55449; tài khoản thử `admin` / `ReviewAdmin49#2026`.

Khởi động lại bản thử: `powershell -ExecutionPolicy Bypass -File scripts/run-model49-review.ps1 -SkipBuild -Port 8093 -Restart`. Dữ liệu được giữ trong database review riêng. Muốn tạo bộ dữ liệu mới và cập nhật ID trong Postman, chạy `node scripts/verify-backend-demo.mjs`.

## Giới hạn còn giữ rõ

- Một chủ nhà; phiên trong RAM của một instance.
- Xác minh ngân hàng/chi hoàn do người vận hành ghi nhận; chưa chuyển tiền tự động.
- Sửa điểm đo có kỳ sau phụ thuộc bị chặn; chưa có quy trình tự động đối soát dây chuyền.
- Đổi đơn giá điện giữa khoảng đo cần điểm ranh giới; không tự chia sản lượng thiếu căn cứ.
- Chưa kiểm chứng tải lớn, phục hồi dữ liệu thật hoặc toàn bộ màn hình nâng cao. Chưa dùng database thật trong đợt này.
