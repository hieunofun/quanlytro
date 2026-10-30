"""Create an editable, 14-slide business/database presentation without notes."""
from pathlib import Path
import json
import re
import zipfile

from pptx import Presentation
from pptx.chart.data import CategoryChartData
from pptx.dml.color import RGBColor
from pptx.enum.chart import XL_CHART_TYPE, XL_LABEL_POSITION
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN, MSO_AUTO_SIZE
from pptx.oxml.xmlchemy import OxmlElement
from pptx.util import Inches, Pt

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "thiet-ke-csdl-va-phan-tich-nghiep-vu.pptx"
BG, WHITE, INK = "F4F7FA", "FFFFFF", "172B42"
NAVY, TEAL, MUTED, BORDER = "122C45", "007D78", "53697D", "DCE5ED"
PALE, CORAL = "E7F4F2", "C44A39"
WIDTH, HEIGHT = 13.333333, 7.5
TOTAL = 14

groups = [
    ("Tài khoản & phân quyền", ["users", "auth_sessions", "password_reset_codes", "employee_assignments", "room_employee_assignments"]),
    ("Khu nhà & phòng", ["properties", "floors", "rooms", "room_images", "room_price_history"]),
    ("Người thuê & hợp đồng", ["tenant_profiles", "contracts", "contract_members", "contract_violations"]),
    ("Dịch vụ & điện nước", ["service_items", "service_price_configs", "alert_thresholds", "meter_readings", "meter_reading_history", "consumption_alerts"]),
    ("Hóa đơn & quyết toán", ["invoices", "invoice_items", "payment_transactions", "checkout_records"]),
    ("Vận hành & lịch sử", ["maintenance_requests", "maintenance_schedules", "notifications", "audit_logs", "uploaded_images"]),
]
sql_files = sorted((ROOT / "src/main/resources/db/migration").glob("V*.sql"))
actual_tables = []
for sql_file in sql_files:
    actual_tables.extend(re.findall(r"CREATE\s+TABLE\s+(\w+)", sql_file.read_text(encoding="utf-8-sig"), re.I))
assert len(sql_files) == 9 and len(actual_tables) == 29
assert set(actual_tables) == {name for _, names in groups for name in names}
assert "UNIQUE (contract_id, billing_period, meter_type)" in next(p for p in sql_files if p.name.startswith("V9__")).read_text(encoding="utf-8-sig")

prs = Presentation()
prs.slide_width, prs.slide_height = Inches(WIDTH), Inches(HEIGHT)
prs.core_properties.title = "Hệ thống quản lý nhà trọ — Phân tích nghiệp vụ và thiết kế CSDL"
prs.core_properties.subject = "Nội dung dựa trên tài liệu V1 và schema PostgreSQL V1–V9 của repo quanlytro"
prs.core_properties.author = "Dự án Quản lý nhà trọ"
prs.core_properties.keywords = "nghiệp vụ, PostgreSQL, ERD, hóa đơn, nhà trọ"


def rgb(value):
    return RGBColor.from_string(value)


def shape(slide, x, y, w, h, fill=WHITE, line=None, kind=MSO_SHAPE.ROUNDED_RECTANGLE):
    obj = slide.shapes.add_shape(kind, Inches(x), Inches(y), Inches(w), Inches(h))
    obj.fill.solid()
    obj.fill.fore_color.rgb = rgb(fill)
    if line:
        obj.line.color.rgb = rgb(line)
        obj.line.width = Pt(0.8)
    else:
        obj.line.fill.background()
    if kind == MSO_SHAPE.ROUNDED_RECTANGLE:
        obj.adjustments[0] = 0.12
    return obj


def text(slide, x, y, w, h, value, size=20, color=INK, bold=False, font="Arial", align=PP_ALIGN.LEFT):
    obj = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = obj.text_frame
    tf.clear()
    tf.word_wrap = True
    tf.auto_size = MSO_AUTO_SIZE.NONE
    tf.margin_left = tf.margin_right = 0
    tf.margin_top = tf.margin_bottom = 0
    for i, line in enumerate(value.split("\n")):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = line
        p.alignment = align
        p.font.name = font
        p.font.size = Pt(size)
        p.font.bold = bold
        p.font.color.rgb = rgb(color)
        p.line_spacing = 1.0 if font == "Consolas" else 1.12
        p.space_before = Pt(0)
        p.space_after = Pt((1 if font == "Consolas" else 5) if i < len(value.split("\n")) - 1 else 0)
    return obj


def line(slide, x1, y1, x2, y2, color=BORDER, width=1.2, arrow=False):
    obj = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    obj.line.color.rgb = rgb(color)
    obj.line.width = Pt(width)
    if arrow:
        end = OxmlElement("a:tailEnd")
        end.set("type", "triangle")
        obj.line._get_or_add_ln().append(end)
    return obj


