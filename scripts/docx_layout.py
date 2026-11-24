"""Print-ready academic Word (.docx) layout helpers tailored for Vietnamese university reports."""
from pathlib import Path
from PIL import Image, ImageOps
import io
import docx
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import qn, nsdecls

NAVY = RGBColor(0x1B, 0x36, 0x5D)
NAVY_HEX = "1B365D"
SUBNAVY = RGBColor(0x22, 0x3A, 0x5E)
INK = RGBColor(0x11, 0x18, 0x27)
MUTED = RGBColor(0x4B, 0x55, 0x63)
HEADER_BG = "E8EEF5"
ALT_BG = "F8FAFC"
BORDER_HEX = "B8C4D4"
CODE_BG = "F4F6F9"


def set_run_font(run, name="Times New Roman", size_pt=None, bold=None, italic=None, color=None):
    run.font.name = name
    rPr = run._r.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.insert(0, rFonts)
    for attr in ("ascii", "hAnsi", "eastAsia", "cs"):
        rFonts.set(qn(f"w:{attr}"), name)
    if size_pt is not None:
        run.font.size = Pt(size_pt)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic
    if color is not None:
        run.font.color.rgb = color


def configure_base_styles(doc: docx.Document):
    # Enable automatic field update when opened in Word as a belt-and-suspenders guard
    settings = doc.settings.element
    if settings.find(qn("w:updateFields")) is None:
        upd = OxmlElement("w:updateFields")
        upd.set(qn("w:val"), "true")
        settings.append(upd)

    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(13)
    normal.font.color.rgb = INK
    rPr = normal._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.insert(0, rFonts)
    for attr in ("ascii", "hAnsi", "eastAsia", "cs"):
        rFonts.set(qn(f"w:{attr}"), "Times New Roman")
    normal.paragraph_format.line_spacing = 1.3
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(6)
    normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    for lvl, (sz, bold, italic, color, sb, sa, pbb) in {
        "Heading 1": (15, True, False, NAVY, 4, 10, True),
        "Heading 2": (13.5, True, False, NAVY, 12, 5, False),
        "Heading 3": (13, True, True, SUBNAVY, 8, 4, False),
    }.items():
        st = doc.styles[lvl]
        st.font.name = "Times New Roman"
        st.font.size = Pt(sz)
        st.font.bold = bold
        st.font.italic = italic
        st.font.color.rgb = color
        srPr = st._element.get_or_add_rPr()
        srFonts = srPr.find(qn("w:rFonts"))
        if srFonts is None:
            srFonts = OxmlElement("w:rFonts")
            srPr.insert(0, srFonts)
        for attr in ("ascii", "hAnsi", "eastAsia", "cs"):
            srFonts.set(qn(f"w:{attr}"), "Times New Roman")
        st.paragraph_format.space_before = Pt(sb)
        st.paragraph_format.space_after = Pt(sa)
        st.paragraph_format.line_spacing = 1.2
        st.paragraph_format.keep_with_next = True
        st.paragraph_format.page_break_before = pbb
        st.paragraph_format.first_line_indent = Cm(0)

    # Set clean TOC styles if present
    for toc_name in ("TOC 1", "TOC 2", "TOC 3", "TOC Heading"):
        if toc_name in doc.styles:
            st = doc.styles[toc_name]
            st.font.name = "Times New Roman"
            st.font.size = Pt(12.5 if toc_name == "TOC 1" else 12)
            st.font.bold = (toc_name == "TOC 1")
            st.font.color.rgb = NAVY if toc_name == "TOC 1" else INK
            st.paragraph_format.space_after = Pt(3)
            st.paragraph_format.line_spacing = 1.15


def setup_a4_margins(section, left=3.0, right=2.0, top=2.5, bottom=2.5):
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.left_margin = Cm(left)
    section.right_margin = Cm(right)
    section.top_margin = Cm(top)
    section.bottom_margin = Cm(bottom)
    section.header_distance = Cm(1.25)
    section.footer_distance = Cm(1.25)


