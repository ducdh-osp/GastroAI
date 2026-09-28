package vn.gastroai.be.application.document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC0031 - xac nhan fix bao cao cua Duc: header/footer lap lai tren nhieu trang
 * (ten benh vien, so trang don le) khong duoc dinh vao giua noi dung sau khi extract.
 * Tao PDF that bang chinh PDFBox (khong can file mau) de test dung code extract thuc te,
 * khong mock.
 */
class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void stripsRepeatedHospitalFooterAndLonePageNumbers(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-test.pdf");

        // 3 trang, moi trang co dung 1 dong noi dung THAT khac nhau, cong them cung 1
        // dong footer "BENH VIEN NHAN DAN 115" va 1 dong so trang don le - dung mo hinh
        // header/footer that trong file viem-dai-trang-man.pdf Duc bao cao.
        writeThreePagePdf(pdfPath,
                "Noi dung trang mot ve viem da day",
                "Noi dung trang hai ve viem dai trang",
                "Noi dung trang ba ve viem tui thua");

        String text = extractor.extract(pdfPath);

        // Noi dung that cua ca 3 trang phai con nguyen.
        assertTrue(text.contains("Noi dung trang mot ve viem da day"));
        assertTrue(text.contains("Noi dung trang hai ve viem dai trang"));
        assertTrue(text.contains("Noi dung trang ba ve viem tui thua"));

        // Diem quan trong nhat: footer lap lai va so trang don le KHONG duoc con sot
        // trong text - day chinh la rac dinh giua noi dung ma Duc bao cao.
        assertFalse(text.contains("BENH VIEN NHAN DAN 115"));
        assertFalse(text.contains("\n1\n"));
        assertFalse(text.contains("\n2\n"));
        assertFalse(text.contains("\n3\n"));
    }

    @Test
    void stripsHeaderByPositionEvenWhenBelowFrequencyThreshold(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-nang-cap-test.pdf");

        // 8 trang. Tieu de chuong "CHUONG 2: BENH LY DAI TRANG" chi xuat hien o TRANG 1
        // va TRANG 5 (2/8 = 25%, DUOI nguong tan suat 40%) nhung luon nam sat DAU trang
        // - dung tinh huong bi bo sot truoc khi co tin hieu vi tri: header chi lap o dau
        // moi chuong, khong phai moi trang, nen khong dat nguong 40% chung.
        //
        // Doi chung: cau "Xem chi tiet o phu luc" lap lai 2 lan (trang 2 va trang 6)
        // nhung nam O GIUA trang (khong phai vung dau/cuoi) - phai KHONG bi loc, de xac
        // nhan tin hieu vi tri khong lam loc nham noi dung that chi vi no tinh co lap
        // lai dung 2 lan.
        try (PDDocument document = new PDDocument()) {
            for (int pageNumber = 1; pageNumber <= 8; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);

                boolean hasChapterHeader = pageNumber == 1 || pageNumber == 5;
                boolean hasMidPageRepeatedNote = pageNumber == 2 || pageNumber == 6;

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    if (hasChapterHeader) {
                        writeLine(stream, "CHUONG 2: BENH LY DAI TRANG", 50, 750);
                    }

                    writeLine(stream, "Noi dung that cua trang " + pageNumber, 50, 400);

                    if (hasMidPageRepeatedNote) {
                        writeLine(stream, "Xem chi tiet o phu luc", 50, 380);
                    }
                }
            }

            document.save(pdfPath.toFile());
        }

        String text = extractor.extract(pdfPath);

        // Tieu de chuong bi loc du chi lap 2/8 trang, nho tin hieu vi tri (nam dau trang).
        assertFalse(text.contains("CHUONG 2: BENH LY DAI TRANG"));

        // Cau lap lai 2 lan nhung o giua trang thi PHAI con nguyen - khong bi loc nham.
        assertTrue(text.contains("Xem chi tiet o phu luc"));

        // Noi dung rieng cua tung trang phai con nguyen.
        assertTrue(text.contains("Noi dung that cua trang 1"));
        assertTrue(text.contains("Noi dung that cua trang 5"));
        assertTrue(text.contains("Noi dung that cua trang 8"));
    }

    @Test
    void marksLargeBoldLineWithFontHeadingMarkerButNotRegularBodyText(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-font-heading-test.pdf");

        // 1 trang: 2 doan than bai co chu 12pt thuong (chiem da so ky tu -> quyet dinh
        // "co so than bai" la 12), xen giua 1 dong tieu de 18pt IN DAM, KHONG danh so -
        // dung tinh huong ChunkingService (chi dua vao mau so "2.2.x") se bo sot hoan
        // toan neu khong co tin hieu font nay.
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, "Doan mo dau gioi thieu tong quan ve benh viem dai trang man tinh", 50, 700, 12, false);
                writeLine(stream, "BIEN CHUNG THUONG GAP", 50, 650, 18, true);
                writeLine(stream, "Xuat huyet tieu hoa la bien chung pho bien nhat can luu y theo doi", 50, 600, 12, false);
            }

            document.save(pdfPath.toFile());
        }

        String text = extractor.extract(pdfPath);

        // Dong tieu de 18pt in dam PHAI duoc gan marker, du khong co so muc dau dong.
        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "BIEN CHUNG THUONG GAP"));

        // Cac dong than bai 12pt thuong (co so) thi KHONG duoc gan marker.
        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Doan mo dau"));
        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Xuat huyet tieu hoa"));
    }

    @Test
    void doesNotMarkLongMultiLineBoldParagraphAsHeadingButStillDetectsShortHeadingAfterIt(
            @TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-khuyen-cao-test.pdf");

        // Phat hien tu du lieu THAT khi test voi tai lieu huong dan lam sang: cau "Khuyen
        // cao..." duoc in dam CA CAU, trai dai NHIEU DONG (PDF tu xuong dong ~60-70 ky tu
        // moi dong) - neu xet tung dong rieng le, moi dong van "ngan" (< 120) nen bi danh
        // dau tieu de RIENG LE, bam vun ca doan thanh hang chuc chunk 1 dong. Dung tinh
        // huong nay de xac nhan: gop ca 3 dong in dam LIEN TIEP lai, tong do dai > 120 ->
        // KHONG duoc danh dau tieu de o dong nao ca.
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, "Khuyen cao 22. Dieu tri corticosteroid tinh mach trong dot cap benh nang can", 50, 700, 12, true);
                writeLine(stream, "duoc chi dinh som cho nguoi benh khong dap ung dieu tri noi khoa thong thuong", 50, 680, 12, true);
                writeLine(stream, "va co nguy co bien chung cao neu khong duoc can thiep kip thoi ngay tu dau", 50, 660, 12, true);
                writeLine(stream, "Hieu qua dieu tri can duoc danh gia lai sau 3 ngay theo doi sat.", 50, 640, 12, false);
                writeLine(stream, "DIEU TRI NGOAI KHOA", 50, 600, 18, true);
            }

            document.save(pdfPath.toFile());
        }

        String text = extractor.extract(pdfPath);

        // Ca 3 dong cua doan in dam DAI (tong > 120 ky tu) KHONG duoc danh dau tieu de.
        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Khuyen cao 22"));
        assertTrue(text.contains("Khuyen cao 22. Dieu tri corticosteroid tinh mach trong dot cap benh nang can"));

        // Tieu de NGAN xuat hien SAU doan dai do (sau khi da co 1 dong thuong xen giua)
        // van phai duoc phat hien binh thuong - xac nhan viec gop khoi khong "nuot" nham
        // ca cac tieu de that su o xa hon.
        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "DIEU TRI NGOAI KHOA"));
    }

    private void writeLine(PDPageContentStream stream, String text, float x, float y) throws IOException {
        writeLine(stream, text, x, y, 12, false);
    }

    private void writeLine(PDPageContentStream stream, String text, float x, float y, float fontSize, boolean bold) throws IOException {
        stream.beginText();
        stream.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private void writeThreePagePdf(Path pdfPath, String... pageContents) throws IOException {
        try (PDDocument document = new PDDocument()) {

            int pageNumber = 1;
            for (String content : pageContents) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(content);
                    stream.newLineAtOffset(0, -650);
                    stream.showText("BENH VIEN NHAN DAN 115");
                    stream.newLineAtOffset(0, -20);
                    stream.showText(String.valueOf(pageNumber));
                    stream.endText();
                }
                pageNumber++;
            }

            document.save(pdfPath.toFile());
        }
    }
}