def route(slide, points, label=None, label_box=None):
    for i in range(len(points) - 1):
        line(slide, *points[i], *points[i + 1], color=MUTED, width=1.6, arrow=i == len(points) - 2)
    if label and label_box:
        x, y, w = label_box
        shape(slide, x, y, w, 0.27, BG, kind=MSO_SHAPE.RECTANGLE)
        text(slide, x, y + 0.015, w, 0.25, label, 12, MUTED, align=PP_ALIGN.CENTER)


def base(title, section, source, subtitle=None):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = rgb(BG)
    shape(slide, 0, 0, WIDTH, 0.075, TEAL, kind=MSO_SHAPE.RECTANGLE)
    text(slide, 0.65, 0.28, 11.8, 0.28, section.upper(), 11, TEAL, True)
    text(slide, 0.65, 0.73, 12.0, 0.63, title, 31, NAVY, True)
    if subtitle:
        text(slide, 0.65, 1.37, 12.0, 0.34, subtitle, 15.5, MUTED)
    line(slide, 0.65, 6.98, 12.68, 6.98)
    text(slide, 0.65, 7.12, 11.4, 0.2, "QUẢN LÝ NHÀ TRỌ  ·  " + source, 9.2, MUTED)
    text(slide, 12.0, 7.10, 0.65, 0.24, f"{len(prs.slides):02d} / {TOTAL:02d}", 10, MUTED, align=PP_ALIGN.RIGHT)
    return slide


def card(slide, x, y, w, h, title, body, size=19, accent=TEAL, title_size=21):
    shape(slide, x, y, w, h, WHITE, BORDER)
    shape(slide, x + 0.18, y + 0.22, 0.045, 0.3, accent, kind=MSO_SHAPE.RECTANGLE)
    text(slide, x + 0.36, y + 0.19, w - 0.62, 0.58, title, title_size, INK, True)
    text(slide, x + 0.26, y + 0.87, w - 0.52, h - 1.04, body, size)


def band(slide, y, value, fill=PALE, color=TEAL, size=18, height=0.48):
    shape(slide, 0.65, y, 12.03, height, fill)
    obj = text(slide, 0.87, y, 11.6, height, value, size, color, True)
    obj.text_frame.vertical_anchor = MSO_ANCHOR.MIDDLE


def table(slide, x, y, widths, rows, heights, size=18, total_row=False):
    obj = slide.shapes.add_table(len(rows), len(widths), Inches(x), Inches(y), Inches(sum(widths)), Inches(sum(heights)))
    tbl = obj.table
    for col, width in zip(tbl.columns, widths):
        col.width = Inches(width)
    for r, values in enumerate(rows):
        tbl.rows[r].height = Inches(heights[r])
        for c, value in enumerate(values):
            cell = tbl.cell(r, c)
            last = total_row and r == len(rows) - 1
            cell.fill.solid()
            cell.fill.fore_color.rgb = rgb(NAVY if r == 0 else PALE if last else WHITE if r % 2 else "EDF2F7")
            cell.margin_left = cell.margin_right = Inches(0.15)
            cell.margin_top = cell.margin_bottom = Inches(0.06)
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
            tf = cell.text_frame
            tf.clear()
            tf.word_wrap = True
            tf.auto_size = MSO_AUTO_SIZE.NONE
            p = tf.paragraphs[0]
            p.text = value
            p.font.name = "Arial"
            p.font.size = Pt(size)
            p.font.bold = r == 0 or last
            p.font.color.rgb = rgb(WHITE if r == 0 else TEAL if last else INK)
            p.space_after = Pt(0)
            if c == len(values) - 1 and total_row:
                p.alignment = PP_ALIGN.RIGHT
    return obj


def entity(slide, x, y, w, h, title, fields, highlight=False):
    shape(slide, x, y, w, h, WHITE, BORDER)
    shape(slide, x, y, w, 0.43, TEAL if highlight else NAVY, kind=MSO_SHAPE.RECTANGLE)
    text(slide, x + 0.12, y + 0.08, w - 0.24, 0.29, title, 15.5, WHITE, True, "Consolas")
    text(slide, x + 0.14, y + 0.55, w - 0.28, h - 0.63, "\n".join(fields), 13.5, INK, font="Consolas")