def set_page_border(section, color_hex=NAVY_HEX, val="double", sz="12", space="24"):
    sectPr = section._sectPr
    pgBorders = sectPr.find(qn("w:pgBorders"))
    if pgBorders is not None:
        sectPr.remove(pgBorders)
    pgBorders = OxmlElement("w:pgBorders")
    pgBorders.set(qn("w:offsetFrom"), "page")
    for side in ("top", "left", "bottom", "right"):
        b = OxmlElement(f"w:{side}")
        b.set(qn("w:val"), val)
        b.set(qn("w:sz"), sz)
        b.set(qn("w:space"), space)
        b.set(qn("w:color"), color_hex)
        pgBorders.append(b)
    sectPr.append(pgBorders)


def clear_page_border(section):
    sectPr = section._sectPr
    pgBorders = sectPr.find(qn("w:pgBorders"))
    if pgBorders is not None:
        sectPr.remove(pgBorders)


def set_page_number_format(section, fmt="decimal", start=1):
    sectPr = section._sectPr
    pgNumType = sectPr.find(qn("w:pgNumType"))
    if pgNumType is not None:
        sectPr.remove(pgNumType)
    pgNumType = OxmlElement("w:pgNumType")
    pgNumType.set(qn("w:fmt"), fmt)
    if start is not None:
        pgNumType.set(qn("w:start"), str(start))
    sectPr.append(pgNumType)


def add_field(paragraph, instruction: str, placeholder: str = "", font_size=12, bold=False, italic=False, color=INK):
    run = paragraph.add_run()
    set_run_font(run, size_pt=font_size, bold=bold, italic=italic, color=color)
    fldChar1 = OxmlElement("w:fldChar")
    fldChar1.set(qn("w:fldCharType"), "begin")
    fldChar1.set(qn("w:dirty"), "true")
    instrText = OxmlElement("w:instrText")
    instrText.set(qn("xml:space"), "preserve")
    instrText.text = f" {instruction} "
    fldChar2 = OxmlElement("w:fldChar")
    fldChar2.set(qn("w:fldCharType"), "separate")
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    if placeholder:
        r_mid = paragraph.add_run(placeholder)
        set_run_font(r_mid, size_pt=font_size, bold=bold, italic=italic, color=color)
    r_end = paragraph.add_run()
    set_run_font(r_end, size_pt=font_size, bold=bold, italic=italic, color=color)
    fldChar3 = OxmlElement("w:fldChar")
    fldChar3.set(qn("w:fldCharType"), "end")
    r_end._r.append(fldChar3)


def set_paragraph_bottom_border(paragraph, color_hex=NAVY_HEX, sz="8"):
    pPr = paragraph._p.get_or_add_pPr()
    pBdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), sz)
    bottom.set(qn("w:space"), "4")
    bottom.set(qn("w:color"), color_hex)
    pBdr.append(bottom)
    pPr.append(pBdr)


def set_paragraph_top_border(paragraph, color_hex=BORDER_HEX, sz="4"):
    pPr = paragraph._p.get_or_add_pPr()
    pBdr = OxmlElement("w:pBdr")
    top = OxmlElement("w:top")
    top.set(qn("w:val"), "single")
    top.set(qn("w:sz"), sz)
    top.set(qn("w:space"), "4")
    top.set(qn("w:color"), color_hex)
    pBdr.append(top)
    pPr.append(pBdr)


def _append_rich_runs(paragraph, text: str, font_size=13, base_bold=False, base_italic=False, base_color=INK, code_size=None):
    import re
    cs = code_size if code_size is not None else max(font_size - 1.5, 8.5)
    tokens = re.split(r"(`[^`]+`|\*\*[^*]+\*\*|\*[^*\n]+\*)", str(text))
    for tok in tokens:
        if not tok:
            continue
        if tok.startswith("`") and tok.endswith("`"):
            r = paragraph.add_run(tok[1:-1])
            set_run_font(r, name="Consolas", size_pt=cs, bold=base_bold, italic=base_italic, color=SUBNAVY if base_color == INK else base_color)
        elif tok.startswith("**") and tok.endswith("**"):
            _append_rich_runs(paragraph, tok[2:-2], font_size=font_size, base_bold=True, base_italic=base_italic, base_color=base_color, code_size=cs)
        elif tok.startswith("*") and tok.endswith("*"):
            _append_rich_runs(paragraph, tok[1:-1], font_size=font_size, base_bold=base_bold, base_italic=True, base_color=base_color, code_size=cs)
        else:
            r = paragraph.add_run(tok)
            set_run_font(r, size_pt=font_size, bold=base_bold, italic=base_italic, color=base_color)


