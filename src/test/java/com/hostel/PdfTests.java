package com.hostel;
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
}