# 01 — Title
s = prs.slides.add_slide(prs.slide_layouts[6])
s.background.fill.solid()
s.background.fill.fore_color.rgb = rgb(NAVY)
shape(s, 9.35, 0, WIDTH - 9.35, HEIGHT, "183B54", kind=MSO_SHAPE.RECTANGLE)
shape(s, 0.75, 0.72, 2.26, 0.43, TEAL)
text(s, 0.9, 0.82, 2.0, 0.25, "ĐỒ ÁN JAVA / WEB", 12, WHITE, True)
text(s, 0.75, 1.62, 8.0, 1.75, "HỆ THỐNG\nQUẢN LÝ NHÀ TRỌ", 44, WHITE, True)
text(s, 0.78, 3.52, 8.0, 1.15, "PHÂN TÍCH NGHIỆP VỤ\n& THIẾT KẾ CƠ SỞ DỮ LIỆU", 27, "8ED8D0", True)
text(s, 0.78, 5.25, 7.9, 0.86, "Hóa đơn minh bạch • Cảnh báo điện nước\nQuản lý luồng thuê phòng và công nợ", 21, "DCE7F0")
for y, n, label in [(1.83, "03", "Vai trò người dùng"), (3.34, "29", "Bảng nghiệp vụ"), (4.85, "09", "Migration dữ liệu")]:
    text(s, 10.0, y, 2.4, 0.72, n, 42, WHITE, True)
    text(s, 10.0, y + 0.8, 2.65, 0.4, label, 16, "B9D3E4")
text(s, 0.78, 7.03, 10.8, 0.25, "Java 21  /  Spring Boot  /  React  /  PostgreSQL", 13, "B9D3E4")
text(s, 12.0, 7.05, 0.65, 0.25, "01 / 14", 10, "B9D3E4", align=PP_ALIGN.RIGHT)

# 02 — Problem and goal
s = base("Bài toán quản lý nhà trọ", "Phân tích nghiệp vụ", "Tài liệu nguồn: docs/source/v1.docx", "Phạm vi: một chủ nhà quản lý khoảng 1–3 khu nhà trọ hoặc chung cư mini.")
card(s, 0.65, 1.94, 5.89, 3.9, "Vấn đề cần giải quyết", "• Hồ sơ phòng, người thuê và hợp đồng phân tán.\n• Tính điện nước, dịch vụ thủ công dễ sai.\n• Khó đối chiếu đơn giá và công nợ cũ.\n• Khó theo dõi sự cố và người xử lý.", 21)
card(s, 6.79, 1.94, 5.89, 3.9, "Mục tiêu hệ thống", "• Tập trung dữ liệu vào một CSDL.\n• Kiểm soát toàn bộ vòng đời thuê phòng.\n• Hiển thị rõ từng khoản thu.\n• Lưu lịch sử và giới hạn quyền truy cập.", 21)
band(s, 6.1, "Trọng tâm: hóa đơn có thể đối chiếu, dữ liệu có lịch sử, quyền được kiểm tra ở backend.", size=18)

# 03 — Actors
s = base("Tác nhân và phạm vi quyền", "Phân tích nghiệp vụ", "v1.docx · security/Access.java", "Ba vai trò dùng chung hệ thống nhưng truy cập theo phạm vi khác nhau.")
card(s, 0.65, 1.94, 3.84, 4.25, "Chủ nhà · ADMIN", "Toàn bộ hệ thống\nQuản lý tài khoản và phân công.\nQuản lý phòng, hợp đồng, giá.\nLập hóa đơn, thu tiền.\nXem báo cáo và xử lý sự cố.", 19)
card(s, 4.745, 1.94, 3.84, 4.25, "Nhân viên · STAFF", "Khu hoặc phòng được giao\nQuản lý hồ sơ và hợp đồng.\nGhi điện nước, lập hóa đơn.\nGhi nhận thanh toán.\nTiếp nhận, xử lý sự cố.", 19)
card(s, 8.84, 1.94, 3.84, 4.25, "Người thuê · TENANT", "Dữ liệu của chính mình\nXem phòng và hợp đồng.\nXem hóa đơn, cách tính tiền.\nXem lịch sử thanh toán.\nGửi sự cố, nhận thông báo.", 19)
band(s, 6.38, "Backend kiểm tra đồng thời vai trò và phạm vi tài nguyên; không chỉ ẩn nút trên giao diện.", size=17.5)

