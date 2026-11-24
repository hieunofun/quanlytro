package com.hostel.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

public final class ContractPdf {
    public record Party(String name, String identity, String phone, String address) {}

    public record Item(String name, String quantity, String condition) {}

    public record Lease(
            String code, int version, String signedOn, Party lessor, Party tenant,
            String room, String property, String address, String area, String start,
            String endExclusive, String rent, String deposit, String payment, String terms,
            String handover, List<Item> items, List<String> residents) {}

    private static final Color INK = new Color(29, 36, 43);
    private static final Color SECONDARY = new Color(77, 87, 98);
    private static final Color LINE = new Color(205, 213, 220);
    private static final Color SHADE = new Color(242, 245, 247);
    private static final float LEFT = 48;
    private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * LEFT;
    private static final float TOP = PDRectangle.A4.getHeight() - 48;
    private static final float BOTTOM = 62;
    private static final float BODY = 10;
    private static final float LEADING = 15;

    private ContractPdf() {}

    public static byte[] render(Lease lease) throws IOException {
        try (var document = new PDDocument();
                var fontFile = ContractPdf.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            var font = PDType0Font.load(document, Objects.requireNonNull(fontFile));
            String title = lease.version() == 1
                    ? "HỢP ĐỒNG THUÊ PHÒNG TRỌ"
                    : "PHỤ LỤC HỢP ĐỒNG THUÊ PHÒNG TRỌ";
            document.getDocumentInformation().setTitle(title + " " + lease.code());
            try (var page = new Layout(document, font, lease.code())) {
                page.cover(title, lease);
                page.parties(lease.lessor(), lease.tenant());

                page.section("01", "Phòng cho thuê và thời hạn");
                page.rows(List.of(
                        new String[] {"Phòng", lease.room()},
                        new String[] {"Khu nhà", lease.property()},
                        new String[] {"Địa chỉ", lease.address()},
                        new String[] {"Diện tích sử dụng", lease.area()},
                        new String[] {"Thời hạn áp dụng", lease.start() + " đến trước ngày " + lease.endExclusive()}));
                page.prose("Mục đích sử dụng: để ở. Hai bên ghi nhận thời điểm nhận phòng thực tế trong biên bản bàn giao.");

                page.section("02", "Giá thuê, đặt cọc và thanh toán");
                page.rows(List.of(
                        new String[] {"Giá thuê", lease.rent() + " / tháng"},
                        new String[] {"Tiền cọc cam kết", lease.deposit()},
                        new String[] {"Lịch thanh toán", lease.payment()}));
                page.prose("Điện, nước và dịch vụ được đối soát theo chỉ số sử dụng, đơn giá và thỏa thuận ghi nhận trong hồ sơ thuê.");

                page.section("03", "Bàn giao và người thực tế ở");
                if (lease.handover().isBlank()) {
                    page.prose("Danh mục, số lượng và hiện trạng được hai bên ghi nhận tại biên bản bàn giao khi nhận phòng.");
                } else {
                    page.prose("Biên bản nhận phòng: " + lease.handover());
                    if (lease.items().isEmpty()) {
                        page.prose("Biên bản không ghi nhận trang thiết bị.");
                    } else {
                        page.inventory(lease.items());
                    }
                }
                if (!lease.residents().isEmpty()) {
                    page.prose("Người thực tế ở theo hồ sơ: " + String.join(", ", lease.residents()) + ".");
                }

                page.section("04", "Trách nhiệm và đối chiếu khi trả phòng");
                page.prose("Bên A giao phòng theo hiện trạng đã ghi nhận, bảo đảm việc sử dụng theo thỏa thuận và tiếp nhận yêu cầu sửa chữa.");
                page.prose("Bên B dùng phòng đúng mục đích, thanh toán đúng hạn, bảo quản tài sản và thông báo khi phát sinh hư hỏng.");
                page.prose("Khi trả phòng, hai bên kiểm kê tài sản, chốt các khoản phải thu và xác nhận cách xử lý tiền cọc theo thỏa thuận.");

                if (!lease.terms().isBlank()) {
                    page.section("05", "Thỏa thuận bổ sung");
                    page.prose(lease.terms());
                }
                page.signatures(lease.lessor().name(), lease.tenant().name());
            }
            writeFooters(document, font, lease.code());
            var output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static void writeFooters(PDDocument doc, PDType0Font font, String code) throws IOException {
        for (int index = 0; index < doc.getNumberOfPages(); index++) {
            try (var stream = new PDPageContentStream(doc, doc.getPage(index),
                    PDPageContentStream.AppendMode.APPEND, true)) {
                rule(stream, LEFT, 48, WIDTH);
                var footer = "Trang " + (index + 1) + " / " + doc.getNumberOfPages();
                draw(stream, font, safe(font, code), LEFT, 33, 8.5f, SECONDARY, false);
                draw(stream, font, footer, LEFT + WIDTH - measure(font, footer, 8.5f), 33, 8.5f, SECONDARY, false);
            }
        }
    }

    private static final class Layout implements AutoCloseable {
        private final PDDocument doc;
        private final PDType0Font font;
        private final String code;
        private PDPageContentStream stream;
        private float y;

        Layout(PDDocument doc, PDType0Font font, String code) throws IOException {
            this.doc = doc;
            this.font = font;
            this.code = code;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            var page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = TOP;
            if (doc.getNumberOfPages() > 1) {
                text("HỢP ĐỒNG THUÊ PHÒNG TRỌ  /  " + code, LEFT, y, 9, SECONDARY, true);
                rule(stream, LEFT, y - 9, WIDTH);
                y -= 32;
            }
        }

        void ensure(float height) throws IOException {
            if (y - height < BOTTOM) newPage();
        }

        void cover(String title, Lease lease) throws IOException {
            center("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", 11, INK, true);
            y -= 17;
            center("Độc lập - Tự do - Hạnh phúc", 10, INK, false);
            y -= 11;
            rule(stream, LEFT + (WIDTH - 68) / 2, y, 68);
            y -= 31;
            center(title, lease.version() == 1 ? 23 : 18, INK, true);
            y -= 20;
            center("Số " + lease.code() + "   ·   Phiên bản " + lease.version(), 10, SECONDARY, false);
            y -= 16;
            center(lease.signedOn().isBlank() ? "BẢN DỰ THẢO" : "Ngày ký ghi nhận: " + lease.signedOn(),
                    9, SECONDARY, lease.signedOn().isBlank());
            y -= 24;
        }

        void parties(Party lessor, Party tenant) throws IOException {
            float gap = 12;
            float width = (WIDTH - gap) / 2;
            var left = partyLines(lessor, width);
            var right = partyLines(tenant, width);
            float height = 39 + Math.max(left.size(), right.size()) * 14;
            ensure(height);
            partyBox("BÊN A  /  BÊN CHO THUÊ", LEFT, width, height, left);
            partyBox("BÊN B  /  BÊN THUÊ", LEFT + width + gap, width, height, right);
            y -= height + 19;
        }

        private List<String> partyLines(Party party, float width) throws IOException {
            var lines = new ArrayList<String>();
            for (String entry : List.of("Họ tên / đại diện: " + party.name(),
                    "CCCD / giấy tờ: " + party.identity(), "Điện thoại: " + party.phone(),
                    "Địa chỉ: " + party.address())) {
                lines.addAll(wrap(font, entry, BODY, width - 24));
            }
            return lines;
        }

        private void partyBox(String heading, float x, float width, float height, List<String> lines)
                throws IOException {
            fill(x, y - height, width, height);
            text(heading, x + 12, y - 17, 9, INK, true);
            float baseline = y - 35;
            for (String line : lines) {
                text(line, x + 12, baseline, BODY, INK, false);
                baseline -= 14;
            }
        }

        void section(String number, String title) throws IOException {
            ensure(58);
            rule(stream, LEFT, y + 4, WIDTH);
            y -= 13;
            text(number, LEFT, y, 9, SECONDARY, true);
            text(title, LEFT + 26, y, 12, INK, true);
            y -= 16;
        }

        void rows(List<String[]> rows) throws IOException {
            for (String[] row : rows) {
                var lines = wrap(font, row[1], BODY, WIDTH - 159);
                ensure(lines.size() * LEADING + 8);
                text(row[0], LEFT + 8, y - 1, BODY, SECONDARY, false);
                for (String line : lines) {
                    text(line, LEFT + 148, y - 1, BODY, INK, true);
                    y -= LEADING;
                }
                rule(stream, LEFT, y + 3, WIDTH);
                y -= 7;
            }
        }

        void prose(String paragraph) throws IOException {
            for (String line : wrap(font, paragraph, BODY, WIDTH - 12)) {
                ensure(LEADING);
                text(line, LEFT + 6, y, BODY, INK, false);
                y -= LEADING;
            }
            y -= 4;
        }

        void inventory(List<Item> items) throws IOException {
            ensure(52);
            inventoryHeader();
            for (Item item : items) {
                var names = wrap(font, item.name(), BODY, 203);
                var conditions = wrap(font, item.condition(), BODY, WIDTH - 282);
                int lineCount = Math.max(names.size(), conditions.size());
                for (int i = 0; i < lineCount; i++) {
                    if (y - LEADING < BOTTOM) {
                        newPage();
                        inventoryHeader();
                    }
                    if (i < names.size()) text(names.get(i), LEFT + 8, y, BODY, INK, false);
                    if (i == 0) text(item.quantity(), LEFT + 221, y, BODY, INK, true);
                    if (i < conditions.size()) text(conditions.get(i), LEFT + 279, y, BODY, INK, false);
                    y -= LEADING;
                }
                rule(stream, LEFT, y + 3, WIDTH);
                y -= 7;
            }
        }

        private void inventoryHeader() throws IOException {
            fill(LEFT, y - 13, WIDTH, 24);
            text("TRANG THIẾT BỊ", LEFT + 8, y - 4, 9, INK, true);
            text("SL", LEFT + 221, y - 4, 9, INK, true);
            text("HIỆN TRẠNG KHI NHẬN", LEFT + 279, y - 4, 9, INK, true);
            y -= 30;
        }

        void signatures(String lessor, String tenant) throws IOException {
            ensure(119);
            y -= 9;
            rule(stream, LEFT, y, WIDTH);
            signature("BÊN A · BÊN CHO THUÊ", lessor, LEFT);
            signature("BÊN B · BÊN THUÊ", tenant, LEFT + WIDTH / 2);
        }

        private void signature(String label, String name, float x) throws IOException {
            float width = WIDTH / 2;
            aligned(label, x, width, y - 23, 10, INK, true);
            aligned("Ký và ghi rõ họ tên", x, width, y - 39, 9, SECONDARY, false);
            rule(stream, x + (width - 134) / 2, y - 90, 134);
            if (!name.startsWith("…")) {
                var lines = wrap(font, name, 9, width - 14);
                for (int i = 0; i < lines.size(); i++) {
                    aligned(lines.get(i), x, width, y - 104 - i * 12, 9, INK, true);
                }
            }
        }

        private void center(String value, float size, Color color, boolean bold) throws IOException {
            aligned(value, LEFT, WIDTH, y, size, color, bold);
        }

        private void aligned(String value, float x, float width, float baseline,
                float size, Color color, boolean bold) throws IOException {
            var content = safe(font, value);
            draw(stream, font, content, x + (width - measure(font, content, size)) / 2, baseline, size, color, bold);
        }

        private void text(String value, float x, float baseline, float size, Color color, boolean bold)
                throws IOException {
            draw(stream, font, safe(font, value), x, baseline, size, color, bold);
        }

        private void fill(float x, float y, float width, float height) throws IOException {
            stream.setNonStrokingColor(SHADE);
            stream.addRect(x, y, width, height);
            stream.fill();
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

    private static void rule(PDPageContentStream stream, float x, float y, float width) throws IOException {
        stream.setStrokingColor(LINE);
        stream.setLineWidth(0.55f);
        stream.moveTo(x, y);
        stream.lineTo(x + width, y);
        stream.stroke();
    }

    private static void draw(PDPageContentStream stream, PDType0Font font, String value,
            float x, float y, float size, Color color, boolean bold) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.setStrokingColor(color);
        stream.setLineWidth(0.26f);
        stream.setRenderingMode(bold ? RenderingMode.FILL_STROKE : RenderingMode.FILL);
        stream.newLineAtOffset(x, y);
        stream.showText(value);
        stream.endText();
    }

    private static float measure(PDType0Font font, String text, float size) throws IOException {
        return font.getStringWidth(text) * size / 1000;
    }

    private static String safe(PDType0Font font, String value) throws IOException {
        var result = new StringBuilder();
        for (int cp : value.codePoints().toArray()) {
            String glyph = Character.isISOControl(cp) ? " " : Character.toString(cp);
            try {
                font.encode(glyph);
                result.append(glyph);
            } catch (IllegalArgumentException e) {
                result.append('?');
            }
        }
        return result.toString();
    }

    private static List<String> wrap(PDType0Font font, String value, float size, float width) throws IOException {
        var lines = new ArrayList<String>();
        for (String part : value.split("\\R", -1)) {
            String line = "";
            for (String word : safe(font, part).strip().split("\\s+")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (measure(font, next, size) <= width) {
                    line = next;
                    continue;
                }
                if (!line.isEmpty()) lines.add(line);
                line = "";
                for (int cp : word.codePoints().toArray()) {
                    var glyph = Character.toString(cp);
                    if (measure(font, line + glyph, size) > width) {
                        lines.add(line);
                        line = "";
                    }
                    line += glyph;
                }
            }
            lines.add(line);
        }
        return lines;
    }
}
