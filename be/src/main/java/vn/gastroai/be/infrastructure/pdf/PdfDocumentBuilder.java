package vn.gastroai.be.infrastructure.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;


public class PdfDocumentBuilder {

    private static final Color PRIMARY = new Color(0x0F, 0x76, 0x6E);
    private static final Color PRIMARY_LIGHT = new Color(0xF0, 0xFD, 0xFA);
    private static final Color TEXT = new Color(0x1E, 0x29, 0x3B);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);
    private static final Color DIVIDER = new Color(0xCB, 0xD5, 0xE1);
    private static final Color ZEBRA = new Color(0xF8, 0xFA, 0xFC);
    private static final Color WARNING_RED = new Color(0xB9, 0x1C, 0x1C);
    private static final Color WARNING_BG = new Color(0xFE, 0xF2, 0xF2);

    private static final String BRAND = "GastroAI";
    private static final String BRAND_TAGLINE = "Hệ thống hỗ trợ sức khỏe tiêu hóa";
    private static final String FOOTER_TEXT = "Tài liệu được tạo tự động từ GastroAI";

    private static final byte[] REGULAR_FONT_BYTES = readFontBytes("NotoSans-Regular.ttf");
    private static final byte[] BOLD_FONT_BYTES = readFontBytes("NotoSans-Bold.ttf");

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final Document document;

    private final BaseFont regularBaseFont;
    private final BaseFont boldBaseFont;
    private final Font normalFont;
    private final Font titleFont;
    private final Font subtitleFont;
    private final Font sectionFont;
    private final Font labelFont;
    private final Font blockTitleFont;
    private final Font tableHeaderFont;
    private final Font tableFont;
    private final Font speakerFont;
    private final Font smallGrayFont;
    private final Font warningFont;
    private final String missingCharReplacement;
    private boolean hasMessage = false;

    public PdfDocumentBuilder() {
        this.document = new Document(PageSize.A4, 48, 48, 72, 64);
        try {
            this.regularBaseFont = loadFont("NotoSans-Regular.ttf", REGULAR_FONT_BYTES);
            this.boldBaseFont = loadFont("NotoSans-Bold.ttf", BOLD_FONT_BYTES);

            this.normalFont = new Font(regularBaseFont, 10, Font.NORMAL, TEXT);
            this.titleFont = new Font(boldBaseFont, 18, Font.NORMAL, PRIMARY);
            this.subtitleFont = new Font(regularBaseFont, 9, Font.NORMAL, MUTED);
            this.sectionFont = new Font(boldBaseFont, 12, Font.NORMAL, PRIMARY);
            this.labelFont = new Font(regularBaseFont, 9.5f, Font.NORMAL, MUTED);
            this.blockTitleFont = new Font(boldBaseFont, 11, Font.NORMAL, PRIMARY);
            this.tableHeaderFont = new Font(boldBaseFont, 9, Font.NORMAL, Color.WHITE);
            this.tableFont = new Font(regularBaseFont, 9, Font.NORMAL, TEXT);
            this.speakerFont = new Font(boldBaseFont, 10, Font.NORMAL, PRIMARY);
            this.smallGrayFont = new Font(regularBaseFont, 8, Font.NORMAL, MUTED);
            this.warningFont = new Font(boldBaseFont, 10, Font.NORMAL, WARNING_RED);
            this.missingCharReplacement = regularBaseFont.charExists(0xFFFD) ? "�" : "[?]";

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new HeaderFooterEvent());

            document.open();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    private static byte[] readFontBytes(String fileName) {
        try (var input = PdfDocumentBuilder.class.getResourceAsStream("/fonts/" + fileName)) {
            if (input == null) {
                throw new IllegalStateException("Không tìm thấy font " + fileName + " trong resources/fonts");
            }
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BaseFont loadFont(String fileName, byte[] bytes) throws DocumentException, IOException {
        return BaseFont.createFont(fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
    }

    private String sanitize(String text) {
        if (text == null)
            return null;
        StringBuilder result = new StringBuilder();
        boolean[] lastWasMissing = {false};
        text.codePoints().forEach(codePoint -> {
            if (codePoint == '\n' || regularBaseFont.charExists(codePoint)) {
                result.appendCodePoint(codePoint);
                lastWasMissing[0] = false;
            } else if (!lastWasMissing[0]) {
                result.append(missingCharReplacement);
                lastWasMissing[0] = true;
            }
        });
        return result.toString();
    }

    public PdfDocumentBuilder title(String text) {
        Paragraph p = new Paragraph(sanitize(text), titleFont);
        p.setSpacingAfter(10);
        add(p);
        return this;
    }

    public PdfDocumentBuilder subtitle(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitize(text), subtitleFont));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(PRIMARY);
        cell.setBorderWidth(1.5f);
        cell.setPaddingLeft(0);
        cell.setPaddingTop(0);
        cell.setPaddingBottom(14);

        PdfPTable table = singleColumn();
        table.setSpacingAfter(22);
        table.addCell(cell);
        add(table);
        return this;
    }

    public PdfDocumentBuilder section(String heading) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitize(heading), sectionFont));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.75f);
        cell.setPaddingLeft(0);
        cell.setPaddingTop(0);
        cell.setPaddingBottom(5);

        PdfPTable table = singleColumn();
        table.setSpacingBefore(28);
        table.setSpacingAfter(12);
        table.addCell(cell);
        add(table);
        return this;
    }

    public PdfDocumentBuilder keyValue(String label, String value) {
        try {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {3, 7});

            PdfPCell labelCell = new PdfPCell(new Phrase(sanitize(label), labelFont));
            PdfPCell valueCell = new PdfPCell(new Phrase(blankToDash(sanitize(value)), normalFont));
            for (PdfPCell cell : new PdfPCell[] {labelCell, valueCell}) {
                cell.setBorder(Rectangle.BOTTOM);
                cell.setBorderColor(BORDER);
                cell.setBorderWidth(0.5f);
                cell.setPaddingTop(4);
                cell.setPaddingBottom(5);
            }
            labelCell.setPaddingLeft(0);

            table.addCell(labelCell);
            table.addCell(valueCell);
            add(table);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    /** Tiêu đề + nhiều dòng "nhãn: giá trị" trong 1 khung nền nhạt, KHÔNG bị cắt sang 2 trang. */
    public PdfDocumentBuilder keyValueBlock(String heading, List<String[]> pairs) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.75f);
        cell.setBackgroundColor(PRIMARY_LIGHT);
        cell.setPadding(12);

        Paragraph title = new Paragraph(sanitize(heading), blockTitleFont);
        title.setSpacingAfter(6);
        cell.addElement(title);

        for (String[] pair : pairs) {
            Paragraph line = new Paragraph();
            line.add(new Chunk(sanitize(pair[0]) + ": ", labelFont));
            line.add(new Chunk(blankToDash(sanitize(pair[1])), normalFont));
            line.setSpacingAfter(3);
            cell.addElement(line);
        }

        PdfPTable block = singleColumn();
        block.setSpacingBefore(28);
        block.setKeepTogether(true);
        block.addCell(cell);
        add(block);
        return this;
    }

    public PdfDocumentBuilder paragraph(String text) {
        Paragraph p = new Paragraph(sanitize(text), normalFont);
        p.setLeading(0, 1.45f);
        p.setSpacingAfter(10);
        add(p);
        return this;
    }

    public PdfDocumentBuilder table(String[] headers, float[] relativeWidths, List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            Paragraph empty = new Paragraph(sanitize("Không có ghi nhận trong khoảng thời gian này."), labelFont);
            empty.setSpacingAfter(6);
            add(empty);
            return this;
        }
        try {
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(relativeWidths);
            table.setHeaderRows(1);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(sanitize(header), tableHeaderFont));
                cell.setBackgroundColor(PRIMARY);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPadding(6);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }

            for (int i = 0; i < rows.size(); i++) {
                Color background = (i % 2 == 1) ? ZEBRA : Color.WHITE;
                for (String value : rows.get(i)) {
                    PdfPCell cell = new PdfPCell(new Phrase(blankToDash(sanitize(value)), tableFont));
                    cell.setBackgroundColor(background);
                    cell.setBorder(Rectangle.BOTTOM);
                    cell.setBorderColor(BORDER);
                    cell.setBorderWidth(0.5f);
                    cell.setPadding(6);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(cell);
                }
            }

            add(table);
            return this;
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    public PdfDocumentBuilder note(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitize(text), smallGrayFont));
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(DIVIDER);
        cell.setBorderWidth(1f);
        cell.setPaddingLeft(0);
        cell.setPaddingTop(8);

        PdfPTable table = singleColumn();
        table.setSpacingBefore(24);
        table.addCell(cell);
        add(table);
        return this;
    }

    /** Dòng mở đầu 1 tin nhắn chat: đường kẻ mảnh phía trên, tên người nói + giờ. */
        /** Dòng mở đầu 1 tin nhắn chat. Từ tin thứ 2 trở đi mới có đường kẻ ngăn cách phía trên. */
    public PdfDocumentBuilder messageHeader(String speaker, String time) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(sanitize(speaker), speakerFont));
        phrase.add(new Chunk("   " + sanitize(time), smallGrayFont));

        PdfPCell cell = new PdfPCell(phrase);
        if (hasMessage) {
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(DIVIDER);
            cell.setBorderWidth(1f);
            cell.setPaddingTop(12);
        } else {
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(0);
        }
        cell.setPaddingLeft(0);
        cell.setPaddingBottom(4);
        hasMessage = true;

        PdfPTable table = singleColumn();
        table.setSpacingBefore(20);
        table.addCell(cell);
        add(table);
        return this;
    }

    /** Cảnh báo khẩn cấp - khung nền đỏ nhạt, viền trái đỏ đậm. */
    public PdfDocumentBuilder warning(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitize(text), warningFont));
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColor(WARNING_RED);
        cell.setBorderWidth(3);
        cell.setBackgroundColor(WARNING_BG);
        cell.setPadding(8);
        cell.setPaddingLeft(10);

        PdfPTable table = singleColumn();
        table.setSpacingBefore(10);
        table.setSpacingAfter(12);
        table.setKeepTogether(true);
        table.addCell(cell);
        add(table);
        return this;
    }

    public byte[] build() {
        document.close();
        return outputStream.toByteArray();
    }

    private PdfPTable singleColumn() {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        return table;
    }

    private void add(Element element) {
        try {
            document.add(element);
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    private String blankToDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    /** Đầu trang: tên GastroAI + khẩu hiệu. Chân trang: dòng ghi chú + "Trang N / Tổng". */
    private class HeaderFooterEvent extends PdfPageEventHelper {

        private PdfTemplate totalPages;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            float top = document.getPageSize().getHeight();

            cb.saveState();

            cb.beginText();
            cb.setFontAndSize(boldBaseFont, 10);
            cb.setColorFill(PRIMARY);
            cb.showTextAligned(Element.ALIGN_LEFT, BRAND, left, top - 40, 0);
            cb.setFontAndSize(regularBaseFont, 8);
            cb.setColorFill(MUTED);
            cb.showTextAligned(Element.ALIGN_RIGHT, BRAND_TAGLINE, right, top - 40, 0);
            cb.endText();

            cb.setColorStroke(BORDER);
            cb.setLineWidth(0.5f);
            cb.moveTo(left, top - 48);
            cb.lineTo(right, top - 48);
            cb.moveTo(left, 44);
            cb.lineTo(right, 44);
            cb.stroke();

            String pageLabel = "Trang " + writer.getPageNumber() + " / ";
            float pageLabelWidth = regularBaseFont.getWidthPoint(pageLabel, 8);

            cb.beginText();
            cb.setFontAndSize(regularBaseFont, 8);
            cb.setColorFill(MUTED);
            cb.showTextAligned(Element.ALIGN_LEFT, FOOTER_TEXT, left, 30, 0);
            cb.showTextAligned(Element.ALIGN_LEFT, pageLabel, right - 16 - pageLabelWidth, 30, 0);
            cb.endText();

            cb.addTemplate(totalPages, right - 16, 30);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPages.beginText();
            totalPages.setFontAndSize(regularBaseFont, 8);
            totalPages.setColorFill(MUTED);
            totalPages.setTextMatrix(0, 0);
            totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPages.endText();
        }
    }
}