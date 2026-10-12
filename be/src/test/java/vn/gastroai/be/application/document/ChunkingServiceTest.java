package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    private static Document documentWithText(String fullText) {
        Document document = new Document();
        document.setId(1L);
        document.setFullText(fullText);
        return document;
    }

    @Test
    void splitsAtSectionHeadingsEvenWithoutBlankLineBetweenThem() {
        String text = """
                2.2.2 Viem dai trang do amibe
                Xet nghiem: soi phan tim the ke hoac the tu duong.
                2.2.3 Viem dai trang mang gia
                Xet nghiem thuong qui: CTM, duong huyet, chuc nang gan.
                2.2.4 Viem tui thua dai trang
                Xet nghiem thuong qui: CTM, CRP.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("2.2.2"));
        assertTrue(chunks.get(1).getContent().startsWith("2.2.3"));
        assertTrue(chunks.get(2).getContent().startsWith("2.2.4"));

        assertTrue(chunks.get(0).getContent().contains("amibe"));
        assertFalse(chunks.get(0).getContent().toLowerCase().contains("mang gia"));
    }

    @Test
    void keepsShortUnrelatedParagraphsTogetherWhenNoHeadingPresent() {
        String text = """
                Doan mo dau ngan gon ve benh tieu hoa noi chung.

                Doan thu hai tiep tuc noi ve trieu chung thuong gap.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("mo dau"));
        assertTrue(chunks.get(0).getContent().contains("trieu chung"));
    }

    @Test
    void doesNotMistakeNumericDataForSectionHeading() {
        String text = """
                2.2.5 Dieu tri
                Lieu dung khuyen cao:
                10 mg moi ngay, chia 2 lan uong sau an.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("10 mg"));
    }

    @Test
    void splitsAtFontHeadingMarkerAndStripsItFromFinalContent() {
        String text = """
                ## BIEN CHUNG THUONG GAP
                Xuat huyet tieu hoa la bien chung pho bien nhat, can theo doi sat.
                ## DIEU TRI
                Dieu tri noi khoa la lua chon dau tay cho hau het cac truong hop benh.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("BIEN CHUNG THUONG GAP"));
        assertTrue(chunks.get(1).getContent().startsWith("DIEU TRI"));

        assertFalse(chunks.get(0).getContent().contains("##"));
        assertFalse(chunks.get(1).getContent().contains("##"));
    }

    @Test
    void mergesHeadingOnlyChunkIntoFollowingChunk() {
        String text = """
                ## 4. DIEU TRI
                4.1 Dieu tri noi khoa la phuong phap dieu tri chinh cho hau het cac truong hop nhe.
                4.2 Dieu tri ngoai khoa duoc chi dinh khi co bien chung nang hon.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("4. DIEU TRI"));
        assertTrue(chunks.get(0).getContent().contains("4.1 Dieu tri noi khoa"));
        assertTrue(chunks.get(1).getContent().startsWith("4.2"));
    }

    @Test
    void neverProducesAChunkLongerThanMaxChunkSizeAndKeepsOverlapContinuity() {
        String paragraph1 = "A".repeat(1899);
        String paragraph2 = "B".repeat(1989);

        String text = paragraph1 + "\n\n" + paragraph2;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        for (Chunk chunk : chunks) {
            assertTrue(
                    chunk.getContent().length() <= 2000,
                    "Chunk vuot qua 2000 ky tu: " + chunk.getContent().length());
        }

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(1).getContent().startsWith("A".repeat(200)));
        assertTrue(chunks.get(2).getContent().startsWith("B".repeat(200)));
    }
}