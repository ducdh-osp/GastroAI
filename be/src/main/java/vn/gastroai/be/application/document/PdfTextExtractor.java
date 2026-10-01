package vn.gastroai.be.application.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PdfTextExtractor implements TextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    private static final Pattern LONE_PAGE_NUMBER = Pattern.compile("^\\d{1,4}$");

    private static final double HEADER_FOOTER_ZONE_RATIO = 0.1;

    private static final int POSITIONAL_REPEAT_MIN_COUNT = 2;

    private static final double HEADING_FONT_SIZE_RATIO = 1.15;
    private static final int HEADING_MAX_LENGTH = 120;

    @Override
    public boolean supports(String fileExtension) {
        return ".pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extract(Path filePath) throws ExtractionException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {

            int pageCount = document.getNumberOfPages();
            List<List<PositionedLine>> linesByPage = new ArrayList<>(pageCount);

            for (int page = 1; page <= pageCount; page++) {
                PDPage pdPage = document.getPage(page - 1);
                float pageHeight = pdPage.getMediaBox().getHeight();

                LineCollectingStripper stripper = new LineCollectingStripper(pageHeight);
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                stripper.getText(document);

                linesByPage.add(stripper.getCollectedLines());
            }

            List<String> boilerplateLines = findBoilerplateLines(linesByPage, pageCount);
            float bodyFontSize = computeBodyFontSizeBaseline(linesByPage);

            List<PositionedLine> survivingLines = new ArrayList<>();
            for (List<PositionedLine> pageLines : linesByPage) {
                for (PositionedLine line : pageLines) {
                    String trimmed = line.text().trim();
                    if (trimmed.isEmpty() || boilerplateLines.contains(trimmed)) {
                        continue;
                    }
                    survivingLines.add(line);
                }
            }

            Set<Integer> headingSpanStarts = findHeadingSpanStarts(survivingLines, bodyFontSize);

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < survivingLines.size(); i++) {
                if (headingSpanStarts.contains(i)) {
                    text.append(ChunkingService.FONT_HEADING_MARKER);
                }
                text.append(survivingLines.get(i).text()).append('\n');
            }

            String result = text.toString();

            if (result.trim().length() < MIN_TEXT_LENGTH) {
                throw new ExtractionException(
                        "Không trích xuất được text — có thể là file scan/ảnh");
            }

            return result;

        } catch (InvalidPasswordException e) {
            throw new ExtractionException(
                    "File PDF có mật khẩu, không thể xử lý",
                    e);

        } catch (IOException e) {
            throw new ExtractionException(
                    "Không thể đọc file PDF",
                    e);
        }
    }

    private List<String> findBoilerplateLines(List<List<PositionedLine>> linesByPage, int pageCount) {
        Map<String, Integer> zoneCountByLine = new HashMap<>();

        for (List<PositionedLine> pageLines : linesByPage) {
            LinkedHashSet<String> distinctInZoneOnPage = new LinkedHashSet<>();

            for (PositionedLine line : pageLines) {
                String trimmed = line.text().trim();
                if (trimmed.isEmpty() || !line.inHeaderOrFooterZone()) {
                    continue;
                }
                distinctInZoneOnPage.add(trimmed);
            }

            // dung Set trong 1 trang de khong dem 2 lan neu 1 dong lap trong cung 1 trang
            distinctInZoneOnPage.forEach(l -> zoneCountByLine.merge(l, 1, Integer::sum));
        }

        List<String> boilerplate = new ArrayList<>();

        zoneCountByLine.forEach((line, zoneCount) -> {
            boolean positionalRepeat = zoneCount >= POSITIONAL_REPEAT_MIN_COUNT;
            boolean lonePageNumberInZone = LONE_PAGE_NUMBER.matcher(line).matches();

            if (positionalRepeat || lonePageNumberInZone) {
                boilerplate.add(line);
            }
        });

        return boilerplate;
    }

    private float computeBodyFontSizeBaseline(List<List<PositionedLine>> linesByPage) {
        Map<Integer, Integer> charCountBySize = new HashMap<>();

        for (List<PositionedLine> pageLines : linesByPage) {
            for (PositionedLine line : pageLines) {
                String trimmed = line.text().trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int roundedSize = Math.round(line.fontSize());
                charCountBySize.merge(roundedSize, trimmed.length(), Integer::sum);
            }
        }

        return (float) charCountBySize.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private Set<Integer> findHeadingSpanStarts(List<PositionedLine> survivingLines, float bodyFontSize) {
        Set<Integer> spanStarts = new HashSet<>();

        int i = 0;
        while (i < survivingLines.size()) {
            if (!isHeadingCandidate(survivingLines.get(i), bodyFontSize)) {
                i++;
                continue;
            }

            int spanStart = i;
            int spanEnd = i;
            int totalLength = 0;

            while (spanEnd < survivingLines.size()
                    && isHeadingCandidate(survivingLines.get(spanEnd), bodyFontSize)) {
                totalLength += survivingLines.get(spanEnd).text().trim().length();
                spanEnd++;
            }

            if (totalLength <= HEADING_MAX_LENGTH) {
                spanStarts.add(spanStart);
            }

            i = spanEnd;
        }

        return spanStarts;
    }

    private boolean isHeadingCandidate(PositionedLine line, float bodyFontSize) {
        String trimmed = line.text().trim();

        if (trimmed.isEmpty() || bodyFontSize <= 0) {
            return false;
        }

        boolean isLarger = line.fontSize() >= bodyFontSize * HEADING_FONT_SIZE_RATIO;

        return isLarger || line.bold();
    }

    private record PositionedLine(String text, boolean inHeaderOrFooterZone, float fontSize, boolean bold) {
    }

    private static class LineCollectingStripper extends PDFTextStripper {

        private final float pageHeight;
        private final List<PositionedLine> collectedLines = new ArrayList<>();

        LineCollectingStripper(float pageHeight) throws IOException {
            this.pageHeight = pageHeight;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            float averageY = averageY(textPositions);
            float averageFontSize = averageFontSize(textPositions);
            boolean bold = isMostlyBold(textPositions);

            boolean inZone = pageHeight > 0
                    && (averageY <= pageHeight * HEADER_FOOTER_ZONE_RATIO
                            || averageY >= pageHeight * (1 - HEADER_FOOTER_ZONE_RATIO));

            collectedLines.add(new PositionedLine(text, inZone, averageFontSize, bold));
        }

        private float averageY(List<TextPosition> textPositions) {
            if (textPositions.isEmpty()) {
                return 0f;
            }
            float sum = 0f;
            for (TextPosition position : textPositions) {
                sum += position.getYDirAdj();
            }
            return sum / textPositions.size();
        }

        private float averageFontSize(List<TextPosition> textPositions) {
            if (textPositions.isEmpty()) {
                return 0f;
            }
            float sum = 0f;
            for (TextPosition position : textPositions) {
                sum += position.getFontSizeInPt();
            }
            return sum / textPositions.size();
        }

        private boolean isMostlyBold(List<TextPosition> textPositions) {
            if (textPositions.isEmpty()) {
                return false;
            }
            int boldCount = 0;
            for (TextPosition position : textPositions) {
                if (isBoldFont(position.getFont())) {
                    boldCount++;
                }
            }
            return boldCount * 2 > textPositions.size();
        }

        private boolean isBoldFont(PDFont font) {
            if (font == null) {
                return false;
            }

            PDFontDescriptor descriptor = font.getFontDescriptor();
            if (descriptor != null && descriptor.isForceBold()) {
                return true;
            }

            String name = font.getName();
            return name != null && name.toLowerCase(Locale.ROOT).contains("bold");
        }

        List<PositionedLine> getCollectedLines() {
            return collectedLines;
        }
    }
}