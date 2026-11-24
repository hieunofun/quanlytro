package com.hostel;
import com.hostel.service.ContractPdf;
import com.hostel.service.PdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class PdfTests {
    @Test void exportedPdfPreservesVietnamese() throws Exception {
        byte[] bytes=new PdfService().render("HÓA ĐƠN MINH BẠCH",List.of("Tiền nước: (128 − 120) × 20.000 = 160.000 VND"));
        try(var doc=Loader.loadPDF(bytes)) {String text=new PDFTextStripper().getText(doc);assertTrue(text.contains("Tiền nước"));assertTrue(text.contains("160.000"));}
    }

    @Test void contractAnnexKeepsVietnameseInventoryAndSignaturesAcrossPages() throws Exception {
        var lessor = new ContractPdf.Party("Nguyễn Minh Hà", "001200000001", "0900000000", "Hà Nội");
        var tenant = new ContractPdf.Party("Lê Thị Bích", "001200000002", "0900000001", "Hà Nội");
        var annex = new ContractPdf.Lease("HD-P101", 2, "10/01/2026", lessor, tenant,
                "P101", "Nhà trọ", "Thanh Xuân, Hà Nội", "30 m²", "01/06/2026",
                "01/06/2027", "3.600.000 đ", "3.500.000 đ", "Ngày 8 · Thu cuối kỳ",
                "Các bên cùng kiểm tra và ký vào biên bản đối chiếu. ".repeat(80),
                "HO-IN-P101", List.of(new ContractPdf.Item("Điều hòa", "1", "Đủ phụ kiện")),
                List.of("Lê Thị Bích"));
        try (var doc = Loader.loadPDF(ContractPdf.render(annex))) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(doc.getNumberOfPages() > 1);
            assertEquals(595.28f, doc.getPage(0).getMediaBox().getWidth(), 0.1f);
            assertTrue(text.contains("PHỤ LỤC HỢP ĐỒNG THUÊ PHÒNG TRỌ"));
            assertTrue(text.contains("đến trước ngày 01/06/2027"));
            assertTrue(text.contains("Điều hòa"));
            assertTrue(text.contains("Ký và ghi rõ họ tên"));
            assertTrue(text.contains("Trang " + doc.getNumberOfPages() + " / " + doc.getNumberOfPages()));
        }
    }
}
