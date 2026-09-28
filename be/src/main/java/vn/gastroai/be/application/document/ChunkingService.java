package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int OVERLAP_SIZE = 200;

    private static final Pattern SECTION_HEADING = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?\\.?\\s+\\S.*",
            Pattern.DOTALL);

    static final String FONT_HEADING_MARKER = "## ";

    private static final int MIN_STANDALONE_CHUNK_SIZE = 60;

    public List<Chunk> chunk(Document document) {

        String fullText = document.getFullText();

        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException(
                    "Document không có fullText để chunk");
        }

        String normalizedText = normalize(fullText);

        List<Paragraph> paragraphs = splitIntoParagraphs(normalizedText);

        List<String> chunkContents = coalesceTinyHeadingChunks(buildChunks(paragraphs));

        List<Chunk> chunks = new ArrayList<>();

        for (int i = 0; i < chunkContents.size(); i++) {
            Chunk chunk = new Chunk();

            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setContent(chunkContents.get(i));
            chunk.setTokenCount(null);

            chunks.add(chunk);
        }

        return chunks;
    }

    private String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private List<Paragraph> splitIntoParagraphs(String text) {

        String[] parts = text.split("\\n\\s*\\n");

        List<Paragraph> paragraphs = new ArrayList<>();

        for (String part : parts) {
            String paragraph = part.trim();

            if (!paragraph.isBlank()) {

                paragraphs.addAll(explodeEmbeddedHeadings(paragraph));
            }
        }

        return paragraphs;
    }

    private List<Paragraph> explodeEmbeddedHeadings(String paragraph) {

        String[] parts = paragraph.split(
                "(?m)(?=^\\d+\\.\\d+(\\.\\d+)?\\.?\\s+\\S)"
                        + "|(?m)(?=^" + Pattern.quote(FONT_HEADING_MARKER) + "\\S)");

        List<Paragraph> pieces = new ArrayList<>();

        for (String part : parts) {
            String piece = part.trim();

            if (!piece.isBlank()) {
                pieces.add(toParagraph(piece));
            }
        }

        return pieces.isEmpty() ? List.of(toParagraph(paragraph)) : pieces;
    }

    private Paragraph toParagraph(String rawText) {
        boolean isFontHeading = rawText.startsWith(FONT_HEADING_MARKER);

        String text = isFontHeading
                ? rawText.substring(FONT_HEADING_MARKER.length()).stripLeading()
                : rawText;

        boolean isSectionBoundary = isFontHeading || SECTION_HEADING.matcher(text).matches();

        return new Paragraph(text, isSectionBoundary);
    }

    private List<String> buildChunks(List<Paragraph> paragraphs) {

        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (Paragraph item : paragraphs) {

            String paragraph = item.text();

            if (item.isSectionBoundary() && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }

            if (paragraph.length() > MAX_CHUNK_SIZE) {

                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }

                chunks.addAll(
                        splitLargeParagraph(paragraph));

                continue;
            }

            int additionalLength = current.isEmpty()
                    ? paragraph.length()
                    : paragraph.length() + 2;

            if (current.length() + additionalLength > MAX_CHUNK_SIZE) {

                String chunk = current.toString().trim();

                chunks.add(chunk);

                String overlap = getOverlap(chunk);

                current.setLength(0);
                current.append(overlap);

                if (!current.isEmpty()) {
                    current.append("\n\n");
                }

                current.append(paragraph);

            } else {

                if (!current.isEmpty()) {
                    current.append("\n\n");
                }

                current.append(paragraph);
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private List<String> coalesceTinyHeadingChunks(List<String> chunks) {

        List<String> result = new ArrayList<>(chunks);

        int i = 0;
        while (i < result.size()) {

            String current = result.get(i);

            if (current.length() >= MIN_STANDALONE_CHUNK_SIZE || result.size() == 1) {
                i++;
                continue;
            }

            boolean canMergeForward = i + 1 < result.size()
                    && current.length() + result.get(i + 1).length() + 2 <= MAX_CHUNK_SIZE;

            if (canMergeForward) {
                result.set(i + 1, current + "\n\n" + result.get(i + 1));
                result.remove(i);
            } else if (i > 0) {
                result.set(i - 1, result.get(i - 1) + "\n\n" + current);
                result.remove(i);
            } else {
                // Chi con 1 chunk duy nhat, khong the ghep di dau - giu nguyen de tranh
                // vong lap vo han.
                i++;
            }
        }

        return result;
    }

    private List<String> splitLargeParagraph(
            String paragraph) {

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < paragraph.length()) {

            int end = Math.min(
                    start + MAX_CHUNK_SIZE,
                    paragraph.length());

            String chunk = paragraph.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= paragraph.length()) {
                break;
            }

            start = Math.max(
                    end - OVERLAP_SIZE,
                    start + 1);
        }

        return chunks;
    }

    private String getOverlap(String text) {

        if (text.length() <= OVERLAP_SIZE) {
            return text;
        }

        return text.substring(
                text.length() - OVERLAP_SIZE);
    }

    private record Paragraph(String text, boolean isSectionBoundary) {
    }
}