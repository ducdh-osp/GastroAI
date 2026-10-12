package vn.gastroai.be.application.document;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Component
public class DocxTextExtractor implements TextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    private static final int HEADING_MAX_LENGTH = 120;

    @Override
    public boolean supports(String fileExtension) {
        return ".docx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extract(Path filePath) throws ExtractionException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraph(text, paragraph);
                } else if (element instanceof XWPFTable table) {
                    appendTable(text, table);
                }
            }

            String fullText = text.toString();

            if (fullText.trim().length() < MIN_TEXT_LENGTH) {
                throw new ExtractionException(
                        "Không trích xuất được text — có thể là file DOCX rỗng hoặc không có nội dung văn bản"
                );
            }

            return fullText;

        } catch (IOException e) {
            throw new ExtractionException(
                    "Không thể đọc file DOCX",
                    e
            );
        }
    }

    private void appendParagraph(StringBuilder text, XWPFParagraph paragraph) {
        String paragraphText = paragraph.getText();

        if (paragraphText != null && !paragraphText.isBlank()) {
            if (isHeadingParagraph(paragraph, paragraphText.trim())) {
                text.append(ChunkingService.FONT_HEADING_MARKER);
            }

            text.append(paragraphText);
            text.append(System.lineSeparator());
        }
    }

    private boolean isHeadingParagraph(XWPFParagraph paragraph, String trimmedText) {
        if (trimmedText.length() > HEADING_MAX_LENGTH) {
            return false;
        }

        if (hasHeadingStyle(paragraph)) {
            return true;
        }

        return isMostlyBold(paragraph);
    }

    private boolean hasHeadingStyle(XWPFParagraph paragraph) {
        String styleId = paragraph.getStyleID();

        if (styleId == null) {
            return false;
        }

        String normalized = styleId.toLowerCase(Locale.ROOT);
        return normalized.startsWith("heading") || normalized.equals("title");
    }

    private boolean isMostlyBold(XWPFParagraph paragraph) {
        List<XWPFRun> runs = paragraph.getRuns();

        if (runs.isEmpty()) {
            return false;
        }

        int boldChars = 0;
        int totalChars = 0;

        for (XWPFRun run : runs) {
            String runText = run.text();
            if (runText == null || runText.isEmpty()) {
                continue;
            }

            totalChars += runText.length();
            if (run.isBold()) {
                boldChars += runText.length();
            }
        }

        return totalChars > 0 && boldChars * 2 > totalChars;
    }
    private void appendTable(StringBuilder text, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            StringBuilder rowText = new StringBuilder();

            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = extractCellText(cell);

                if (!cellText.isBlank()) {
                    if (!rowText.isEmpty()) {
                        rowText.append(" | ");
                    }
                    rowText.append(cellText);
                }
            }

            if (!rowText.isEmpty()) {
                text.append(rowText);
                text.append(System.lineSeparator());
            }
        }

        text.append(System.lineSeparator());
    }

    private String extractCellText(XWPFTableCell cell) {
        StringBuilder cellText = new StringBuilder();

        for (IBodyElement element : cell.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.isBlank()) {
                    if (!cellText.isEmpty()) {
                        cellText.append(' ');
                    }
                    cellText.append(paragraphText.trim());
                }
            } else if (element instanceof XWPFTable nestedTable) {
                appendTable(cellText, nestedTable);
            }
        }

        return cellText.toString().trim();
    }
}