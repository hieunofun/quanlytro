"""Build the defense-ready final academic report docs/bao-cao-do-an.docx (Model49 / V14)."""
from pathlib import Path
import sys
import docx
from docx.shared import Cm, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.section import WD_SECTION

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "report-assets"
OUT_DOCX = ROOT / "docs" / "bao-cao-do-an.docx"

sys.path.insert(0, str(Path(__file__).resolve().parent))
from docx_layout import (
    NAVY, INK, MUTED, SUBNAVY,
    configure_base_styles, setup_a4_margins, set_page_border, clear_page_border,
    set_page_number_format, configure_running_header_footer, set_run_font,
    add_front_title, add_chapter, add_h2, add_h3, add_para, add_bullet,
    add_code_block, add_callout_box, add_styled_table, add_figure,
    add_side_by_side_figures, add_toc_field,
)


def build_cover(doc: docx.Document):
    sec = doc.sections[0]
    setup_a4_margins(sec, left=2.5, right=2.0, top=2.2, bottom=2.2)
    set_page_border(sec)
    sec.header.is_linked_to_previous = False
    sec.footer.is_linked_to_previous = False

    def centered(text, size, bold=False, italic=False, color=INK, sb=0, sa=4):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.space_before = Pt(sb)
        p.paragraph_format.space_after = Pt(sa)
        p.paragraph_format.line_spacing = 1.25
        r = p.add_run(text)
        set_run_font(r, size_pt=size, bold=bold, italic=italic, color=color)
        return p

    centered("BỘ GIÁO DỤC VÀ ĐÀO TẠO", 13.5, bold=True, color=INK, sb=6, sa=2)
    centered("TRƯỜNG ĐẠI HỌC CMC", 15, bold=True, color=NAVY, sb=0, sa=4)
    centered("KHOA CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG", 12.5, bold=True, color=SUBNAVY, sb=0, sa=6)
    centered("─────────────── ❖ ───────────────", 11, color=NAVY, sb=2, sa=34)

    centered("BÁO CÁO ĐỒ ÁN HỌC PHẦN", 15, bold=True, color=NAVY, sb=4, sa=4)
    centered("KỸ NĂNG LẬP TRÌNH NÂNG CAO — LỚP N03", 13.5, bold=True, color=INK, sb=0, sa=28)

    centered("ĐỀ TÀI:", 13, bold=True, color=MUTED, sb=4, sa=8)
    centered("XÂY DỰNG HỆ THỐNG QUẢN LÝ NHÀ TRỌ", 20, bold=True, color=NAVY, sb=0, sa=6)
    centered(
        "Hóa đơn minh bạch, sổ quỹ tài chính tách bạch,\ncảnh báo tiêu thụ điện nước bất thường và đối chiếu bàn giao tài sản",
        13.5, italic=True, color=SUBNAVY, sb=0, sa=36,
    )

    # Clean info block using borderless 2-column table so text aligns cleanly when printed
    tbl = doc.add_table(rows=8, cols=2)
    tbl.alignment = 1  # center
    tbl.autofit = False
    from docx.oxml import parse_xml
    from docx.oxml.ns import nsdecls
    tbl._tbl.tblPr.append(parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/><w:left w:val="none"/><w:bottom w:val="none"/>'
        f'<w:right w:val="none"/><w:insideH w:val="none"/><w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    ))
    meta_rows = [
        ("Nhóm thực hiện:", "Nhóm gehihi"),
        ("Sinh viên thực hiện:", "Nguyễn Doãn Huy — MSSV: [Điền MSSV]"),
        ("", "Phan Văn Hiếu — MSSV: BIT240094"),
        ("", "Nguyễn Trần Tuấn Khôi — MSSV: [Điền MSSV]"),
        ("", "Niê Phương Anh — MSSV: BIT240014"),
        ("", "Trần Đức Nguyên — MSSV: BIT240173"),
        ("", "Nguyễn Quang Trung — MSSV: [Điền MSSV]"),
        ("Giảng viên hướng dẫn:", "[Điền họ tên Giảng viên hướng dẫn]"),
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
        r0 = p0.add_run(k)
        set_run_font(r0, size_pt=12.5, bold=True, color=NAVY)
        r1 = p1.add_run(v)
        set_run_font(r1, size_pt=12.5, bold=(i in (0, 7)), color=INK)

    centered("Hà Nội, tháng 11 năm 2026", 13, bold=True, color=INK, sb=58, sa=0)


def build_front_matter(doc: docx.Document):
    sec = doc.add_section(WD_SECTION.NEW_PAGE)
    setup_a4_margins(sec, left=3.0, right=2.0, top=2.5, bottom=2.5)
    clear_page_border(sec)
    set_page_number_format(sec, fmt="lowerRoman", start=1)
    configure_running_header_footer(sec, header_text="", footer_left="", show_page_num=True)

    # 1. Lời cam đoan
    add_front_title(doc, "Lời cam đoan", page_break=False)
    add_para(
        doc,
        "Nhóm sinh viên gehihi (lớp N03, học phần Kỹ năng lập trình nâng cao, Trường Đại học CMC) xin cam đoan toàn bộ mã nguồn, thiết kế cơ sở dữ liệu, kịch bản kiểm thử và nội dung trình bày trong báo cáo này là kết quả nghiên cứu, thiết kế và lập trình của tập thể nhóm trong quá trình học tập.",
    )
    add_para(
        doc,
        "Các số liệu kiểm thử, ảnh chụp giao diện, mẫu hợp đồng PDF và kết quả đối chiếu cơ sở dữ liệu trong báo cáo đều được xuất trực tiếp từ phiên bản chạy thực tế của hệ thống (phiên bản lược đồ V14, 49 bảng trên PostgreSQL 18.6). Những dự án mã nguồn mở mà nhóm tham khảo để đối chiếu bài toán nghiệp vụ (như OpenKOS, LeaseBook, TechFunWay/rental) đều được trích dẫn rõ nguồn và chỉ rõ phần khác biệt mà nhóm tự thiết kế lại.",
    )
    p_sig = doc.add_paragraph()
    p_sig.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    p_sig.paragraph_format.first_line_indent = Cm(0)
    p_sig.paragraph_format.space_before = Pt(14)
    p_sig.paragraph_format.space_after = Pt(2)
    r_d = p_sig.add_run("Hà Nội, tháng 11 năm 2026\n")
    set_run_font(r_d, size_pt=12.5, italic=True, color=INK)
    r_s = p_sig.add_run("Đại diện nhóm thực hiện (Nhóm gehihi)\n\n\n[Ký và ghi rõ họ tên]")
    set_run_font(r_s, size_pt=12.5, bold=True, color=NAVY)

    # 2. Lời cảm ơn
    add_front_title(doc, "Lời cảm ơn", page_break=True)
    add_para(
        doc,
        "Để hoàn thành đồ án học phần Kỹ năng lập trình nâng cao, nhóm gehihi xin gửi lời cảm ơn chân thành đến Giảng viên hướng dẫn [Điền họ tên Giảng viên hướng dẫn] đã trực tiếp giảng dạy, góp ý chi tiết trong từng buổi rà soát tiến độ và đặt ra những yêu cầu bổ sung rất sát với thực tế vận hành nhà trọ (từ báo cáo theo tháng/quý/năm, 6 chỉ số theo dõi thu tiền–điện nước, quản lý tài sản phòng cho đến đối chiếu bàn giao nhận–trả và xuất văn bản hợp đồng PDF).",
    )
    add_para(
        doc,
        "Chính những góp ý thẳng thắn ở các đợt kiểm tra giữa kỳ đã buộc nhóm phải nhìn lại các tình huống biên dễ gây sai tiền trong thực tế, từ đó chỉnh sửa lại cách thiết kế cơ sở dữ liệu và luồng nghiệp vụ backend thay vì chỉ dừng ở một ứng dụng thêm–sửa–xóa đơn giản. Nhóm cũng xin cảm ơn các thầy cô Khoa Công nghệ Thông tin và Truyền thông, Trường Đại học CMC đã tạo điều kiện về môi trường học tập để nhóm hoàn thiện sản phẩm.",
    )
    add_para(
        doc,
        "Mặc dù cả nhóm đã cố gắng kiểm chứng kỹ lưỡng bằng bộ kiểm thử tự động và chạy thử trên dữ liệu mô phỏng, hệ thống vẫn không tránh khỏi những hạn chế nhất định về phạm vi vận hành thực tế. Nhóm rất mong tiếp tục nhận được nhận xét của Hội đồng để hoàn thiện sản phẩm tốt hơn.",
    )

    # 3. Tóm tắt đề tài
    add_front_title(doc, "Tóm tắt đề tài", page_break=True)
    add_para(
        doc,
        "Đề tài tập trung xây dựng hệ thống quản lý nhà trọ phục vụ mô hình một chủ nhà vận hành nhiều khu và nhiều phòng, giải quyết bốn vướng mắc thường gặp khi quản lý bằng sổ sách hoặc bảng tính rời rạc: thiếu minh bạch căn cứ tính hóa đơn, không phát hiện sớm điện nước tăng đột biến trước khi thu tiền, lẫn lộn giữa tiền cọc cam kết với dòng tiền thực thu/tiền thừa, và thiếu hồ sơ đối chiếu tình trạng đồ đạc lúc nhận phòng với lúc trả phòng.",
    )
    add_para(
        doc,
        "Hệ thống được phát triển theo kiến trúc 4 tầng với Backend Java 21 / Spring Boot 4.1.1, Frontend React 19 / Vite 7 và cơ sở dữ liệu PostgreSQL 18.6 quản lý nâng cấp qua Flyway (V1–V14). Lược đồ dữ liệu hiện hành gồm 49 bảng (461 cột, 152 khóa ngoại thật, 133 ràng buộc CHECK, 7 ràng buộc GiST EXCLUDE chống chồng khoảng thời gian và 10 View dẫn xuất), tách bạch giữa chứng từ phải thu bất biến (`billing_documents`, `billing_items`), dòng tiền thực thu/thực chi (`cash_transactions`) và sổ cái quỹ tiền khách theo nguyên tắc chỉ ghi thêm (`fund_accounts`, `fund_entries`). Hệ thống hỗ trợ phân quyền 3 vai trò (`ADMIN`, `STAFF` theo phạm vi khu/phòng và hạn mức ủy quyền, `TENANT`), phát hiện điện nước bất thường bằng phương pháp trung bình ngày có trọng số (`WEIGHTED_DAILY`), quản lý tài sản trong phòng, đối chiếu bàn giao nhận–trả có ảnh minh chứng và xuất hợp đồng/phụ lục/báo cáo PDF tiếng Việt Unicode.",
    )
    add_para(
        doc,
        "Toàn bộ luồng nghiệp vụ trọng yếu đã được kiểm chứng với 131/131 ca kiểm thử tự động Java/HTTP/Migration/Concurrency đạt yêu cầu, 20/20 ca kiểm tra ràng buộc SQL trực tiếp trên PostgreSQL 18.6 và kiểm thử giao diện đầu-cuối trên trình duyệt Chrome qua 13 màn hình nghiệp vụ cùng khung nhìn điện thoại 390px.",
    )
    add_para(
        doc,
        "Quản lý nhà trọ, Spring Boot 4, PostgreSQL 18, Flyway, Sổ cái bất biến (Append-only Ledger), Kiểm soát đồng thời, Phát hiện bất thường điện nước, Đối chiếu bàn giao tài sản.",
        indent=False,
        bold_prefix="Từ khóa: ",
    )

    # 4. Bảng phân công công việc nhóm
    add_front_title(doc, "Bảng phân công công việc nhóm", page_break=True)
    add_para(
        doc,
        "Dự án được thực hiện bởi 6 thành viên nhóm gehihi (lớp N03). Các hạng mục dưới đây tóm tắt đầu việc chính của từng thành viên trong quá trình phân tích, thiết kế CSDL, lập trình backend/frontend, kiểm thử và viết tài liệu. Cột tỷ lệ đóng góp để nhóm thống nhất điền con số chính xác trước khi nộp bản in.",
        indent=False,
    )
    add_styled_table(
        doc,
        headers=["STT", "Họ và tên", "MSSV", "Phần việc chính phụ trách", "Tỷ lệ"],
        rows=[
            ["1", "Nguyễn Doãn Huy", "[Điền MSSV]", "Phân tích yêu cầu nghiệp vụ, thiết kế luồng hợp đồng – cư trú – bàn giao, rà soát kịch bản kiểm thử.", "[...%]"],
            ["2", "Phan Văn Hiếu", "BIT240094", "Kiến trúc Backend Spring Boot (model49), cơ sở dữ liệu PostgreSQL V1–V14, sổ quỹ, báo cáo và tích hợp kiểm thử.", "[...%]"],
            ["3", "Nguyễn Trần Tuấn Khôi", "[Điền MSSV]", "Xây dựng luồng điện nước, cảnh báo bất thường, hóa đơn minh bạch và kiểm thử quy tắc tính phí.", "[...%]"],
            ["4", "Niê Phương Anh", "BIT240014", "Thiết kế giao diện người dùng React, màn hình tổng quan, công nợ, tài sản phòng và kiểm thử giao diện.", "[...%]"],
            ["5", "Trần Đức Nguyên", "BIT240173", "Xác thực JWT, phân quyền ADMIN/STAFF/TENANT, xử lý sự cố – bảo trì, tải tệp đính kèm và xuất PDF.", "[...%]"],
            ["6", "Nguyễn Quang Trung", "[Điền MSSV]", "Chuẩn bị bộ dữ liệu mô phỏng (seed), kiểm thử tích hợp/SQL, sơ đồ hệ thống và biên tập báo cáo.", "[...%]"],
        ],
        col_widths_cm=[1.1, 3.8, 2.3, 7.3, 1.5],
        align_cols=[
            WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER,
            WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER,
        ],
        font_size=10.5,
    )
    add_callout_box(
        doc,
        "Lưu ý trước khi in nộp",
        "Các thành viên kiểm tra lại chính xác 3 MSSV còn trống, điền họ tên Giảng viên hướng dẫn ở trang bìa và chốt tỷ lệ đóng góp thực tế (tổng 100%) trong bảng trên, sau đó nhấn Ctrl+A và F9 trong Word nếu có thay đổi số trang.",
    )

    # 5. Mục lục (Auto Word TOC)
    add_front_title(doc, "Mục lục", page_break=True)
    add_toc_field(doc, 'TOC \\o "1-3" \\h \\z \\u', "[Mục lục tự động sẽ được Microsoft Word cập nhật kèm số trang chính xác]")

    # 6. Danh mục ký hiệu và chữ viết tắt
    add_front_title(doc, "Danh mục ký hiệu và chữ viết tắt", page_break=True)
    add_styled_table(
        doc,
        headers=["Ký hiệu / Viết tắt", "Tiếng Anh đầy đủ", "Giải nghĩa trong báo cáo"],
        rows=[
            ["ACID", "Atomicity, Consistency, Isolation, Durability", "Bốn tính chất đảm bảo giao dịch cơ sở dữ liệu an toàn, nhất quán."],
            ["AOP", "Aspect-Oriented Programming", "Lập trình hướng khía cạnh (dùng trong `ApiTimingAspect` đo thời gian API)."],
            ["API / REST", "Application Programming Interface", "Giao diện lập trình ứng dụng theo kiến trúc REST qua HTTP/JSON."],
            ["CHECK / EXCLUDE", "SQL Integrity Constraints", "Ràng buộc kiểm tra miền giá trị và loại trừ chồng lấn (GiST) tại PostgreSQL."],
            ["CRUD", "Create, Read, Update, Delete", "Các thao tác thêm, đọc, sửa, xóa dữ liệu cơ bản."],
            ["DDL", "Data Definition Language", "Ngôn ngữ định nghĩa cấu trúc bảng, khóa, chỉ mục và ràng buộc SQL."],
            ["ERD", "Entity-Relationship Diagram", "Sơ đồ thực thể – mối kết hợp biểu diễn cấu trúc cơ sở dữ liệu."],
            ["FK / PK / UQ", "Foreign / Primary / Unique Key", "Khóa ngoại, khóa chính và ràng buộc duy nhất trên bảng dữ liệu."],
            ["GiST", "Generalized Search Tree", "Cấu trúc chỉ mục của PostgreSQL hỗ trợ ràng buộc chống chồng khoảng thời gian."],
            ["JWT", "JSON Web Token", "Chuỗi mã thông báo chuẩn hóa dùng xác thực người dùng không trạng thái."],
            ["MVC", "Model – View – Controller", "Mô hình tách biệt tầng dữ liệu, giao diện và điều phối yêu cầu."],
            ["ORM / JPA", "Object-Relational Mapping / Jakarta Persistence", "Ánh xạ lớp Java sang bảng quan hệ (dùng ở chế độ `validate` trong đồ án)."],
            ["RBAC", "Role-Based Access Control", "Kiểm soát truy cập theo vai trò (`ADMIN`, `STAFF`, `TENANT`)."],
            ["SPA", "Single Page Application", "Ứng dụng web một trang xây dựng bằng React 19 + Vite 7."],
            ["UML", "Unified Modeling Language", "Ngôn ngữ mô hình hóa thống nhất (biểu đồ Use Case, Sequence)."],
        ],
        col_widths_cm=[3.2, 5.4, 7.4],
        font_size=10.2,
    )

    # 7. Danh mục các bảng
    add_front_title(doc, "Danh mục các bảng", page_break=True)
    add_toc_field(doc, 'TOC \\h \\z \\c "Bảng"', "[Danh mục các bảng sẽ được cập nhật tự động]")

    # 8. Danh mục các hình vẽ và sơ đồ
    add_front_title(doc, "Danh mục các hình vẽ và sơ đồ", page_break=True)
    add_toc_field(doc, 'TOC \\h \\z \\c "Hình"', "[Danh mục các hình vẽ và sơ đồ sẽ được cập nhật tự động]")


def build_chapter_1(doc: docx.Document):
    sec = doc.add_section(WD_SECTION.NEW_PAGE)
    setup_a4_margins(sec, left=3.0, right=2.0, top=2.5, bottom=2.5)
    clear_page_border(sec)
    set_page_number_format(sec, fmt="decimal", start=1)
    configure_running_header_footer(
        sec,
        header_text="Báo cáo Đồ án Kỹ năng lập trình nâng cao — Hệ thống Quản lý Nhà trọ",
        footer_left="Nhóm gehihi — Lớp N03 — Trường Đại học CMC",
        show_page_num=True,
    )

    add_chapter(doc, "Chương 1. Tổng quan đề tài và khảo sát bài toán", page_break=False)

    add_h2(doc, "1.1. Thực trạng quản lý nhà trọ và lý do chọn đề tài")
    add_para(
        doc,
        "Tại các khu vực tập trung nhiều sinh viên và người đi làm như quận Cầu Giấy, Thanh Xuân hay Nam Từ Liêm (Hà Nội), phần lớn các dãy trọ quy mô từ 10 đến 60 phòng vẫn đang được chủ nhà hoặc người quản lý theo dõi bằng sổ tay kết hợp bảng tính Excel và tin nhắn Zalo. Cách làm này khá gọn khi mới có vài phòng, nhưng khi số phòng tăng lên, khách ra vào giữa tháng hoặc chia sẻ nhiều khoản phí khác nhau thì rất dễ phát sinh sai sót và tranh cãi.",
    )
    add_para(
        doc,
        "Qua trao đổi thực tế và tự tổng hợp từ trải nghiệm đi thuê trọ của các thành viên trong nhóm, chúng tôi nhận thấy bốn vấn đề lặp đi lặp lại thường xuyên nhất:",
    )
    add_bullet(
        doc,
        "Phiếu báo tiền hàng tháng thường chỉ ghi một con số tổng hoặc ghi gọn 'điện 420k, nước 160k' mà không thể hiện rõ chỉ số cũ–mới, đơn giá áp dụng tại thời điểm đó hay công thức chia ngày khi khách ở không tròn tháng. Khi giá dịch vụ thay đổi giữa chừng, người thuê khó kiểm tra ngược lại hóa đơn các tháng trước.",
        bold_prefix="Hóa đơn thiếu căn cứ tính toán: ",
    )
    add_bullet(
        doc,
        "Khi người ghi số công tơ nhập nhầm một chữ số hoặc đường ống nước trong phòng bị rò rỉ ngầm, tiền điện nước tăng gấp hai, gấp ba lần nhưng chủ nhà vẫn gửi hóa đơn bình thường. Đến lúc khách phản ánh thì hóa đơn đã phát hành, phải sửa tay đè lên bảng tính cũ khiến lịch sử không còn khớp.",
        bold_prefix="Không phát hiện bất thường điện nước trước khi phát hành hóa đơn: ",
    )
    add_bullet(
        doc,
        "Nhiều phần mềm cơ bản chỉ lưu một cột tiền cọc trên hợp đồng và một cột 'đã thanh toán' trên hóa đơn. Khi khách mới đóng trước một phần cọc, hoặc chuyển khoản dư tiền phòng tháng này để trừ sang tháng sau, hay chủ nhà cấn cọc khi trả phòng, bảng tính thường trộn lẫn tiền thật đã vào tài khoản với nghĩa vụ cam kết, dẫn tới báo cáo doanh thu và tiền mặt không khớp nhau.",
        bold_prefix="Lẫn lộn giữa cam kết, công nợ và dòng tiền thực tế: ",
    )
    add_bullet(
        doc,
        "Lúc khách dọn vào chỉ ghi vội mấy dòng danh sách đồ đạc trên giấy, đến một năm sau khi trả phòng thì giấy tờ thất lạc hoặc không rõ vết nứt/hỏng đã có từ trước hay mới phát sinh. Việc xác định tiền bồi thường và số tiền cọc còn phải hoàn thường phụ thuộc vào trí nhớ hai bên.",
        bold_prefix="Thiếu hồ sơ đối chiếu đồ đạc khi bàn giao nhận và trả phòng: ",
    )
    add_para(
        doc,
        "Từ bốn vấn đề cụ thể trên, nhóm chọn đề tài **Xây dựng hệ thống quản lý nhà trọ** cho học phần Kỹ năng lập trình nâng cao. Mục tiêu không phải là làm một bản CRUD lưu danh sách phòng đơn thuần, mà là xây dựng một hệ thống có độ chặt chẽ cao về nghiệp vụ tài chính, bảo toàn dữ liệu lịch sử, kiểm soát đồng thời ở cả tầng ứng dụng lẫn tầng cơ sở dữ liệu PostgreSQL.",
    )

    add_h2(doc, "1.2. Khảo sát giải pháp hiện có và đối chiếu nguồn tham khảo")
    add_para(
        doc,
        "Trước khi chốt mô hình dữ liệu và luồng xử lý ở các phiên bản nâng cấp, nhóm đã khảo sát một số hệ thống quản lý nhà trọ thương mại tại Việt Nam cùng một số dự án mã nguồn mở trên GitHub có bài toán gần với đề tài. Nhóm không sao chép mã nguồn mà chỉ đọc cấu trúc nghiệp vụ, kịch bản kiểm thử và các quyết định kiến trúc để rút ra điểm phù hợp và điểm cần thiết kế khác đi.",
    )
    add_styled_table(
        doc,
        headers=["Hệ thống / Dự án", "Công nghệ & Phạm vi", "Điểm đáng học hỏi", "Khác biệt nhóm tự thiết kế"],
        rows=[
            [
                "OpenKOS\n(Apache-2.0)",
                "Laravel, React, PostgreSQL; quản lý nhà trọ (boarding house).",
                "Nối chuỗi chỉ số công tơ, chặn chỉ số giảm, ranh giới thuê và khấu trừ cọc có chi tiết.",
                "`VerifyPayment` của OpenKOS từ chối tiền thu vượt hóa đơn; nhóm tách quỹ `ADVANCE` để giữ phần tiền khách chuyển dư và phân biệt cọc cam kết với cọc thực giữ.",
            ],
            [
                "LeaseBook\n(AGPL-3.0)",
                "ASP.NET Core, React, PostgreSQL; sổ tài chính cho thuê.",
                "Tư duy tách tiền thu dư với cấn quỹ (ADR-011) và bộ kiểm thử tranh chấp đồng thời trên số dư.",
                "Giữ nguyên stack Java 21 / Spring Boot / PostgreSQL và rút gọn phạm vi sổ quỹ về đúng quy mô một chủ trọ thay vì kế toán ủy thác doanh nghiệp.",
            ],
            [
                "TechFunWay/rental\n(MIT)",
                "Go/Gin, Vue 3, SQLite; chủ cho thuê quy mô nhỏ.",
                "Quy trình gọn: ghi công tơ → lập hóa đơn → phiếu thu, không thu lặp tiền cọc ở tháng sau.",
                "Dự án dùng `float64` dễ sai số làm tròn tiền tệ; nhóm bắt buộc dùng `NUMERIC(14,2)` trong PostgreSQL và `BigDecimal` trong Java.",
            ],
            [
                "Resident (VN)\n(Sản phẩm thương mại)",
                "Nền tảng SaaS quản lý nhà trọ phổ biến tại Việt Nam.",
                "Luồng vận hành quen thuộc với chủ trọ Việt: chốt điện nước → hóa đơn → thu tiền/công nợ → sự cố.",
                "Tập trung bổ sung chiều sâu kiểm soát lỗi nhập liệu: cảnh báo bất thường `WEIGHTED_DAILY` chặn phát hành hóa đơn và đối chiếu ảnh từng món đồ khi trả phòng.",
            ],
        ],
        col_widths_cm=[2.8, 3.3, 4.4, 5.5],
        caption="So sánh các hệ thống tham khảo và định hướng thiết kế riêng của nhóm",
        font_size=9.8,
    )

    add_h2(doc, "1.3. Mục tiêu, phạm vi và quá trình tiến hóa của hệ thống")
    add_para(
        doc,
        "Hệ thống hướng tới phạm vi **một chủ nhà quản lý nhiều khu nhà và nhiều phòng**, có thể phân công nhân viên theo từng khu hoặc từng phòng kèm hạn mức ủy quyền, và cấp tài khoản cho người thuê tự theo dõi hợp đồng, hóa đơn đã phát hành, lịch sử thanh toán cũng như gửi yêu cầu sửa chữa kèm ảnh. Các khoản thanh toán được ghi nhận và xác minh thủ công theo sao kê hoặc tiền mặt; cổng thanh toán tự động, gửi SMS/Email ngoài và cảm biến IoT đo điện nước tự động nằm ngoài phạm vi đồ án.",
    )
    add_para(
        doc,
        "Một điểm đáng chú ý trong quá trình làm đồ án là hệ thống không được thiết kế cố định một lần mà trải qua ba giai đoạn nâng cấp rõ ràng (tất cả vẫn được giữ nguyên lịch sử trong các file Flyway migration từ `V1` đến `V14`):",
    )
    add_bullet(
        doc,
        "Xây dựng bản mẫu chạy nhanh (prototype) để kiểm chứng khung Spring Boot, xác thực JWT và giao diện cơ bản. Tuy nhiên, khi chạy thử các tình huống như đổi giá giữa kỳ hay quyết toán trả phòng sớm, cấu trúc bảng phẳng bắt đầu bộc lộ hạn chế về toàn vẹn dữ liệu.",
        bold_prefix="Giai đoạn 1 (V1–V9 — Prototype ban đầu): ",
    )
    add_bullet(
        doc,
        "Chuẩn hóa lại lược đồ cho bài nộp phân tích yêu cầu và thiết kế CSDL bước 1–2 (gồm 17 bảng lõi, tách hồ sơ người thuê khỏi hợp đồng, lưu lịch sử thành viên ở cùng và khóa hóa đơn đã phát hành), đồng thời bảo lưu dữ liệu cũ vào schema `prototype_v9`.",
        bold_prefix="Giai đoạn 2 (V10 — Chuẩn hóa 17 bảng): ",
    )
    add_bullet(
        doc,
        "Khi rà soát sâu các tình huống tài chính khó (thu thiếu cọc, chuyển dư tiền phòng, sửa chỉ số sau phát hành, thay công tơ, trả phòng khi chưa chốt điện cuối) và tiếp nhận thêm yêu cầu bổ sung của giảng viên, nhóm tái cấu trúc lên mô hình 49 bảng (V11, bảo lưu dữ liệu cũ ở `model_v10`), bổ sung ràng buộc ranh giới công tơ (V12), gắn hóa đơn định danh cho phiếu thu để chống gửi lặp (V13) và mở rộng đối chiếu đồ đạc bàn giao kèm thông tin xuất hợp đồng PDF (V14).",
        bold_prefix="Giai đoạn 3 (V11–V14 — Hoàn thiện mô hình 49 bảng): ",
    )

    add_h2(doc, "1.4. Tổng hợp yêu cầu bổ sung của giảng viên và mức độ đáp ứng")
    add_para(
        doc,
        "Trong đợt kiểm tra tiến độ trước khi bảo vệ, giảng viên đã yêu cầu nhóm hoàn thiện thêm một số tính năng quản trị sát với thực tế vận hành. Bảng 2 tổng hợp đầy đủ các yêu cầu này cùng vị trí hiện thực tương ứng trong bản V14.",
    )
    add_styled_table(
        doc,
        headers=["STT", "Yêu cầu bổ sung của giảng viên", "Cách nhóm hiện thực ở bản V14", "Trạng thái"],
        rows=[
            ["1", "Báo cáo doanh thu – dòng tiền theo Tháng, Quý, Năm và hỗ trợ xuất PDF.", "Xây dựng các API `/reports/revenue`, `/revenue-quarterly`, `/revenue-yearly`, `/revenue/pdf?group=...` và màn hình `ReportsPanel` có biểu đồ cột + bảng tách `Tiền vào quỹ` / `Hoàn quỹ`.", "Hoàn thành"],
            ["2", "Tổng quan hiển thị đủ 6 chỉ số: thu trước hạn, đúng hạn, sau hạn, chưa thu (nợ), tiền điện, tiền nước; cho phép chuyển chế độ Tháng/Quý.", "Cài đặt truy vấn loại trừ lẫn nhau `ITEM_CLASSIFICATION` tại `Reports49.java` (khắc phục lỗi `lower('Đ')` dưới `locale=C`), đảm bảo đẳng thức đối soát `Tổng lập HĐ = Trước hạn + Đúng hạn + Sau hạn + Chưa thu`.", "Hoàn thành"],
            ["3", "Theo dõi danh sách khách thuê còn công nợ, số ngày quá hạn và thu tiền trực tiếp.", "Cài đặt `/reports/tenant-debts` và màn hình `DebtsPanel` liệt kê khách đang nợ, cho phép lọc quá hạn, mở chi tiết từng hóa đơn nợ, xuất CSV và mở hộp thoại thu tiền chọn tài khoản quỹ.", "Hoàn thành"],
            ["4", "Cảnh báo điện nước bất thường nổi bật rõ ràng và có đơn vị đo cụ thể.", "Sắp xếp mặc định đưa các dòng cảnh báo lên đầu bảng Điện nước, tô nền cam, hiển thị mức trung bình ngày kèm đơn vị `kWh/ngày` và `m³/ngày`, chặn phát hành hóa đơn khi còn cảnh báo `PENDING`.", "Hoàn thành"],
            ["5", "Quản lý danh mục đồ đạc/tài sản bố trí trong từng phòng.", "Bổ sung API `/rooms/{id}/assets` và giao diện `RoomAssetsPanel` trong chi tiết phòng (chỉ `ADMIN` được thêm tài sản mới/gỡ bố trí; `TENANT` chỉ xem).", "Hoàn thành"],
            ["6", "Biên bản bàn giao nhận–trả phòng đối chiếu từng món đồ, ảnh minh chứng và lập bồi thường.", "Migration `V14` thêm cột tự tham chiếu `handover_items.original_item_id`; khi tạo `MOVE_OUT` tự nạp danh mục từ `MOVE_IN`, so sánh song song `Lúc nhận` vs `Lúc trả`, đính ảnh qua `Files49` và phát hành chứng từ `COMPENSATION` gắn đúng dòng hư hỏng.", "Hoàn thành"],
            ["7", "Xuất file PDF hợp đồng thuê phòng và phụ lục gia hạn theo từng phiên bản.", "Migration `V14` thêm 5 cột thông tin hai bên trên `contract_terms`; lớp `ContractPdf.java` dùng Apache PDFBox 3.0.7 + font NotoSans xuất PDF tiếng Việt có dấu, tự động đổi tiêu đề sang `PHỤ LỤC HỢP ĐỒNG` cho phiên bản từ số 2 trở đi.", "Hoàn thành"],
        ],
        col_widths_cm=[1.0, 4.7, 8.2, 2.1],
        caption="Đối chiếu yêu cầu bổ sung của giảng viên và kết quả hiện thực ở phiên bản V14",
        align_cols=[
            WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT,
            WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER,
        ],
        font_size=9.8,
    )


def build_chapter_2(doc: docx.Document):
    add_chapter(doc, "Chương 2. Cơ sở lý thuyết và kỹ thuật lập trình nâng cao áp dụng")

    add_h2(doc, "2.1. Kiến trúc Backend Java 21 và Spring Boot 4.1.1")
    add_para(
        doc,
        "Backend của hệ thống được xây dựng trên nền tảng **Java 21 (LTS)** kết hợp **Spring Boot 4.1.1** và quản lý thư viện bằng Apache Maven (`pom.xml`). Thay vì để toàn bộ mã nguồn trộn lẫn giữa tầng giao tiếp HTTP và truy vấn SQL, nhóm tổ chức các thành phần theo nguyên lý đảo ngược điều khiển (IoC) và tiêm phụ thuộc qua hàm khởi tạo (Constructor Injection) của Spring Framework. Cách tiêm qua constructor giúp mọi phụ thuộc của một service đều là `final`, dễ phát hiện phụ thuộc vòng ngay lúc khởi động và thuận tiện khi viết kiểm thử tích hợp.",
    )
    add_para(
        doc,
        "Để đáp ứng yêu cầu vận dụng các kỹ thuật lập trình hướng đối tượng và nâng cao của môn học, nhóm áp dụng rõ nét ba cơ chế trong mã nguồn:",
    )
    add_bullet(
        doc,
        "Nhóm định nghĩa giao diện `FeeCalculator` (`src/main/java/com/hostel/service/FeeCalculator.java`) kèm phương thức tĩnh chuẩn hóa làm tròn tiền tệ `RoundingMode.HALF_UP`, từ đó triển khai 4 lớp cụ thể (`RoomFeeCalculator`, `ElectricityFeeCalculator`, `WaterFeeCalculator`, `ServiceFeeCalculator`) cho từng quy tắc tính phí và kiểm thử độc lập bằng `FeeCalculatorTests`.",
        bold_prefix="Tính đa hình và tách biệt quy tắc tính phí: ",
    )
    add_bullet(
        doc,
        "Lớp `ApiTimingAspect` (`com.hostel.config.ApiTimingAspect`) sử dụng chú thích `@Aspect` và `@Around(\"@within(org.springframework.web.bind.annotation.RestController)\")` của AspectJ để tự động đo thời gian xử lý (tính bằng mili-giây) của toàn bộ các controller REST mà không phải viết lặp mã đo thời gian trong từng endpoint.",
        bold_prefix="Lập trình hướng khía cạnh (AOP): ",
    )
    add_bullet(
        doc,
        "Hệ thống sử dụng `@Profile(\"model49\")` (được bật mặc định trong các profile `dev` và `prod`) để cô lập hoàn toàn các controller/service/entity của mô hình 49 bảng (`com.hostel.model49.*`) với các thành phần V10 cũ (`@Profile(\"!model49\")`). Nhờ vậy, bộ kiểm thử tự động vẫn chạy song song được cả 68 ca kiểm thử V14 hiện hành lẫn 55 ca hồi quy của mô hình trước đó mà không xung đột bean.",
        bold_prefix="Cô lập phiên bản mô hình bằng Spring Profile: ",
    )

    add_h2(doc, "2.2. Kết hợp Spring Data JPA (validate) và JdbcTemplate")
    add_para(
        doc,
        "Một quyết định kỹ thuật quan trọng mà nhóm cân nhắc kỹ là cách truy cập cơ sở dữ liệu. Với những hệ thống quản lý nhiều bảng có ràng buộc tài chính chặt chẽ, nếu giao toàn bộ việc ghi dữ liệu cho ORM/Hibernate với các mối quan hệ `@OneToMany` lồng nhau rất dễ phát sinh hai rắc rối: thứ nhất là truy vấn N+1 gây chậm ở các màn hình báo cáo tổng hợp; thứ hai là Hibernate tự gom lệnh `INSERT`/`UPDATE` theo thứ tự flush nội bộ, khó kiểm soát chính xác thời điểm chiếm khóa dòng (`FOR UPDATE`) khi có nhiều giao dịch ghi sổ đồng thời.",
    )
    add_para(
        doc,
        "Vì vậy, nhóm chọn hướng kết hợp thực dụng giữa hai công cụ sẵn có của Spring:",
    )
    add_bullet(
        doc,
        "Khai báo đầy đủ 49 lớp thực thể trong `com.hostel.model49.entity` (tương ứng 49 bảng vật lý) và bật cấu hình `spring.jpa.hibernate.ddl-auto=validate`. Mỗi khi ứng dụng khởi động, Hibernate tự đối chiếu toàn bộ ánh xạ bảng–cột–kiểu dữ liệu với schema thực tế trong PostgreSQL. Nếu migration bị thiếu cột hoặc sai kiểu dữ liệu, ứng dụng dừng ngay lập tức thay vì chạy ngầm rồi lỗi ở runtime.",
        bold_prefix="Dùng JPA ở chế độ kiểm chứng lược đồ (`validate`): ",
    )
    add_bullet(
        doc,
        "Toàn bộ thao tác đọc/ghi nghiệp vụ trong `Leases49`, `Meters49`, `Billing49`, `Finance49`, `Settlements49` và `Reports49` đều chạy qua lớp bao `Database.java` bọc `JdbcTemplate` với câu lệnh SQL tham số hóa (PreparedStatement). Cách này giúp nhóm kiểm soát chính xác 100% thứ tự khóa bản ghi, tận dụng trực tiếp CTE (`WITH`), `LATERAL JOIN`, hàm thời gian múi giờ `Asia/Ho_Chi_Minh` của PostgreSQL và phòng tránh hoàn toàn lỗi SQL Injection.",
        bold_prefix="Dùng `JdbcTemplate` cho toàn bộ truy vấn nghiệp vụ và khóa đồng thời: ",
    )

    add_h2(doc, "2.3. Kiểm soát đồng thời, giao dịch ACID và chống gửi lặp")
    add_para(
        doc,
        "Trong thực tế vận hành, rất dễ xảy ra tình huống hai nhân viên cùng lúc bấm ghi nhận thanh toán cho một hóa đơn, hoặc mạng chập chờn khiến trình duyệt gửi lặp cùng một yêu cầu thu tiền hay hoàn cọc hai lần. Nếu chỉ kiểm tra điều kiện số dư bằng một câu `SELECT` thông thường ở Java rồi mới `INSERT`, hai luồng chạy song song đều có thể thấy số dư còn đủ và cùng trừ tiền, gây âm quỹ hoặc thu trùng.",
    )
    add_para(
        doc,
        "Để xử lý triệt để, nhóm phối hợp đồng thời ba cơ chế bảo vệ ở tầng service (`@Transactional`) và tầng PostgreSQL:",
    )
    add_bullet(
        doc,
        "Mọi thao tác sửa đổi dữ liệu liên quan đến phòng, hợp đồng, công tơ, chứng từ hay quỹ đều khóa bản ghi cha theo một thứ tự cố định (ví dụ `rooms` → `contracts` → `meters` hoặc `fund_accounts` sắp xếp tăng dần theo `id`) bằng mệnh đề `SELECT ... FOR UPDATE`. Việc giữ cố định thứ tự khóa trên các service giúp triệt tiêu nguy cơ khóa chết (deadlock) giữa hai giao dịch cạnh tranh.",
        bold_prefix="Khóa bi quan theo thứ tự xác định (`FOR UPDATE`): ",
    )
    add_bullet(
        doc,
        "Với thao tác khởi tạo mới mà bản ghi đích chưa tồn tại để khóa dòng (như lập hóa đơn tháng đầu tiên của một hợp đồng/kỳ hoặc tạo phiếu thu theo mã), backend dùng hàm `pg_advisory_xact_lock` của PostgreSQL để tuần tự hóa ngắn hạn trong phạm vi transaction và tự động giải phóng ngay khi `COMMIT` hoặc `ROLLBACK`.",
        bold_prefix="Khóa tư vấn theo giao dịch (`pg_advisory_xact_lock`): ",
    )
    add_bullet(
        doc,
        "Các endpoint thu tiền (`/invoices/{id}/payments`, `/cash-transactions`), ghi bút toán (`/fund-entries`) và hoàn tiền (`/fund-accounts/{id}/refund`) đều yêu cầu mã định danh duy nhất (`code`) và cho phép kèm mã tham chiếu giao dịch ngân hàng (`externalReference`). Từ bản V13, cột `cash_transactions.payment_document_id` gắn chặt phiếu thu với hóa đơn đích: nếu người dùng bấm gửi lại đúng mã cùng nội dung (số tiền, phương thức, thời điểm, tham chiếu), hệ thống trả về bản ghi gốc mà không cộng tiền hai lần; ngược lại nếu dùng lại mã cũ cho hóa đơn hoặc số tiền khác, hệ thống từ chối bằng mã lỗi HTTP `409 Conflict`.",
        bold_prefix="Chống gửi lặp có kiểm tra toàn vẹn nội dung (Idempotency): ",
    )

    add_h2(doc, "2.4. PostgreSQL 18.6 nâng cao và Flyway Forward-Only Migration")
    add_para(
        doc,
        "Nhóm lựa chọn **PostgreSQL 18.6** thay vì các hệ quản trị nhẹ như SQLite hay MySQL vì PostgreSQL hỗ trợ rất mạnh các ràng buộc toàn vẹn phức tạp ngay ở tầng lưu trữ, đóng vai trò như lớp phòng thủ cuối cùng ngay cả khi có lỗi ở tầng ứng dụng:",
    )
    add_bullet(
        doc,
        "Nhờ tiện ích mở rộng `btree_gist` (kích hoạt từ `V1__enable_extensions.sql`), hệ thống cài đặt **7 ràng buộc `EXCLUDE USING gist`** để cấm chồng khoảng thời gian ngay trong CSDL: một phòng không thể có hai khoảng khai thác trùng nhau (`room_operating_periods`), một tài sản không thể bố trí ở hai nơi cùng lúc (`asset_placements`), cùng một người thuê không thể có hai khoảng cư trú chồng nhau trong một hợp đồng (`contract_residencies`), một phòng không có hai công tơ cùng loại dịch vụ hoạt động đồng thời (`meters`), v.v.",
        bold_prefix="Ràng buộc loại trừ chống chồng khoảng thời gian (`EXCLUDE USING gist`): ",
    )
    add_bullet(
        doc,
        "Hệ thống cài đặt nhiều trigger PL/pgSQL để thực thi các bất biến liên bảng mà `CHECK` đơn lẻ không diễn đạt được: khóa mọi lệnh `UPDATE`/`DELETE` trên `billing_documents` và `billing_items` sau khi đã phát hành (`ISSUED`), biến `fund_entries` thành sổ cái chỉ ghi thêm (`append-only` — sửa sai phải tạo bút toán `REVERSAL` trỏ `original_entry_id`), kiểm tra tổng phân bổ không vượt số tiền thực nhận trên phiếu thu và không vượt số dư khả dụng của quỹ sau khi trừ khoản tạm giữ (`fund_holds`). Một số kiểm tra tổng khớp cuối giao dịch dùng `CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED` để cho phép một phiếu thu chia thành nhiều dòng `RECEIVE` vào nhiều quỹ trong cùng một transaction.",
        bold_prefix="Trigger bảo vệ bất biến sổ cái và `DEFERRABLE INITIALLY DEFERRED`: ",
    )
    add_bullet(
        doc,
        "Mọi thay đổi lược đồ đều đi qua **Flyway** theo nguyên tắc tiến tới (`forward-only`): các file `V1` đến `V14` không bị sửa ngược sau khi đã phát hành. Riêng `V11__approved_49_table_model.sql` thực hiện chuyển đổi nguyên tử trong một giao dịch PostgreSQL: di dời 17 bảng V10 sang schema `model_v10`, tạo 49 bảng mới, nạp dữ liệu lịch sử, tự động đối soát tổng tiền và khóa ghi toàn bộ schema `model_v10` để giữ bằng chứng đối chiếu.",
        bold_prefix="Quản lý nâng cấp lược đồ an toàn với Flyway (`V1–V14`): ",
    )

    add_h2(doc, "2.5. Bảo mật ứng dụng Web và kiểm soát tệp tải lên")
    add_para(
        doc,
        "Tầng bảo mật được xây dựng trên **Spring Security** kết hợp thư viện **JJWT 0.13.0** (ký khóa bí mật đối xứng HMAC-SHA từ biến môi trường `JWT_SECRET`). Hệ thống cấu hình phiên không trạng thái (`SessionCreationPolicy.STATELESS`), mật khẩu băm một chiều bằng `BCryptPasswordEncoder` và không bao giờ lưu hay ghi mật khẩu/mã khôi phục vào log. Khi người dùng đăng nhập thành công, server cấp cặp `accessToken` ngắn hạn và `refreshToken` theo cơ chế xoay vòng (`Refresh Token Rotation`) quản lý trong `SessionRegistry`: mỗi lần làm mới sẽ thu hồi token cũ, và khi chủ nhà khóa tài khoản hoặc người dùng đổi mật khẩu, toàn bộ phiên đang mở của tài khoản đó lập tức bị vô hiệu hóa ở `JwtFilter`.",
    )
    add_para(
        doc,
        "Đối với tính năng tải ảnh sự cố, ảnh biên bản bàn giao và tệp PDF hợp đồng (`Files49.java`), hệ thống không tin vào phần mở rộng tệp hay header `Content-Type` do trình duyệt gửi lên. Mọi tệp ảnh đều được đọc kiểm tra thực sự qua `javax.imageio.ImageIO.read(...)` (tệp PDF kiểm tra chuỗi magic byte `%PDF-`), giới hạn dung lượng tối đa, tính mã băm `SHA-256`, đổi tên thành khóa lưu trữ ngẫu nhiên (`storage_key`) nằm ngoài thư mục public web và chỉ cho phép tải về qua endpoint `/api/uploads/{key}` sau khi `Access49` xác nhận người gọi có quyền xem đúng đối tượng nghiệp vụ gắn với tệp đó.",
    )

    add_h2(doc, "2.6. Xuất văn bản PDF tiếng Việt và giao diện React 19 SPA")
    add_para(
        doc,
        "Để xuất trực tiếp hóa đơn, báo cáo doanh thu và đặc biệt là hợp đồng/phụ lục thuê phòng dưới dạng PDF in được ngay, nhóm sử dụng thư viện **Apache PDFBox 3.0.7** kết hợp nhúng sẵn bộ phông chữ TrueType **NotoSans** (`NotoSans-Regular.ttf`, `NotoSans-Bold.ttf`) trong `src/main/resources/fonts/`. Việc nhúng trực tiếp phông Unicode giúp văn bản PDF hiển thị chính xác toàn bộ dấu thanh tiếng Việt và ký hiệu `₫`, không bị lỗi ô vuông trên các máy chủ Linux/Windows thiếu phông hệ thống.",
    )
    add_para(
        doc,
        "Ở tầng giao diện, nhóm dùng **React 19.2** và công cụ đóng gói **Vite 7.1**, kết hợp bộ biểu tượng vector `lucide-react` và phông chữ cục bộ `@fontsource/inter`. Khi đóng gói phát hành (`npm.cmd run build`), toàn bộ mã giao diện được biên dịch thành tệp tĩnh đặt trực tiếp vào `src/main/resources/static/`, nhờ đó ứng dụng chỉ cần chạy duy nhất một tệp JAR độc lập của Spring Boot để phục vụ đồng thời cả giao diện lẫn REST API, giảm đáng kể độ phức tạp khi triển khai và chấm bảo vệ.",
    )


def build_chapter_3(doc: docx.Document):
    add_chapter(doc, "Chương 3. Phân tích và thiết kế hệ thống")

    add_h2(doc, "3.1. Phân loại tác nhân và ma trận phân quyền")
    add_para(
        doc,
        "Hệ thống phục vụ ba nhóm tác nhân chính, trong đó mọi kiểm tra quyền đều được cưỡng chế tại tầng backend (`Access49.java`) thay vì chỉ ẩn nút trên giao diện. Điều này đảm bảo kể cả khi người dùng thay đổi trực tiếp mã định danh (`roomId`, `contractId`, `fundAccountId`) trong yêu cầu HTTP, backend vẫn chặn lại bằng mã `403 Forbidden`.",
    )
    add_styled_table(
        doc,
        headers=["Tác nhân", "Phạm vi dữ liệu", "Quyền thao tác chính", "Giới hạn bảo mật"],
        rows=[
            [
                "**Chủ nhà**\n(`ADMIN`)",
                "Toàn bộ khu nhà, phòng, quỹ tiền và tài khoản của hệ thống.",
                "Tạo/khóa tài khoản, phân công nhân viên, cấp hạn mức ủy quyền (`staff_permission_grants`), quản lý khu/phòng/đơn giá/ngưỡng cảnh báo, thêm tài sản mới, cấp mã khôi phục mật khẩu và thực hiện mọi nghiệp vụ vận hành.",
                "Không thể sửa/xóa trực tiếp hóa đơn đã phát hành hay bút toán sổ cái đã ghi; mọi điều chỉnh phải tạo chứng từ hoặc bút toán đảo có lý do và dấu vết kiểm toán.",
            ],
            [
                "**Nhân viên**\n(`STAFF`)",
                "Chỉ các khu nhà hoặc phòng được phân công còn hiệu lực (`employee_assignments`).",
                "Lập hợp đồng, ghi nhận cư trú, lập biên bản bàn giao, ghi chỉ số điện nước, kiểm tra cảnh báo, lập/phát hành hóa đơn, thu tiền, xử lý sự cố và xem báo cáo trong phạm vi được giao.",
                "Không quản trị tài khoản/đơn giá/tạo tài sản mới. Các thao tác nhạy cảm (giao phòng thiếu cọc `WAIVE_DEPOSIT`, điều chỉnh hóa đơn `ADJUST`, cấn cọc, hoàn tiền) chỉ được làm khi có quyền ủy quyền còn hạn và trong hạn mức tiền.",
            ],
            [
                "**Người thuê**\n(`TENANT`)",
                "Chỉ hợp đồng mình là bên đại diện, người đang cư trú hoặc người liên hệ có cờ `can_view_billing`.",
                "Xem hợp đồng, danh sách đồ đạc phòng, hóa đơn đã phát hành, lịch sử thanh toán; gửi yêu cầu sửa chữa kèm ảnh chụp hiện trường; xem và đánh dấu đã đọc thông báo.",
                "Không thấy hóa đơn nháp (`DRAFT`), không thấy dữ liệu phòng khác, không tự xác nhận thanh toán hay chỉnh sửa danh sách đồ đạc phòng.",
            ],
        ],
        col_widths_cm=[2.5, 3.6, 5.1, 4.8],
        caption="Phân loại tác nhân và phạm vi kiểm soát quyền truy cập (RBAC + Scope)",
        font_size=9.8,
    )

    add_h2(doc, "3.2. Phân tích chức năng và biểu đồ Use Case")
    add_para(
        doc,
        "Hình 1 khái quát toàn bộ các nhóm chức năng chính và mối quan hệ giữa ba tác nhân trong phiên bản V14. Trong biểu đồ, tác nhân `ADMIN` kế thừa toàn bộ quyền vận hành của `STAFF` theo quan hệ tổng quát hóa UML và sở hữu riêng nhóm quản trị danh mục, phân công, đơn giá, ngưỡng cảnh báo cùng danh mục tài sản.",
    )
    add_figure(
        doc,
        ASSETS / "diag-02-usecase.png",
        "Biểu đồ Use Case tổng quát hệ thống quản lý nhà trọ (phiên bản V14)",
        max_width_cm=15.6,
        add_border=False,
    )

    add_para(
        doc,
        "Để làm rõ các điều kiện ràng buộc ở tầng nghiệp vụ, Bảng 4 đặc tả chi tiết ba Use Case trọng tâm có nguy cơ sai lệch tiền cao nhất nếu thiết kế lỏng lẻo.",
    )
    add_styled_table(
        doc,
        headers=["Use Case", "Tiền điều kiện", "Luồng xử lý chính", "Nhánh ngoại lệ & Hậu điều kiện"],
        rows=[
            [
                "**Phát hành hóa đơn tháng**\n(`POST /billing-documents/{id}/issue`)",
                "Người thao tác là `ADMIN` hoặc `STAFF` trong phạm vi; chứng từ đang ở trạng thái `DRAFT` và đã có đầy đủ dòng khoản thu.",
                "1. Khóa dòng chứng từ và hợp đồng liên quan.\n2. Kiểm tra tất cả các đoạn tiêu thụ (`segment_id`) không còn cảnh báo ở trạng thái `PENDING`.\n3. Kiểm tra ngày phát hành hợp lệ và đơn giá/chỉ số nguồn đầy đủ.\n4. Chuyển trạng thái sang `ISSUED`, ghi nhận `issued_by`, `issued_at` và phát thông báo cho người thuê.",
                "**Ngoại lệ:** Còn cảnh báo điện nước `PENDING` → từ chối phát hành (`409`), giữ nguyên nháp để nhân viên kiểm tra thực tế.\n**Hậu điều kiện:** Trigger PostgreSQL khóa cứng đầu và chi tiết hóa đơn, không cho phép sửa/xóa đè.",
            ],
            [
                "**Ghi nhận thanh toán & tách tiền thừa**\n(`POST /invoices/{id}/payments`)",
                "Hóa đơn đã ở trạng thái `ISSUED`; số tiền thu `> 0` (tối đa 2 chữ số thập phân); tài khoản nhận (`cashAccountId`) đang hoạt động.",
                "1. Khóa chống gửi lặp theo `code` và kiểm tra trùng `externalReference`.\n2. Tạo 1 phiếu thu `cash_transactions` (chiều `IN`, trạng thái `CONFIRMED`, gắn `payment_document_id`).\n3. Ghi bút toán `RECEIVE` toàn bộ tiền thật vào quỹ khách.\n4. Ghi bút toán `PAYMENT` đúng bằng phần nhỏ hơn giữa tiền nhận và nợ hiện tại; phần tiền thừa (nếu có) tự động ở lại quỹ `ADVANCE`.",
                "**Ngoại lệ:** Gửi lại đúng `code` và cùng dữ liệu → trả về kết quả cũ, không thu lần hai. Dùng lại `code` với hóa đơn/số tiền khác → chặn (`409`).\n**Hậu điều kiện:** Dư nợ hóa đơn tự cập nhật qua `v_document_balances`; tiền thực thu chỉ ghi 1 lần.",
            ],
            [
                "**Trả phòng, đối chiếu đồ đạc & quyết toán**\n(`handovers` → `checkout` → `approve` → `refund`)",
                "Hợp đồng đang `ACTIVE` và đã có bản ghi nhận phòng (`room_possessions`); biên bản nhận phòng (`MOVE_IN`) đã xác nhận.",
                "1. Tạo biên bản trả phòng (`MOVE_OUT`), hệ thống tự nạp hạng mục từ `MOVE_IN` gắn `original_item_id`, nhập hiện trạng và đính ảnh hỏng, sau đó xác nhận khóa biên bản.\n2. Nếu có hư hỏng/thiếu đồ, phát hành chứng từ bồi thường (`COMPENSATION`) gắn `handover_item_id`.\n3. Ghi nhận trả phòng vật lý (`checkout`), chuyển phòng sang `TURNOVER`.\n4. Duyệt quyết toán (`checkout/approve`): điều chỉnh tiền phòng trả sớm, chốt điện nước cuối, cấn nợ từ `ADVANCE`/`DEPOSIT` và chụp snapshot.\n5. Thực hiện chi hoàn thực tế (`refund`) khi đã chuyển tiền lại cho khách.",
                "**Ngoại lệ:** Chưa chốt được chỉ số điện nước cuối → bắt buộc nhập lý do và tạo khoản tạm giữ (`fund_holds`) có ngày rà lại, làm giảm số được phép hoàn.\n**Hậu điều kiện:** Phân biệt rạch ròi giữa *số dự kiến phải hoàn* sau quyết toán với *phiếu chi hoàn thật (`OUT`)*.",
            ],
        ],
        col_widths_cm=[3.0, 3.0, 5.4, 4.6],
        caption="Đặc tả chi tiết ba Use Case nghiệp vụ trọng tâm của hệ thống",
        font_size=9.5,
    )

    add_h2(doc, "3.3. Thiết kế kiến trúc tổng thể hệ thống")
    add_para(
        doc,
        "Hệ thống áp dụng kiến trúc phân tầng rõ ràng gồm 4 tầng chính như minh họa tại Hình 2. Việc tách biệt rạch ròi giữa tầng giao diện SPA, tầng giao tiếp API & bảo mật, tầng xử lý nghiệp vụ miền (`model49`) và tầng lưu trữ toàn vẹn giúp từng lớp dễ bảo trì và kiểm thử độc lập.",
    )
    add_figure(
        doc,
        ASSETS / "diag-01-architecture.png",
        "Kiến trúc tổng thể 4 tầng của hệ thống quản lý nhà trọ phiên bản V14",
        max_width_cm=15.6,
        add_border=False,
    )
    add_para(
        doc,
        "Ở tầng giao tiếp và xử lý nghiệp vụ, thay vì gom toàn bộ hàng nghìn dòng mã vào một lớp dịch vụ duy nhất, nhóm chia miền nghiệp vụ `com.hostel.model49` thành các lớp chuyên trách theo đúng biên giới dữ liệu: `Leases49` quản lý vòng đời hợp đồng và cư trú; `Meters49` quản lý công tơ, điểm đo, đoạn tiêu thụ và cảnh báo; `Billing49` lập và khóa chứng từ; `Finance49` vận hành sổ tiền thật và sổ cái quỹ; `Settlements49` xử lý trả phòng và quyết toán; `Reports49` tổng hợp báo cáo và công nợ; `Catalog49` quản lý danh mục có danh sách trường cho phép (whitelist); `Files49` quản lý tệp đính kèm có kiểm quyền và `ContractPdf` chịu trách nhiệm xuất PDF hợp đồng/phụ lục.",
    )

    add_h2(doc, "3.4. Thiết kế cơ sở dữ liệu 49 bảng (PostgreSQL 18.6 — V14)")
    add_h3(doc, "3.4.1. Tổ chức 8 phân hệ dữ liệu và các chỉ số vật lý")
    add_para(
        doc,
        "Theo kết quả đối chiếu trực tiếp từ metadata hệ thống PostgreSQL 18.6 (`docs/database-validation.json`), lược đồ hiện hành ở phiên bản V14 gồm **49 bảng nghiệp vụ, 461 cột, 49 khóa chính (PK), 152 khóa ngoại (FK), 133 ràng buộc CHECK, 7 ràng buộc EXCLUDE và 10 View dẫn xuất** (không tính bảng `flyway_schema_history` và các schema lưu trữ `prototype_v9`, `model_v10`). Số lượng 49 bảng không xuất phát từ mong muốn làm phức tạp, mà là kết quả tự nhiên của việc chuẩn hóa tách lịch sử giá, lịch sử cư trú, tách bên ký với người thực ở, và tách chứng từ phải thu khỏi tiền thật.",
    )
    add_figure(
        doc,
        ASSETS / "diag-03-db-8modules.png",
        "Phân bố 49 bảng cơ sở dữ liệu theo 8 phân hệ chức năng và mở rộng V14",
        max_width_cm=15.6,
        add_border=False,
    )
    add_para(
        doc,
        "Hình 4 phóng cận sơ đồ thực thể – mối kết hợp (ERD) của hai phân hệ cốt lõi nhất là **Hợp đồng – Bàn giao (đã cập nhật V14)** và **Chứng từ – Sổ quỹ tài chính**. Trên sơ đồ thể hiện rõ cách dòng hư hỏng trên biên bản trả phòng (`handover_items`) tự tham chiếu về dòng nhận phòng gốc (`original_item_id`) và trở thành căn cứ khóa ngoại trực tiếp (`handover_item_id`) cho dòng bồi thường trong `billing_items`, sau đó được thanh toán hoặc cấn cọc qua `fund_entries` mà không tạo ra bảng tổng dư thừa.",
    )
    add_figure(
        doc,
        ASSETS / "diag-04-erd-core-finance.png",
        "Sơ đồ ERD lõi: Hợp đồng – Bàn giao V14 và Chứng từ – Sổ quỹ tài chính (Append-only Ledger)",
        max_width_cm=15.6,
        add_border=False,
    )

    add_h3(doc, "3.4.2. Chuẩn hóa dữ liệu, bảo toàn snapshot lịch sử và 10 View dẫn xuất")
    add_para(
        doc,
        "Trong quá trình chuẩn hóa lên 3NF, nhóm phân biệt rạch ròi giữa **dữ liệu dư thừa gây dị thường cập nhật** (bắt buộc loại bỏ) với **ảnh chụp lịch sử (snapshot)** cần cố định để hóa đơn và biên bản quá khứ không bao giờ bị biến đổi khi danh mục thay đổi về sau:",
    )
    add_styled_table(
        doc,
        headers=["Nguyên tắc thiết kế", "Vấn đề tránh được", "Cách hiện thực trong mô hình 49 bảng"],
        rows=[
            [
                "Đạt 1NF, 2NF, 3NF",
                "Lưu nhiều ảnh vào một chuỗi; lặp họ tên khách trên nhiều bảng; lưu trùng `room_id`/`property_id` ở nhiều chỗ dễ lệch nhau.",
                "Mỗi tệp một dòng ở `stored_files` liên kết qua `file_attachments` (10 cột FK đích có CHECK đúng 1 cột khác NULL); hồ sơ người thuê tập trung tại `tenant_profiles`; phòng của hợp đồng suy qua `contracts.room_id`.",
            ],
            [
                "Bảo toàn Snapshot có chủ đích",
                "Chủ nhà tăng giá điện từ 3.500 đ lên 4.000 đ hoặc đổi tên đồ đạc làm thay đổi ngược số tiền trên hóa đơn các tháng trước.",
                "`billing_items` lưu `item_name_snapshot`, `unit_name_snapshot`, `unit_price_snapshot`, `quantity_snapshot` khi phát hành; `handover_items` lưu `item_name_snapshot` tại lúc bàn giao; `contract_terms` lưu nguyên bản các thông tin hai bên cho từng phiên bản hợp đồng/phụ lục.",
            ],
            [
                "Không lưu cột tổng hợp hay số dư chỉnh tay",
                "Cột `total_amount`, `paid_amount`, `balance` dễ lệch với các dòng chi tiết khi có điều chỉnh, đảo bút toán hoặc thu nhiều lần.",
                "Mọi con số tổng hợp được tính nhất quán qua **10 View dẫn xuất**: `v_contract_current`, `v_room_status`, `v_segments`, `v_billing_lines`, `v_fund_movements`, `v_fund_balances`, `v_item_applications`, `v_item_balances`, `v_document_balances`, `v_maintenance`.",
            ],
        ],
        col_widths_cm=[3.2, 4.8, 8.0],
        caption="Nguyên tắc chuẩn hóa dữ liệu, bảo toàn lịch sử và sử dụng View dẫn xuất",
        font_size=9.8,
    )

    add_h3(doc, "3.4.3. Chi tiết các mở rộng cơ sở dữ liệu tại Flyway V13 và V14")
    add_para(
        doc,
        "Để đáp ứng yêu cầu chống thu lặp và bổ sung tính năng bàn giao/xuất PDF của giảng viên mà không phá vỡ 49 bảng cốt lõi, nhóm bổ sung hai migration nhỏ gọn nhưng có giá trị nghiệp vụ cao:",
    )
    add_bullet(
        doc,
        "Bổ sung `cash_transactions.payment_document_id` (khóa ngoại tới `billing_documents`) giúp ràng buộc một mã phiếu thu của hóa đơn này không thể tái sử dụng cho hóa đơn khác ngay cả trong cùng một hợp đồng; đồng thời thêm `meter_readings.correction_item_id` để khi lập chứng từ điều chỉnh sai chỉ số đã phát hành, điểm đo điều chỉnh mới làm số đầu chuẩn cho kỳ kế tiếp mà vẫn giữ nguyên điểm đo gốc.",
        bold_prefix="Migration V13 (`V13__billing_integrity_and_receipt_intent.sql`): ",
    )
    add_bullet(
        doc,
        "Bổ sung cột tự tham chiếu `handover_items.original_item_id REFERENCES handover_items(id)` kèm ràng buộc `CHECK (original_item_id IS DISTINCT FROM id)` và partial unique index `uq_handover_item_original (handover_id, original_item_id)` đảm bảo mỗi món đồ lúc nhận chỉ ánh xạ 1–1 sang đúng một dòng kiểm kê lúc trả. Đồng thời bổ sung 5 cột trên `contract_terms` (`lessor_name`, `lessor_identity`, `lessor_phone`, `lessor_address`, `tenant_address`) để mỗi phiên bản hợp đồng hay phụ lục lưu độc lập thông tin hai bên phục vụ in PDF.",
        bold_prefix="Migration V14 (`V14__contract_handover_details.sql`): ",
    )

    add_h2(doc, "3.5. Thiết kế các luồng nghiệp vụ trọng tâm và thuật toán")

    add_h3(doc, "3.5.1. Quy tắc khoảng thời gian nửa mở [vào, ra) và tính phí minh bạch")
    add_para(
        doc,
        "Mọi khoảng hiệu lực cư trú và tính tiền phòng trong hệ thống đều tuân theo quy ước toán học khoảng nửa mở `[service_from, service_to)`. Ví dụ: khách trả phòng ngày 16/09 thì `service_to = 2026-09-16`, hệ thống chỉ tính tiền đến hết ngày 15/09 (15 ngày); khách mới nhận phòng ngay ngày 16/09 sẽ bắt đầu từ `2026-09-16`, nhờ đó hai khách đổi phòng giữa tháng không bao giờ bị tính trùng ngày 16/09.",
    )
    add_para(
        doc,
        "Với dịch vụ tính theo ngày hoặc đầu người (`PRORATED`), mẫu số luôn lấy đúng số ngày thực tế của tháng tính phí (28, 29, 30 hoặc 31 ngày) chứ không chia cứng cho 30. Với dịch vụ tính theo đầu người, tử số là tổng số **ngày-người thực ở** cộng dồn từ bảng `contract_residencies` (người đứng tên ký hợp đồng nhưng không trực tiếp ở sẽ không tự động bị cộng ngầm một suất). Khi lập dòng hóa đơn, công thức đầy đủ được lưu vào trường `basis_note` (ví dụ `(347.000 - 265.000) kWh × 3500.0000 = 287000.00 VND`) để cả chủ nhà lẫn người thuê nhìn vào là kiểm chứng được ngay.",
    )

    add_h3(doc, "3.5.2. Thuật toán phát hiện điện nước bất thường WEIGHTED_DAILY")
    add_para(
        doc,
        "Một hạn chế của cách so sánh thô 'lượng dùng tháng này với tháng trước' là nếu kỳ trước ghi chỉ số sau 28 ngày còn kỳ này ghi sau 34 ngày, hoặc khách mới dọn vào thay cho nhóm khách cũ có số người khác hẳn, hệ thống sẽ báo động giả liên tục. Do đó, tại phương thức `Meters49.analyze(long segmentId)`, nhóm cài đặt thuật toán so sánh **mức tiêu thụ bình quân theo ngày có trọng số thời gian (`WEIGHTED_DAILY`)**:",
    )
    add_bullet(
        doc,
        "Lấy chính sách cảnh báo hiệu lực mới nhất trong `alert_policies` theo độ ưu tiên cấp phòng ghi đè cấp khu (`ORDER BY (room_id IS NOT NULL) DESC, effective_from DESC LIMIT 1`).",
        bold_prefix="Bước 1 — Chọn ngưỡng cấu hình: ",
    )
    add_bullet(
        doc,
        "Truy vấn các đoạn tiêu thụ hợp lệ (chưa bị thay thế qua `supersedes_segment_id`) của cùng phòng và cùng dịch vụ trong cửa sổ `history_months` tháng liền trước thời điểm bắt đầu đoạn hiện tại.",
        bold_prefix="Bước 2 — Thu thập lịch sử đối chiếu: ",
    )
    add_bullet(
        doc,
        "Tính mức tiêu thụ bình quân ngày lịch sử bằng tổng lượng tiêu thụ lịch sử chia cho tổng số ngày đo lịch sử: `baseline_daily_average = Σ(consumption_i) / Σ(duration_days_i)` (làm tròn 6 chữ số thập phân). Toàn bộ ID các đoạn lịch sử tham gia phép tính được ghi vào bảng `alert_baseline_sources` để về sau luôn truy vết lại được vì sao cảnh báo đó xuất hiện.",
        bold_prefix="Bước 3 — Tính chuẩn nền và lưu căn cứ: ",
    )
    add_bullet(
        doc,
        "Phân loại rõ 4 tình huống: (a) nếu độ dài đoạn đo nhỏ hơn `minimum_days` hoặc số kỳ lịch sử chưa đủ `history_months` → gắn nhãn `INSUFFICIENT_HISTORY` (không chặn hóa đơn); (b) nếu trong cửa sổ lịch sử có sự thay đổi hợp đồng/nhóm khách thuê → gắn nhãn `INCOMPARABLE` (chỉ cung cấp ngữ cảnh tham khảo); (c) nếu cùng nhóm thuê và mức dùng ngày hiện tại `current_daily` vượt quá `baseline_daily_average × (1 + threshold_percentage / 100)` (hoặc vượt `absolute_daily_threshold` khi nền bằng 0) → tạo cảnh báo trạng thái **`PENDING`**; (d) ngược lại là bình thường.",
        bold_prefix="Bước 4 — Đánh giá và phân loại trạng thái: ",
    )
    add_para(
        doc,
        "Khi một đoạn tiêu thụ đang có cảnh báo `PENDING`, nút phát hành hóa đơn sẽ bị chặn ở cả backend lẫn thông báo rõ trên giao diện cho đến khi người quản lý kiểm tra lại công tơ (hoặc kiểm tra rò rỉ nước) rồi chuyển cảnh báo sang `CONFIRMED` hoặc `DISMISSED` kèm ghi chú, hoặc sửa lại chỉ số sai trước khi lập lại hóa đơn nháp. Hình 5 mô tả trọn vẹn chuỗi tương tác này từ lúc ghi chỉ số đến khi phát hành hóa đơn và ghi nhận thanh toán.",
    )
    add_figure(
        doc,
        ASSETS / "diag-05-seq-billing-alert.png",
        "Biểu đồ tuần tự luồng ghi chỉ số, đánh giá cảnh báo bất thường, phát hành hóa đơn và thanh toán",
        max_width_cm=15.6,
        add_border=False,
    )

    add_h3(doc, "3.5.3. Tách biệt Chứng từ phải thu – Tiền thật – Quỹ khách")
    add_para(
        doc,
        "Để số liệu tài chính không bị rối giữa *doanh thu đã lập hóa đơn*, *tiền mặt thực vào két/ngân hàng* và *tiền cọc đang giữ hộ khách*, hệ thống tách bạch ba tầng ghi sổ:",
    )
    add_bullet(
        doc,
        "`billing_documents` và `billing_items` trả lời câu hỏi *'khách có nghĩa vụ phải trả bao nhiêu và vì khoản gì?'* (gồm 6 loại chứng từ: `MONTHLY`, `FINAL_CHARGE`, `COMPENSATION`, `ADJUSTMENT`, `REIMBURSEMENT`, `OPENING_CHARGE`).",
        bold_prefix="Tầng nghĩa vụ chứng từ: ",
    )
    add_bullet(
        doc,
        "`cash_accounts` (quỹ tiền mặt `CASH-1`, tài khoản ngân hàng `BANK-SKYHOME`...) và `cash_transactions` trả lời câu hỏi *'thực tế hôm nay có bao nhiêu tiền chảy vào (`IN`) hoặc chi ra (`OUT`)?'*. Thao tác cấn trừ tiền cọc vào hóa đơn không bao giờ tạo phiếu thu tiền mặt mới.",
        bold_prefix="Tầng dòng tiền thật: ",
    )
    add_bullet(
        doc,
        "`fund_accounts` (phân loại `DEPOSIT`, `ADVANCE`, `RESERVATION`, `UNAPPLIED`) và `fund_entries` (chỉ ghi thêm với các loại `RECEIVE`, `PAYMENT`, `TRANSFER`, `REFUND`, `CREDIT_TO_FUND`, `ADJUSTMENT_RELEASE`, `REVERSAL`, `OPENING_BALANCE`) liên kết tiền thật với từng dòng hóa đơn. Nhờ đó, trên màn hình Tổng quan và Báo cáo luôn phân biệt rành mạch `Thực tế đã thu (gạch nợ hóa đơn)` với `Tiền vào quỹ (kể cả thu cọc, trả trước)` và `Hoàn quỹ (OUT)`.",
        bold_prefix="Tầng sổ cái quỹ tiền khách: ",
    )

    add_h3(doc, "3.5.4. Luồng trả phòng, đối chiếu đồ đạc và quyết toán hoàn cọc")
    add_para(
        doc,
        "Thay vì gộp tất cả vào một nút 'Trả phòng & hoàn cọc' duy nhất dễ nhầm lẫn giữa tiền dự kiến hoàn và tiền đã thực chi, nhóm thiết kế luồng trả phòng thành bốn bước có điểm dừng kiểm tra rõ ràng như Hình 6: (1) lập biên bản trả phòng `MOVE_OUT` đối chiếu từng món đồ với biên bản nhận phòng `MOVE_IN`, đính ảnh minh chứng và khóa biên bản; (2) nếu có đồ hư hỏng/thiếu phụ kiện thì phát hành chứng từ bồi thường `COMPENSATION` liên kết trực tiếp với `handover_item_id`; (3) ghi nhận trả phòng vật lý (`checkout`) đóng quyền sử dụng phòng và tạo hồ sơ quyết toán nháp; (4) duyệt quyết toán (`checkout/approve`) để tự động cấn trừ nợ/bồi thường từ tiền trả trước và tiền cọc, sau đó mới thực hiện lệnh chi hoàn thực tế (`refund`) khi đã chuyển tiền cho khách.",
    )
    add_figure(
        doc,
        ASSETS / "diag-06-seq-checkout-handover.png",
        "Biểu đồ tuần tự luồng trả phòng, đối chiếu bàn giao đồ đạc V14, lập bồi thường và quyết toán",
        max_width_cm=15.6,
        add_border=False,
    )


def build_chapter_4(doc: docx.Document):
    add_chapter(doc, "Chương 4. Hiện thực hệ thống, giao diện và kiểm thử")

    add_h2(doc, "4.1. Tổ chức mã nguồn và cấu trúc triển khai")
    add_para(
        doc,
        "Mã nguồn dự án được tổ chức thống nhất trong một kho lưu trữ Git (`quanlytro`), chia thành ba nhánh thư mục chính: backend Java (`src/main/java/com/hostel/`), migration & tài nguyên nhúng (`src/main/resources/`), và giao diện React (`frontend/src/`). Bảng 6 tóm tắt cấu trúc các gói mã nguồn trọng tâm ở phiên bản hiện hành.",
    )
    add_styled_table(
        doc,
        headers=["Thư mục / Tệp nguồn", "Số tệp / Quy mô", "Vai trò chính trong hệ thống"],
        rows=[
            ["`com.hostel.model49.*`", "14 lớp dịch vụ/API\n(~6.500 dòng)", "Toàn bộ logic V14: `Api49`, `Access49`, `Leases49`, `Meters49`, `Billing49`, `Finance49`, `Settlements49`, `Reports49`, `Catalog49`, `Operations49`, `Files49`, `Events49`, `Recovery49`, `Input49`."],
            ["`com.hostel.model49.entity`", "49 lớp Entity", "Ánh xạ 1–1 toàn bộ 49 bảng dữ liệu phục vụ chế độ `spring.jpa.hibernate.ddl-auto=validate`."],
            ["`com.hostel.config` & `.security`", "8 lớp cấu hình/bảo mật", "`SecurityConfig`, `JwtFilter`, `SessionRegistry`, `ApiTimingAspect` (AOP), `AdminBootstrap`, `ModelPersistence`."],
            ["`com.hostel.service` & `.exception`", "14 lớp tiện ích/lỗi", "`Database`, `ContractPdf` (369 dòng), `PdfService`, nhóm `FeeCalculator`, `ApiErrors` (ánh xạ 22 lỗi trigger DB)."],
            ["`src/main/resources/db/migration`", "14 file SQL\n(`V1` → `V14`)", "Chuỗi nâng cấp lược đồ forward-only từ prototype đến mô hình 49 bảng V14 kèm trigger, view và kiểm tra di trú."],
            ["`frontend/src/*`", "5 tệp nguồn\n(~4.780 dòng)", "`main.jsx` (điều hướng & màn hình lõi), `Model49Panels.jsx` (báo cáo, công nợ, đồ đạc phòng, bàn giao, hợp đồng PDF, vận hành), `api.js`, `style.css`, `tokens.css`."],
            ["`src/test/java/com/hostel/*`", "13 bộ kiểm thử\n(131 test)", "Kiểm thử toàn vẹn nghiệp vụ, HTTP bảo mật, tải ảnh, xuất PDF, di trú Flyway và tranh chấp đồng thời."],
        ],
        col_widths_cm=[4.5, 3.0, 8.5],
        caption="Tổ chức cấu trúc mã nguồn chính của hệ thống (phiên bản V14)",
        font_size=9.8,
    )

    add_h2(doc, "4.2. Hiện thực một số xử lý kỹ thuật tiêu biểu")
    add_para(
        doc,
        "Thay vì chép lại toàn bộ mã nguồn dài dòng, mục này trích dẫn và phân tích bốn đoạn xử lý tiêu biểu thể hiện rõ các vấn đề kỹ thuật thực tế mà nhóm gặp phải và giải quyết trong quá trình lập trình.",
    )

    add_h3(doc, "4.2.1. Phân loại khoản thu tránh lỗi locale tiếng Việt trong PostgreSQL")
    add_para(
        doc,
        "Trong quá trình kiểm thử màn hình Tổng quan và Báo cáo trên PostgreSQL cài đặt trên Windows với cấu hình mặc định `--locale=C`, nhóm phát hiện một lỗi khá kín: biểu thức `lower('Điện sinh hoạt')` trong SQL không hạ chữ `'Đ'` tiếng Việt về `'đ'`, nên điều kiện `LIKE '%điện%'` bị bỏ sót, làm dòng tiền điện bị đẩy nhầm sang nhóm dịch vụ khác (`services_other`) và lệch tổng đối chiếu 6 chỉ số. Ngoài ra, nếu một dòng bồi thường hỏng thiết bị (`item_kind = 'DAMAGE'`) có tên chứa chữ 'điện' (như *'Bồi thường ổ cắm điện'*), phép lọc theo tên đơn thuần sẽ cộng nhầm vào tiền điện công tơ.",
    )
    add_para(
        doc,
        "Nhóm đã viết lại biểu thức phân loại loại trừ lẫn nhau thông qua `CROSS JOIN LATERAL` trong `Reports49.java`, vừa kiểm tra cả `'Đ'` lẫn `'đ'` độc lập với locale của hệ điều hành, vừa ưu tiên chặn nhóm `DAMAGE`/`PENALTY` trước khi nhận diện điện nước:",
    )
    add_code_block(
        doc,
        "Trích đoạn 1 — Phân loại khoản thu loại trừ lẫn nhau tại Reports49.java",
        """private static final String ITEM_CLASSIFICATION =
    " cross join lateral ("
    + " select case"
    + "   when i.item_kind = 'RENT' then 'RENT'"
    + "   when i.item_kind in ('DAMAGE', 'PENALTY', 'OTHER') then 'OTHER'"
    + "   when i.item_kind = 'UTILITY' and ("
    + "        lower(i.item_name_snapshot) like '%điện%'"
    + "     or i.item_name_snapshot like '%Điện%'"
    + "     or lower(i.item_name_snapshot) like '%dien%') then 'ELECTRICITY'"
    + "   when i.item_kind = 'UTILITY' and ("
    + "        lower(i.item_name_snapshot) like '%nước%'"
    + "     or i.item_name_snapshot like '%Nước%'"
    + "     or lower(i.item_name_snapshot) like '%nuoc%') then 'WATER'"
    + "   else 'OTHER' end as metric_bucket"
    + " ) cls";""",
    )

    add_h3(doc, "4.2.2. Ánh xạ lỗi Trigger PostgreSQL sang mã HTTP 409 rõ nghĩa")
    add_para(
        doc,
        "Khi ràng buộc nghiệp vụ đặt trong Trigger hoặc `EXCLUDE` của PostgreSQL bị vi phạm (ví dụ: cố sửa hóa đơn đã phát hành, cấn cọc vượt số dư khả dụng, hoặc ghi chỉ số nằm ngoài khoảng cư trú), PostgreSQL ném ra ngoại lệ `DataAccessException` kèm chuỗi kỹ thuật rất dài. Để không để lộ tên bảng/câu SQL ra giao diện người dùng mà vẫn trả đúng ngữ nghĩa HTTP `409 Conflict` kèm thông báo tiếng Việt dễ hiểu, lớp `ApiErrors.java` phân tích nguyên nhân gốc (`rootCause`) và ánh xạ có kiểm soát 22 thông điệp trigger nghiệp vụ.",
    )
    add_code_block(
        doc,
        "Trích đoạn 2 — Bắt và ánh xạ lỗi toàn vẹn dữ liệu sang HTTP 409 tại ApiErrors.java",
        """@ExceptionHandler({DataIntegrityViolationException.class, DataAccessException.class})
public ResponseEntity<Map<String, Object>> database(DataAccessException ex) {
    String raw = rootMessage(ex);
    String message = translateDatabaseError(raw); // Ánh xạ 22 lỗi trigger/constraint sang tiếng Việt
    HttpStatus status = isBusinessConflict(raw) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status).body(Map.of("error", message));
}""",
    )

    add_h3(doc, "4.2.3. Khóa đồng thời và bảo vệ quy trình xác nhận biên bản bàn giao")
    add_para(
        doc,
        "Khi rà soát an ninh luồng bàn giao đồ đạc, nhóm phát hiện bảng `handovers` nằm trong nhóm tài nguyên quản trị danh mục `Catalog49`, nên về lý thuyết một yêu cầu `PUT /api/catalog/handovers/{id}` gửi trực tiếp `status: 'CONFIRMED'` có thể đi vòng qua bước kiểm tra đủ hiện trạng đồ đạc của endpoint chuyên dụng `POST /api/handovers/{id}/confirm`. Nhóm đã chặn triệt để nhánh đi tắt này ngay trong `Catalog49.java` (cả thao tác `create` và `update`) để bắt buộc mọi biên bản muốn khóa xác nhận đều phải đi qua hàm kiểm tra đủ hiện trạng, người xác nhận và ghi vết kiểm toán.",
    )

    add_h3(doc, "4.2.4. Đo hiệu năng tự động các REST API bằng AspectJ AOP")
    add_para(
        doc,
        "Lớp `ApiTimingAspect` áp dụng lập trình hướng khía cạnh bao quanh tất cả các `@RestController`, giúp theo dõi thời gian thực thi từng yêu cầu trong log phát triển mà không làm rối mã nghiệp vụ:",
    )
    add_code_block(
        doc,
        "Trích đoạn 3 — Theo dõi thời gian xử lý API bằng AOP tại ApiTimingAspect.java",
        """@Aspect
@Component
public class ApiTimingAspect {
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object time(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        try {
            return call.proceed();
        } finally {
            LoggerFactory.getLogger(ApiTimingAspect.class).debug(
                "API {} took {} ms", call.getSignature().toShortString(),
                (System.nanoTime() - start) / 1_000_000);
        }
    }
}""",
    )

    add_h2(doc, "4.3. Giao diện ứng dụng thực tế và văn bản xuất PDF")
    add_para(
        doc,
        "Toàn bộ ảnh chụp dưới đây được ghi nhận trực tiếp từ bản chạy thực tế V14 trên cơ sở dữ liệu PostgreSQL 18.6 với kịch bản dữ liệu mẫu *Tòa nhà SkyHome Central* gồm 4 phòng (P101 đang thuê đã gia hạn phụ lục đổi ngày thu sang ngày 8; P102 đang thuê có đột biến điện nước tháng 01/2026 và còn nợ hóa đơn tháng 12/2025; P201 vừa trả phòng có đối chiếu đồ đạc hỏng điều hòa và lập bồi thường 350.000 đ; P202 đang trống).",
    )

    add_figure(
        doc,
        ASSETS / "ui-login.png",
        "Màn hình đăng nhập xác thực JWT phân quyền theo vai trò",
        max_width_cm=13.5,
    )

    add_figure(
        doc,
        ASSETS / "ui-dashboard-month.png",
        "Màn hình Tổng quan chế độ Theo Tháng (kỳ 2026-01): 4 thẻ KPI, 6 chỉ số thu–điện–nước và cảnh báo phòng P102",
        max_width_cm=14.6,
        max_height_cm=16.5,
    )
    add_para(
        doc,
        "Tại màn hình Tổng quan tháng 2026-01 (Hình 8), các con số đối chiếu khớp tuyệt đối đến từng đồng: tổng hóa đơn phát hành kỳ là 4.087.000 ₫, tách đúng thành 4 phần bằng nhau gồm Thu trước hạn (1.021.750 ₫), Thu đúng hạn (1.021.750 ₫), Thu sau hạn (1.021.750 ₫) và Chưa thu (1.021.750 ₫), bên cạnh hai ô tách riêng Tiền điện (287.000 ₫) và Tiền nước (250.000 ₫). Dòng giải thích ngay dưới khối thẻ còn nêu rõ tổng *Tiền vào quỹ* và *Hoàn quỹ* theo ngày giao dịch thực tế. Khi người quản lý chuyển sang chế độ **Theo Quý** (Quý 4 / 2025, Hình 9), toàn bộ 6 chỉ số tự động cộng gộp theo quý (22.835.000 ₫ lập hóa đơn, 20.722.500 ₫ thực thu, 27.422.500 ₫ tiền vào quỹ bao gồm các khoản cọc thực nhận).",
    )
    add_figure(
        doc,
        ASSETS / "ui-dashboard-quarter.png",
        "Màn hình Tổng quan khi chuyển sang chế độ Theo Quý (Quý 4 / 2025)",
        max_width_cm=15.2,
    )

    add_figure(
        doc,
        ASSETS / "ui-reports.png",
        "Màn hình Báo cáo Doanh thu & Dòng tiền hỗ trợ nhóm Tháng/Quý/Năm và xuất PDF",
        max_width_cm=15.4,
        max_height_cm=15.5,
    )
    add_para(
        doc,
        "Màn hình Báo cáo (Hình 10) trực quan hóa chênh lệch giữa doanh số lập hóa đơn và số thực thu từng kỳ bằng biểu đồ cột, đi kèm bảng chi tiết phân tách Tiền phòng, Tiền điện, Tiền nước, phân loại thời điểm thu và hai cột dòng tiền thực (*Tiền vào quỹ* / *Hoàn quỹ*). Chẳng hạn tháng 2025-08 chưa phát hành hóa đơn (0 ₫) nhưng vẫn hiển thị Tiền vào quỹ 3.800.000 ₫ — phản ánh đúng khoản tiền cọc phòng P201 đã thực thu vào két.",
    )

    add_figure(
        doc,
        ASSETS / "ui-debts.png",
        "Màn hình Quản lý Dư nợ Khách hàng (mở rộng chi tiết hóa đơn nợ của phòng P102)",
        max_width_cm=15.2,
        max_height_cm=14.8,
    )
    add_para(
        doc,
        "Ở màn hình Công nợ khách (Hình 11), hệ thống gom theo từng khách thuê đang mở nợ (3.134.250 ₫ trên 3 khách), hiển thị số ngày quá hạn, cho phép xuất CSV và mở chi tiết từng chứng từ nợ (như `INV-2-2025-12-01` của Lê Thị Bích còn nợ 1.762.500 ₫) để bấm thu tiền ngay kèm chọn tài khoản quỹ tiền mặt hoặc ngân hàng.",
    )

    add_figure(
        doc,
        ASSETS / "ui-meter-alerts.png",
        "Màn hình Điện nước: cảnh báo bất thường P102 được đẩy lên đầu và tô màu nổi bật",
        max_width_cm=15.2,
    )
    add_para(
        doc,
        "Tại màn hình Điện nước (Hình 12), hai chỉ số bất thường kỳ 2026-01 của phòng P102 (nước tăng +236,2% so với chuẩn nền 0,32 m³/ngày; điện tăng +202,1% so với chuẩn nền 2,43 kWh/ngày) được tự động đưa lên đầu bảng, tô nền cam nhạt kèm hộp *Cảnh báo cần kiểm tra* phía trên để người quản lý xử lý trước khi phát hành hóa đơn.",
    )

    add_figure(
        doc,
        ASSETS / "ui-invoice-detail.png",
        "Chi tiết hóa đơn minh bạch INV-1-2026-01-01 hiển thị rõ công thức từng dòng và 3 đợt thanh toán",
        max_width_cm=15.2,
        max_height_cm=15.0,
    )
    add_figure(
        doc,
        ASSETS / "ui-room-assets.png",
        "Chi tiết phòng P101 hiển thị danh mục Đồ đạc & Tài sản bố trí trong phòng",
        max_width_cm=15.2,
        max_height_cm=14.5,
    )
    add_figure(
        doc,
        ASSETS / "ui-contract-p101.png",
        "Chi tiết hợp đồng HD-P101-AN: ngày thu kỳ mới (Ngày 8 hàng tháng) và lịch sử 2 phiên bản điều khoản",
        max_width_cm=15.2,
        max_height_cm=14.8,
    )
    add_figure(
        doc,
        ASSETS / "ui-contract-p201-handover.png",
        "Đối chiếu biên bản trả phòng HO-OUT-3 của phòng P201: so sánh Lúc nhận – Lúc trả và ảnh minh chứng hư hỏng",
        max_width_cm=15.2,
        max_height_cm=14.8,
    )
    add_para(
        doc,
        "Hình 16 minh họa rõ tính năng đối chiếu đồ đạc V14 trên hợp đồng `HD-P201-CUONG`: mỗi món đồ trong biên bản trả phòng `HO-OUT-3` đặt cạnh hiện trạng lúc nhận ban đầu (*Giường ngủ gỗ Sồi 1.8m*: hoạt động tốt → trầy xước nhẹ; *Điều hòa Inverter Daikin*: hoạt động tốt, đầy đủ phụ kiện → hư cánh vẫy, thiếu điều khiển, kèm ảnh chụp minh chứng `hien-trang-tra-phong-mo-phong.png`). Từ chính dòng hỏng điều hòa này, nhân viên phát hành chứng từ bồi thường `COMP-HO-8` trị giá 350.000 ₫, và phần quyết toán trả phòng tính rõ cọc đang giữ 3.800.000 ₫, công nợ hiện tại 350.000 ₫, dự kiến còn phải hoàn 3.450.000 ₫.",
    )

    add_side_by_side_figures(
        doc,
        ASSETS / "ui-contract-pdf-v2.png",
        ASSETS / "ui-mobile-dashboard.png",
        "Trang 1 Phụ lục Hợp đồng PDF xuất thật từ hệ thống (trái) và giao diện di động 390px không vỡ khung (phải)",
        left_w_cm=9.4,
        right_w_cm=5.2,
        add_border=True,
    )
    add_para(
        doc,
        "Hình 17 cho thấy hai kết quả thực tế ở đầu ra: bên trái là trang đầu của tệp PDF phụ lục hợp đồng `HD-P101-AN` (phiên bản 2) do `ContractPdf.java` sinh trực tiếp từ dữ liệu PostgreSQL (tự động đặt tiêu đề *PHỤ LỤC HỢP ĐỒNG THUÊ PHÒNG TRỌ*, thể hiện đủ Bên A/Bên B, giá thuê mới 3.600.000 ₫/tháng, lịch thu Ngày 8 hằng tháng · Thu cuối kỳ và bảng trang thiết bị bàn giao); bên phải là giao diện khi mở trên khung nhìn điện thoại 390×844 px, toàn bộ thẻ KPI và nút điều khiển xếp gọn theo chiều dọc, không tràn ngang.",
    )

    add_h2(doc, "4.4. Kiểm thử hệ thống và kết quả kiểm chứng định lượng")
    add_para(
        doc,
        "Để tránh tình trạng 'chạy thử bằng tay vài nút rồi kết luận đạt', nhóm tổ chức kiểm thử thành ba lớp tách biệt, mỗi lớp lưu bằng chứng kết quả riêng trên bản V14 (cập nhật ngày 24/11/2026).",
    )

    add_h3(doc, "4.4.1. Kiểm thử ràng buộc cơ sở dữ liệu trực tiếp trên PostgreSQL 18.6")
    add_para(
        doc,
        "Kịch bản `scripts/verify-model49.py` tạo một cơ sở dữ liệu trắng trên PostgreSQL 18.6, nạp `docs/quanlytro.sql` cùng `docs/database-rules.sql`, đối chiếu metadata (khớp chính xác 49 bảng, 461 cột, 49 PK, 152 FK, 133 CHECK, 7 EXCLUDE, 10 View), rồi chạy **20 ca kiểm tra SQL** (`docs/database-cases49.sql`) cố tình vi phạm từng quy tắc nghiệp vụ để xác nhận PostgreSQL chủ động từ chối đúng kỳ vọng (cấm chồng lịch khai thác phòng, cấm chồng cư trú, cấm sửa hóa đơn `ISSUED`, cấm sửa/xóa sổ cái `fund_entries`, cấm cấn vượt số dư khả dụng khi có tạm giữ, cấm gắn nhiều hơn 1 FK đích trên `file_attachments`, cấm tạo vòng tham chiếu `original_item_id` trên `handover_items`, v.v.). Kết quả: **20/20 ca SQL PASS** (`docs/database-validation.json`).",
    )

    add_h3(doc, "4.4.2. Kiểm thử tự động tầng ứng dụng, API, di trú và đồng thời (131/131 PASS)")
    add_para(
        doc,
        "Bộ kiểm thử JUnit 5 + Spring Boot Test + MockMvc chạy trực tiếp trên cụm PostgreSQL riêng biệt (không dùng H2 giả lập để đảm bảo hành vi trigger, GiST và khóa dòng giống hệt thực tế). Bảng 7 tổng hợp kết quả thực thi đầy đủ của cả 13 bộ kiểm thử theo `docs/implementation-validation49.json`.",
    )
    add_styled_table(
        doc,
        headers=["STT", "Bộ kiểm thử (JUnit 5)", "Số ca", "Đạt", "Phạm vi kiểm chứng trọng tâm"],
        rows=[
            ["1", "`BusinessIntegrityTests`", "35", "35", "Toàn vẹn V14: phân loại 6 chỉ số không trùng điện/nước, `payment_cycle_day`, xuất PDF hợp đồng/báo cáo, chặn xác nhận tắt bàn giao, đối chiếu `MOVE_OUT`, quyền tài sản phòng."],
            ["2", "`Model49Tests`", "21", "21", "Luồng 49 bảng: hợp đồng, gia hạn, thu dư vào `ADVANCE`, cấn cọc, hoàn tiền, tạm giữ, điều chỉnh chỉ số sau phát hành và kiểm thử tranh chấp đồng thời nhiều luồng."],
            ["3", "`OpenKosWorkflowTests`", "8", "8", "Kịch bản đối chiếu OpenKOS: hết hạn giấy chưa trả phòng, điện nước ranh giới đổi khách, thay công tơ, trả phòng sớm."],
            ["4", "`Model49HttpTests`", "4", "4", "Kiểm thử HTTP end-to-end với JWT thật, kiểm tra phạm vi `STAFF` và từ chối truy cập chéo khu/phòng."],
            ["5", "`BusinessMigrationTests`", "1", "1", "Kiểm chứng nâng cấp Flyway nối tiếp toàn bộ chuỗi `V1` → `V14` không lỗi."],
            ["6", "`Model49MigrationTests`", "3", "3", "Kiểm chứng di trú dữ liệu `V10` → `V11` → `V14`, đối soát tổng tiền và khóa ghi `model_v10`."],
            ["7", "`MigrationTests`", "4", "4", "Kiểm chứng chuyển đổi lịch sử `V1`–`V10` và bảo lưu `prototype_v9`."],
            ["8", "`BusinessWorkflowTests`", "38", "38", "Bộ hồi quy nghiệp vụ nền: lập hóa đơn, thanh toán, cảnh báo, quyết toán, sự cố."],
            ["9", "`HttpSecurityTests`", "6", "6", "Xác thực JWT, xoay vòng Refresh Token, thu hồi phiên khi khóa tài khoản/đổi mật khẩu."],
            ["10", "`ImageWorkflowTests`", "5", "5", "Xác thực ảnh bằng `ImageIO`, từ chối tệp giả mạo định dạng, kiểm quyền tải tệp."],
            ["11", "`FeeCalculatorTests`", "3", "3", "Kiểm thử đơn vị đa hình tính phí phòng, điện, nước, dịch vụ và làm tròn `HALF_UP`."],
            ["12", "`PdfTests`", "2", "2", "Kiểm thử xuất PDF hợp đồng v1 và tiêu đề trang 2+ phụ lục v2 (`PHỤ LỤC HỢP ĐỒNG`)."],
            ["13", "`HostelManagementApplicationTests`", "1", "1", "Kiểm tra khởi động Spring Context và xác thực Hibernate `ddl-auto=validate`."],
            ["", "**Tổng cộng**", "**131**", "**131**", "**68 ca V14 hiện hành · 8 ca Migration · 55 ca hồi quy & đơn vị (0 lỗi, 0 bỏ qua)**"],
        ],
        col_widths_cm=[1.0, 5.8, 1.2, 1.2, 6.8],
        caption="Kết quả thực thi 13 bộ kiểm thử tự động Java/Spring Boot trên PostgreSQL 18.6",
        align_cols=[
            WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT,
            WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT,
        ],
        font_size=9.4,
    )

    add_h3(doc, "4.4.3. Kiểm thử trình duyệt E2E và các lỗi thực tế đã phát hiện, xử lý")
    add_para(
        doc,
        "Ngoài kiểm thử backend, nhóm chạy kịch bản tự động trên trình duyệt Chrome (Playwright) đi qua toàn bộ 13 màn hình nghiệp vụ (*Tổng quan*, *Báo cáo*, *Công nợ khách*, *Khu nhà*, *Phòng*, *Người thuê*, *Hợp đồng*, *Điện nước*, *Hóa đơn*, *Sự cố*, *Thông báo*, *Thiết lập*, *Vận hành*) và khung nhìn điện thoại 390×844 px, đồng thời chạy lặp hai lần script nạp dữ liệu demo (`scripts/seed-defense.mjs`) để xác nhận tính lũy đẳng (idempotent) và 0 lỗi JavaScript trên console.",
    )
    add_para(
        doc,
        "Bảng 8 ghi nhận sáu lỗi thực tế tiêu biểu mà nhóm tự phát hiện được trong giai đoạn rà soát cuối trước bảo vệ và cách nhóm xử lý triệt để kèm test khóa hồi quy:",
    )
    add_styled_table(
        doc,
        headers=["STT", "Lỗi thực tế phát hiện", "Nguyên nhân gốc", "Cách xử lý & Kiểm chứng"],
        rows=[
            ["1", "Tổng `Tiền điện + Nước + Phòng + Dịch vụ` trên báo cáo bị đếm trùng điện vào dịch vụ khác.", "PostgreSQL Windows chạy `--locale=C` không hạ chữ `'Đ'` tiếng Việt khi gọi `lower(...)`.", "Thay bằng `ITEM_CLASSIFICATION` loại trừ lẫn nhau tại `Reports49.java` và thêm test khẳng định đẳng thức tổng."],
            ["2", "Chi tiết hợp đồng P101 hiển thị thiếu ngày đóng tiền định kỳ (`Ngày 8 hàng tháng`).", "Frontend đọc nhầm tên thuộc tính JSON và `Leases49.detail()` thiếu fallback cho hợp đồng `DRAFT`.", "Đồng bộ trường `payment_cycle_day` ở cả backend lẫn `Model49Panels.jsx`; kiểm tra trực tiếp trên P101."],
            ["3", "Trang 2 của phụ lục hợp đồng PDF vẫn in tiêu đề đầu trang là `HỢP ĐỒNG` thay vì `PHỤ LỤC`.", "Hàm vẽ header lặp trang mới trong `ContractPdf.java` ghi cứng chuỗi tiêu đề mặc định.", "Truyền biến `title` động vào header trang 2+ và bổ sung kiểm tra trong `PdfTests.java`."],
            ["4", "Có thể gọi API danh mục `PUT /catalog/handovers` để đặt thẳng `status='CONFIRMED'`.", "Endpoint CRUD chung chưa chặn riêng trạng thái chốt của bảng `handovers`.", "Chặn tại `Catalog49.java`, buộc mọi thao tác xác nhận đi qua `POST /api/handovers/{id}/confirm`."],
            ["5", "Hộp thoại thu tiền trên màn hình Hóa đơn và Công nợ chưa cho chọn tài khoản tiền mặt / ngân hàng.", "Frontend mặc định lấy tài khoản quỹ đầu tiên, không lộ danh sách `/catalog/cash-accounts`.", "Bổ sung dropdown chọn `cashAccountId` (`CASH-1` / `BANK-SKYHOME`) và tự điền giờ hiện tại Việt Nam."],
            ["6", "Tài khoản `TENANT` khi xem chi tiết phòng vẫn thấy nút thêm/gỡ đồ đạc trên giao diện (dù backend đã chặn `403`).", "Thành phần `RoomAssetsPanel` chưa nhận cờ `tenant` để ẩn nút thao tác quản trị.", "Truyền `tenant` prop ẩn nút ở UI đồng thời siết chặt `access.action(id, \"MANAGE\")` ở `Api49.java`."],
        ],
        col_widths_cm=[1.0, 4.3, 4.7, 6.0],
        caption="Một số lỗi thực tế phát hiện trong quá trình rà soát trước bảo vệ và hướng xử lý",
        align_cols=[
            WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT,
            WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT,
        ],
        font_size=9.5,
    )


def build_chapter_5(doc: docx.Document):
    add_chapter(doc, "Chương 5. Đánh giá kết quả, giới hạn và hướng phát triển")

    add_h2(doc, "5.1. Đánh giá kết quả đạt được")
    add_para(
        doc,
        "Đối chiếu với mục tiêu ban đầu của học phần và các yêu cầu bổ sung của giảng viên, nhóm gehihi đã hoàn thiện trọn vẹn sản phẩm từ khâu phân tích nghiệp vụ, chuẩn hóa cơ sở dữ liệu, hiện thực backend/frontend đến kiểm thử tự động:",
    )
    add_bullet(
        doc,
        "Thiết kế và triển khai thành công mô hình cơ sở dữ liệu 49 bảng (461 cột, 152 FK, 133 CHECK, 7 GiST EXCLUDE, 10 View) quản lý tiến hóa xuyên suốt 14 phiên bản Flyway (`V1–V14`) mà không phá vỡ dữ liệu lịch sử.",
        bold_prefix="Về dữ liệu và tính toàn vẹn: ",
    )
    add_bullet(
        doc,
        "Giải quyết gọn các tình huống nghiệp vụ khó thường gây sai tiền: tách tiền cọc cam kết khỏi tiền thực giữ, tự động tách phần khách chuyển dư sang quỹ trả trước, khóa hóa đơn đã phát hành, cảnh báo tiêu thụ điện nước bất thường theo chuẩn nền `WEIGHTED_DAILY`, đối chiếu đồ đạc nhận–trả gắn ảnh minh chứng và quyết toán tách biệt với chi hoàn thực tế.",
        bold_prefix="Về chiều sâu nghiệp vụ: ",
    )
    add_bullet(
        doc,
        "Đáp ứng đầy đủ 7 nhóm yêu cầu bổ sung của giảng viên (báo cáo tháng/quý/năm + PDF, 6 chỉ số tổng quan, theo dõi công nợ quá hạn, cảnh báo nổi bật kèm đơn vị đo, đồ đạc phòng, đối chiếu bàn giao và xuất PDF hợp đồng/phụ lục tiếng Việt).",
        bold_prefix="Về mức độ hoàn thiện chức năng: ",
    )
    add_bullet(
        doc,
        "Có bộ kiểm chứng lặp lại được bất cứ lúc nào: 131/131 test Java, 20/20 ca SQL và kiểm thử giao diện Chrome 13 màn hình + di động đều đạt 100%.",
        bold_prefix="Về chất lượng kiểm thử: ",
    )

    add_h2(doc, "5.2. Những hạn chế hiện tại")
    add_para(
        doc,
        "Nhóm nhìn nhận thẳng thắn các giới hạn hiện tại của sản phẩm để không đánh giá quá phạm vi đã kiểm chứng:",
    )
    add_bullet(
        doc,
        "Mô hình hiện tại thiết kế cho một chủ nhà quản lý nhiều khu và nhiều phòng; chưa hỗ trợ kiến trúc đa chủ nhà (multi-tenant) dùng chung một phiên bản triển khai độc lập về dữ liệu.",
        bold_prefix="Phạm vi đơn chủ nhà: ",
    )
    add_bullet(
        doc,
        "Các giao dịch thanh toán hiện vẫn do chủ nhà hoặc nhân viên xác minh thủ công theo sao kê ngân hàng/tiền mặt; hệ thống chưa tích hợp webhook đối soát tự động (như VietQR/SePay/PayOS).",
        bold_prefix="Chưa tự động đối soát ngân hàng: ",
    )
    add_bullet(
        doc,
        "Thông báo phát hành hóa đơn và cập nhật sự cố hiện hiển thị trong ứng dụng (`notifications`), chưa kết nối dịch vụ gửi tin nhắn Zalo OA, SMS hay Email ra ngoài.",
        bold_prefix="Kênh thông báo nội bộ: ",
    )
    add_bullet(
        doc,
        "Khi sửa sai chỉ số công tơ của một hóa đơn đã phát hành mà các kỳ tiếp theo cũng đã phát hành phụ thuộc vào chỉ số đó, API hiện từ chối sửa dây chuyền tự động để tránh làm biến đổi hàng loạt chứng từ cũ và yêu cầu người quản lý đối soát rõ từng kỳ.",
        bold_prefix="Sửa dây chuyền nhiều kỳ đã phát hành: ",
    )
    add_bullet(
        doc,
        "Một số nghiệp vụ tài chính nâng cao như đảo bút toán (`REVERSAL`), chuyển cọc giữa hai hợp đồng liên tiếp hoặc điều chỉnh từng dòng lẻ đã có đầy đủ REST API và kiểm thử backend nhưng chưa xây dựng giao diện biểu mẫu riêng trên web.",
        bold_prefix="Giao diện cho nghiệp vụ kế toán ít gặp: ",
    )

    add_h2(doc, "5.3. Hướng phát triển tiếp theo")
    add_para(
        doc,
        "Nếu tiếp tục mở rộng hệ thống để đưa vào vận hành thực tế cho các dãy trọ thương mại, nhóm định hướng phát triển theo bốn bước ưu tiên:",
    )
    add_bullet(
        doc,
        "Tích hợp sinh mã VietQR động kèm mã hóa đơn và nhận webhook biến động số dư để tự động đối khớp tạo phiếu thu `cash_transactions` (tận dụng ngay cơ chế chống trùng `external_reference` sẵn có ở V13).",
        bold_prefix="1. Tự động hóa thu tiền bằng VietQR & Webhook: ",
    )
    add_bullet(
        doc,
        "Kết nối hàng đợi gửi thông báo qua Email hoặc Zalo OA định kỳ trước ngày đến hạn đóng tiền và ngay khi hóa đơn chuyển sang `ISSUED`.",
        bold_prefix="2. Mở rộng kênh thông báo ra ngoài ứng dụng: ",
    )
    add_bullet(
        doc,
        "Hoàn thiện màn hình quản trị chuyên biệt cho thao tác đảo bút toán sai, chuyển cọc khi khách đổi phòng và lịch sử kiểm toán (`audit_events`) trực quan hơn trên giao diện.",
        bold_prefix="3. Bổ sung giao diện cho các nghiệp vụ điều chỉnh nâng cao: ",
    )
    add_bullet(
        doc,
        "Thiết lập kịch bản sao lưu định kỳ đồng bộ cả bản dump PostgreSQL và thư mục tệp ảnh/PDF đính kèm (`uploads`), kèm kiểm thử diễn tập phục hồi trên máy chủ thật.",
        bold_prefix="4. Chuẩn hóa vận hành và sao lưu phục hồi: ",
    )


def build_references_and_appendices(doc: docx.Document):
    # Tài liệu tham khảo
    add_chapter(doc, "Tài liệu tham khảo", page_break=True)
    refs = [
        "[1] Spring Team, *Spring Boot Reference Documentation (v4.1)*, VMware / Broadcom, 2026. Truy cập tại: https://docs.spring.io/spring-boot/",
        "[2] Spring Team, *Spring Security Reference & Architecture*, VMware / Broadcom, 2026. Truy cập tại: https://docs.spring.io/spring-security/reference/",
        "[3] The PostgreSQL Global Development Group, *PostgreSQL 18 Documentation — Constraints, Exclusion Constraints (btree_gist), Triggers and Explicit Locking*, 2026. Truy cập tại: https://www.postgresql.org/docs/18/",
        "[4] Redgate / Flyway, *Flyway Database Migrations Documentation*, 2026. Truy cập tại: https://documentation.red-gate.com/fd",
        "[5] The Apache Software Foundation, *Apache PDFBox 3.0.x Documentation*, 2026. Truy cập tại: https://pdfbox.apache.org/",
        "[6] Meta Open Source, *React 19 Documentation* & *Vite 7 Guide*, 2026. Truy cập tại: https://react.dev/ và https://vite.dev/",
        "[7] senatroxx, *OpenKOS — Boarding House Management System (Apache-2.0, snapshot aeb3544)*, GitHub. Tham khảo đối chiếu luồng đo công tơ và khấu trừ cọc: https://github.com/senatroxx/OpenKos",
        "[8] jwh3times, *LeaseBook — Rental Property Accounting & ADR-011 Deposit/Prepayment Policy (AGPL-3.0)*, GitHub. Tham khảo đối chiếu tách quỹ cọc/trả trước và kiểm thử đồng thời: https://github.com/jwh3times/LeaseBook",
        "[9] Resident Việt Nam, *Tài liệu quy trình quản lý nhà trọ công khai*, 2026. Tham khảo đối chiếu quy trình vận hành thực tế tại Việt Nam: https://resident.vn/",
    ]
    for item in refs:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.left_indent = Cm(0.9)
        p.paragraph_format.first_line_indent = Cm(-0.9)
        p.paragraph_format.line_spacing = 1.25
        p.paragraph_format.space_after = Pt(5)
        import re
        for tok in re.split(r"(\*[^*]+\*)", item):
            if not tok:
                continue
            if tok.startswith("*") and tok.endswith("*"):
                r = p.add_run(tok[1:-1])
                set_run_font(r, size_pt=12, italic=True, color=INK)
            else:
                r = p.add_run(tok)
                set_run_font(r, size_pt=12, color=INK)

    # Phụ lục
    add_chapter(doc, "Phụ lục", page_break=True)

    add_h2(doc, "Phụ lục A. Danh mục tóm tắt 49 bảng cơ sở dữ liệu theo 8 phân hệ (V14)")
    add_styled_table(
        doc,
        headers=["Phân hệ", "Số bảng", "Danh sách tên bảng vật lý trong PostgreSQL"],
        rows=[
            ["A. Tài khoản & Quyền", "6", "`users`, `tenant_profiles`, `employee_assignments`, `staff_permission_grants`, `password_reset_tokens`, `audit_events`"],
            ["B. Khu, Phòng & Tài sản", "7", "`properties`, `rooms`, `room_price_history`, `room_operating_periods`, `asset_types`, `assets`, `asset_placements`"],
            ["C. Hợp đồng & Bàn giao", "12", "`reservations`, `contracts`, `contract_parties`, `contract_contacts`, `contract_residencies`, `contract_terms` (+5 cột V14), `room_possessions`, `move_out_notices`, `handovers`, `handover_items` (+`original_item_id` V14), `contract_violations`, `checkout_records`"],
            ["D. Dịch vụ & Điện nước", "9", "`service_items`, `service_price_configs`, `contract_service_subscriptions`, `meters`, `meter_readings`, `consumption_segments`, `alert_policies`, `consumption_alerts`, `alert_baseline_sources`"],
            ["E. Sửa chữa & Bảo trì", "4", "`maintenance_requests`, `maintenance_events`, `maintenance_costs`, `maintenance_schedules`"],
            ["F. Chứng từ & Dòng tiền", "7", "`billing_documents`, `billing_items`, `cash_accounts`, `cash_transactions`, `fund_accounts`, `fund_entries`, `fund_holds`"],
            ["G. Thông báo", "2", "`notifications`, `notification_recipients`"],
            ["H. Tệp & Chứng cứ ảnh", "2", "`stored_files`, `file_attachments`"],
        ],
        col_widths_cm=[3.9, 1.7, 10.4],
        caption="Tổng hợp 49 bảng vật lý theo 8 phân hệ ở phiên bản V14",
        align_cols=[WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT],
        font_size=9.5,
    )

    add_h2(doc, "Phụ lục B. Các nhóm REST API chính của hệ thống (tiền tố /api)")
    add_styled_table(
        doc,
        headers=["Nhóm nghiệp vụ", "Các endpoint đại diện", "Ghi chú nghiệp vụ"],
        rows=[
            ["Xác thực & Tài khoản", "`POST /auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/change-password`, `/users/{id}/reset-code`", "JWT + Refresh Token Rotation; mã khôi phục 1 lần hết hạn 15 phút."],
            ["Hợp đồng & Bàn giao", "`POST /contracts`, `/{id}/activate`, `/{id}/renew`, `/{id}/possessions`, `/{id}/handovers`, `PUT /handovers/{id}/items/{itemId}`, `POST /handovers/{id}/confirm`, `POST /handovers/{id}/damage-compensation`, `GET /contracts/{id}/pdf?version=n`", "Tách nhận/trả thực tế; đối chiếu `MOVE_OUT` tự nạp `original_item_id`; xuất PDF bản ký và phụ lục."],
            ["Tài sản phòng & Tệp", "`GET/POST /rooms/{id}/assets`, `POST /rooms/{id}/assets/{placementId}/remove`, `GET/POST /files/{kind}/{id}`, `GET /uploads/{key}`", "Chỉ ADMIN tạo tài sản mới/gỡ bố trí; xác thực `ImageIO` + kiểm quyền khi tải tệp."],
            ["Điện nước & Cảnh báo", "`POST /meter-readings`, `/meter-points`, `/consumption-segments/{id}/revise`, `/meters/{id}/replace`, `GET /alerts`, `PUT /alerts/{id}`", "Chặn chỉ số giảm; tự đánh giá `WEIGHTED_DAILY` và lưu `alert_baseline_sources`."],
            ["Hóa đơn, Sổ quỹ & Quyết toán", "`POST /invoices`, `/billing-documents/{id}/issue`, `/billing-items/{id}/meter-corrections`, `/invoices/{id}/payments`, `/cash-transactions`, `/fund-accounts/{id}/refund`, `POST /contracts/{id}/checkout`, `/checkout/approve`", "Khóa hóa đơn `ISSUED`; chống gửi lặp theo `code` & `externalReference`; tiền dư vào `ADVANCE`."],
            ["Tổng quan & Báo cáo", "`GET /dashboard?period=|quarter=`, `/reports/revenue`, `/revenue-quarterly`, `/revenue-yearly`, `/revenue/pdf?group=`, `/reports/tenant-debts`", "Tách 6 chỉ số thu/điện/nước; tách doanh số lập hóa đơn với dòng tiền vào/ra quỹ."],
        ],
        col_widths_cm=[3.2, 7.6, 5.2],
        caption="Tóm tắt các nhóm REST API trọng yếu của phiên bản V14",
        font_size=9.3,
    )

    add_h2(doc, "Phụ lục C. Hướng dẫn kiểm thử và khởi chạy bản trình diễn cục bộ")
    add_para(
        doc,
        "Hệ thống có thể được biên dịch, kiểm thử toàn diện và khởi chạy bản trình diễn cục bộ trên Windows từ thư mục gốc `quanlytro` bằng các lệnh sau (yêu cầu JDK 21, Node.js 20+ và PostgreSQL 18):",
        indent=False,
    )
    add_code_block(
        doc,
        "Lệnh biên dịch giao diện, chạy 131 test Java, 20 ca SQL và khởi chạy demo",
        """# 1. Đóng gói giao diện React vào src/main/resources/static
cd frontend; npm.cmd run build; cd ..

# 2. Chạy toàn bộ 131 kiểm thử Java/HTTP/Migration/Concurrency trên PostgreSQL riêng
powershell -ExecutionPolicy Bypass -File scripts/test-backend-business.ps1

# 3. Kiểm chứng lược đồ 49 bảng và 20 ca ràng buộc SQL trực tiếp trên PostgreSQL
python scripts/verify-model49.py --port 55473

# 4. Khởi chạy bản trình diễn cục bộ (http://127.0.0.1:8091) và nạp dữ liệu bảo vệ
powershell -ExecutionPolicy Bypass -File scripts/run-model49-review.ps1
node scripts/seed-defense.mjs""",
    )


def main():
    doc = docx.Document()
    configure_base_styles(doc)
    build_cover(doc)
    build_front_matter(doc)
    build_chapter_1(doc)
    build_chapter_2(doc)
    build_chapter_3(doc)
    build_chapter_4(doc)
    build_chapter_5(doc)
    build_references_and_appendices(doc)

    OUT_DOCX.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(OUT_DOCX))
    print(f"Wrote {OUT_DOCX} ({OUT_DOCX.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