def configure_running_header_footer(section, header_text: str, footer_left: str, show_page_num=True):
    section.header.is_linked_to_previous = False
    section.footer.is_linked_to_previous = False
    section.different_first_page_header_footer = False

    hp = section.header.paragraphs[0]
    hp.text = ""
    hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hp.paragraph_format.space_after = Pt(2)
    hp.paragraph_format.first_line_indent = Cm(0)
    if header_text:
        r = hp.add_run(header_text)
        set_run_font(r, size_pt=9.5, italic=True, color=MUTED)
        set_paragraph_bottom_border(hp, color_hex=BORDER_HEX, sz="4")

    # Clear any existing paragraphs/tables in footer cleanly
    fp = section.footer.paragraphs[0]
    fp.text = ""
    fp.paragraph_format.space_before = Pt(0)
    fp.paragraph_format.space_after = Pt(0)
    fp.paragraph_format.first_line_indent = Cm(0)

    if footer_left and show_page_num:
        # Remove empty default paragraph spacing by placing a clean 2-col table with top rule
        ftbl = section.footer.add_table(rows=1, cols=2, width=Cm(16.0))
        ftbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        ftbl.autofit = False
        ftbl._tbl.tblPr.append(parse_xml(
            f'<w:tblBorders {nsdecls("w")}>'
            f'<w:top w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
            f'<w:left w:val="none"/><w:bottom w:val="none"/><w:right w:val="none"/>'
            f'<w:insideH w:val="none"/><w:insideV w:val="none"/>'
            f'</w:tblBorders>'
        ))
        ftbl._tbl.tblPr.append(parse_xml(f'<w:tblLayout {nsdecls("w")} w:type="fixed"/>'))
        for idx, w_cm in enumerate((12.8, 3.2)):
            c = ftbl.cell(0, idx)
            c.width = Cm(w_cm)
            tcPr = c._tc.get_or_add_tcPr()
            tcW = tcPr.find(qn("w:tcW"))
            if tcW is None:
                tcW = OxmlElement("w:tcW")
                tcPr.append(tcW)
            tcW.set(qn("w:w"), str(int(round(w_cm * 567))))
            tcW.set(qn("w:type"), "dxa")
            tcPr.append(parse_xml(
                f'<w:tcMar {nsdecls("w")}><w:top w:w="50" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/>'
                f'<w:left w:w="0" w:type="dxa"/><w:right w:w="0" w:type="dxa"/></w:tcMar>'
            ))
        p_left = ftbl.cell(0, 0).paragraphs[0]
        p_left.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p_left.paragraph_format.first_line_indent = Cm(0)
        p_left.paragraph_format.space_after = Pt(0)
        r1 = p_left.add_run(footer_left)
        set_run_font(r1, size_pt=10, color=MUTED)

        p_right = ftbl.cell(0, 1).paragraphs[0]
        p_right.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p_right.paragraph_format.first_line_indent = Cm(0)
        p_right.paragraph_format.space_after = Pt(0)
        r2 = p_right.add_run("Trang ")
        set_run_font(r2, size_pt=10, color=MUTED)
        add_field(p_right, "PAGE", "1", font_size=10, color=MUTED)
        # Shrink the empty paragraph above table so footer stays compact
        fp.paragraph_format.line_spacing = Pt(1)
    elif show_page_num:
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        fp.paragraph_format.space_before = Pt(3)
        set_paragraph_top_border(fp, color_hex=BORDER_HEX, sz="4")
        add_field(fp, "PAGE", "i", font_size=10.5, color=MUTED)


