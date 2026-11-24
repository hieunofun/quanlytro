"""
Single-source-of-truth builder for the finalized 5-chapter BTL report:
  1. Preserves teammate's 5-chapter structure (FR01-FR16, BR01-BR06, 1NF-3NF, ERD,
     Use Cases + UC01-UC07, 7 Activity diagrams, MVC, Deployment, 4 code excerpts,
     Test Plan TP01-TP09, Results & Direction) while fixing ALL 9 audit issues.
  2. Emits Google Docs batch JSON + image insert manifest + TOC page-token map
     targeting Doc ID 1N4F1YrdcKpGbiYJu89M89mL8XdX-I10J0IllSZNUQ2A (Tab 1 / t.0).
  3. Builds synchronized Word file docs/bao-cao-do-an.docx (lossless images, auto TOC).
"""
from pathlib import Path
import json
import re
import sys
import docx
from docx.shared import Cm, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "report-assets"
OUT_DOCX = ROOT / "docs" / "bao-cao-do-an.docx"
RT = ROOT / ".runtime" / "teammate-report"
GDOC_BATCH = RT / "sync_batch.json"
GDOC_IMAGES = RT / "sync_images.json"
GDOC_TOC_MAP = RT / "sync_toc_map.json"

sys.path.insert(0, str(Path(__file__).resolve().parent))
from docx_layout import (
    NAVY, INK, MUTED, SUBNAVY,
    configure_base_styles, setup_a4_margins, set_page_border, clear_page_border,
    set_page_number_format, configure_running_header_footer, set_run_font,
    add_front_title, add_chapter, add_h2, add_h3, add_para, add_bullet,
    add_styled_table, add_toc_field,
)

# Verified CMC University — Nhóm gehihi — Lớp N03 members & real MSSVs
MEMBERS = [
    ("1", "Nguyễn Doãn Huy", "BIT240115",
     "Khảo sát bài toán, phân tích yêu cầu (FR01–FR16, BR01–BR06), luồng hợp đồng – bàn giao và điều phối tổng hợp báo cáo."),
    ("2", "Phan Văn Hiếu", "BIT240094",
     "Thiết kế CSDL 49 bảng (V1–V14), chuẩn hóa 3NF, ERD, cài đặt backend sổ quỹ (`Finance49`, `Settlements49`, `Reports49`) và khóa đồng thời."),
    ("3", "Nguyễn Trần Tuấn Khôi", "BIT240131",
     "Cài đặt nghiệp vụ ghi công tơ, phát hiện cảnh báo bất thường (`Meters49`), tính hóa đơn theo ngày lẻ và kiểm thử nghiệp vụ."),
    ("4", "Niê Phương Anh", "BIT240014",
     "Xây dựng giao diện React (`main.jsx`, `Model49Panels.jsx`), màn hình tổng quan, báo cáo, công nợ, đồ đạc phòng và tối ưu di động."),
    ("5", "Trần Đức Nguyên", "BIT240173",
     "Cài đặt xác thực JWT (`AuthController`), phân quyền (`Access49`), xử lý sự cố (`Operations49`), quản lý tệp và xuất hợp đồng PDF (`ContractPdf`)."),
    ("6", "Nguyễn Quang Trung", "BIT240230",
     "Xây dựng dữ liệu mẫu (`seed-defense.mjs`), lập kế hoạch và ca kiểm thử, chạy kiểm chứng SQL/Java/trình duyệt và hoàn thiện sơ đồ."),
]


