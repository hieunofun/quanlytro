# Cơ sở dữ liệu hiện hành — V13 / 49 bảng

Cập nhật **30/10/2026**. Phương án đã được chủ nhiệm đồng ý triển khai. PostgreSQL 18.6 đã thực thi: **49 bảng, 455 cột, 49 PK, 151 FK, 132 CHECK, 7 EXCLUDE, 10 view**. Không tính Flyway hoặc bảng lưu trữ V10/V9.

- [Kết quả triển khai, chuyển dữ liệu, cách chạy và giới hạn](TRIEN-KHAI-MO-HINH-49-BANG.md).
- [Danh sách 49 bảng và các liên kết](PHUONG-AN-CSDL-49-BANG.md).
- [Từ điển đủ 455 trường](TU-DIEN-DU-LIEU-49-BANG.md).
- [SQL bảng](quanlytro.sql), [view/trigger hiện hành](database-rules.sql), [DBML](quanlytro.dbml), [metadata](design-schema.json).
- ERD local: [SVG](diagrams/quanlytro-dbdiagram.svg), [PNG](diagrams/quanlytro-dbdiagram.png), [PDF](diagrams/quanlytro-dbdiagram.pdf).
- ERD chi tiết: [cư trú](diagrams/erd-cu-tru.svg), [điện nước](diagrams/erd-dien-nuoc.svg), [tài chính](diagrams/erd-tai-chinh.svg), [sự cố](diagrams/erd-su-co.svg).
- [Chuẩn hóa và snapshot lịch sử](NORMALIZATION.md), [API V13](API49.md).
- [20 ca SQL](database-cases49.sql), [kết quả SQL](database-validation.json), [kết quả Java](implementation-validation49.json).

Nguồn nâng cấp database là Flyway V1–V13. `scripts/schema49.py` và `build-schema49.py` giữ riêng định nghĩa lịch sử V11, không dùng để ghi đè migration đã chạy. Tạo mới schema độc lập bằng `quanlytro.sql` rồi `database-rules.sql` (gộp quy tắc V11/V12/V13). V13 thêm `cash_transactions.payment_document_id` và `meter_readings.correction_item_id` cùng các ràng buộc nghiệp vụ; vẫn 49 bảng. Xem [biên bản triển khai backend V13](TRIEN-KHAI-BACKEND-V13.md).

Ứng dụng dùng profile `model49`, được nhóm `dev` và `prod` kích hoạt. Các entity hiện hành nằm ở `com.hostel.model49.entity`; entity/controller V10 vẫn giữ cho kiểm thử lịch sử với Flyway target 10.

Các view: `v_contract_current`, `v_room_status`, `v_segments`, `v_billing_lines`, `v_fund_movements`, `v_fund_balances`, `v_item_applications`, `v_item_balances`, `v_document_balances`, `v_maintenance`.

**Database đang dùng chưa được chuyển đổi trong phiên triển khai này.** Bản xem riêng dùng localhost 8091 và PostgreSQL 55449. V1–V10 giữ nguyên nội dung; V11 lưu 17 bảng nguồn vào `model_v10`, đối soát và khóa ghi lịch sử.

Hồ sơ 17 bảng cũ: [archive/v10/DATABASE.md](archive/v10/DATABASE.md). Bản dbdiagram trực tuyến, ảnh mang hậu tố `official` và slide cũ vẫn thuộc V10; chưa dùng chúng để trình bày V11.