def add_front_title(doc, title: str, page_break=True):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.page_break_before = page_break
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(12)
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.keep_with_next = True
    r = p.add_run(title.upper())
    set_run_font(r, size_pt=14.5, bold=True, color=NAVY)
    set_paragraph_bottom_border(p, color_hex=NAVY_HEX, sz="8")
    return p


def add_chapter(doc, text: str, page_break=True):
    p = doc.add_heading(text.upper(), level=1)
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.page_break_before = page_break
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(10)
    for r in p.runs:
        set_run_font(r, size_pt=15, bold=True, color=NAVY)
    set_paragraph_bottom_border(p, color_hex=NAVY_HEX, sz="10")
    return p


def add_h2(doc, text: str):
    p = doc.add_heading(text, level=2)
    p.paragraph_format.first_line_indent = Cm(0)
    for r in p.runs:
        set_run_font(r, size_pt=13.5, bold=True, color=NAVY)
    return p


def add_h3(doc, text: str):
    p = doc.add_heading(text, level=3)
    p.paragraph_format.first_line_indent = Cm(0)
    for r in p.runs:
        set_run_font(r, size_pt=13, bold=True, italic=True, color=SUBNAVY)
    return p


def add_para(doc, text: str, indent=True, bold_prefix: str = None, space_after=6, align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    """Add natural prose paragraph supporting inline `code`, **bold**, and *italic* spans cleanly."""
    p = doc.add_paragraph()
    p.alignment = align
    p.paragraph_format.line_spacing = 1.3
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(space_after)
    p.paragraph_format.first_line_indent = Cm(0.85) if indent else Cm(0)

    if bold_prefix:
        _append_rich_runs(p, bold_prefix, font_size=13, base_bold=True, base_color=INK, code_size=11)
    _append_rich_runs(p, text, font_size=13, base_bold=False, base_color=INK, code_size=11)
    return p


def add_bullet(doc, text: str, bold_prefix: str = None):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.left_indent = Cm(1.1)
    p.paragraph_format.first_line_indent = Cm(-0.45)
    p.paragraph_format.line_spacing = 1.28
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(4)
    rb0 = p.add_run("–  ")
    set_run_font(rb0, size_pt=13, bold=True, color=NAVY)
    if bold_prefix:
        _append_rich_runs(p, bold_prefix, font_size=13, base_bold=True, base_color=INK, code_size=11)
    _append_rich_runs(p, text, font_size=13, base_bold=False, base_color=INK, code_size=11)
    return p


def add_code_block(doc, title: str, code: str):
    if title:
        pt = doc.add_paragraph()
        pt.paragraph_format.first_line_indent = Cm(0)
        pt.paragraph_format.space_before = Pt(6)
        pt.paragraph_format.space_after = Pt(2)
        pt.paragraph_format.keep_with_next = True
        rt = pt.add_run(title)
        set_run_font(rt, size_pt=11.5, bold=True, italic=True, color=NAVY)

    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    cell = tbl.cell(0, 0)
    cell.width = Cm(16.0)
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{CODE_BG}"/>'))
    tcPr.append(parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:right w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:left w:val="single" w:sz="18" w:space="0" w:color="{NAVY_HEX}"/>'
        f'</w:tcBorders>'
    ))
    tcPr.append(parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="100" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/>'
        f'<w:left w:w="150" w:type="dxa"/><w:right w:w="120" w:type="dxa"/>'
        f'</w:tcMar>'
    ))
    # Prevent cell split across pages for short code excerpts
    trPr = tbl.rows[0]._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:cantSplit"))

    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.line_spacing = 1.12
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    r = p.add_run(code.strip("\n"))
    set_run_font(r, name="Consolas", size_pt=9.2, color=INK)

    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(4)


