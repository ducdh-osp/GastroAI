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

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void stripsRepeatedHospitalFooterAndLonePageNumbers(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-test.pdf");

        writeThreePagePdf(pdfPath,
                "Noi dung trang mot ve viem da day",
                "Noi dung trang hai ve viem dai trang",
                "Noi dung trang ba ve viem tui thua");

        String text = extractor.extract(pdfPath);

        assertTrue(text.contains("Noi dung trang mot ve viem da day"));
        assertTrue(text.contains("Noi dung trang hai ve viem dai trang"));
        assertTrue(text.contains("Noi dung trang ba ve viem tui thua"));

        assertFalse(text.contains("BENH VIEN NHAN DAN 115"));
        assertFalse(text.contains("\n1\n"));
        assertFalse(text.contains("\n2\n"));
        assertFalse(text.contains("\n3\n"));
    }

    @Test
    void stripsHeaderByPositionEvenWhenBelowFrequencyThreshold(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-nang-cap-test.pdf");

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

        assertFalse(text.contains("CHUONG 2: BENH LY DAI TRANG"));
        assertTrue(text.contains("Xem chi tiet o phu luc"));

        assertTrue(text.contains("Noi dung that cua trang 1"));
        assertTrue(text.contains("Noi dung that cua trang 5"));
        assertTrue(text.contains("Noi dung that cua trang 8"));
    }

    @Test
    void marksLargeBoldLineWithFontHeadingMarkerButNotRegularBodyText(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-font-heading-test.pdf");

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

        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "BIEN CHUNG THUONG GAP"));

        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Doan mo dau"));
        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Xuat huyet tieu hoa"));
    }

    @Test
    void doesNotMarkLongMultiLineBoldParagraphAsHeadingButStillDetectsShortHeadingAfterIt(
            @TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-khuyen-cao-test.pdf");

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

        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Khuyen cao 22"));
        assertTrue(text.contains("Khuyen cao 22. Dieu tri corticosteroid tinh mach trong dot cap benh nang can"));

        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "DIEU TRI NGOAI KHOA"));
    }

    @Test
    void doesNotStripBodyTextThatRepeatsOutsideHeaderFooterZoneEvenAcrossManyPages(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-nhieu-benh-test.pdf");

        try (PDDocument document = new PDDocument()) {
            for (int pageNumber = 1; pageNumber <= 5; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    writeLine(stream, "Benh ly so " + pageNumber, 50, 500);
                    writeLine(stream, "Dieu tri", 50, 400);
                    writeLine(stream, "Noi dung dieu tri rieng cua benh " + pageNumber, 50, 380);
                }
            }

            document.save(pdfPath.toFile());
        }

        String text = extractor.extract(pdfPath);

        assertTrue(text.contains("Dieu tri"));
        assertTrue(text.contains("Noi dung dieu tri rieng cua benh 1"));
        assertTrue(text.contains("Noi dung dieu tri rieng cua benh 5"));
    }

    @Test
    void doesNotStripStandaloneNumericTableValuesOutsideHeaderFooterZone(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("phac-do-so-lieu-test.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, "Lieu dung khuyen cao cho benh nhan viem da day man tinh", 50, 500);
                writeLine(stream, "10", 50, 480);
                writeLine(stream, "mg moi ngay chia hai lan uong sau khi an sang", 50, 460);
            }

            document.save(pdfPath.toFile());
        }

        String text = extractor.extract(pdfPath);

        assertTrue(text.contains("10"));
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