package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int OVERLAP_SIZE = 200;

    public List<Chunk> chunk(Document document) {

        String fullText = document.getFullText();

        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException(
                    "Document không có fullText để chunk"
            );
        }

        String normalizedText = normalize(fullText);

        List<String> paragraphs =
                splitIntoParagraphs(normalizedText);

        List<String> chunkContents =
                buildChunks(paragraphs);

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

    private List<String> splitIntoParagraphs(String text) {

        String[] parts = text.split("\\n\\s*\\n");

        List<String> paragraphs = new ArrayList<>();

        for (String part : parts) {
            String paragraph = part.trim();

            if (!paragraph.isBlank()) {
                paragraphs.add(paragraph);
            }
        }

        return paragraphs;
    }

    private List<String> buildChunks(
            List<String> paragraphs) {

        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {

            if (paragraph.length() > MAX_CHUNK_SIZE) {

                if (!current.isEmpty()) {
                    chunks.add(current.toString().trim());
                    current.setLength(0);
                }

                chunks.addAll(
                        splitLargeParagraph(paragraph)
                );

                continue;
            }

            int additionalLength =
                    current.isEmpty()
                            ? paragraph.length()
                            : paragraph.length() + 2;

            if (current.length() + additionalLength
                    > MAX_CHUNK_SIZE) {

                String chunk =
                        current.toString().trim();

                chunks.add(chunk);

                String overlap =
                        getOverlap(chunk);

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

    private List<String> splitLargeParagraph(
            String paragraph) {

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < paragraph.length()) {

            int end = Math.min(
                    start + MAX_CHUNK_SIZE,
                    paragraph.length()
            );

            String chunk =
                    paragraph.substring(start, end).trim();

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end >= paragraph.length()) {
                break;
            }

            start =
                    Math.max(
                            end - OVERLAP_SIZE,
                            start + 1
                    );
        }

        return chunks;
    }

    private String getOverlap(String text) {

        if (text.length() <= OVERLAP_SIZE) {
            return text;
        }

        return text.substring(
                text.length() - OVERLAP_SIZE
        );
    }
}