def add_callout_box(doc, label: str, text: str):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    cell = tbl.cell(0, 0)
    cell.width = Cm(16.0)
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="F1F5F9"/>'))
    tcPr.append(parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:bottom w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:right w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:left w:val="single" w:sz="18" w:space="0" w:color="{NAVY_HEX}"/>'
        f'</w:tcBorders>'
    ))
    tcPr.append(parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="100" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/>'
        f'<w:left w:w="160" w:type="dxa"/><w:right w:w="140" w:type="dxa"/>'
        f'</w:tcMar>'
    ))
    tbl.rows[0]._tr.get_or_add_trPr().append(OxmlElement("w:cantSplit"))

    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.line_spacing = 1.25
    p.paragraph_format.space_after = Pt(0)
    r0 = p.add_run(label + ": ")
    set_run_font(r0, size_pt=12, bold=True, color=NAVY)
    _append_rich_runs(p, text, font_size=12, base_bold=False, base_color=INK, code_size=10.5)

    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(4)


def add_table_caption(doc, title: str, seq_label="Bảng"):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.keep_with_next = True
    r1 = p.add_run(f"{seq_label} ")
    set_run_font(r1, size_pt=11.5, bold=True, italic=True, color=NAVY)
    add_field(p, f"SEQ {seq_label} \\* ARABIC", "1", font_size=11.5, bold=True, italic=True, color=NAVY)
    r2 = p.add_run(f": {title}")
    set_run_font(r2, size_pt=11.5, italic=True, color=INK)
    return p


def add_styled_table(doc, headers, rows, col_widths_cm, caption=None, align_cols=None, font_size=10.5):
    if caption:
        add_table_caption(doc, caption, seq_label="Bảng")

    tbl = doc.add_table(rows=1 + len(rows), cols=len(headers))
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False

    # Table-wide crisp academic borders
    tblPr = tbl._tbl.tblPr
    tblPr.append(parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="10" w:space="0" w:color="{NAVY_HEX}"/>'
        f'<w:bottom w:val="single" w:sz="10" w:space="0" w:color="{NAVY_HEX}"/>'
        f'<w:insideH w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:insideV w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:left w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'<w:right w:val="single" w:sz="4" w:space="0" w:color="{BORDER_HEX}"/>'
        f'</w:tblBorders>'
    ))
    tblPr.append(parse_xml(
        f'<w:tblLayout {nsdecls("w")} w:type="fixed"/>'
    ))

    def format_cell(cell, text, width_cm, is_header=False, shaded=False, col_align=WD_ALIGN_PARAGRAPH.LEFT):
        cell.width = Cm(width_cm)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        tcPr = cell._tc.get_or_add_tcPr()
        tcW = tcPr.find(qn("w:tcW"))
        if tcW is None:
            tcW = OxmlElement("w:tcW")
            tcPr.append(tcW)
        tcW.set(qn("w:w"), str(int(round(width_cm * 567))))
        tcW.set(qn("w:type"), "dxa")
        if is_header:
            tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{HEADER_BG}"/>'))
            tcPr.append(parse_xml(
                f'<w:tcBorders {nsdecls("w")}>'
                f'<w:bottom w:val="single" w:sz="8" w:space="0" w:color="{NAVY_HEX}"/>'
                f'</w:tcBorders>'
            ))
        elif shaded:
            tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{ALT_BG}"/>'))
        tcPr.append(parse_xml(
            f'<w:tcMar {nsdecls("w")}>'
            f'<w:top w:w="70" w:type="dxa"/><w:bottom w:w="70" w:type="dxa"/>'
            f'<w:left w:w="95" w:type="dxa"/><w:right w:w="95" w:type="dxa"/>'
            f'</w:tcMar>'
        ))
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER if is_header else col_align
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.line_spacing = 1.15
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        _append_rich_runs(
            p,
            str(text),
            font_size=font_size,
            base_bold=is_header,
            base_color=NAVY if is_header else INK,
            code_size=max(font_size - 1.0, 8.5),
        )

    # Header row with tblHeader + cantSplit
    hdr_row = tbl.rows[0]
    trPr = hdr_row._tr.get_or_add_trPr()
    trPr.append(OxmlElement("w:tblHeader"))
    trPr.append(OxmlElement("w:cantSplit"))
    for j, h in enumerate(headers):
        format_cell(hdr_row.cells[j], h, col_widths_cm[j], is_header=True)

    # Data rows
    for i, rdata in enumerate(rows):
        row = tbl.rows[i + 1]
        row._tr.get_or_add_trPr().append(OxmlElement("w:cantSplit"))
        for j, val in enumerate(rdata):
            al = WD_ALIGN_PARAGRAPH.LEFT
            if align_cols and j < len(align_cols):
                al = align_cols[j]
            format_cell(row.cells[j], val, col_widths_cm[j], is_header=False, shaded=(i % 2 == 1), col_align=al)

    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(5)
    return tbl


