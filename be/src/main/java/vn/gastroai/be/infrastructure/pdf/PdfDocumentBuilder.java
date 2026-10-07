package vn.gastroai.be.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Lớp dùng chung để dựng PDF tiếng Việt (UC0019 xuất nhật ký sức khỏe, UC0027 xuất phiên
 * chat sẽ dùng lại). KHÔNG phải @Component - mỗi lần xuất PDF thì new 1 cái mới, dùng
 * xong bỏ đi, giống 1 object Java bình thường (không giữ state giữa các lần xuất).
 *
 * Những font mặc định của PDF (Helvetica...) không có chữ tiếng Việt có dấu, nên phải nhúng
 * (EMBEDDED) font Noto Sans vào file, dùng chế độ mã hóa IDENTITY_H (Unicode) thì mới hiện
 * đúng dấu tiếng Việt. Đọc font bằng getResourceAsStream (không dùng File trực tiếp) vì khi
 * đóng gói thành .jar, đường dẫn File trên đĩa không còn tồn tại, còn getResourceAsStream
 * đọc được cả trong .jar.
 */
public class PdfDocumentBuilder {

    private static final Color HEADER_BACKGROUND = new Color(0xF0, 0xF0, 0xF0);
    private static final Color GRAY_TEXT = new Color(0x66, 0x66, 0x66);

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final Document document;

    private final Font normalFont;
    private final Font boldFont;
    private final Font titleFont;
    private final Font smallGrayFont;
    private final BaseFont pageNumberBaseFont;

    public PdfDocumentBuilder() {
        this.document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            BaseFont regularBase = loadFont("NotoSans-Regular.ttf");
            BaseFont boldBase = loadFont("NotoSans-Bold.ttf");

            this.normalFont = new Font(regularBase, 10, Font.NORMAL);
            this.boldFont = new Font(boldBase, 10, Font.BOLD);
            this.titleFont = new Font(boldBase, 18, Font.BOLD);
            this.smallGrayFont = new Font(regularBase, 8, Font.NORMAL, GRAY_TEXT);
            // Font đánh số trang chỉ cần in số 0-9, dùng Helvetica mặc định (không tiếng Việt)
            // là đủ, tạo 1 lần ở đây để PageNumberEvent dùng lại, tránh gọi lại createFont()
            // (có khai báo throws) bên trong onEndPage.
            this.pageNumberBaseFont = BaseFont.createFont();

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PageNumberEvent());

            document.open();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    private BaseFont loadFont(String fileName) throws IOException, DocumentException {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream("/fonts/" + fileName)) {
            if (input == null) {
                throw new IOException("Không tìm thấy font " + fileName + " trong resources/fonts");
            }
            bytes = input.readAllBytes();
        }
        return BaseFont.createFont(fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
    }

    public PdfDocumentBuilder title(String text) {
        try {
            Paragraph p = new Paragraph(text, titleFont);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(4);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder subtitle(String text) {
        try {
            Paragraph p = new Paragraph(text, smallGrayFont);
            p.setAlignment(Element.ALIGN_CENTER);
            p.setSpacingAfter(16);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder section(String heading) {
        try {
            Paragraph p = new Paragraph(heading, boldFont);
            p.setSpacingBefore(16);
            p.setSpacingAfter(6);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder keyValue(String label, String value) {
        try {
            Paragraph p = new Paragraph();
            p.add(new Chunk(label + ": ", boldFont));
            p.add(new Chunk(blankToDash(value), normalFont));
            p.setSpacingAfter(2);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder paragraph(String text) {
        try {
            Paragraph p = new Paragraph(text, normalFont);
            p.setSpacingAfter(6);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder table(String[] headers, float[] relativeWidths, List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return paragraph("Không có ghi nhận trong khoảng thời gian này.");
        }
        try {
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(relativeWidths);
            table.setHeaderRows(1);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, boldFont));
                cell.setBackgroundColor(HEADER_BACKGROUND);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (String[] row : rows) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Paragraph(blankToDash(value), normalFont));
                    cell.setPadding(5);
                    table.addCell(cell);
                }
            }

            document.add(table);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder note(String text) {
        try {
            Paragraph p = new Paragraph(text, smallGrayFont);
            p.setSpacingBefore(16);
            document.add(p);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public byte[] build() {
        document.close();
        return outputStream.toByteArray();
    }

    private String blankToDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    /** In "Trang N" ở chân mỗi trang. Không static để dùng lại pageNumberBaseFont của outer. */
    private class PageNumberEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.beginText();
            cb.setFontAndSize(pageNumberBaseFont, 8);
            cb.showTextAligned(Element.ALIGN_CENTER,
                    "Trang " + writer.getPageNumber(),
                    document.getPageSize().getWidth() / 2,
                    document.bottomMargin() - 10,
                    0);
            cb.endText();
            cb.restoreState();
        }
    }
}