# Chuẩn hóa và dữ kiện lịch sử — mô hình 49 bảng

Phạm vi là các bảng trong [quanlytro.sql](quanlytro.sql). Từ điển và khóa thực thi xem [Từ điển dữ liệu](TU-DIEN-DU-LIEU-49-BANG.md). Không dùng việc mỗi bảng có cột id để kết luận chuẩn hóa.

## 1. Các nhóm lặp và phụ thuộc được tách

| Dữ kiện | Nơi lưu và ý nghĩa |
| --- | --- |
| Hồ sơ một người | tenant_profiles; người ký, người liên hệ và người ở tham chiếu cùng hồ sơ |
| Nhiều người / nhiều khoảng ở | contract_residencies, mỗi người và khoảng một dòng |
| Giá niêm yết thay đổi | room_price_history, khóa nghiệp vụ (room_id,effective_from) |
| Điều kiện đã ký | contract_terms, (contract_id,version_no), mốc hiệu lực của phiên bản |
| Dịch vụ khách đăng ký | contract_service_subscriptions, khoảng hiệu lực không chồng cùng hợp đồng/dịch vụ |
| Giá dịch vụ theo khu | service_price_configs, (property_id,service_item_id,effective_from) |
| Điểm đo vật lý | meter_readings, (meter_id,observed_at,revision_no); mỗi sửa đổi là bản mới |
| Khoảng tiêu thụ | consumption_segments tham chiếu hai điểm; không lưu lại consumption để sửa tay |
| Nhiều tài sản/vị trí | asset_placements, mỗi lần bố trí một khoảng |
| Nhiều tệp | stored_files giữ một tệp; file_attachments giữ một liên kết đúng đối tượng bằng FK |
| Nhiều người nhận | notification_recipients, (notification_id,user_id), trạng thái đọc riêng |
| Nhân viên và phạm vi | employee_assignments chọn khu XOR phòng; staff_permission_grants theo phân công/quyền |

Các khóa ghép/UNIQUE/EXCLUDE và đầy đủ FK nằm trong SQL. Những điều kiện như sức chứa theo thời gian, nợ được phép trả và số dư có thể hoàn có trigger/giao dịch riêng.

## 2. Tránh lưu đồng thời dữ kiện và kết quả suy ra

- Chỉ số thuộc công tơ, công tơ thuộc phòng. Khoảng tiêu thụ không lưu lại room_id/service_item_id của công tơ.
- Mức dùng = điểm cuối − điểm đầu; thời lượng lấy từ thời điểm đo.
- Số dư quỹ = bút toán vào/ra/đảo; số có thể dùng trừ phần tạm giữ chưa giải tỏa.
- Số nợ = khoản phải thu sau điều chỉnh − phân bổ đã ghi; không chỉnh tay tổng hóa đơn/paid_amount/balance_due.
- Tình trạng đang thuê theo lần chiếm hữu thực tế; hết ngày trên hợp đồng không tự sinh sự kiện trả phòng.
- Tiền thu/chi thực lấy từ cash_transactions; PAYMENT/TRANSFER không được cộng như tiền mới nhận.

## 3. Snapshot được giữ có chủ đích

Tên dòng, đơn vị, giá, số lượng tính và khoảng dịch vụ trên billing_items là dữ kiện lúc lập chứng từ. Chúng không có nghĩa phải bằng danh mục hiện tại; cập nhật tên dịch vụ/giá tương lai không sửa chứng từ quá khứ.

Bàn giao giữ tên và tình trạng đồ vật lúc giao. Quyết toán giữ số dư trước cấn ở lần duyệt. Cảnh báo giữ ngưỡng và nguồn baseline tại lần đánh giá. Đây là dữ kiện thời điểm khác với trạng thái hiện tại.

Nhật ký before_data/after_data phục vụ tra soát, không thay quan hệ nghiệp vụ hoặc sổ tiền. Đối tượng nghiệp vụ quan trọng có FK cụ thể; resource_id của nhật ký chỉ là định danh tra cứu.

## 4. Giới hạn kết luận

49 bảng không tự chứng minh toàn bộ BCNF hoặc toàn bộ yêu cầu thực tế đã nghiệm thu. Thiết kế đã thực thi các khóa/phụ thuộc quan trọng và kiểm thử giao dịch/chuyển dữ liệu. Một số quy tắc thời gian, chuyển trạng thái và phân quyền còn phụ thuộc nghiệp vụ ứng dụng; xem [kết quả và giới hạn triển khai](TRIEN-KHAI-MO-HINH-49-BANG.md).