# 04 — Modules
s = base("Các nhóm chức năng", "Phân tích nghiệp vụ", "v1.docx · docs/V1-COVERAGE.md")
modules = [
    ("01  Nhà trọ & phòng", "Khu nhà, tầng, phòng.\nGiá thuê, sức chứa, tiện nghi.\nẢnh và trạng thái phòng."),
    ("02  Thuê phòng", "Hồ sơ người thuê, hợp đồng.\nThành viên, gia hạn, vi phạm.\nTrả phòng, quyết toán cọc."),
    ("03  Điện nước", "Ghi chỉ số, giá theo thời gian.\nCảnh báo bất thường.\nLịch sử sửa chỉ số."),
    ("04  Hóa đơn & thu tiền", "Nháp, phát hành, xuất PDF.\nNhiều lần thanh toán.\nCông nợ và cấn trừ cọc."),
    ("05  Sự cố & bảo trì", "Tiếp nhận và giao người xử lý.\nƯu tiên, hạn xử lý, chi phí.\nLịch bảo trì định kỳ."),
    ("06  Thông báo & báo cáo", "Thông báo trong ứng dụng.\nDoanh thu, công nợ, lấp đầy.\nThống kê tình trạng sự cố."),
]
for i, (title, body) in enumerate(modules):
    card(s, 0.65 + (i % 3) * 4.095, 1.73 + (i // 3) * 2.4, 3.84, 2.17, title, body, 17, title_size=19.5)
band(s, 6.53, "Sự cố: Mới tạo → Đã tiếp nhận → Đang xử lý → Hoàn thành", size=17, height=0.34)

# 05 — Workflow, native editable shapes
s = base("Luồng nghiệp vụ chính", "Phân tích nghiệp vụ", "v1.docx · ContractService / MeterService / InvoiceService")
steps = [
    ("01", "Chuẩn bị phòng", "Khu nhà → tầng → phòng"),
    ("02", "Hồ sơ người thuê", "Người đại diện và thành viên"),
    ("03", "Lập hợp đồng", "Giá thuê, cọc, chỉ số ban đầu"),
    ("04", "Ghi điện nước", "Chốt chỉ số theo tháng"),
    ("05", "Kiểm tra cảnh báo", "Xác minh hoặc sửa chỉ số"),
    ("06", "Phát hành hóa đơn", "Kiểm tra nháp, phát hành"),
    ("07", "Ghi nhận thanh toán", "Thanh toán và công nợ"),
    ("08", "Gia hạn / trả phòng", "Gia hạn hoặc kết thúc thuê"),
    ("09", "Quyết toán tiền cọc", "Cấn cọc; hoàn tiền / thu thêm"),
]
positions = [(0.65, 1.75), (4.745, 1.75), (8.84, 1.75), (8.84, 3.36), (4.745, 3.36), (0.65, 3.36), (0.65, 4.97), (4.745, 4.97), (8.84, 4.97)]
for (num, title, body), (x, y) in zip(steps, positions):
    shape(s, x, y, 3.84, 1.13, WHITE, BORDER)
    text(s, x + 0.18, y + 0.15, 0.52, 0.5, num, 25, TEAL, True)
    text(s, x + 0.84, y + 0.13, 2.83, 0.4, title, 20, INK, True)
    text(s, x + 0.84, y + 0.64, 2.83, 0.36, body, 15.5, MUTED)
for i in range(8):
    x1, y1 = positions[i]
    x2, y2 = positions[i + 1]
    if y1 == y2:
        a, b = ((x1 + 3.84, y1 + 0.55), (x2, y2 + 0.55)) if x2 > x1 else ((x1, y1 + 0.55), (x2 + 3.84, y2 + 0.55))
    else:
        a, b = (x1 + 1.92, y1 + 1.13), (x2 + 1.92, y2)
    route(s, [a, b])
band(s, 6.42, "Bước 04–07 lặp theo tháng. Quyết toán cọc được thực hiện khi trả phòng.", size=18)

# 06 — Business rules
s = base("Quy tắc nghiệp vụ cốt lõi", "Phân tích nghiệp vụ", "ContractService · MeterService · BillingCalendar · InvoiceService")
rules = [
    ("Phòng đủ điều kiện", "Chỉ tạo hợp đồng khi phòng trống; phòng bảo trì không được cho thuê."),
    ("Không chồng thời gian", "Một phòng không có hai hợp đồng ACTIVE chồng thời gian; kiểm soát sức chứa."),
    ("Ngày ở thực tế", "Ngày kết thúc / trả phòng là mốc không tính. Trả ngày 16: tính ngày 1–15."),
    ("Kỳ và đơn giá", "Kỳ là tháng dương lịch; đơn giá dịch vụ áp dụng tại ngày đầu kỳ."),
    ("Chỉ số hợp lệ", "Chỉ số mới ≥ chỉ số cũ. Sửa kỳ cuối chưa có hóa đơn và lưu lý do."),
    ("Hóa đơn duy nhất", "Một hóa đơn / hợp đồng / kỳ. Hóa đơn phát hành giữ nguyên dữ liệu tính tiền."),
]
for i, (title, body) in enumerate(rules):
    x, y = 0.65 + (i % 2) * 6.14, 1.72 + (i // 2) * 1.51
    shape(s, x, y, 5.89, 1.32, WHITE, BORDER)
    text(s, x + 0.22, y + 0.15, 5.45, 0.39, title, 21, TEAL, True)
    text(s, x + 0.22, y + 0.63, 5.45, 0.6, body, 17.5)
band(s, 6.4, "Đổi người thuê giữa tháng: mỗi hợp đồng liên tiếp có chỉ số và hóa đơn riêng.", size=18)

# 07 — Actual demo invoice
s = base("Hóa đơn minh bạch", "Phân tích nghiệp vụ", "scripts/seed-demo.ps1 · scripts/verify-demo.ps1", "Ví dụ dữ liệu demo: phòng 203, kỳ tháng 09/2026, ở đủ tháng.")
rows = [
    ["Khoản thu", "Cách tính", "Thành tiền"],
    ["Tiền phòng", "Ở đủ tháng", "3.000.000đ"],
    ["Điện", "(200 − 100) × 3.500", "350.000đ"],
    ["Nước", "(155 − 147) × 20.000", "160.000đ"],
    ["Internet", "Theo phòng", "100.000đ"],
    ["Tiền rác", "Theo phòng", "30.000đ"],
    ["Gửi xe", "1 người, đủ tháng", "50.000đ"],
    ["TỔNG", "", "3.690.000đ"],
]
table(s, 0.65, 1.94, [1.76, 3.52, 2.31], rows, [0.52] + [0.52] * 7, 18, True)
card(s, 8.48, 1.94, 4.2, 4.16, "Các công thức", "", 17.2)
for formula_y, label, formula in [
    (2.81, "Điện / nước", "(mới − cũ) × đơn giá"),
    (3.88, "Phòng / dịch vụ theo phòng", "đơn giá × ngày ở / ngày tháng"),
    (4.95, "Dịch vụ theo người", "đơn giá × ngày-người\n/ số ngày trong tháng"),
]:
    text(s, 8.74, formula_y, 3.68, 0.36, label, 17.2, TEAL, True)
    text(s, 8.74, formula_y + 0.37, 3.68, 0.74, formula, 16.5)
band(s, 6.42, "Ngày-người = tổng số ngày ở của người đại diện và các thành viên trong kỳ.", size=18)

# 08 — Anomaly chart, editable native chart with embedded workbook
s = base("Cảnh báo tiêu thụ bất thường", "Phân tích nghiệp vụ", "MeterService.analyze · alert_thresholds", "So sánh lượng tiêu thụ hiện tại với trung bình lịch sử trong cửa sổ cấu hình.")
shape(s, 0.65, 1.94, 7.14, 4.44, WHITE, BORDER)
data = CategoryChartData()
data.categories = ["Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9"]
data.add_series("Nước (m³)", [8, 9, 10, 27])
chart = s.shapes.add_chart(XL_CHART_TYPE.COLUMN_CLUSTERED, Inches(0.9), Inches(2.25), Inches(6.57), Inches(3.85), data).chart
chart.has_legend = False
chart.has_title = False
chart.value_axis.maximum_scale = 30
chart.value_axis.minimum_scale = 0
chart.value_axis.major_unit = 10
chart.value_axis.has_major_gridlines = True
chart.value_axis.major_gridlines.format.line.color.rgb = rgb(BORDER)
chart.category_axis.tick_labels.font.name = chart.value_axis.tick_labels.font.name = "Arial"
chart.category_axis.tick_labels.font.size = chart.value_axis.tick_labels.font.size = Pt(14)
chart.category_axis.tick_labels.font.color.rgb = chart.value_axis.tick_labels.font.color.rgb = rgb(MUTED)
chart.plots[0].gap_width = 85
chart.plots[0].has_data_labels = True
chart.plots[0].data_labels.position = XL_LABEL_POSITION.OUTSIDE_END
chart.plots[0].data_labels.font.name = "Arial"
chart.plots[0].data_labels.font.size = Pt(19)
chart.plots[0].data_labels.font.color.rgb = rgb(INK)
for i, point in enumerate(chart.series[0].points):
    point.format.fill.solid()
    point.format.fill.fore_color.rgb = rgb(CORAL if i == 3 else TEAL)
    point.format.line.fill.background()
text(s, 0.99, 2.0, 3.0, 0.25, "Lượng nước sử dụng · m³", 13, MUTED)
shape(s, 8.05, 1.94, 4.63, 4.44, WHITE, BORDER)
text(s, 8.3, 2.17, 4.05, 0.38, "TRUNG BÌNH: 9 m³", 21, TEAL, True)
text(s, 8.3, 2.75, 4.05, 0.75, "+200%", 45, CORAL, True)
text(s, 8.3, 3.6, 4.05, 0.76, "(27 − 9) / 9 × 100%\nVượt ngưỡng cấu hình 50%.", 19)
text(s, 8.3, 4.58, 4.05, 1.43, "• Ưu tiên ngưỡng riêng của phòng.\n• Quản lý xác minh hoặc sửa chỉ số.\n• Cảnh báo PENDING chặn phát hành.", 17.5)
band(s, 6.54, "Thiếu lịch sử hoặc trung bình bằng 0: không đánh giá mức tăng theo phần trăm.", size=17.5, height=0.34)

# 09 — Payment and checkout
s = base("Thanh toán và quyết toán trả phòng", "Phân tích nghiệp vụ", "InvoiceService.pay · ContractService.checkout")
shape(s, 0.65, 1.75, 5.89, 4.91, WHITE, BORDER)
text(s, 0.92, 1.97, 5.3, 0.42, "Thanh toán nhiều lần", 24, TEAL, True)
for y, label, amount, color in [(2.7, "Tổng hóa đơn", "3.690.000đ", INK), (3.35, "Đã thanh toán", "2.000.000đ", TEAL), (4.0, "Còn nợ", "1.690.000đ", CORAL)]:
    text(s, 0.96, y, 2.4, 0.35, label, 19, MUTED)
    text(s, 3.4, y - 0.025, 2.78, 0.44, amount, 26, color, True, align=PP_ALIGN.RIGHT)
text(s, 0.96, 4.93, 5.2, 1.56, "• Một hóa đơn có nhiều giao dịch.\n• Không thu vượt số còn nợ.\n• Ghi nhận thủ công; chưa xác minh ngân hàng.", 19)
shape(s, 6.79, 1.75, 5.89, 4.91, WHITE, BORDER)
text(s, 7.06, 1.97, 5.3, 0.42, "Quyết toán tiền cọc", 24, TEAL, True)
shape(s, 7.06, 2.65, 5.34, 1.08, PALE)
text(s, 7.26, 2.78, 4.92, 0.91, "Tiền quyết toán =\nTiền cọc − Công nợ − Thiệt hại", 20, TEAL, True)
text(s, 7.08, 3.96, 5.25, 2.36, "• Chốt chỉ số và phát hành đủ các tháng đã ở.\n• Số dương: hoàn tiền; số âm: thu thêm.\n• Điện nước đã nằm trong nợ hóa đơn.\n• Cấn cọc lưu giao dịch DEPOSIT_OFFSET.\n• Kết thúc hợp đồng, phòng về trống.", 17.8)

# 10 — All 29 tables grouped by responsibility
s = base("Tổ chức cơ sở dữ liệu", "Thiết kế CSDL", "src/main/resources/db/migration/V1–V9", "PostgreSQL: 29 bảng nghiệp vụ, không tính flyway_schema_history.")
for i, (title, names) in enumerate(groups):
    x, y = 0.65 + (i % 3) * 4.095, 1.92 + (i // 3) * 2.43
    shape(s, x, y, 3.84, 2.26, WHITE, BORDER)
    text(s, x + 0.19, y + 0.17, 3.14, 0.42, title, 18.2, INK, True)
    text(s, x + 3.34, y + 0.14, 0.3, 0.42, str(len(names)), 24, TEAL, True, align=PP_ALIGN.RIGHT)
    text(s, x + 0.19, y + 0.66, 3.46, 1.51, "\n".join(names), 14.2, MUTED, font="Consolas")

# 11 — Accommodation ERD, native editable entities and relationships
s = base("ERD · Nhóm thuê phòng", "Thiết kế CSDL", "V2__create_core_tables · V6__billing_policy_and_member_history", "Các quan hệ chính; bảng hỗ trợ phân công, ảnh và nhật ký được trình bày ở slide 10.")
top_y, low_y, w, h = 1.97, 4.33, 2.78, 1.62
xs = [0.65, 3.79, 6.93, 10.07]
entity(s, xs[0], top_y, w, h, "properties", ["id PK", "property_code UQ", "name", "address"])
entity(s, xs[1], top_y, w, h, "floors", ["id PK", "property_id FK", "floor_number", "name"])
entity(s, xs[2], top_y, w, h, "rooms", ["id PK", "property_id FK", "floor_id FK", "status"])
entity(s, xs[3], top_y, w, h, "contracts", ["id PK", "room_id FK", "primary_tenant_id FK", "start_date / end_date"], True)
entity(s, xs[0], low_y, w, h, "users", ["id PK", "username UQ", "password_hash", "role / status"])
entity(s, xs[1], low_y, w, h, "tenant_profiles", ["id PK", "user_id FK, UQ, NULL", "full_name", "identity_number"])
entity(s, xs[3], low_y, w, h, "contract_members", ["id PK", "contract_id FK", "effective_from", "effective_to"])
for i in range(3):
    a, b = xs[i] + w, xs[i + 1]
    route(s, [(a, top_y + 0.84), (b, top_y + 0.84)], "1:N", ((a + b) / 2 - 0.22, top_y + 0.48, 0.44))
route(s, [(xs[0] + w, low_y + 0.84), (xs[1], low_y + 0.84)], "0..1 ↔ 0..1", (2.98, low_y - 0.31, 1.32))
route(s, [(5.18, low_y), (5.18, 3.97), (10.77, 3.97), (10.77, top_y + h)], "1:N", (7.4, 3.68, 0.57))
route(s, [(12.2, top_y + h), (12.2, low_y)], "1:N", (12.33, 3.86, 0.48))
text(s, 0.67, 6.16, 12.0, 0.4, "rooms.property_id tham chiếu properties; FK ghép (floor_id, property_id) bảo đảm phòng thuộc đúng tầng và khu.", 14.2, MUTED)
text(s, 0.67, 6.67, 12.0, 0.22, "PK: khóa chính   ·   FK: khóa ngoại   ·   UQ: duy nhất   ·   1:N: một–nhiều   ·   NULL: có thể trống", 12.5, MUTED)

# 12 — Financial ERD; no fictitious direct FK between readings and invoices
s = base("ERD · Điện nước và tài chính", "Thiết kế CSDL", "V2__create_core_tables · V9__mid_month_turnover_and_checkout_details", "Hợp đồng là điểm liên kết giữa chỉ số, hóa đơn và bản quyết toán.")
y1, y2, w2, h2 = 1.98, 4.28, 3.64, 1.6
x1, x2, x3 = 0.65, 4.82, 8.99
entity(s, x1, y1, w2, h2, "invoices", ["id PK / contract_id FK", "billing_period", "total_amount / paid_amount", "balance_due / status"], True)
entity(s, x2, y1, w2, h2, "contracts", ["id PK", "room_id FK", "rental_price", "deposit_amount"])
entity(s, x3, y1, w2, h2, "meter_readings", ["id PK / contract_id FK", "room_id FK / meter_type", "billing_period", "old_index / new_index"])
entity(s, x1, y2, w2, h2, "invoice_items", ["id PK / invoice_id FK", "service_item_id FK, NULL", "unit_price_snapshot", "quantity / subtotal"])
entity(s, x2, y2, w2, h2, "payment_transactions", ["id PK / invoice_id FK", "received_by FK", "amount / payment_method", "payment_date"])
entity(s, x3, y2, w2, h2, "checkout_records", ["id PK / contract_id FK,UQ", "unpaid_invoices_amount", "damage_amount", "actual_refund_amount"])
route(s, [(x2, y1 + 0.84), (x1 + w2, y1 + 0.84)], "1:N", (4.31, y1 + 0.47, 0.47))
route(s, [(x2 + w2, y1 + 0.84), (x3, y1 + 0.84)], "1:N", (8.47, y1 + 0.47, 0.47))
route(s, [(2.16, y1 + h2), (2.16, y2)], "1:N", (2.3, 3.81, 0.55))
route(s, [(3.4, y1 + h2), (3.4, 3.92), (5.62, 3.92), (5.62, y2)], "1:N", (4.19, 3.65, 0.58))
route(s, [(7.65, y1 + h2), (7.65, 3.92), (10.77, 3.92), (10.77, y2)], "1:0..1", (8.7, 3.65, 0.84))
band(s, 6.16, "Chỉ số ↔ hóa đơn: đối chiếu qua contract_id + billing_period, không có FK trực tiếp.", size=16.5, height=0.45)
text(s, 0.67, 6.71, 12.0, 0.2, "Sơ đồ rút gọn. service_item_id có thể trống, ví dụ dòng tiền phòng; recorded_by / received_by tham chiếu users.", 11.8, MUTED)

# 13 — Keys, integrity and types
s = base("Khóa và ràng buộc toàn vẹn", "Thiết kế CSDL", "V2, V3, V6, V9 · PostgreSQL constraints")
shape(s, 0.65, 1.74, 7.32, 4.83, WHITE, BORDER)
text(s, 0.92, 1.97, 6.78, 0.43, "Chống trùng và sai tham chiếu", 23, TEAL, True)
for y, label, code in [
    (2.67, "Một hóa đơn / hợp đồng / kỳ", "UNIQUE (contract_id, billing_period)"),
    (3.63, "Một chỉ số / hợp đồng / kỳ / loại", "UNIQUE (contract_id, billing_period, meter_type)"),
    (4.59, "Phòng thuộc đúng tầng và khu", "FK (floor_id, property_id) → floors(id, property_id)"),
]:
    text(s, 0.97, y, 6.7, 0.34, label, 18, INK, True)
    text(s, 0.97, y + 0.41, 6.71, 0.39, code, 15.4, MUTED, font="Consolas")
text(s, 0.97, 5.76, 6.72, 0.68, "Exclusion constraint GiST: chặn hợp đồng ACTIVE chồng thời gian trên cùng phòng.", 17.1, TEAL, True)
shape(s, 8.22, 1.74, 4.46, 4.83, WHITE, BORDER)
text(s, 8.49, 1.97, 3.92, 0.43, "CHECK & kiểu dữ liệu", 22, TEAL, True)
text(s, 8.49, 2.68, 3.92, 2.06, "• Chỉ số mới ≥ chỉ số cũ.\n• Lượng dùng = mới − cũ.\n• Đã thu ≤ tổng hóa đơn.\n• Công nợ = tổng − đã thu.\n• Ngày bắt đầu < kết thúc.", 18)
line(s, 8.49, 5.01, 12.39, 5.01)
text(s, 8.49, 5.22, 3.9, 1.11, "Tiền: NUMERIC / BigDecimal\nNgày lịch: DATE\nThời điểm: TIMESTAMPTZ", 17)

# 14 — Historical prices, snapshots, transactions and normalization
s = base("Lưu lịch sử và bảo đảm giao dịch", "Thiết kế CSDL", "PricingService · InvoiceService · ContractService · Flyway")
shape(s, 0.65, 1.75, 5.89, 4.71, WHITE, BORDER)
text(s, 0.94, 1.98, 5.31, 0.43, "Giữ nguyên hóa đơn cũ", 24, TEAL, True)
text(s, 0.94, 2.66, 5.27, 1.61, "• Giá thuê thỏa thuận lưu trong hợp đồng.\n• Giá dịch vụ có ngày bắt đầu hiệu lực.\n• Chi tiết hóa đơn lưu snapshot đơn giá, lượng dùng, chỉ số và công thức.", 19)
shape(s, 0.94, 4.58, 5.3, 1.55, PALE)
text(s, 1.16, 4.78, 4.89, 1.22, "Hóa đơn cũ: 8 × 20.000 = 160.000đ\nGiá kỳ sau: 25.000đ/m³\nHóa đơn cũ vẫn là 160.000đ", 19, TEAL, True)
shape(s, 6.79, 1.75, 5.89, 4.71, WHITE, BORDER)
text(s, 7.08, 1.98, 5.31, 0.43, "Transaction & migration", 24, TEAL, True)
text(s, 7.08, 2.66, 5.27, 3.45, "• Hóa đơn và các dòng lưu cùng transaction; lỗi thì rollback.\n• Khóa hàng hóa đơn khi thu tiền để kiểm soát thao tác đồng thời.\n• Quyết toán, cấn cọc, kết thúc hợp đồng và cập nhật phòng cùng transaction.\n• Flyway quản lý schema qua 9 migration.", 19)
band(s, 6.58, "Tách bảng theo thực thể; lưu số tổng hợp và snapshot có kiểm soát, không khẳng định toàn bộ schema đạt 3NF.", size=15.5, height=0.36)

assert len(prs.slides) == TOTAL
assert all(not slide.has_notes_slide for slide in prs.slides)
OUT.parent.mkdir(parents=True, exist_ok=True)
prs.save(OUT)

loaded = Presentation(OUT)
outside = []
for i, slide in enumerate(loaded.slides, 1):
    for obj in slide.shapes:
        if obj.left < 0 or obj.top < 0 or obj.left + obj.width > loaded.slide_width + 5 or obj.top + obj.height > loaded.slide_height + 5:
            outside.append([i, obj.name])
assert not outside, outside
with zipfile.ZipFile(OUT) as z:
    assert z.testzip() is None
    notes = [name for name in z.namelist() if name.startswith("ppt/notesSlides/")]
    assert not notes, notes
summary = {"file": str(OUT), "slides": len(loaded.slides), "business_tables": len(actual_tables), "migrations": len(sql_files), "notes_slides": 0, "outside_canvas": outside, "bytes": OUT.stat().st_size}
runtime = ROOT / ".runtime" / "presentation-review"
runtime.mkdir(parents=True, exist_ok=True)
(runtime / "validation.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(summary, ensure_ascii=False, indent=2))