def add_figure(doc, img_path: Path, caption: str, max_width_cm=15.4, max_height_cm=17.5, add_border=True):
    img_path = Path(img_path)
    im = Image.open(img_path)
    w_px, h_px = im.size
    if add_border:
        im = ImageOps.expand(im.convert("RGB"), border=2, fill="#CBD5E1")
        w_px, h_px = im.size
        buf = io.BytesIO()
        im.save(buf, format="PNG", optimize=True)
        buf.seek(0)
        img_source = buf
    else:
        img_source = str(img_path)

    aspect = h_px / w_px
    width_cm = max_width_cm
    height_cm = width_cm * aspect
    if height_cm > max_height_cm:
        height_cm = max_height_cm
        width_cm = height_cm / aspect

    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.paragraph_format.first_line_indent = Cm(0)
    p_img.paragraph_format.space_before = Pt(6)
    p_img.paragraph_format.space_after = Pt(3)
    p_img.paragraph_format.keep_with_next = True
    run = p_img.add_run()
    run.add_picture(img_source, width=Cm(width_cm))

    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.first_line_indent = Cm(0)
    p_cap.paragraph_format.space_before = Pt(0)
    p_cap.paragraph_format.space_after = Pt(8)
    r1 = p_cap.add_run("Hình ")
    set_run_font(r1, size_pt=11.5, bold=True, italic=True, color=NAVY)
    add_field(p_cap, "SEQ Hình \\* ARABIC", "1", font_size=11.5, bold=True, italic=True, color=NAVY)
    r2 = p_cap.add_run(f": {caption}")
    set_run_font(r2, size_pt=11.5, italic=True, color=INK)


def add_side_by_side_figures(doc, left_path: Path, right_path: Path, caption: str, left_w_cm=9.5, right_w_cm=5.4, add_border=True):
    """Place a PDF page and a mobile screenshot neatly side by side inside a borderless 2-col table."""
    tbl = doc.add_table(rows=1, cols=2)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    tblPr = tbl._tbl.tblPr
    tblPr.append(parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/><w:left w:val="none"/><w:bottom w:val="none"/>'
        f'<w:right w:val="none"/><w:insideH w:val="none"/><w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    ))
    tbl.rows[0]._tr.get_or_add_trPr().append(OxmlElement("w:cantSplit"))

    for idx, (path_obj, w_cm) in enumerate([(left_path, left_w_cm), (right_path, right_w_cm)]):
        cell = tbl.cell(0, idx)
        cell.width = Cm(w_cm + 0.5)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.first_line_indent = Cm(0)
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.keep_with_next = True
        im = Image.open(path_obj)
        if add_border:
            im = ImageOps.expand(im.convert("RGB"), border=2, fill="#CBD5E1")
        buf = io.BytesIO()
        im.save(buf, format="PNG", optimize=True)
        buf.seek(0)
        p.add_run().add_picture(buf, width=Cm(w_cm))

    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.first_line_indent = Cm(0)
    p_cap.paragraph_format.space_before = Pt(4)
    p_cap.paragraph_format.space_after = Pt(8)
    r1 = p_cap.add_run("Hình ")
    set_run_font(r1, size_pt=11.5, bold=True, italic=True, color=NAVY)
    add_field(p_cap, "SEQ Hình \\* ARABIC", "1", font_size=11.5, bold=True, italic=True, color=NAVY)
    r2 = p_cap.add_run(f": {caption}")
    set_run_font(r2, size_pt=11.5, italic=True, color=INK)


def add_toc_field(doc, instruction: str, hint: str):
    p = doc.add_paragraph()
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.space_after = Pt(6)
    add_field(p, instruction, hint, font_size=12, italic=True, color=MUTED)
    return p