# ==============================================================================
# REPORT STRUCTURE (Single Source of Truth)
# Block tuples:
#   ("FRONT_H1", title)
#   ("CH", toc_key, title)
#   ("H2", toc_key, title)
#   ("H3", title)
#   ("P", text)
#   ("B", text)
#   ("TABLE", caption, headers, rows, widths_cm)
#   ("FIG", fig_key, filename, caption, width_cm_docx)
#   ("CODE", title, code_text)
# ==============================================================================
BLOCKS = [
    # ---------- TÓM TẮT & DANH MỤC TỪ VIẾT TẮT ----------
    ("FRONT_H1", "TÓM TẮT"),
    ("P", "Bài tập lớn học phần Kỹ năng lập trình nâng cao xây dựng hệ thống quản lý nhà trọ phục vụ ba nhóm người dùng: chủ nhà (`ADMIN`), nhân viên quản lý (`STAFF`) và người thuê phòng (`TENANT`). Chương trình giải quyết trọn vẹn các nghiệp vụ vận hành hằng ngày gồm quản lý khu nhà, phòng, đồ đạc trong phòng, hồ sơ khách thuê, hợp đồng và phụ lục theo phiên bản, cư trú thực tế, biên bản bàn giao nhận–trả phòng, ghi chỉ số và cảnh báo điện nước bất thường, lập và phát hành hóa đơn, ghi nhận thu tiền tách bạch sổ quỹ, quyết toán trả phòng, xử lý sự cố và báo cáo doanh thu – công nợ."),
    ("P", "Ứng dụng được phát triển bằng **Java 21 + Spring Boot 4.1.1** cho tầng API và nghiệp vụ, **React 19 + Vite 7** cho giao diện web đáp ứng cả máy tính lẫn điện thoại, và **PostgreSQL** quản lý lược đồ **V14 gồm 49 bảng (461 cột, 152 khóa ngoại, 133 CHECK, 7 GiST EXCLUDE, 10 View)** nâng cấp qua Flyway `V1–V14` tại nhánh `main` (commit tham chiếu `d05d1cb`). Toàn bộ mô hình dữ liệu và mã nguồn đã được kiểm chứng với **131/131 ca kiểm thử tự động Java/Spring Boot**, **20/20 ca kiểm chứng ràng buộc SQL** trên PostgreSQL 18.6 và kiểm tra trực tiếp 13 màn hình trên trình duyệt Chrome."),

    ("H3", "Danh mục từ viết tắt"),
    ("TABLE", "Bảng 0.1: Danh mục các từ viết tắt sử dụng trong báo cáo",
     ["Từ viết tắt", "Diễn giải tiếng Anh / chuyên môn", "Ý nghĩa trong báo cáo"],
     [
         ["API", "Application Programming Interface", "Giao diện lập trình ứng dụng trao đổi dữ liệu JSON."],
         ["ERD", "Entity Relationship Diagram", "Sơ đồ thực thể – liên kết mô tả cơ sở dữ liệu."],
         ["PK / FK / UQ", "Primary Key / Foreign Key / Unique", "Khóa chính, khóa ngoại và ràng buộc duy nhất."],
         ["3NF", "Third Normal Form", "Dạng chuẩn thứ ba trong chuẩn hóa cơ sở dữ liệu."],
         ["MVC", "Model – View – Controller", "Mô hình phân lớp giao diện, điều phối và nghiệp vụ."],
         ["JWT", "JSON Web Token", "Chuỗi mã xác thực phiên người dùng gửi qua HTTP Header."],
         ["RBAC", "Role-Based Access Control", "Kiểm soát truy cập theo vai trò và phạm vi được giao."],
         ["PDF", "Portable Document Format", "Định dạng văn bản cố định dùng xuất hợp đồng, hóa đơn, báo cáo."],
     ],
     [2.4, 5.8, 7.8]),

    # ---------- CHƯƠNG 1 ----------
    ("CH", "CH1", "CHƯƠNG 1. PHÁT BIỂU BÀI TOÁN"),

    ("H2", "S1_1", "1.1. Bài toán thực tế và người sử dụng"),
    ("P", "Khi vận hành một hoặc nhiều khu nhà trọ từ vài chục phòng trở lên, chủ nhà phải theo dõi đồng thời thông tin phòng, người thuê, hợp đồng, đồ đạc bàn giao, giá dịch vụ, chỉ số điện nước, hóa đơn, tiền cọc và lịch sử thu tiền. Nếu quản lý bằng sổ giấy hoặc các file Excel rời rạc, dữ liệu rất dễ trùng lặp, khó truy vết khi điều chỉnh giá giữa chừng và hay xảy ra tranh cãi lúc khách nhận phòng, trả phòng giữa tháng hoặc phát sinh hư hỏng thiết bị."),
    ("P", "Để tránh các sai lệch nghiệp vụ phổ biến đó, nhóm tổ chức bài toán theo ba nguyên tắc tách bạch: (1) tách **người đứng tên ký hợp đồng** với **người thực tế cư trú**; (2) tách **ngày hết hạn trên hợp đồng** với **ngày trả phòng thực tế**; và (3) tách **tiền cọc cam kết** với **tiền đã thực thu vào quỹ**. Ngoài các luồng quản lý nền tảng, phiên bản V14 hoàn thiện đầy đủ 7 nhóm chức năng giảng viên yêu cầu bổ sung: báo cáo doanh thu – dòng tiền theo tháng/quý/năm kèm xuất PDF, 6 chỉ số tổng quan thu tiền và điện nước, quản lý công nợ theo từng khách thuê, cảnh báo điện nước bất thường nổi bật, quản lý đồ đạc theo phòng, biên bản bàn giao đối chiếu đồ đạc lúc nhận–lúc trả kèm ảnh hư hỏng, và xuất hợp đồng/phụ lục PDF tiếng Việt."),
    ("TABLE", "Bảng 1.1: Các nhóm người sử dụng và phạm vi truy cập hệ thống",
     ["Tác nhân", "Phạm vi dữ liệu và quyền hạn chính"],
     [
         ["Chủ nhà (`ADMIN`)", "Quản lý toàn bộ các khu nhà, phòng, tài khoản, phân công và hạn mức nhân viên, bảng giá dịch vụ, hợp đồng, biên bản bàn giao, chứng từ, sổ quỹ và báo cáo tổng hợp."],
         ["Nhân viên (`STAFF`)", "Thực hiện nghiệp vụ vận hành tại đúng khu nhà hoặc phòng được phân công (`employee_assignments`); các thao tác nhạy cảm như hoàn tiền, điều chỉnh chứng từ hay giao phòng thiếu cọc cần có ủy quyền và nằm trong hạn mức (`staff_permission_grants`)."],
         ["Người thuê (`TENANT`)", "Xem hợp đồng, danh sách đồ đạc phòng, hóa đơn đã phát hành (`ISSUED`), lịch sử thanh toán của chính mình; đọc thông báo và gửi yêu cầu báo hỏng kèm ảnh tại phòng đang ở."],
     ],
     [3.4, 12.6]),
    ("P", "Phạm vi bài tập lớn tập trung vào mô hình một chủ nhà quản lý nhiều khu phòng. Các tính năng mở rộng như tự động gạch nợ qua webhook ngân hàng hay vận hành đa chủ nhà độc lập (multi-tenant) được đặt ở hướng phát triển của Chương 5."),

    ("H2", "S1_2", "1.2. Yêu cầu chức năng theo người dùng"),
    ("P", "Toàn bộ yêu cầu chức năng của hệ thống được hệ thống hóa thành 16 nhóm từ `FR01` đến `FR16`, chia thành nhóm quản trị – vận hành (Bảng 1.2) và nhóm tài chính – báo cáo – hỗ trợ (Bảng 1.3)."),
    ("TABLE", "Bảng 1.2: Yêu cầu chức năng nhóm quản trị và vận hành (FR01–FR08)",
     ["Mã", "Tác nhân", "Nội dung chức năng và ràng buộc kiểm soát"],
     [
         ["FR01", "Cả ba vai trò", "Đăng nhập hệ thống: kiểm tra đầu vào không rỗng, tài khoản ở trạng thái `ACTIVE`, đối chiếu mật khẩu băm BCrypt; từ chối cấp phiên khi sai thông tin hoặc vượt giới hạn lần thử."],
         ["FR02", "ADMIN", "Quản lý tài khoản, phân công nhân viên theo khu/phòng và cấp hạn mức ủy quyền; tên đăng nhập duy nhất, không cho tự khóa tài khoản quản trị đang sử dụng."],
         ["FR03", "ADMIN / STAFF", "Quản lý khu nhà, phòng và đồ đạc bố trí trong phòng; sức chứa phòng dương, mã phòng duy nhất trong khu; nhân viên chỉ thao tác trong phạm vi được giao."],
         ["FR04", "ADMIN / STAFF", "Quản lý hồ sơ người thuê; số giấy tờ tùy thân (nếu có) không trùng lặp; tách độc lập hồ sơ cá nhân với tài khoản đăng nhập."],
         ["FR05", "ADMIN / STAFF", "Lập, ký, gia hạn hợp đồng và phụ lục theo từng phiên bản (`contract_terms`); lưu thông tin Bên A/Bên B tại thời điểm ký và xuất file PDF tiếng Việt."],
         ["FR06", "ADMIN / STAFF", "Quản lý người thực ở (`contract_residencies`), kiểm kê đồ đạc và lập biên bản bàn giao nhận phòng (`MOVE_IN`); kiểm soát sức chứa và khoảng thời gian."],
         ["FR07", "ADMIN / STAFF", "Quản lý danh mục dịch vụ và lịch sử cấu hình đơn giá; giá không âm, khi lập hóa đơn luôn tra đúng đơn giá có hiệu lực theo ngày."],
         ["FR08", "ADMIN / STAFF", "Ghi chỉ số công tơ điện nước và thay công tơ; chỉ số không âm, không giảm, xác định rõ đoạn tiêu thụ của khách hay phòng trống và tự đánh giá cảnh báo bất thường."],
     ],
     [1.5, 2.8, 11.7]),
    ("TABLE", "Bảng 1.3: Yêu cầu chức năng nhóm tài chính, báo cáo và hỗ trợ (FR09–FR16)",
     ["Mã", "Tác nhân", "Nội dung chức năng và ràng buộc kiểm soát"],
     [
         ["FR09", "ADMIN / STAFF", "Lập nháp và phát hành hóa đơn tháng/điều chỉnh; tính đúng theo kỳ, ngày lẻ và chỉ số hiệu lực; khóa cứng không cho sửa trực tiếp chứng từ đã phát hành (`ISSUED`)."],
         ["FR10", "ADMIN / STAFF", "Ghi nhận thu tiền vào đúng quỹ tiền mặt hoặc ngân hàng; lưu đủ số tiền thực nhận, phân bổ vào từng dòng nợ, tự giữ phần dư vào quỹ trả trước (`ADVANCE`) và chống ghi trùng."],
         ["FR11", "Người có quyền", "Điều chỉnh hóa đơn và ghi nhận hoàn tiền theo đúng quyền, hạn mức và căn cứ; không sửa đè số tiền trên chứng từ gốc."],
         ["FR12", "ADMIN / STAFF", "Trả phòng và quyết toán: tạo biên bản trả (`MOVE_OUT`) đối chiếu từng món đồ với biên bản nhận, đính kèm ảnh hư hỏng, lập bồi thường có căn cứ và tách bước duyệt quyết toán với bước chi hoàn cọc thật."],
         ["FR13", "Cả ba vai trò", "Báo hỏng và xử lý sự cố: `TENANT` chỉ gửi phiếu cho phòng đang cư trú; `ADMIN/STAFF` tiếp nhận, phân công, ghi tiến độ – chi phí và hoàn tất theo đúng trình tự trạng thái."],
         ["FR14", "ADMIN / STAFF", "Tổng quan 6 chỉ số (thu trước hạn, đúng hạn, sau hạn, chưa thu, điện, nước), báo cáo doanh thu – dòng tiền theo tháng/quý/năm xuất PDF và theo dõi công nợ từng khách thuê."],
         ["FR15", "Người có quyền", "Tải lên, xem và tải tệp đính kèm, ảnh minh chứng và thông báo; kiểm tra quyền sở hữu đối tượng, định dạng tệp thật và giới hạn dung lượng."],
         ["FR16", "ADMIN / Người có mã", "Khôi phục mật khẩu bằng mã dùng một lần có thời hạn (`password_reset_tokens`), chỉ lưu giá trị băm trong cơ sở dữ liệu."],
     ],
     [1.5, 2.8, 11.7]),

    ("H2", "S1_3", "1.3. Yêu cầu ràng buộc nghiệp vụ"),
    ("P", "Sáu ràng buộc cốt lõi dưới đây (Bảng 1.4) được đảm bảo đồng thời ở tầng dịch vụ Java và ràng buộc/trigger dưới PostgreSQL để ngăn dữ liệu sai ngay cả khi có nhiều thao tác đồng thời."),
    ("TABLE", "Bảng 1.4: Sáu ràng buộc nghiệp vụ trọng tâm (BR01–BR06)",
     ["Mã", "Nội dung ràng buộc và cơ chế kiểm soát"],
     [
         ["BR01", "Hợp đồng hết hạn không tự động giải phóng phòng; trạng thái phòng luôn căn cứ vào khoảng chiếm hữu thực tế (`room_possessions`) và biên bản bàn giao trả phòng."],
         ["BR02", "Mọi khoảng thời gian cư trú, chiếm hữu và tính tiền dùng quy ước nửa mở `[bắt đầu, kết thúc)` để không bao giờ tính trùng ngày ranh giới khi đổi khách giữa tháng."],
         ["BR03", "Tiền cọc cam kết trên hợp đồng không đồng nghĩa đã có tiền trong quỹ; chỉ các giao dịch thu thực tế đã xác nhận (`cash_transactions`) mới làm tăng số dư quỹ."],
         ["BR04", "Chứng từ đã phát hành (`ISSUED`) và bút toán sổ quỹ (`fund_entries`) là bất biến; mọi sửa sai phải lập dòng điều chỉnh hoặc bút toán đảo (`REVERSAL`) có liên kết căn cứ."],
         ["BR05", "Tiền thu được phân bổ tối đa bằng phần còn nợ của từng dòng hóa đơn; nếu khách chuyển dư, phần chênh lệch giữ nguyên tại quỹ `ADVANCE` thay vì bỏ sót hoặc làm âm công nợ."],
         ["BR06", "Điện nước tiêu thụ trong giai đoạn phòng trống do chủ nhà chịu; chi phí sửa chữa sự cố (`maintenance_costs`) là chi phí nội bộ, không tự động chuyển thành công nợ của khách thuê."],
     ],
     [1.6, 14.4]),

    ("H2", "S1_4", "1.4. Yêu cầu phi chức năng"),
    ("B", "**An toàn và bảo mật:** Mật khẩu băm bằng BCrypt; xác thực qua JWT kết hợp xoay vòng refresh token dùng một lần; kiểm tra vai trò, phạm vi khu/phòng và hạn mức tại backend (`Access49`). Mọi truy vấn SQL đều dùng tham số hóa để tránh SQL Injection; tệp tải lên được kiểm tra định dạng thật bằng `ImageIO`/header `%PDF-` và lưu mã băm `SHA-256`."),
    ("B", "**Toàn vẹn và chống xung đột dữ liệu:** Các thao tác ghi chỉ số, lập hóa đơn, thu tiền và quyết toán đều chạy trong `@Transactional`, khóa dòng theo thứ tự cố định (`SELECT ... FOR UPDATE`), dùng `pg_advisory_xact_lock` khi tạo mới và kiểm tra mã giao dịch chống gửi lặp (idempotency)."),
    ("B", "**Khả năng truy vết và sử dụng:** Mọi dòng hóa đơn giữ nguyên snapshot đơn giá, công thức và chỉ số; nhật ký thao tác lưu tại `audit_events`. Giao diện React hiển thị rõ ràng trên màn hình máy tính lẫn điện thoại (`390px`), thông báo lỗi tiếng Việt ngắn gọn (ánh xạ 22 lỗi ràng buộc DB sang HTTP `409 Conflict`)."),
    ("B", "**Giới hạn về hiệu năng và vận hành:** Hệ thống mới kiểm chứng ở mức chức năng, toàn vẹn dữ liệu và chạy thực tế cục bộ; kiểm thử tải lưu lượng lớn, cấu hình HTTPS và diễn tập sao lưu – phục hồi dữ liệu là hạng mục cần thực hiện thêm trước khi đưa lên máy chủ công khai."),

    # ---------- CHƯƠNG 2 ----------
    ("CH", "CH2", "CHƯƠNG 2. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG"),

    ("H2", "S2_1", "2.1. Phân tích hệ thống"),

    ("H3", "2.1.1. Xác định thực thể, thuộc tính và khóa"),
    ("P", "Lược đồ cơ sở dữ liệu phiên bản V14 gồm **49 bảng nghiệp vụ, 461 cột, 49 khóa chính, 152 khóa ngoại, 133 ràng buộc CHECK, 7 ràng buộc GiST EXCLUDE và 10 View**. Mỗi đối tượng độc lập hoặc sự kiện cần lưu lịch sử được tách thành một bảng riêng có khóa chính thay thế `id` (`bigint`) kèm các khóa nghiệp vụ (`UNIQUE`) và khóa thời gian chống chồng lấn. Bảng 2.1 tóm tắt 8 nhóm thực thể chính; danh mục đầy đủ cả 461 cột được lưu trong kho mã nguồn tại `docs/TU-DIEN-DU-LIEU-49-BANG.md`, `docs/quanlytro.sql` và `docs/quanlytro.dbml`."),
    ("TABLE", "Bảng 2.1: Phân nhóm thực thể trong mô hình cơ sở dữ liệu V14 (49 bảng)",
     ["Nhóm phân hệ", "Số bảng", "Các bảng đại diện và vai trò nghiệp vụ"],
     [
         ["A. Tài khoản & phân quyền", "6", "`users`, `tenant_profiles`, `employee_assignments`, `staff_permission_grants`, `password_reset_tokens`, `audit_events`."],
         ["B. Khu nhà, phòng & đồ đạc", "7", "`properties`, `rooms`, `room_price_history`, `room_operating_periods`, `asset_types`, `assets`, `asset_placements`."],
         ["C. Hợp đồng, cư trú & bàn giao", "12", "`reservations`, `contracts`, `contract_parties`, `contract_contacts`, `contract_residencies`, `contract_terms` (+5 cột V14), `room_possessions`, `handovers`, `handover_items` (+`original_item_id` V14), `checkout_records`, `move_out_notices`, `contract_violations`."],
         ["D. Dịch vụ & điện nước", "9", "`service_items`, `service_price_configs`, `contract_service_subscriptions`, `meters`, `meter_readings`, `consumption_segments`, `alert_policies`, `consumption_alerts`, `alert_baseline_sources`."],
         ["E. Sự cố & bảo trì", "4", "`maintenance_requests`, `maintenance_events`, `maintenance_costs`, `maintenance_schedules`."],
         ["F. Hóa đơn & sổ quỹ", "7", "`billing_documents`, `billing_items`, `cash_accounts`, `cash_transactions`, `fund_accounts`, `fund_entries`, `fund_holds`."],
         ["G. Thông báo", "2", "`notifications`, `notification_recipients`."],
         ["H. Tệp & ảnh minh chứng", "2", "`stored_files`, `file_attachments` (10 cột FK đích với CHECK đúng 1 cột khác NULL)."],
     ],
     [4.3, 1.5, 10.2]),
    ("P", "Riêng ở bản nâng cấp V14, nhóm bổ sung 5 cột thông tin Bên A / Bên B vào `contract_terms` (để xuất hợp đồng/phụ lục PDF đúng dữ liệu từng phiên bản) và cột `original_item_id` vào `handover_items` (để liên kết từng món đồ lúc trả phòng về đúng dòng kiểm kê lúc nhận phòng); tổng số bảng giữ nguyên 49 bảng. Bảng 2.2 trình bày thuộc tính và khóa của 8 bảng lõi."),
    ("TABLE", "Bảng 2.2: Thuộc tính chính và khóa của các bảng trọng tâm",
     ["Thực thể", "Thuộc tính chính", "Khóa và liên kết chính"],
     [
         ["`users`", "`id`, `username`, `role`, `status`", "PK `id`; UQ `username`."],
         ["`rooms`", "`id`, `property_id`, `room_code`, `max_occupants`", "PK `id`; FK `property_id`; UQ (`property_id`, `room_code`)."],
         ["`contracts`", "`id`, `room_id`, `contract_code`, `start_date`", "PK `id`; FK `room_id`; UQ `contract_code`."],
         ["`contract_residencies`", "`id`, `contract_id`, `tenant_id`, `effective_from`, `effective_to`", "PK `id`; FK hợp đồng & hồ sơ; GiST EXCLUDE chống chồng ngày ở."],
         ["`billing_documents`", "`id`, `contract_id`, `document_code`, `status`", "PK `id`; FK `contract_id`; UQ `document_code`."],
         ["`billing_items`", "`id`, `document_id`, `line_no`, `unit_price_snapshot`", "PK `id`; FK `document_id`; UQ (`document_id`, `line_no`)."],
         ["`cash_transactions`", "`id`, `transaction_code`, `amount`, `cash_account_id`, `payment_document_id`", "PK `id`; UQ `transaction_code`; FK tài khoản nhận & hóa đơn."],
         ["`fund_entries`", "`id`, `from_fund_id`, `to_fund_id`, `debit_item_id`, `amount`", "PK `id`; FK quỹ nguồn/đích, phiếu thu & dòng nợ; append-only."],
     ],
     [3.4, 6.2, 6.4]),

    ("H3", "2.1.2. Xác định các mối quan hệ (1–1, 1–N, N–M)"),
    ("P", "Khi thiết kế liên kết giữa các bảng, bản số (cardinality) được thể hiện kết hợp giữa tính bắt buộc (`NOT NULL`) của khóa ngoại và ràng buộc `UNIQUE`. Bảng 2.3 liệt kê các kiểu quan hệ tiêu biểu trong mô hình V14."),
    ("TABLE", "Bảng 2.3: Các mối quan hệ tiêu biểu (1–1, 1–N, N–M) trong lược đồ V14",
     ["Loại", "Hai thực thể liên kết", "Cách cài đặt và ý nghĩa nghiệp vụ"],
     [
         ["1 – 1", "`users` ↔ `tenant_profiles`", "FK `user_id` đặt `UNIQUE` và cho phép `NULL`: một tài khoản gắn tối đa một hồ sơ người thuê, còn hồ sơ có thể tồn tại trước khi cấp tài khoản."],
         ["1 – N", "`properties` → `rooms`", "FK `rooms.property_id` bắt buộc (`NOT NULL`): một khu nhà quản lý nhiều phòng."],
         ["1 – N", "`contracts` → `contract_terms`", "Một hợp đồng có nhiều phiên bản điều khoản/phụ lục (`version_no`), không ghi đè lịch sử giá cũ."],
         ["1 – N", "`handover_items` → `handover_items`", "FK tự tham chiếu `original_item_id` (V14) trỏ dòng kiểm kê lúc trả phòng về đúng dòng lúc nhận phòng; tránh lặp một món đồ trong cùng biên bản trả."],
         ["1 – N", "`billing_documents` → `billing_items`", "Một chứng từ gồm nhiều dòng chi tiết; mỗi dòng thuộc duy nhất một chứng từ."],
         ["N – M", "`contracts` ↔ `tenant_profiles`", "Tách qua `contract_parties`, `contract_contacts` và `contract_residencies` để lưu riêng vai trò ký, liên hệ và khoảng thời gian thực ở."],
         ["N – M", "`contracts` ↔ `service_items`", "Tách qua `contract_service_subscriptions` để lưu dịch vụ đăng ký, số lượng và khoảng thời gian hiệu lực."],
     ],
     [1.5, 4.5, 10.0]),

    ("H3", "2.1.3. Chuẩn hóa dữ liệu đến dạng chuẩn 3NF"),
    ("B", "**Dạng chuẩn 1 (1NF):** Mọi cột chỉ lưu giá trị nguyên tử, không gộp danh sách người ở, danh sách thiết bị hay nhiều dịch vụ vào một chuỗi. Các nhóm đa trị được tách riêng thành `contract_residencies`, `handover_items`, `contract_service_subscriptions` và `billing_items`."),
    ("B", "**Dạng chuẩn 2 (2NF):** Mọi thuộc tính không khóa phụ thuộc hàm đầy đủ vào khóa chính. Chẳng hạn đơn giá dịch vụ phụ thuộc vào tổ hợp `(property_id, service_item_id, effective_from)` trong `service_price_configs`, không lặp lại ở bảng hợp đồng hay hồ sơ khách thuê."),
    ("B", "**Dạng chuẩn 3 (3NF):** Loại bỏ phụ thuộc bắc cầu giữa các thuộc tính không khóa bằng cách tách thông tin khu nhà ra `properties`, thông tin cá nhân ra `tenant_profiles`, thông tin loại tài sản ra `asset_types`, và không lưu các cột tổng hợp sửa tay như `total_amount` hay `balance` (các giá trị này được tính nhất quán qua 10 View)."),
    ("TABLE", "Bảng 2.4: Phụ thuộc hàm tiêu biểu và cách tách bảng đạt 3NF",
     ["Phụ thuộc hàm", "Bảng lưu trữ và lý do thiết kế"],
     [
         ["`property_id` → tên, địa chỉ khu nhà", "Tách sang `properties`; bảng `rooms` chỉ giữ `property_id` để đổi địa chỉ khu không phải sửa mọi phòng."],
         ["`tenant_id` → họ tên, CCCD, điện thoại", "Tách sang `tenant_profiles`; hợp đồng và cư trú chỉ tham chiếu `tenant_id`."],
         ["`(contract_id, version_no)` → giá thuê, cọc, thông tin hai bên", "Lưu trong `contract_terms` theo từng phiên bản để giữ lịch sử gia hạn và xuất PDF chính xác."],
         ["`(document_id, line_no)` → tên dòng, đơn giá, số lượng snapshot", "Lưu trong `billing_items`; đây là dữ kiện lịch sử thuộc chứng từ tại thời điểm lập, không phải dư thừa bắc cầu."],
     ],
     [5.6, 10.4]),

    ("H3", "2.1.4. Tổ chức cơ sở dữ liệu, ERD và bảng thuộc tính minh họa"),
    ("P", "Hình 2.1 trình bày bản đồ đầy đủ 49 bảng chia theo 8 phân hệ cùng 10 View tính số dư và các cột nâng cấp ở bản V14. Hình 2.2 phóng lớn sơ đồ ERD trọng tâm nối khối **Hợp đồng – Bàn giao đồ đạc (V14)** sang khối **Chứng từ phải thu – Sổ quỹ tài chính**. Toàn bộ lược đồ định nghĩa đầy đủ nằm tại `docs/quanlytro.dbml` và `docs/quanlytro.sql`."),
    ("FIG", "FIG_2_1", "diag-03-db-8modules.png", "Hình 2.1: Tổ chức cơ sở dữ liệu 49 bảng theo 8 phân hệ và các cột bổ sung V14.", 15.2),
    ("FIG", "FIG_2_2", "diag-04-erd-core-finance.png", "Hình 2.2: Sơ đồ ERD trọng tâm khối Hợp đồng – Bàn giao V14 và Hóa đơn – Sổ quỹ.", 15.2),
    ("P", "Hai bảng 2.5 và 2.6 minh họa cấu trúc mô tả chi tiết thuộc tính của bảng `users` và `rooms` trích từ từ điển dữ liệu 49 bảng (`docs/TU-DIEN-DU-LIEU-49-BANG.md`)."),
    ("TABLE", "Bảng 2.5: Mô tả thuộc tính minh họa của bảng users",
     ["Thuộc tính", "Kiểu dữ liệu", "Bắt buộc", "Khóa / Ràng buộc và ý nghĩa"],
     [
         ["`id`", "`bigint`", "Có", "PK tự tăng, định danh duy nhất tài khoản."],
         ["`username`", "`varchar(50)`", "Có", "UQ, tên đăng nhập không phân biệt khoảng trắng thừa."],
         ["`password_hash`", "`varchar(255)`", "Có", "Chuỗi mật khẩu đã băm bằng BCrypt."],
         ["`role`", "`varchar(20)`", "Có", "CHECK thuộc `{ADMIN, STAFF, TENANT}`."],
         ["`status`", "`varchar(20)`", "Có", "CHECK thuộc `{ACTIVE, LOCKED, DISABLED}`."],
     ],
     [3.2, 2.8, 2.0, 8.0]),
    ("TABLE", "Bảng 2.6: Mô tả thuộc tính minh họa của bảng rooms",
     ["Thuộc tính", "Kiểu dữ liệu", "Bắt buộc", "Khóa / Ràng buộc và ý nghĩa"],
     [
         ["`id`", "`bigint`", "Có", "PK tự tăng, định danh duy nhất phòng."],
         ["`property_id`", "`bigint`", "Có", "FK → `properties.id`; cùng `room_code` tạo khóa UQ."],
         ["`room_code`", "`varchar(30)`", "Có", "Mã phòng duy nhất trong cùng khu nhà (ví dụ `P101`)."],
         ["`max_occupants`", "`smallint`", "Có", "CHECK `> 0`, giới hạn số người thực ở tối đa."],
     ],
     [3.2, 2.8, 2.0, 8.0]),

    ("H3", "2.1.5. Sinh dữ liệu mẫu kiểm chứng (Seed V14)"),
    ("P", "Để kiểm chứng đồng thời tất cả nghiệp vụ trước khi bảo vệ, nhóm viết kịch bản `scripts/seed-defense.mjs` nạp dữ liệu mô phỏng cho khu **Tòa nhà SkyHome Central** gồm 4 phòng đại diện cho 4 tình huống thực tế (Bảng 2.7). Toàn bộ họ tên, số CCCD, số điện thoại và ảnh hiện trạng trong bộ seed đều là dữ liệu mô phỏng chạy trên cơ sở dữ liệu kiểm thử riêng."),
    ("TABLE", "Bảng 2.7: Bộ dữ liệu mẫu V14 (scripts/seed-defense.mjs) và kết quả đối chiếu",
     ["Nhóm kịch bản", "Dữ liệu mô phỏng tiêu biểu", "Kết quả đối chiếu thực tế trên ứng dụng"],
     [
         ["Khu và 4 phòng", "SkyHome Central: `P101`, `P102`, `P201`, `P202`", "Đủ trạng thái: đang thuê, đang chuẩn bị sau trả phòng (`TURNOVER`) và phòng trống."],
         ["Hợp đồng & PDF `P101`", "Giá gốc 3.500.000đ; phụ lục bản 2 giá 3.600.000đ", "Đổi ngày thanh toán sang ngày 8 hàng tháng; xuất PDF phụ lục có đầy đủ Bên A/Bên B."],
         ["Tổng quan kỳ `2026-01`", "Tổng hóa đơn `INV-1-2026-01-01`: 4.087.000đ", "Thu trước/đúng/sau hạn mỗi nhóm 1.021.750đ + chưa thu 1.021.750đ = 4.087.000đ; điện 287.000đ, nước 250.000đ."],
         ["Điện nước & công nợ `P102`", "Lịch sử 4 tháng và kỳ `2026-01` tăng vọt; còn nợ 1 hóa đơn", "Sinh 2 cảnh báo cam (nước +236,2%, điện +202,1%) nổi lên đầu bảng; công nợ Lê Thị Bích 1.762.500đ."],
         ["Bàn giao trả phòng `P201`", "Biên bản `HO-OUT-3` nối dòng gốc `MOVE_IN`; hỏng điều hòa", "Lập chứng từ bồi thường `COMPENSATION` 350.000đ, trừ vào cọc 3.800.000đ, chi hoàn thật 3.450.000đ."],
     ],
     [3.4, 5.4, 7.2]),

    ("H2", "S2_2", "2.2. Thiết kế hệ thống"),

    ("H3", "2.2.1. Biểu đồ Use Case mức khung"),
    ("P", "Hình 2.3 mô tả toàn cảnh tương tác của ba tác nhân với 7 nhóm chức năng chính của hệ thống. Chủ nhà (`ADMIN`) kế thừa toàn bộ thao tác vận hành của nhân viên (`STAFF`) và giữ quyền quản trị cấu hình, trong khi người thuê (`TENANT`) chỉ tương tác với cổng tra cứu và báo sự cố. Vì phiên bản hiện tại ghi nhận giao dịch do người quản lý xác minh theo sao kê nên ngân hàng chưa xuất hiện như một tác nhân tích hợp tự động."),
    ("FIG", "FIG_2_3", "diag-02-usecase.png", "Hình 2.3: Biểu đồ Use Case mức khung cho ba tác nhân hệ thống (V14).", 15.2),

    ("H3", "2.2.2. Biểu đồ Use Case mức đỉnh theo từng tác nhân"),
    ("P", "**a) Tác nhân Chủ nhà (`ADMIN`):** Chủ nhà nắm toàn quyền quản trị tài khoản, phân công và cấp hạn mức cho nhân viên, cấu hình khu phòng – giá dịch vụ – ngưỡng cảnh báo, lập hợp đồng, duyệt quyết toán và theo dõi báo cáo toàn hệ thống (Hình 2.4)."),
    ("FIG", "FIG_2_4", "diag-uc-admin.png", "Hình 2.4: Biểu đồ Use Case mức đỉnh của Chủ nhà (ADMIN).", 15.0),
    ("P", "**b) Tác nhân Nhân viên (`STAFF`):** Nhân viên thao tác nghiệp vụ trong đúng phạm vi khu/phòng được phân công; các hành động tài chính đặc biệt (điều chỉnh chứng từ, hoàn tiền) phải qua kiểm tra hạn mức tại `Access49` và `staff_permission_grants` ở backend, không dựa vào việc ẩn nút trên giao diện (Hình 2.5)."),
    ("FIG", "FIG_2_5", "diag-uc-staff.png", "Hình 2.5: Biểu đồ Use Case mức đỉnh của Nhân viên (STAFF).", 15.0),
    ("P", "**c) Tác nhân Người thuê (`TENANT`):** Người thuê chỉ được xem hợp đồng, danh sách đồ đạc, hóa đơn đã phát hành, tệp minh chứng và báo sự cố gắn với nơi đang cư trú; mọi yêu cầu truy cập trái phép sang ID phòng khác đều bị API chặn `403 Forbidden` (Hình 2.6)."),
    ("FIG", "FIG_2_6", "diag-uc-tenant.png", "Hình 2.6: Biểu đồ Use Case mức đỉnh của Người thuê (TENANT).", 15.0),

    ("H3", "2.2.3. Đặc tả chi tiết bảy Use Case trọng tâm (UC01–UC07)"),
    ("P", "Bảy bảng dưới đây đặc tả chi tiết tiền điều kiện, luồng sự kiện chính, nhánh ngoại lệ, hậu điều kiện và nơi cài đặt trong mã nguồn của 7 nghiệp vụ cốt lõi từ `UC01` đến `UC07`."),

    ("TABLE", "Bảng 2.8: Đặc tả UC01 — Đăng nhập hệ thống",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN`, `STAFF`, `TENANT`."],
         ["Mục đích", "Xác thực danh tính và cấp phiên làm việc đúng vai trò."],
         ["Tiền điều kiện", "Tài khoản tồn tại, ở trạng thái `ACTIVE` và chưa bị giới hạn số lần nhập sai."],
         ["Đầu vào", "Tên đăng nhập (`username`), mật khẩu (`password`)."],
         ["Luồng chính", "1. Người dùng nhập thông tin và gửi `POST /api/auth/login`. 2. Backend kiểm tra rỗng, tra tài khoản và đối chiếu BCrypt. 3. Xóa bộ đếm sai, ghi nhận phiên tại `SessionRegistry`, trả `accessToken`, `refreshToken` và vai trò. 4. Frontend lưu phiên vào `sessionStorage` và mở giao diện tương ứng."],
         ["Ngoại lệ", "Thiếu trường bắt buộc: báo lỗi nhập (`400`). Sai mật khẩu hoặc tài khoản không hợp lệ/khóa: ghi nhận lần sai và trả thông báo xác thực chung (`401`), không cấp token."],
         ["Hậu điều kiện", "Phiên hợp lệ được tạo nếu thành công; thất bại không mở bất kỳ API bảo vệ nào."],
         ["Ràng buộc & Mã nguồn", "`FR01`, `FR02`. Refresh token dùng một lần xoay vòng. Cài đặt tại `AuthController.login`, `SessionRegistry`, `frontend/src/api.js`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.9: Đặc tả UC02 — Lập hợp đồng và giao phòng",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN` hoặc `STAFF` có quyền trong phạm vi khu/phòng."],
         ["Mục đích", "Ghi nhận thỏa thuận thuê theo phiên bản, kiểm kê đồ đạc và mốc giao phòng thực tế."],
         ["Tiền điều kiện", "Phòng và hồ sơ người thuê đã có; khoảng thời gian và sức chứa hợp lệ."],
         ["Đầu vào", "Phòng, người ký, thông tin hai bên A/B, thời hạn, giá thuê, mức cọc, ngày thanh toán và hiện trạng đồ đạc."],
         ["Luồng chính", "1. Tạo hợp đồng và phiên bản điều kiện đầu tiên (`contract_terms`). 2. Ghi nhận đã ký; xuất hợp đồng hoặc phụ lục PDF. 3. Lập biên bản nhận phòng (`MOVE_IN`), ghi hiện trạng từng món đồ và xác nhận. 4. Ghi mốc giao phòng (`room_possessions`) và người thực ở (`contract_residencies`)."],
         ["Ngoại lệ", "Vượt phạm vi, trùng khoảng chiếm hữu hoặc vượt sức chứa: từ chối (`403`/`409`). Biên bản còn thiếu hiện trạng không được xác nhận."],
         ["Hậu điều kiện", "Có hợp đồng, điều kiện và mốc giao phòng hợp lệ; việc ký hợp đồng không tự cộng tiền cọc vào sổ quỹ."],
         ["Ràng buộc & Mã nguồn", "`FR05`, `FR06`, `BR01`–`BR03`. Cài đặt tại `Api49`, `Leases49`, `ContractPdf`, `Access49`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.10: Đặc tả UC03 — Ghi chỉ số điện nước",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN` hoặc `STAFF` trong phạm vi phòng được phân công."],
         ["Mục đích", "Ghi điểm đo, tạo đoạn tiêu thụ truy nguồn được và phát hiện tăng đột biến."],
         ["Tiền điều kiện", "Công tơ đang hoạt động, có mốc chỉ số trước và xác định được khoảng ở/phòng trống."],
         ["Đầu vào", "Công tơ, thời điểm đầu–cuối, chỉ số đầu–cuối, hợp đồng (nếu bên chịu là người thuê)."],
         ["Luồng chính", "1. Chọn công tơ và nhập chỉ số mới. 2. Backend kiểm tra số không âm, không giảm, không trùng mốc. 3. Lưu điểm đo (`meter_readings`), tạo đoạn tiêu thụ (`consumption_segments`) gán cho `TENANT` hoặc `OWNER`. 4. So với trung bình ngày các kỳ trước (`WEIGHTED_DAILY`), tạo cảnh báo `PENDING` nếu vượt ngưỡng."],
         ["Ngoại lệ", "Chỉ số giảm, trùng thời điểm hoặc chỉnh sửa điểm đã đưa vào hóa đơn: từ chối và yêu cầu lập điều chỉnh có căn cứ."],
         ["Hậu điều kiện", "Mỗi đoạn tiêu thụ gắn đúng hai điểm đo, khoảng thời gian và bên chịu trách nhiệm."],
         ["Ràng buộc & Mã nguồn", "`FR08`, `BR06`. Cài đặt tại `Meters49`; lưu trên `meters`, `meter_readings`, `consumption_segments`, `consumption_alerts`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.11: Đặc tả UC04 — Lập và phát hành hóa đơn",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN` hoặc `STAFF` có quyền lập chứng từ."],
         ["Mục đích", "Tạo chứng từ phải thu minh bạch công thức, đơn giá và khóa bất biến khi phát hành."],
         ["Tiền điều kiện", "Hợp đồng đang hiệu lực, dữ liệu giá, ngày ở và chỉ số điện nước hợp lệ."],
         ["Đầu vào", "Hợp đồng, kỳ `YYYY-MM`, ngày lập và hạn thanh toán."],
         ["Luồng chính", "1. Gửi yêu cầu lập hóa đơn theo hợp đồng và kỳ. 2. Kiểm tra gửi lại (idempotency), lấy đơn giá và đoạn tiêu thụ hiệu lực. 3. Sinh các dòng snapshot (`billing_items`) tính đúng ngày lẻ và chỉ số. 4. Rà soát cảnh báo điện nước `PENDING`; khi đủ điều kiện chuyển trạng thái sang `ISSUED` và khóa chứng từ."],
         ["Ngoại lệ", "Sai định dạng kỳ, thiếu dữ kiện hoặc còn cảnh báo điện nước chưa xác nhận: chặn phát hành. Gửi lại nội dung mâu thuẫn không tạo hóa đơn thứ hai."],
         ["Hậu điều kiện", "Chứng từ `ISSUED` hình thành số phải thu; số tiền thực thu trong quỹ chưa thay đổi."],
         ["Ràng buộc & Mã nguồn", "`FR09`, `BR04`. Cài đặt tại `Billing49.monthly`, `Finance49.issue`; lưu trên `billing_documents`, `billing_items`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.12: Đặc tả UC05 — Ghi nhận thanh toán hóa đơn",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN` hoặc `STAFF` được phép thu tiền."],
         ["Mục đích", "Ghi đủ khoản tiền thực nhận, gạch nợ hóa đơn và bảo toàn phần tiền dư."],
         ["Tiền điều kiện", "Hóa đơn ở trạng thái `ISSUED`; người vận hành đã xác minh tiền nhận thực tế."],
         ["Đầu vào", "Mã giao dịch (`code`), số tiền, phương thức (`CASH`/`BANK`), tài khoản quỹ nhận, thời điểm nhận và mã tham chiếu."],
         ["Luồng chính", "1. Mở hóa đơn, nhập thông tin thu và chọn quỹ nhận. 2. Kiểm tra quyền, số tiền dương, mã chống trùng và khóa dòng dữ liệu. 3. Ghi 1 phiếu thu thật (`cash_transactions`) và phân bổ tối đa bằng phần còn nợ qua `fund_entries`. 4. Nếu tiền nhận lớn hơn nợ (ví dụ nợ 1.000.000đ, nhận 1.200.000đ), tự chuyển phần dư 200.000đ vào quỹ `ADVANCE`."],
         ["Ngoại lệ", "Gửi lặp đúng mã và nội dung: trả lại kết quả cũ. Trùng mã khác số tiền/hóa đơn hoặc trùng tham chiếu: từ chối `409 Conflict`; lỗi giữa chừng rollback toàn bộ."],
         ["Hậu điều kiện", "Tiền thực nhận ghi đúng một lần trong transaction, không làm âm công nợ và không thất thoát tiền dư."],
         ["Ràng buộc & Mã nguồn", "`FR10`, `BR03`, `BR05`. Cài đặt tại `Billing49.pay`, `Finance49`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.13: Đặc tả UC06 — Trả phòng và quyết toán",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`ADMIN` hoặc `STAFF` có quyền và hạn mức tương ứng."],
         ["Mục đích", "Kết thúc quyền sử dụng phòng, đối chiếu đồ đạc, cấn cọc và tách bước hoàn tiền thật."],
         ["Tiền điều kiện", "Hợp đồng đã giao phòng thực tế; trên giao diện V14 phải chọn biên bản trả đã xác nhận."],
         ["Đầu vào", "Ngày trả thực tế, ngày kết thúc tính tiền, `returnHandoverId`, chỉ số cuối và khoản tạm giữ (nếu có)."],
         ["Luồng chính", "1. Tạo biên bản trả (`MOVE_OUT`) đối chiếu từng món đồ với biên bản nhận, đính ảnh hỏng và xác nhận. 2. Lập chứng từ bồi thường (`COMPENSATION`) nếu có hư hỏng; ghi nhận trả phòng vật lý. 3. Lập và duyệt quyết toán (`checkout/approve`) để cấn nợ vào quỹ cọc `DEPOSIT`, chuyển phòng sang chuẩn bị (`TURNOVER`). 4. Khi chi tiền trả lại khách mới ghi phiếu hoàn thật (`REFUND` / `OUT`)."],
         ["Ngoại lệ", "Chọn biên bản sai hợp đồng hoặc chưa xác nhận: từ chối. Khoản tạm giữ bắt buộc có lý do và ngày rà soát; nếu nợ vượt cọc phần thiếu tiếp tục theo dõi."],
         ["Hậu điều kiện", "Mốc trả và bản quyết toán lưu đầy đủ snapshot; số tiền duyệt hoàn tách riêng với tiền đã chi thực tế."],
         ["Ràng buộc & Mã nguồn", "`FR11`, `FR12`, `BR01`. Cài đặt tại `Leases49`, `Api49`, `Settlements49`, `Finance49`."],
     ],
     [3.4, 12.6]),

    ("TABLE", "Bảng 2.14: Đặc tả UC07 — Báo và xử lý sự cố",
     ["Thuộc tính", "Nội dung đặc tả"],
     [
         ["Tác nhân", "`TENANT` báo hỏng; `ADMIN` hoặc `STAFF` tiếp nhận và xử lý."],
         ["Mục đích", "Theo dõi toàn bộ vòng đời sự cố theo đúng vị trí và lưu chi phí sửa chữa."],
         ["Tiền điều kiện", "Vị trí phòng/khu/tài sản tồn tại; người báo hoặc người xử lý có quyền hợp lệ."],
         ["Đầu vào", "Vị trí (phòng, khu hoặc tài sản), tiêu đề, mô tả, mức ưu tiên và ảnh minh họa (nếu có)."],
         ["Luồng chính", "1. Người thuê gửi phiếu báo sự cố, backend kiểm tra quyền tại phòng đang ở. 2. Quản lý tiếp nhận (`ACCEPTED`) và phân công nhân viên đúng phạm vi. 3. Nhân viên chuyển sang đang xử lý (`IN_PROGRESS`), cập nhật tiến độ và chi phí sửa chữa (`maintenance_costs`). 4. Hoàn tất (`COMPLETED`), lưu sự kiện vào `maintenance_events` và thông báo kết quả."],
         ["Ngoại lệ", "Báo sai phòng đang ở hoặc chuyển tắt từ `NEW` sang `COMPLETED`: từ chối. Phiếu đã hoàn tất chỉ mở lại theo luồng hợp lệ."],
         ["Hậu điều kiện", "Lưu đủ trạng thái, lịch sử xử lý và chi phí thực tế; chi phí sửa chữa không tự cộng thành nợ của người thuê."],
         ["Ràng buộc & Mã nguồn", "`FR13`, `BR06`. Cài đặt hiện hành tại `Operations49.create`, `progress`, `cost`."],
     ],
     [3.4, 12.6]),

    ("H3", "2.2.4. Biểu đồ hoạt động (Activity) và mô tả luồng"),
    ("P", "Bảy biểu đồ hoạt động dưới đây (Hình 2.7 đến Hình 2.13) cụ thể hóa các nhánh rẽ kiểm tra quyền, kiểm tra dữ liệu và điểm kết thúc giao dịch tương ứng với 7 Use Case trọng tâm."),

    ("P", "**Activity 1 — Đăng nhập:** Người dùng gửi tên đăng nhập và mật khẩu; backend lần lượt kiểm tra đầu vào, trạng thái tài khoản và băm BCrypt. Thành công xóa bộ đếm sai và cấp token; thất bại ghi nhận lần thử sai và trả lỗi xác thực chung (Hình 2.7)."),
    ("FIG", "FIG_2_7", "act-01-login.png", "Hình 2.7: Biểu đồ hoạt động luồng đăng nhập.", 11.2),

    ("P", "**Activity 2 — Lập hợp đồng và nhận phòng:** Quản lý lập điều kiện theo phiên bản, ghi nhận ký, kiểm kê đồ đạc qua biên bản `MOVE_IN` rồi mới ghi mốc giao phòng và người thực ở. Việc ký thỏa thuận được tách độc lập với sự kiện giao phòng và thu cọc (Hình 2.8)."),
    ("FIG", "FIG_2_8", "act-02-contract.png", "Hình 2.8: Biểu đồ hoạt động luồng lập hợp đồng và nhận phòng.", 11.2),

    ("P", "**Activity 3 — Ghi chỉ số điện nước:** Mỗi khoảng đo đều kiểm tra phạm vi công tơ, thứ tự chỉ số và quyền chiếm hữu thực tế trước khi lưu điểm đo, tạo đoạn tiêu thụ và đối chiếu ngưỡng cảnh báo theo mức tiêu thụ ngày (Hình 2.9)."),
    ("FIG", "FIG_2_9", "act-03-meter.png", "Hình 2.9: Biểu đồ hoạt động luồng ghi chỉ số điện nước.", 14.2),

    ("P", "**Activity 4 — Lập và phát hành hóa đơn:** Hệ thống nhận hợp đồng và kỳ `YYYY-MM`, kiểm tra yêu cầu gửi lại, tổng hợp giá và khoảng đo hiệu lực để ghi các dòng snapshot. Chứng từ chỉ chuyển sang `ISSUED` khi không còn dữ kiện thiếu hoặc cảnh báo điện nước chưa xử lý (Hình 2.10)."),
    ("FIG", "FIG_2_10", "act-04-invoice.png", "Hình 2.10: Biểu đồ hoạt động luồng lập và phát hành hóa đơn.", 13.6),

    ("P", "**Activity 5 — Thanh toán và giữ tiền dư:** Sau khi người thu xác minh tiền thực nhận, backend kiểm tra mã chống trùng, mở transaction khóa dữ liệu, ghi đủ khoản thu vào quỹ, phân bổ vào từng dòng nợ và giữ phần dư tại `ADVANCE` (Hình 2.11)."),
    ("FIG", "FIG_2_11", "act-05-payment.png", "Hình 2.11: Biểu đồ hoạt động luồng thanh toán và giữ tiền dư.", 12.4),

    ("P", "**Activity 6 — Trả phòng, quyết toán và hoàn cọc:** Giao diện V14 yêu cầu hoàn tất biên bản đối chiếu nhận–trả trước khi ghi trả phòng vật lý. Khoản bồi thường lập chứng từ riêng; quyết toán cấn trừ cọc và phiếu chi hoàn tiền thực tế được thực hiện ở hai bước độc lập (Hình 2.12)."),
    ("FIG", "FIG_2_12", "act-06-checkout.png", "Hình 2.12: Biểu đồ hoạt động luồng trả phòng, quyết toán và hoàn cọc.", 8.2),

    ("P", "**Activity 7 — Báo và xử lý sự cố:** Người thuê gửi phiếu đúng phòng đang ở; quản lý tiếp nhận, phân công nhân viên theo phạm vi, ghi tiến độ – chi phí thực tế và hoàn tất, đồng thời lưu đầy đủ sự kiện lịch sử (Hình 2.13)."),
    ("FIG", "FIG_2_13", "act-07-maintenance.png", "Hình 2.13: Biểu đồ hoạt động luồng báo và xử lý sự cố.", 7.8),

    ("H3", "2.2.5. Biểu đồ lớp và thành phần theo kiến trúc MVC"),
    ("P", "Toàn bộ mã nguồn được phân tầng rõ theo kiến trúc MVC kết hợp lớp Service chuyên biệt (Hình 2.14): tầng **View** là ứng dụng React (`main.jsx`, `Model49Panels.jsx`, `api.js`); tầng **Controller** (`AuthController`, `Api49`, `Reports49`) nhận REST JSON, xác thực quyền qua `Access49` và ánh xạ lỗi DB qua `ApiErrors`; tầng **Model nghiệp vụ** tách thành từng lớp dịch vụ (`Leases49`, `Meters49`, `Billing49`, `Finance49`, `Settlements49`, `Operations49`, `Catalog49`, `Files49`, `ContractPdf`). Ở tầng lưu trữ, các lớp nghiệp vụ truy vấn PostgreSQL chủ yếu qua `Database` (bọc `JdbcTemplate`) để kiểm soát chính xác khóa dòng `FOR UPDATE` và 10 View tài chính, đồng thời khai báo đủ 49 JPA Entity chạy với `ddl-auto=validate` để xác minh lược đồ khớp hoàn toàn khi khởi động."),
    ("FIG", "FIG_2_14", "diag-mvc-classes.png", "Hình 2.14: Biểu đồ lớp và thành phần theo kiến trúc MVC đối chiếu mã nguồn V14.", 15.2),

    ("H3", "2.2.6. Biểu đồ triển khai (Deployment)"),
    ("P", "Hình 2.15 mô tả kiến trúc đóng gói triển khai theo tệp `compose.yml` của dự án. Giao diện React sau khi biên dịch (`npm run build`) được đặt trực tiếp vào `src/main/resources/static/` trong cùng tệp Spring Boot JAR, giúp trình duyệt giao tiếp với API cùng origin trên cổng `8080` mà không phát sinh phức tạp CORS. Container ứng dụng kết nối sang PostgreSQL qua mạng nội bộ Docker cổng `5432`, sử dụng hai volume tách biệt là `hostel-data` (dữ liệu CSDL) và `hostel-images` (ảnh hiện trạng và tệp đính kèm). Lưu ý `compose.yml` khai báo image `postgres:16-alpine` trong khi cụm kiểm chứng cục bộ chạy PostgreSQL 18.6 — toàn bộ migration `V1–V14` tương thích trên cả hai phiên bản."),
    ("FIG", "FIG_2_15", "diag-deployment-v14.png", "Hình 2.15: Biểu đồ triển khai hệ thống theo cấu hình Docker Compose (compose.yml).", 15.2),

    # ---------- CHƯƠNG 3 ----------
    ("CH", "CH3", "CHƯƠNG 3. XÂY DỰNG HỆ THỐNG"),

    ("H2", "S3_1", "3.1. Kế hoạch thực hiện và phân công thành viên"),
    ("P", "Dự án do 6 thành viên nhóm **gehihi** (lớp `N03`) triển khai nối tiếp qua 5 giai đoạn: (1) khảo sát bài toán và chốt bộ yêu cầu/ràng buộc; (2) chuẩn hóa dữ liệu 3NF, viết migration `V1–V14` và bộ kiểm chứng SQL; (3) xây dựng tầng backend Spring Boot và giao diện React; (4) tích hợp 7 yêu cầu bổ sung, viết kịch bản seed và chạy 131 bài test tự động; (5) rà soát lỗi thực tế trên trình duyệt và hoàn thiện báo cáo. Bảng 3.1 ghi nhận phân công cụ thể của từng thành viên."),
    ("TABLE", "Bảng 3.1: Danh sách thành viên nhóm gehihi (Lớp N03) và phân công công việc",
     ["STT", "Họ và tên", "MSSV", "Phần việc phụ trách và sản phẩm bàn giao"],
     [[m[0], m[1], m[2], m[3]] for m in MEMBERS],
     [1.1, 3.8, 2.3, 8.8]),

    ("H2", "S3_2", "3.2. Minh họa cài đặt mã nguồn Frontend và Backend"),
    ("P", "Bốn trích đoạn mã nguồn tiêu biểu dưới đây được lấy trực tiếp từ kho mã nguồn tại commit `d05d1cb` (giữ nguyên số dòng để dễ đối chiếu) nhằm làm rõ cách nhóm hiện thực cơ chế gọi API tự làm mới phiên, xác thực đăng nhập, kiểm soát vòng đời sự cố và khóa giao dịch thanh toán."),

    ("H3", "3.2.1. Frontend gọi API và quản lý phiên (frontend/src/api.js)"),
    ("P", "Đoạn mã tại Hình 3.1 (trích `frontend/src/api.js`, dòng 14–23) định nghĩa hàm `api(path, method, data, retry)` dùng chung cho mọi màn hình. Mọi yêu cầu tự động gắn tiền tố `/api`, gửi thân JSON và bổ sung header `Authorization: Bearer <accessToken>` khi phiên đã đăng nhập. Khi nhận mã `401` do hết hạn token, hàm tự gọi `/api/auth/refresh` một lần để xoay vòng phiên mới rồi gửi lại request gốc; nếu refresh thất bại sẽ xóa phiên và phát sự kiện đưa người dùng về màn hình đăng nhập. Hiện phiên được lưu trong `sessionStorage` để cô lập theo từng tab trình duyệt; khi triển khai thực tế trên máy chủ công khai có thể chuyển refresh token sang cookie `HttpOnly` để giảm rủi ro XSS."),
    ("FIG", "FIG_3_1", "code-01-api.png", "Hình 3.1: Trích đoạn frontend/src/api.js (dòng 14–23) về gửi yêu cầu HTTP và quản lý phiên.", 15.0),

    ("H3", "3.2.2. Backend xác thực đăng nhập (AuthController.java)"),
    ("P", "Tại Hình 3.2 (trích `AuthController.java`, dòng 69–81), yêu cầu đăng nhập đi qua Bean Validation trước khi tra cứu tài khoản và kiểm tra trạng thái `ACTIVE`. Mật khẩu người dùng nhập được đối chiếu với chuỗi băm BCrypt thông qua `PasswordEncoder.matches`, tuyệt đối không lưu hay so sánh mật khẩu dạng rõ. Trường hợp sai tài khoản hoặc sai mật khẩu đều ghi nhận vào bộ đếm chống đoán mật khẩu trong `SessionRegistry` và trả cùng một thông báo xác thực chung để tránh lộ tên tài khoản hợp lệ."),
    ("FIG", "FIG_3_2", "code-02-auth.png", "Hình 3.2: Trích đoạn AuthController.login (dòng 69–81) kiểm tra xác thực BCrypt và phiên.", 15.0),

    ("H3", "3.2.3. Backend kiểm soát chuyển trạng thái sự cố (Operations49.java)"),
    ("P", "Hình 3.3 (trích `Operations49.java`, dòng 141–150) thể hiện bước kiểm tra quyền quản lý, phạm vi vị trí của phiếu và đối chiếu bảng chuyển trạng thái hợp lệ (`NEW → ACCEPTED → IN_PROGRESS → COMPLETED`). Mọi yêu cầu nhảy cóc trực tiếp từ `NEW` sang `COMPLETED` hoặc gán cho nhân viên ngoài phạm vi khu/phòng đều bị từ chối. Cập nhật phiếu, ghi thêm dòng lịch sử `maintenance_events` và ghi `audit_events` đều nằm chung trong một transaction để đảm bảo toàn vẹn."),
    ("FIG", "FIG_3_3", "code-03-ops.png", "Hình 3.3: Trích đoạn Operations49.progress (dòng 141–150) kiểm tra quyền và luồng trạng thái sự cố.", 15.0),

    ("H3", "3.2.4. Backend ghi nhận thanh toán và giữ tiền dư (Billing49.java)"),
    ("P", "Tại Hình 3.4 (trích `Billing49.java`, dòng 398–405), sau khi kiểm tra quyền thu tiền, trạng thái `ISSUED` của hóa đơn, số tiền dương và sự tương khớp giữa phương thức (`CASH`/`BANK`) với tài khoản quỹ nhận, hệ thống khóa dòng dữ liệu liên quan rồi gọi `Finance49` ghi đủ khoản tiền thực nhận và phân bổ nợ. Với trường hợp khách nợ 1.000.000đ nhưng chuyển 1.200.000đ, hệ thống gạch nợ 1.000.000đ và tự giữ 200.000đ vào quỹ `ADVANCE`; gửi lặp cùng mã giao dịch sẽ trả kết quả cũ chứ không tạo phiếu thu thứ hai. Các bút toán đồng thời lưu đúng ngày nhận tiền thực tế để báo cáo tổng quan phân loại chuẩn ba nhóm thu trước hạn, đúng hạn và sau hạn."),
    ("FIG", "FIG_3_4", "code-04-pay.png", "Hình 3.4: Trích đoạn Billing49.pay (dòng 398–405) kiểm tra quyền thu và khóa dữ liệu giao dịch.", 15.0),

    ("P", "Trong quá trình chạy kiểm thử trên máy Windows ở giai đoạn hoàn thiện V14, nhóm còn phát hiện và xử lý kịp thời nhiều lỗi thực tế: (1) PostgreSQL chạy `--locale=C` khiến `lower('Điện...')` không đổi được chữ `'Đ'` hoa sang `'đ'` thường, làm lệch tổng tiền điện ở trang Tổng quan — nhóm đã sửa bằng khối `CROSS JOIN LATERAL` phân loại rõ tại `Reports49.java`; (2) tiêu đề trang 2 file PDF Phụ lục bị in nhầm thành `HỢP ĐỒNG` trong `ContractPdf.java`; (3) chặn API danh mục `Catalog49` không cho sửa tắt cột `status` của biên bản bàn giao; và (4) bổ sung ô chọn tài khoản quỹ nhận tiền cùng ẩn nút thêm/gỡ đồ đạc ở giao diện `TENANT`."),

    # ---------- CHƯƠNG 4 ----------
    ("CH", "CH4", "CHƯƠNG 4. KIỂM THỬ VÀ TRIỂN KHAI"),

    ("H2", "S4_1", "4.1. Kế hoạch kiểm thử"),
    ("P", "Kế hoạch kiểm thử tập trung ưu tiên hai nhóm rủi ro cao nhất của hệ thống quản lý nhà trọ: **vượt quyền truy cập** (`STAFF` ngoài khu được giao, `TENANT` xem phòng khác) và **sai lệch tiền tệ** (thu lặp, mất tiền dư, sửa hóa đơn đã phát hành, cấn cọc vượt số dư). Chín nhóm kiểm thử từ `TP01` đến `TP09` được tổng hợp trong Bảng 4.1."),
    ("TABLE", "Bảng 4.1: Kế hoạch kiểm thử tổng thể hệ thống (TP01–TP09)",
     ["Mã", "Phạm vi kiểm thử", "Các tình huống trọng tâm cần xác minh"],
     [
         ["TP01", "Đăng nhập & phiên", "Đăng nhập hợp lệ; bỏ trống trường; sai mật khẩu; tài khoản khóa; xoay vòng refresh token dùng một lần."],
         ["TP02", "Phân quyền RBAC", "`STAFF` thao tác ngoài phạm vi khu/phòng; `TENANT` truy cập hợp đồng/hóa đơn phòng khác; vượt hạn mức hoàn tiền."],
         ["TP03", "Hợp đồng & bàn giao", "Lập, ký, gia hạn hợp đồng; xuất PDF tiếng Việt; đối chiếu biên bản `MOVE_IN`/`MOVE_OUT` và ảnh hư hỏng."],
         ["TP04", "Điện nước & cảnh báo", "Chỉ số tăng bình thường, giảm, trùng mốc; tiêu thụ phòng trống; thay công tơ; cảnh báo vượt ngưỡng `WEIGHTED_DAILY`."],
         ["TP05", "Hóa đơn & snapshot", "Lập hóa đơn kỳ hợp lệ, kỳ sai định dạng; chống tạo trùng; chặn phát hành khi còn cảnh báo; khóa bất biến `ISSUED`."],
         ["TP06", "Thu tiền & tiền dư", "Thu thiếu, thu đủ, thu dư sang `ADVANCE`; chọn tài khoản quỹ `CASH`/`BANK`; chống gửi lặp; phân loại ngày thu."],
         ["TP07", "Trả phòng & quyết toán", "Ngày trả thực tế; lập bồi thường có căn cứ; cấn trừ cọc; nợ vượt cọc; khoản tạm giữ; chi hoàn thực tế tách biệt."],
         ["TP08", "Sự cố & tệp đính kèm", "Báo hỏng đúng/sai phòng; cấm nhảy trạng thái `NEW → COMPLETED`; kiểm tra định dạng ảnh/PDF thật và quyền tải."],
         ["TP09", "Migration & báo cáo V14", "Di trú sạch `V1 → V14`; đối khớp 6 chỉ số tổng quan, báo cáo tháng/quý/năm, PDF báo cáo và công nợ khách."],
     ],
     [1.5, 3.6, 10.9]),

    ("H2", "S4_2", "4.2. Ca kiểm thử trọng tâm và kết quả thực thi"),
    ("P", "Toàn bộ các kịch bản trong kế hoạch `TP01–TP09` đã được thực thi trên cơ sở dữ liệu kiểm thử PostgreSQL 18.6 riêng thông qua 13 bộ kiểm thử tích hợp Java/HTTP (`src/test/java/com/hostel/`), 20 ca SQL trực tiếp (`docs/database-cases49.sql`) và kiểm tra thao tác tay trên 13 màn hình trình duyệt Chrome. Bảng 4.2 trích dẫn 12 ca kiểm thử tiêu biểu đại diện cho đủ 9 nhóm phạm vi cùng kết quả thực tế đã xác minh."),
    ("TABLE", "Bảng 4.2: Các ca kiểm thử trọng tâm đã thực thi và kết quả đối chiếu (V14)",
     ["Mã ca", "Thuộc nhóm", "Đầu vào & bước thực hiện chính", "Kết quả mong đợi & thực tế", "Kết quả"],
     [
         ["TC01", "TP01", "Đăng nhập `admin` đúng mật khẩu, sau đó gửi mật khẩu sai và dùng lại refresh token cũ.", "Đúng mật khẩu cấp JWT + vai trò `ADMIN`; sai mật khẩu trả `401` chung; dùng lại refresh token cũ bị thu hồi và chặn.", "ĐẠT"],
         ["TC02", "TP02", "`STAFF` phụ trách khu A gọi API xem/sửa hợp đồng khu B; `TENANT` phòng P101 gọi API xem hóa đơn phòng P102.", "Cả hai yêu cầu đều bị `Access49` chặn trả về `403 Forbidden`, không rò rỉ dữ liệu.", "ĐẠT"],
         ["TC03", "TP03", "Tạo phụ lục bản 2 cho `P101` (đổi giá 3.600.000đ, ngày thu 8) và gọi `GET /contracts/{id}/pdf?version=2`.", "Xuất file PDF tiếng Việt đúng tiêu đề `PHỤ LỤC HỢP ĐỒNG` trên cả trang 1 và trang 2, đủ thông tin Bên A/Bên B.", "ĐẠT"],
         ["TC04", "TP03", "Tạo biên bản trả `HO-OUT-3` cho `P201`, gắn `original_item_id` về biên bản nhận, tải ảnh điều hòa hỏng và xác nhận.", "Lưu đủ đối chiếu Lúc nhận – Lúc trả; lập chứng từ bồi thường `COMPENSATION` 350.000đ gắn đúng `handover_item_id`.", "ĐẠT"],
         ["TC05", "TP04", "Nhập chỉ số giảm hơn kỳ trước cho `P101`; sau đó nhập nước/điện `P102` tăng >200% so với trung bình 4 tháng trước.", "Chỉ số giảm bị từ chối `409`; chỉ số tăng vọt của `P102` sinh 2 cảnh báo `PENDING` nổi cam lên đầu bảng.", "ĐẠT"],
         ["TC06", "TP05", "Phát hành hóa đơn `P102` khi còn cảnh báo `PENDING`; sau đó thử `UPDATE` trực tiếp hóa đơn `ISSUED` dưới SQL.", "API chặn phát hành hóa đơn có cảnh báo chưa xác nhận; trigger PostgreSQL chặn sửa hóa đơn `ISSUED`.", "ĐẠT"],
         ["TC07", "TP06", "Thu 1.200.000đ cho hóa đơn nợ 1.000.000đ với mã `PAY-01`, sau đó gửi lặp lại đúng mã `PAY-01` hai lần.", "Gạch nợ đủ 1.000.000đ, tự cất 200.000đ vào quỹ `ADVANCE`; gửi lặp trả kết quả cũ, không tạo phiếu thu thứ hai.", "ĐẠT"],
         ["TC08", "TP06", "Thu tiền chọn phương thức `BANK` nhưng truyền mã quỹ tiền mặt `CASH-1`.", "Backend phát hiện sai loại tài khoản nhận và từ chối yêu cầu, không ghi sổ quỹ lệch kênh.", "ĐẠT"],
         ["TC09", "TP07", "Trả phòng `P201`, duyệt quyết toán cấn 350.000đ bồi thường vào cọc 3.800.000đ, sau đó ghi phiếu chi hoàn 3.450.000đ.", "Bước duyệt chỉ tạo snapshot cấn cọc (quỹ còn 3.450.000đ); sau khi ghi `REFUND`/`OUT` quỹ về 0đ và báo cáo ghi đúng cột Hoàn quỹ.", "ĐẠT"],
         ["TC10", "TP08", "`TENANT` báo hỏng tại phòng đang ở kèm ảnh PNG giả mạo (file text đổi đuôi `.png`); quản lý thử chuyển thẳng `NEW → COMPLETED`.", "`Files49` phát hiện tệp không phải ảnh thật qua `ImageIO` và từ chối; `Operations49` chặn chuyển tắt trạng thái.", "ĐẠT"],
         ["TC11", "TP09", "Chạy Flyway di trú từ CSDL trắng qua toàn bộ `V1 → V14` và đối soát Hibernate `ddl-auto=validate`.", "Tạo đủ 49 bảng, 461 cột, 152 FK, 133 CHECK, 7 EXCLUDE, 10 View; ứng dụng khởi động thành công không lệch schema.", "ĐẠT"],
         ["TC12", "TP09", "Mở Tổng quan kỳ `2026-01`, Báo cáo quý/năm và Công nợ khách trên Chrome (desktop + mobile `390px`).", "Tổng 4 ô trạng thái thu = 4.087.000đ khớp tổng hóa đơn; công nợ Lê Thị Bích = 1.762.500đ; 0 lỗi console JS, không vỡ khung.", "ĐẠT"],
     ],
     [1.3, 1.4, 5.2, 6.6, 1.5]),

    ("H2", "S4_3", "4.3. Tổng hợp kết quả kiểm chứng và minh chứng trong kho mã nguồn"),
    ("P", "Bảng 4.3 tổng hợp kết quả từ ba lớp kiểm chứng độc lập được lưu cùng mã nguồn tại commit `d05d1cb`. Tất cả bài test Java đều chạy trực tiếp trên PostgreSQL 18.6 thật (không dùng H2 giả lập) để xác minh đầy đủ trigger PL/pgSQL, ràng buộc `GiST EXCLUDE` và khóa đồng thời."),
    ("TABLE", "Bảng 4.3: Tổng hợp kết quả kiểm chứng tự động và trình duyệt phiên bản V14",
     ["Lớp kiểm chứng", "Phạm vi và số lượng", "Kết quả", "Tệp minh chứng trong kho mã nguồn"],
     [
         ["Kiểm thử Java — Mô hình 49 bảng (V14)", "68 ca (`BusinessIntegrityTests` 35, `Model49Tests` 21, `OpenKosWorkflowTests` 8, `Model49HttpTests` 4)", "68 / 68 ĐẠT", "`docs/implementation-validation49.json`"],
         ["Kiểm thử Java — Di trú Flyway `V1–V14`", "8 ca (`BusinessMigrationTests` 1, `Model49MigrationTests` 3, `MigrationTests` 4)", "8 / 8 ĐẠT", "`docs/implementation-validation49.json`"],
         ["Kiểm thử Java — Bảo mật, PDF, Ảnh & Hồi quy", "55 ca (`BusinessWorkflowTests` 38, `HttpSecurityTests` 6, `ImageWorkflowTests` 5, `FeeCalculatorTests` 3, `PdfTests` 2, `AppTests` 1)", "55 / 55 ĐẠT", "`docs/implementation-validation49.json`"],
         ["Kiểm chứng lược đồ & Trigger SQL", "20 ca kiểm tra 49 bảng, 461 cột, khóa sổ, GiST EXCLUDE trên PostgreSQL 18.6", "20 / 20 ĐẠT", "`docs/database-validation.json`, `scripts/verify-model49.py`"],
         ["Kiểm thử giao diện Chrome & Seed V14", "13 màn hình desktop + responsive `390px`, xuất PDF hợp đồng/báo cáo, 0 lỗi JS", "13 / 13 ĐẠT", "`scripts/seed-defense.mjs`, ảnh tại `docs/report-assets/`"],
     ],
     [3.8, 5.8, 2.2, 4.2]),
    ("P", "Tổng cộng toàn dự án đạt **131/131 bài test Java (0 failure, 0 error)** và **20/20 ca SQL**. Kết quả này chứng minh tính đúng đắn về nghiệp vụ và toàn vẹn dữ liệu ở phạm vi bài tập lớn; các hạng mục kiểm thử chịu tải đồng thời lớn và diễn tập phục hồi sao lưu chưa nằm trong đợt kiểm thử này."),

    ("H2", "S4_4", "4.4. Triển khai và quản lý mã nguồn"),
    ("P", "Toàn bộ mã nguồn, script kiểm thử và tài liệu kỹ thuật được quản lý tập trung bằng Git tại kho lưu trữ `https://github.com/hieunofun/quanlytro` (nhánh `main`, commit tham chiếu `d05d1cb`). Bảng 4.4 tóm tắt cấu hình công nghệ và đóng gói của chương trình."),
    ("TABLE", "Bảng 4.4: Môi trường công nghệ và thành phần triển khai",
     ["Thành phần", "Phiên bản / Công cụ", "Vai trò trong hệ thống"],
     [
         ["Backend", "Java 21, Spring Boot 4.1.1, Maven", "REST API, bảo mật JWT, phân quyền `Access49`, dịch vụ nghiệp vụ, transaction."],
         ["Frontend", "React 19.2, Vite 7.1", "Giao diện SPA đáp ứng desktop/mobile, build tĩnh vào JAR."],
         ["Cơ sở dữ liệu", "PostgreSQL 18.6 (compose: `16-alpine`), Flyway `V1–V14`", "Lưu 49 bảng, 10 View, 7 GiST EXCLUDE và trigger khóa sổ."],
         ["Xuất văn bản", "Apache PDFBox 3.0.7 + font NotoSans", "Xuất PDF tiếng Việt có dấu cho hóa đơn, hợp đồng/phụ lục và báo cáo."],
         ["Đóng gói & kiểm thử", "Dockerfile, `compose.yml`, JUnit 5, Playwright", "Đóng gói container và tự động hóa kiểm chứng."],
     ],
     [3.0, 4.8, 8.2]),
    ("P", "Quy trình dựng và chạy kiểm chứng cục bộ gồm bốn bước chuẩn hóa sẵn bằng script trong thư mục `scripts/`: (1) biên dịch giao diện vào tài nguyên tĩnh (`cd frontend; npm.cmd run build`); (2) chạy toàn bộ 131 bài test Java (`powershell -ExecutionPolicy Bypass -File scripts/test-backend-business.ps1`); (3) dựng schema trắng kiểm tra 20 ca SQL (`python scripts/verify-model49.py`); và (4) khởi chạy máy chủ demo cùng kịch bản dữ liệu mẫu (`scripts/run-model49-review.ps1` và `node scripts/seed-defense.mjs`). Khi triển khai bằng Docker Compose (`docker compose up --build`), ứng dụng mở tại cổng `8080` với các tham số nhạy cảm (`DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`) nạp qua tệp `.env`."),

    # ---------- CHƯƠNG 5 ----------
    ("CH", "CH5", "CHƯƠNG 5. KẾT QUẢ VÀ HƯỚNG PHÁT TRIỂN"),

    ("H2", "S5_1", "5.1. Hình ảnh kết quả chạy thực tế và giải thích"),
    ("P", "Các hình ảnh dưới đây (Hình 5.1 đến Hình 5.7) được chụp trực tiếp từ ứng dụng V14 đang chạy trên bộ dữ liệu mô phỏng khu *Tòa nhà SkyHome Central* (`scripts/seed-defense.mjs`), bao phủ đầy đủ các màn hình nghiệp vụ trọng tâm và bản PDF xuất thực tế."),

    ("P", "**Màn hình Tổng quan và 6 chỉ số (Hình 5.1):** Tại kỳ `2026-01`, 4 thẻ chỉ số phía trên và 6 ô phân loại bên dưới đối khớp nhau tuyệt đối: tổng lập hóa đơn `4.087.000đ` bằng tổng bốn nhóm Thu trước hạn (`1.021.750đ`) + Thu đúng hạn (`1.021.750đ`) + Thu sau hạn (`1.021.750đ`) + Chưa thu (`1.021.750đ`), tách riêng Tiền điện (`287.000đ`) và Tiền nước (`250.000đ`), kèm nút chuyển nhanh sang chế độ xem theo Quý."),
    ("FIG", "FIG_5_1", "ui-dashboard-month.png", "Hình 5.1: Màn hình Tổng quan kỳ 2026-01 hiển thị 4 thẻ tổng hợp và 6 chỉ số thu – điện – nước.", 14.6),

    ("P", "**Báo cáo Doanh thu & Dòng tiền theo Tháng / Quý / Năm (Hình 5.2):** Bộ lọc thời gian cho phép chuyển linh hoạt giữa báo cáo tháng, quý và năm, vẽ biểu đồ cột so sánh lập hóa đơn với thực thu, tách riêng cột *Tiền vào quỹ* (bao gồm tiền cọc mới thu) với *Hoàn quỹ* và xuất báo cáo PDF trực tiếp."),
    ("FIG", "FIG_5_2", "ui-reports.png", "Hình 5.2: Màn hình Báo cáo doanh thu & dòng tiền theo Tháng / Quý / Năm kèm xuất PDF.", 14.8),

    ("P", "**Quản lý Công nợ theo khách thuê (Hình 5.3):** Màn hình gom toàn bộ hóa đơn chưa thanh toán theo từng khách thuê, hiển thị số ngày quá hạn, lọc riêng khách quá hạn và mở chi tiết từng hóa đơn nợ (ví dụ khách Lê Thị Bích phòng `P102` còn nợ `1.762.500đ`) để bấm thu tiền ngay hoặc xuất CSV."),
    ("FIG", "FIG_5_3", "ui-debts.png", "Hình 5.3: Màn hình Công nợ khách thuê mở rộng chi tiết hóa đơn quá hạn của Lê Thị Bích (P102).", 14.8),

    ("P", "**Cảnh báo điện nước bất thường (Hình 5.4):** Các bản ghi vượt ngưỡng tiêu thụ ngày của phòng `P102` (nước tăng `+236,2%` so với mức `0,32 m³/ngày`; điện tăng `+202,1%` so với `2,43 kWh/ngày`) được tự động đẩy lên đầu bảng, tô nền cam rõ ràng để quản lý kiểm tra trước khi phát hành hóa đơn."),
    ("FIG", "FIG_5_4", "ui-meter-alerts.png", "Hình 5.4: Màn hình Điện nước đẩy nổi bật 2 cảnh báo bất thường của phòng P102 lên đầu bảng.", 14.8),

    ("P", "**Chi tiết hóa đơn minh bạch (Hình 5.5):** Mỗi dòng trên hóa đơn `INV-1-2026-01-01` ghi rõ công thức tính tiền phòng, chỉ số điện nước cũ–mới và đơn giá snapshot, đồng thời liệt kê đầy đủ lịch sử 3 đợt thanh toán giúp cả chủ nhà lẫn khách thuê dễ dàng đối chiếu."),
    ("FIG", "FIG_5_5", "ui-invoice-detail.png", "Hình 5.5: Chi tiết hóa đơn INV-1-2026-01-01 với công thức tính từng khoản và lịch sử thanh toán.", 14.8),

    ("P", "**Đối chiếu đồ đạc biên bản nhận–trả phòng (Hình 5.6):** Biên bản trả phòng `HO-OUT-3` của phòng `P201` đặt song song cột *Lúc nhận* và *Lúc trả* nhờ khóa ngoại `original_item_id`, đính kèm ảnh chụp tình trạng hư hỏng của điều hòa Daikin để làm căn cứ phát hành chứng từ bồi thường `350.000đ` trước khi quyết toán trả cọc `3.450.000đ`."),
    ("FIG", "FIG_5_6", "ui-contract-p201-handover.png", "Hình 5.6: Biên bản trả phòng HO-OUT-3 của phòng P201 đối chiếu đồ đạc Lúc nhận – Lúc trả và ảnh hỏng.", 14.8),

    ("P", "**Xuất PDF Hợp đồng và Phụ lục gia hạn (Hình 5.7):** Trang 1 bản PDF *PHỤ LỤC HỢP ĐỒNG* (phiên bản 2 của phòng `P101`) xuất trực tiếp từ `ContractPdf.java` bằng font tiếng Việt NotoSans, thể hiện đầy đủ thông tin Bên A, Bên B, điều khoản giá mới (`3.600.000đ`), ngày thanh toán mới (`Ngày 8 hàng tháng`) và danh mục thiết bị bàn giao."),
    ("FIG", "FIG_5_7", "ui-contract-pdf-v2.png", "Hình 5.7: Trang 1 file PDF Phụ lục hợp đồng thuê phòng xuất trực tiếp từ chương trình (bản 2 – P101).", 11.4),

    ("H2", "S5_2", "5.2. Kết quả đạt được, đánh giá và kinh nghiệm rút ra"),
    ("P", "Phiên bản V14 đã hoàn thiện trọn vẹn chuỗi nghiệp vụ quản lý nhà trọ từ quản trị khu phòng, đồ đạc, hợp đồng/phụ lục PDF, đối chiếu biên bản nhận–trả, cảnh báo điện nước bất thường cho đến hóa đơn khóa sổ, sổ quỹ tách bạch và báo cáo doanh thu – công nợ. Quá trình thực hiện giúp các thành viên rèn luyện thực tế về phân lớp ứng dụng, viết REST API có kiểm soát quyền chặt chẽ, xử lý giao dịch đồng thời, chuẩn hóa cơ sở dữ liệu có migration tuần tự và thói quen viết kiểm thử tự động để bắt lỗi sớm. Bảng 5.1 tổng hợp các ưu điểm nổi bật cùng những giới hạn còn tồn tại."),
    ("TABLE", "Bảng 5.1: Đánh giá ưu điểm và hạn chế của hệ thống hiện tại",
     ["Khía cạnh", "Ưu điểm đạt được", "Hạn chế cần cải thiện"],
     [
         ["Cơ sở dữ liệu", "49 bảng chuẩn 3NF, đủ khóa ngoại, 7 GiST EXCLUDE, 10 View tính tự động và lịch sử snapshot rõ ràng.", "Số lượng bảng lớn nên đòi hỏi duy trì từ điển dữ liệu và tài liệu DBML đồng bộ mỗi khi nâng cấp."],
         ["Nghiệp vụ tài chính", "Tách bạch hóa đơn phải thu, tiền thực nhận, quỹ cọc và tiền dư `ADVANCE`; có đối chiếu bàn giao đồ đạc.", "Việc ghi nhận thanh toán hiện vẫn do quản lý xác nhận theo sao kê, chưa gạch nợ ngân hàng tự động."],
         ["Bảo mật & phân quyền", "Kiểm tra vai trò, phạm vi khu/phòng và hạn mức hoàn tiền ngay tại backend (`Access49`).", "Phạm vi hiện phục vụ một chủ nhà có nhiều khu; phiên đăng nhập và bộ đếm sai đang lưu trên RAM một instance."],
         ["Triển khai vận hành", "Đóng gói gọn 1 JAR + cấu hình `compose.yml` và bộ kiểm thử 131 test Java + 20 ca SQL.", "Chưa triển khai lên máy chủ public có HTTPS, chưa có kiểm thử tải lớn và diễn tập sao lưu định kỳ."],
     ],
     [3.0, 6.8, 6.2]),

    ("H2", "S5_3", "5.3. Hướng phát triển"),
    ("B", "**Tự động hóa đối soát thanh toán:** Sinh mã VietQR động kèm mã hóa đơn và tiếp nhận webhook ngân hàng để tự động lập phiếu thu (tận dụng trực tiếp cột chống trùng `external_reference` đã thiết kế sẵn trong bảng `cash_transactions`)."),
    ("B", "**Mở rộng kênh thông báo và nhật ký tra cứu:** Tích hợp gửi thông báo phát hành hóa đơn, nhắc hạn thanh toán và cảnh báo điện nước qua Email hoặc Zalo OA; xây dựng thêm màn hình tra cứu trực quan lịch sử thao tác từ `audit_events`."),
    ("B", "**Nâng cao độ sẵn sàng vận hành:** Chuyển kho lưu phiên đăng nhập và giới hạn tốc độ từ RAM sang Redis để mở rộng nhiều instance, bổ sung HTTPS, giám sát lỗi tập trung, lịch sao lưu PostgreSQL tự động và rà soát thống nhất các đường API cũ với chính sách bàn giao V14."),
    ("B", "**Mở rộng mô hình nhiều chủ nhà:** Bổ sung tầng định danh đơn vị vận hành (`landlord_id`) để nhiều chủ nhà độc lập có thể dùng chung một nền tảng SaaS mà vẫn cô lập hoàn toàn dữ liệu."),

    ("H2", "S5_4", "5.4. Kết luận"),
    ("P", "Bài tập lớn đã xây dựng thành công hệ thống quản lý nhà trọ phiên bản V14 hoàn chỉnh từ phân tích thiết kế, cơ sở dữ liệu 49 bảng trên PostgreSQL, backend Java Spring Boot, giao diện React cho đến xuất văn bản PDF và bộ kiểm thử tự động (131 test Java, 20 ca SQL). Quan trọng hơn, hệ thống giải quyết trực diện các điểm dễ sai sót nhất trong thực tế vận hành trọ như chia ngày lẻ, cảnh báo điện nước đột biến, tách bạch tiền cọc – tiền dư – hóa đơn và đối chiếu đồ đạc khi bàn giao phòng. Đây là nền tảng vững chắc để nhóm tiếp tục mở rộng thành sản phẩm triển khai thực tế."),

    # ---------- TÀI LIỆU THAM KHẢO ----------
    ("CH", "REF", "TÀI LIỆU THAM KHẢO"),
    ("B", "[1] Kho mã nguồn đề tài nhóm gehihi: https://github.com/hieunofun/quanlytro (nhánh `main`, phiên bản tham chiếu V14 tại commit `d05d1cb`)."),
    ("B", "[2] Tài liệu kỹ thuật trong kho mã nguồn: `README.md`, `docs/API49.md`, `docs/DATABASE.md`, `docs/NORMALIZATION.md`, `docs/TU-DIEN-DU-LIEU-49-BANG.md`, `docs/quanlytro.dbml`, `docs/quanlytro.sql` và chuỗi migration Flyway `V1–V14`."),
    ("B", "[3] Hồ sơ kết quả kiểm chứng V14: `docs/implementation-validation49.json`, `docs/database-validation.json`, `scripts/verify-model49.py` và `scripts/seed-defense.mjs`."),
    ("B", "[4] Spring Team, *Spring Boot 4.1 & Spring Security Reference Documentation*, https://docs.spring.io/spring-boot/reference/."),
    ("B", "[5] PostgreSQL Global Development Group, *PostgreSQL 18 Documentation — Constraints, btree_gist, Triggers & Explicit Locking*, https://www.postgresql.org/docs/18/ddl-constraints.html."),
    ("B", "[6] Redgate, *Flyway Database Migrations Documentation*, https://documentation.red-gate.com/fd."),
    ("B", "[7] Apache Software Foundation, *Apache PDFBox 3.0 Documentation*, https://pdfbox.apache.org/."),
    ("B", "[8] PlantUML Documentation, https://plantuml.com/ (biểu diễn biểu đồ hoạt động UML)."),
]


# ==============================================================================
# HELPER FUNCTIONS
# ==============================================================================
def parse_runs_for_gdocs(text: str, font_size=13, base_bold=False, base_italic=False, base_color="#111827"):
    """Convert inline **bold**, *italic*, `code` into explicit non-leaking gdocs rich-text runs."""
    runs = []

    def emit(s, b, it, col, fam="Times New Roman", sz=font_size):
        if not s:
            return
        runs.append({
            "text": s,
            "font_family": fam,
            "font_size": sz,
            "bold": bool(b),
            "italic": bool(it),
            "underline": False,
            "text_color": col,
        })

    def walk(segment, b, it):
        for tok in re.split(r"(`[^`]+`|\*\*[^*]+\*\*|\*[^*\n]+\*)", segment):
            if not tok:
                continue
            if tok.startswith("`") and tok.endswith("`"):
                emit(tok[1:-1], b, it, "#1E3A5F", fam="Consolas", sz=max(font_size - 1.5, 9.5))
            elif tok.startswith("**") and tok.endswith("**"):
                walk(tok[2:-2], True, it)
            elif tok.startswith("*") and tok.endswith("*"):
                walk(tok[1:-1], b, True)
            else:
                emit(tok, b, it, base_color)

    walk(text, base_bold, base_italic)
    return runs


def strip_md_for_cell(text: str) -> str:
    s = re.sub(r"\*\*([^*]+)\*\*", r"\1", str(text))
    s = re.sub(r"\*([^*]+)\*", r"\1", s)
    s = re.sub(r"`([^`]+)`", r"\1", s)
    return s


# ==============================================================================
# 1. BUILD GOOGLE DOCS BATCH PAYLOAD + IMAGE PLAN + TOC TOKEN MAP
# ==============================================================================
def build_gdoc_payloads():
    # Collect TOC items (CH & H2) and Figures (FIG)
    toc_rows = []
    fig_rows = []
    toc_lookup = {}  # token -> exact heading/caption string to locate in exported PDF

    for blk in BLOCKS:
        if blk[0] == "CH":
            _, key, title = blk
            tok = f"{{{{PG_{key}}}}}"
            toc_rows.append([title, tok])
            toc_lookup[tok] = title
        elif blk[0] == "H2":
            _, key, title = blk
            tok = f"{{{{PG_{key}}}}}"
            toc_rows.append([f"    {title}", tok])
            toc_lookup[tok] = title
        elif blk[0] == "FIG":
            _, key, _fname, cap, _w = blk
            tok = f"{{{{PG_{key}}}}}"
            fig_rows.append([cap.rstrip("."), tok])
            toc_lookup[tok] = cap

    ops = [
        # Page setup: A4 margins (Left 85pt=3.0cm, Right 57pt=2.0cm, Top 58pt, Bottom 58pt)
        # + useFirstPageHeaderFooter=True so Cover Page has NO page number!
        {
            "op": "raw",
            "raw_request": {
                "updateDocumentStyle": {
                    "documentStyle": {
                        "marginLeft": {"magnitude": 85, "unit": "PT"},
                        "marginRight": {"magnitude": 57, "unit": "PT"},
                        "marginTop": {"magnitude": 58, "unit": "PT"},
                        "marginBottom": {"magnitude": 58, "unit": "PT"},
                        "useFirstPageHeaderFooter": True,
                    },
                    "fields": "marginLeft,marginRight,marginTop,marginBottom,useFirstPageHeaderFooter",
                }
            },
        },
        {
            "op": "set-default-style",
            "font_family": "Times New Roman",
            "font_size": 13,
            "text_color": "#111827",
            "line_spacing": 120,
        },
        # ==================== TRANG BÌA ====================
        {"op": "append", "text": "BỘ GIÁO DỤC VÀ ĐÀO TẠO", "alignment": "CENTER", "bold": True, "font_size": 13.5, "space_above": 10, "space_below": 2},
        {"op": "append", "text": "TRƯỜNG ĐẠI HỌC CMC", "alignment": "CENTER", "bold": True, "font_size": 15.5, "text_color": "#1B365D", "space_below": 2},
        {"op": "append", "text": "KHOA CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG", "alignment": "CENTER", "bold": True, "font_size": 12.5, "text_color": "#223A5E", "space_below": 4},
        {"op": "append", "text": "─────────────── ❖ ───────────────", "alignment": "CENTER", "font_size": 11, "text_color": "#1B365D", "space_below": 30},

        {"op": "append", "text": "BÁO CÁO BÀI TẬP LỚN", "alignment": "CENTER", "bold": True, "font_size": 17, "text_color": "#1B365D", "space_below": 4},
        {"op": "append", "text": "HỌC PHẦN: KỸ NĂNG LẬP TRÌNH NÂNG CAO — LỚP N03", "alignment": "CENTER", "bold": True, "font_size": 13.5, "space_below": 28},

        {"op": "append", "text": "ĐỀ TÀI", "alignment": "CENTER", "bold": True, "font_size": 13, "text_color": "#4B5563", "space_below": 6},
        {"op": "append", "text": "XÂY DỰNG HỆ THỐNG QUẢN LÝ NHÀ TRỌ", "alignment": "CENTER", "bold": True, "font_size": 19, "text_color": "#1B365D", "space_below": 4},
        {"op": "append", "text": "QUẢN LÝ HỢP ĐỒNG, HÓA ĐƠN, ĐIỆN NƯỚC VÀ BÀN GIAO ĐỒ ĐẠC", "alignment": "CENTER", "bold": True, "font_size": 13, "text_color": "#223A5E", "space_below": 30},

        {
            "op": "insert-table",
            "table_data": [
                ["Nhóm thực hiện:", "Nhóm gehihi — Lớp N03"],
                ["Sinh viên thực hiện:", f"{MEMBERS[0][1]} — MSSV: {MEMBERS[0][2]}"],
                ["", f"{MEMBERS[1][1]} — MSSV: {MEMBERS[1][2]}"],
                ["", f"{MEMBERS[2][1]} — MSSV: {MEMBERS[2][2]}"],
                ["", f"{MEMBERS[3][1]} — MSSV: {MEMBERS[3][2]}"],
                ["", f"{MEMBERS[4][1]} — MSSV: {MEMBERS[4][2]}"],
                ["", f"{MEMBERS[5][1]} — MSSV: {MEMBERS[5][2]}"],
                ["Giảng viên hướng dẫn:", "................................................................"],
            ],
            "table_header_bold": True,
            "table_font_family": "Times New Roman",
            "table_font_size": 12.5,
            "table_col_widths": [155, 297],
            "table_border_color": "#FFFFFF",
            "table_border_width": 0.5,
        },
        {"op": "append", "text": "Hà Nội, tháng 11 năm 2026", "alignment": "CENTER", "bold": True, "font_size": 13, "space_above": 44, "space_below": 4},
        {"op": "page-break"},
    ]

    image_inserts = []
    fig_idx = 0
    started_main = False

    for blk in BLOCKS:
        kind = blk[0]
        if kind == "FRONT_H1":
            ops.append({
                "op": "heading", "level": "2", "text": blk[1],
                "font_family": "Times New Roman", "font_size": 15, "bold": True,
                "text_color": "#1B365D", "alignment": "CENTER", "space_above": 4, "space_below": 8,
            })
        elif kind == "CH":
            if not started_main:
                # Insert MỤC LỤC + DANH MỤC HÌNH before Chapter 1
                ops.append({"op": "page-break"})
                ops.append({
                    "op": "heading", "level": "2", "text": "MỤC LỤC",
                    "font_family": "Times New Roman", "font_size": 15, "bold": True,
                    "text_color": "#1B365D", "alignment": "CENTER", "space_above": 2, "space_below": 8,
                })
                ops.append({
                    "op": "insert-table",
                    "table_data": [["Nội dung", "Trang"]] + toc_rows,
                    "table_header_bg_color": "#E8EEF5",
                    "table_header_text_color": "#1B365D",
                    "table_header_bold": True,
                    "table_font_family": "Times New Roman",
                    "table_font_size": 11.5,
                    "table_col_widths": [404, 48],
                    "table_border_color": "#E2E8F0",
                    "table_border_width": 0.5,
                })
                ops.append({"op": "page-break"})
                ops.append({
                    "op": "heading", "level": "2", "text": "DANH MỤC HÌNH ẢNH VÀ SƠ ĐỒ",
                    "font_family": "Times New Roman", "font_size": 15, "bold": True,
                    "text_color": "#1B365D", "alignment": "CENTER", "space_above": 2, "space_below": 8,
                })
                ops.append({
                    "op": "insert-table",
                    "table_data": [["Tên hình ảnh / sơ đồ", "Trang"]] + fig_rows,
                    "table_header_bg_color": "#E8EEF5",
                    "table_header_text_color": "#1B365D",
                    "table_header_bold": True,
                    "table_font_family": "Times New Roman",
                    "table_font_size": 10.5,
                    "table_col_widths": [404, 48],
                    "table_border_color": "#E2E8F0",
                    "table_border_width": 0.5,
                })
                started_main = True

            ops.append({"op": "page-break"})
            ops.append({
                "op": "heading", "level": "1", "text": blk[2],
                "font_family": "Times New Roman", "font_size": 15, "bold": True,
                "text_color": "#1B365D", "space_above": 2, "space_below": 8,
            })
        elif kind == "H2":
            ops.append({
                "op": "heading", "level": "2", "text": blk[2],
                "font_family": "Times New Roman", "font_size": 13.5, "bold": True,
                "text_color": "#1B365D", "space_above": 11, "space_below": 4,
            })
        elif kind == "H3":
            ops.append({
                "op": "heading", "level": "3", "text": blk[1],
                "font_family": "Times New Roman", "font_size": 13, "bold": True,
                "text_color": "#223A5E", "space_above": 8, "space_below": 3,
            })
        elif kind == "P":
            ops.append({
                "op": "rich-text",
                "alignment": "JUSTIFIED",
                "indent_first_line": 22,
                "space_above": 0,
                "space_below": 4.5,
                "runs": parse_runs_for_gdocs(blk[1], font_size=13),
            })
        elif kind == "B":
            ops.append({
                "op": "rich-text",
                "alignment": "JUSTIFIED",
                "bullet_preset": "BULLET_DISC_CIRCLE_SQUARE",
                "space_above": 1,
                "space_below": 3.5,
                "runs": parse_runs_for_gdocs(blk[1], font_size=13),
            })
        elif kind == "TABLE":
            _, cap, headers, rows, widths_cm = blk
            ops.append({
                "op": "append", "text": cap, "alignment": "CENTER",
                "font_family": "Times New Roman", "font_size": 11.5, "bold": True, "italic": True,
                "text_color": "#1B365D", "space_above": 7, "space_below": 2.5,
            })
            total_cm = sum(widths_cm)
            col_pts = [round(w / total_cm * 452, 1) for w in widths_cm]
            clean_data = [[strip_md_for_cell(c) for c in headers]] + [
                [strip_md_for_cell(c) for c in r] for r in rows
            ]
            ops.append({
                "op": "insert-table",
                "table_data": clean_data,
                "table_header_bg_color": "#E8EEF5",
                "table_header_text_color": "#1B365D",
                "table_header_bold": True,
                "table_font_family": "Times New Roman",
                "table_font_size": 10.5,
                "table_col_widths": col_pts,
                "table_border_color": "#94A3B8",
                "table_border_width": 0.6,
            })
        elif kind == "FIG":
            _, _key, fname, cap, _w_cm = blk
            fig_idx += 1
            anchor = f"__IMG_ANCHOR_{fig_idx:02d}__"
            ops.append({
                "op": "append", "text": anchor, "alignment": "CENTER",
                "font_family": "Times New Roman", "font_size": 10,
                "space_above": 5, "space_below": 1,
            })
            ops.append({
                "op": "append", "text": cap, "alignment": "CENTER",
                "font_family": "Times New Roman", "font_size": 11.5, "italic": True, "bold": True,
                "text_color": "#1B365D", "space_above": 2, "space_below": 7,
            })
            image_inserts.append({
                "anchor": anchor,
                "path": str(ASSETS / fname),
                "alt": cap,
            })

    RT.mkdir(parents=True, exist_ok=True)
    GDOC_BATCH.write_text(json.dumps(ops, ensure_ascii=False, indent=2), encoding="utf-8")
    GDOC_IMAGES.write_text(json.dumps(image_inserts, ensure_ascii=False, indent=2), encoding="utf-8")
    GDOC_TOC_MAP.write_text(json.dumps(toc_lookup, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {GDOC_BATCH} ({len(ops)} ops), {GDOC_IMAGES} ({len(image_inserts)} images), {GDOC_TOC_MAP} ({len(toc_lookup)} TOC tokens)")


# ==============================================================================
# 2. BUILD SYNCHRONIZED WORD DOCUMENT (docs/bao-cao-do-an.docx)
# ==============================================================================
def build_docx():
    doc = docx.Document()
    configure_base_styles(doc)

    # Lossless embedded PNGs in Word
    settings = doc.settings.element
    if settings.find(qn("w:doNotCompressPictures")) is None:
        settings.append(OxmlElement("w:doNotCompressPictures"))

    # Dedicated FigureCaption style so Word's TOC \t "FigureCaption,1" populates List of Figures automatically!
    from docx.enum.style import WD_STYLE_TYPE
    if "FigureCaption" not in [s.name for s in doc.styles]:
        fig_style = doc.styles.add_style("FigureCaption", WD_STYLE_TYPE.PARAGRAPH)
        fig_style.base_style = doc.styles["Normal"]

    # Cover Page (No page number)
    sec = doc.sections[0]
    setup_a4_margins(sec, left=2.5, right=2.0, top=2.1, bottom=2.1)
    set_page_border(sec)
    sec.header.is_linked_to_previous = False
    sec.footer.is_linked_to_previous = False

    def centered(text, size, bold=False, italic=False, color=INK, sb=0, sa=4):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.space_before = Pt(sb)
        p.paragraph_format.space_after = Pt(sa)
        p.paragraph_format.line_spacing = 1.2
        r = p.add_run(text)
        set_run_font(r, size_pt=size, bold=bold, italic=italic, color=color)

    centered("BỘ GIÁO DỤC VÀ ĐÀO TẠO", 13.5, bold=True, color=INK, sb=8, sa=2)
    centered("TRƯỜNG ĐẠI HỌC CMC", 15.5, bold=True, color=NAVY, sb=0, sa=3)
    centered("KHOA CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG", 12.5, bold=True, color=SUBNAVY, sb=0, sa=5)
    centered("─────────────── ❖ ───────────────", 11, color=NAVY, sb=2, sa=32)

    centered("BÁO CÁO BÀI TẬP LỚN", 17, bold=True, color=NAVY, sb=4, sa=4)
    centered("HỌC PHẦN: KỸ NĂNG LẬP TRÌNH NÂNG CAO — LỚP N03", 13.5, bold=True, color=INK, sb=0, sa=28)

    centered("ĐỀ TÀI", 13, bold=True, color=MUTED, sb=4, sa=6)
    centered("XÂY DỰNG HỆ THỐNG QUẢN LÝ NHÀ TRỌ", 19, bold=True, color=NAVY, sb=0, sa=5)
    centered("QUẢN LÝ HỢP ĐỒNG, HÓA ĐƠN, ĐIỆN NƯỚC VÀ BÀN GIAO ĐỒ ĐẠC", 13, bold=True, color=SUBNAVY, sb=0, sa=32)

    tbl = doc.add_table(rows=8, cols=2)
    tbl.alignment = 1
    tbl.autofit = False
    tbl._tbl.tblPr.append(parse_xml(
        f'<w:tblBorders {nsdecls("w")}><w:top w:val="none"/><w:left w:val="none"/>'
        f'<w:bottom w:val="none"/><w:right w:val="none"/><w:insideH w:val="none"/><w:insideV w:val="none"/></w:tblBorders>'
    ))
    meta_rows = [
        ("Nhóm thực hiện:", "Nhóm gehihi — Lớp N03"),
        ("Sinh viên thực hiện:", f"{MEMBERS[0][1]} — MSSV: {MEMBERS[0][2]}"),
        ("", f"{MEMBERS[1][1]} — MSSV: {MEMBERS[1][2]}"),
        ("", f"{MEMBERS[2][1]} — MSSV: {MEMBERS[2][2]}"),
        ("", f"{MEMBERS[3][1]} — MSSV: {MEMBERS[3][2]}"),
        ("", f"{MEMBERS[4][1]} — MSSV: {MEMBERS[4][2]}"),
        ("", f"{MEMBERS[5][1]} — MSSV: {MEMBERS[5][2]}"),
        ("Giảng viên hướng dẫn:", "................................................................"),
    ]
    for i, (k, v) in enumerate(meta_rows):
        c0, c1 = tbl.cell(i, 0), tbl.cell(i, 1)
        c0.width = Cm(5.4)
        c1.width = Cm(9.8)
        p0, p1 = c0.paragraphs[0], c1.paragraphs[0]
        p0.paragraph_format.first_line_indent = Cm(0)
        p1.paragraph_format.first_line_indent = Cm(0)
        p0.paragraph_format.space_after = Pt(4)
        p1.paragraph_format.space_after = Pt(4)
        set_run_font(p0.add_run(k), size_pt=12.5, bold=True, color=NAVY)
        set_run_font(p1.add_run(v), size_pt=12.5, bold=(i == 0), color=INK)

    centered("Hà Nội, tháng 11 năm 2026", 13, bold=True, color=INK, sb=52, sa=0)

    # Front section: Tóm tắt, Từ viết tắt, Mục lục, Danh mục hình
    sec_front = doc.add_section(WD_SECTION.NEW_PAGE)
    setup_a4_margins(sec_front, left=3.0, right=2.0, top=2.1, bottom=2.1)
    clear_page_border(sec_front)
    set_page_number_format(sec_front, fmt="lowerRoman", start=1)
    configure_running_header_footer(sec_front, header_text="", footer_left="", show_page_num=True)

    started_main = False
    for blk in BLOCKS:
        kind = blk[0]
        if kind == "FRONT_H1":
            add_front_title(doc, blk[1], page_break=False)
        elif kind == "CH":
            if not started_main:
                add_front_title(doc, "MỤC LỤC", page_break=True)
                add_toc_field(doc, 'TOC \\o "1-3" \\h \\z \\u', "[Mục lục tự động]")
                add_front_title(doc, "DANH MỤC HÌNH ẢNH VÀ SƠ ĐỒ", page_break=True)
                add_toc_field(doc, 'TOC \\h \\z \\t "FigureCaption,1"', "[Danh mục hình tự động]")
                sec_main = doc.add_section(WD_SECTION.NEW_PAGE)
                setup_a4_margins(sec_main, left=3.0, right=2.0, top=2.1, bottom=2.1)
                clear_page_border(sec_main)
                set_page_number_format(sec_main, fmt="decimal", start=1)
                configure_running_header_footer(
                    sec_main,
                    header_text="Báo cáo Bài tập lớn Kỹ năng lập trình nâng cao — Nhóm gehihi (Lớp N03)",
                    footer_left="Hệ thống Quản lý Nhà trọ — Trường Đại học CMC",
                    show_page_num=True,
                )
                add_chapter(doc, blk[2], page_break=False)
                started_main = True
            else:
                add_chapter(doc, blk[2], page_break=True)
        elif kind == "H2":
            add_h2(doc, blk[2])
        elif kind == "H3":
            add_h3(doc, blk[1])
        elif kind == "P":
            add_para(doc, blk[1], indent=True, space_after=4.5)
        elif kind == "B":
            add_bullet(doc, blk[1])
        elif kind == "TABLE":
            _, cap, headers, rows, widths = blk
            p_cap = doc.add_paragraph()
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.first_line_indent = Cm(0)
            p_cap.paragraph_format.space_before = Pt(6)
            p_cap.paragraph_format.space_after = Pt(2.5)
            p_cap.paragraph_format.keep_with_next = True
            m = re.match(r"^(Bảng [\d\.]+:)\s*(.*)$", cap)
            if m:
                set_run_font(p_cap.add_run(m.group(1) + " "), size_pt=11.5, bold=True, italic=True, color=NAVY)
                set_run_font(p_cap.add_run(m.group(2)), size_pt=11.5, italic=True, color=INK)
            else:
                set_run_font(p_cap.add_run(cap), size_pt=11.5, bold=True, italic=True, color=NAVY)
            add_styled_table(doc, headers, rows, widths, caption=None, font_size=10.2)
        elif kind == "FIG":
            _, _key, fname, cap, w_cm = blk
            img_path = ASSETS / fname
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.first_line_indent = Cm(0)
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after = Pt(2)
            p_img.paragraph_format.keep_with_next = True
            p_img.add_run().add_picture(str(img_path), width=Cm(w_cm))

            p_cap = doc.add_paragraph(style="FigureCaption")
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.first_line_indent = Cm(0)
            p_cap.paragraph_format.space_before = Pt(0)
            p_cap.paragraph_format.space_after = Pt(6)
            m = re.match(r"^(Hình [\d\.]+:)\s*(.*)$", cap)
            if m:
                set_run_font(p_cap.add_run(m.group(1) + " "), size_pt=11.5, bold=True, italic=True, color=NAVY)
                set_run_font(p_cap.add_run(m.group(2)), size_pt=11.5, italic=True, color=INK)
            else:
                set_run_font(p_cap.add_run(cap), size_pt=11.5, italic=True, color=INK)

    OUT_DOCX.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUT_DOCX))
    print(f"Saved {OUT_DOCX} ({OUT_DOCX.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    build_gdoc_payloads()
    build_docx()
