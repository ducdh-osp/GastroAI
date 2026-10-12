package vn.gastroai.be.application.document;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC0031 - xac nhan DocxTextExtractor doc DUNG THU TU doan van xen ke bang, va khong
 * bo sot noi dung trong bang (vd bang lieu dung thuoc) nhu cach lam cu (getParagraphs()
 * roi bo qua bang).
 */
class DocxTextExtractorTest {

    private final DocxTextExtractor extractor = new DocxTextExtractor();

    @Test
    void extractsTableContentInDocumentOrderAlongsideParagraphs(@TempDir Path tempDir) throws IOException {
        Path docxPath = tempDir.resolve("phac-do-test.docx");

        writeDocxWithParagraphThenTableThenParagraph(docxPath);

        String text = extractor.extract(docxPath);

        // Noi dung doan van truoc va sau bang phai con nguyen.
        assertTrue(text.contains("Doan mo dau ve dieu tri viem dai trang"));
        assertTrue(text.contains("Doan ket luan sau bang lieu dung"));

        // Noi dung trong bang KHONG duoc bo sot - day la lo hong cua cach lam cu.
        assertTrue(text.contains("Thuoc"));
        assertTrue(text.contains("Amoxicillin"));
        assertTrue(text.contains("500mg"));

        // Thu tu doc phai dung: doan mo dau -> bang -> doan ket luan, khong bi dong het
        // bang xuong cuoi cung.
        int introIndex = text.indexOf("Doan mo dau ve dieu tri viem dai trang");
        int tableIndex = text.indexOf("Amoxicillin");
        int conclusionIndex = text.indexOf("Doan ket luan sau bang lieu dung");

        assertTrue(introIndex < tableIndex);
        assertTrue(tableIndex < conclusionIndex);
    }

    @Test
    void marksWordHeadingStyleAndBoldFallbackButNotRegularParagraphs(@TempDir Path tempDir) throws IOException {
        Path docxPath = tempDir.resolve("phac-do-heading-test.docx");

        // 3 doan: 1 doan thuong, 1 doan gan style "Heading1" chuan cua Word (tin hieu
        // ngu nghia manh nhat), 1 doan KHONG co style nhung in dam toan bo (fallback cho
        // tai lieu khong dung style chuan) - ca 2 truong hop deu phai duoc gan marker,
        // doan thuong thi khong.
        try (XWPFDocument document = new XWPFDocument()) {

            addParagraph(document, "Doan mo dau gioi thieu chung, khong phai tieu de.");

            XWPFParagraph headingStyleParagraph = document.createParagraph();
            headingStyleParagraph.setStyle("Heading1");
            headingStyleParagraph.createRun().setText("BIEN CHUNG THUONG GAP");

            XWPFParagraph boldFallbackParagraph = document.createParagraph();
            XWPFRun boldRun = boldFallbackParagraph.createRun();
            boldRun.setBold(true);
            boldRun.setText("DIEU TRI NOI KHOA");

            try (FileOutputStream out = new FileOutputStream(docxPath.toFile())) {
                document.write(out);
            }
        }

        String text = extractor.extract(docxPath);

        // Doan gan style Heading1 va doan in dam (fallback) deu phai duoc gan marker.
        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "BIEN CHUNG THUONG GAP"));
        assertTrue(text.contains(ChunkingService.FONT_HEADING_MARKER + "DIEU TRI NOI KHOA"));

        // Doan van thuong khong duoc gan marker.
        assertFalse(text.contains(ChunkingService.FONT_HEADING_MARKER + "Doan mo dau"));
    }

    private void writeDocxWithParagraphThenTableThenParagraph(Path docxPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {

            addParagraph(document, "Doan mo dau ve dieu tri viem dai trang");

            XWPFTable table = document.createTable(2, 2);

            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("Thuoc");
            header.getCell(1).setText("Lieu dung");

            XWPFTableRow row = table.getRow(1);
            row.getCell(0).setText("Amoxicillin");
            row.getCell(1).setText("500mg");

            addParagraph(document, "Doan ket luan sau bang lieu dung");

            try (FileOutputStream out = new FileOutputStream(docxPath.toFile())) {
                document.write(out);
            }
        }
    }

    private void addParagraph(XWPFDocument document, String content) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(content);
    }
}