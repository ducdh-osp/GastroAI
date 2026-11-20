package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkContentRow;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkSearchRow;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentContentSearchServiceTest {

    @Mock private ChunkRepository chunkRepository;

    @InjectMocks private DocumentContentSearchService service;

    private ChunkSearchRow row(long documentId, String title, long matchCount, String firstContent, long total) {
        return new ChunkSearchRow() {
            @Override public Long getDocumentId() { return documentId; }
            @Override public String getTitle() { return title; }
            @Override public Long getMatchCount() { return matchCount; }
            @Override public String getFirstContent() { return firstContent; }
            @Override public Long getTotalDocuments() { return total; }
        };
    }

    // Bang chu trong SQL (TRANSLATE) phai cho ket qua giong het Java (VietnameseText.normalize va
    // toLowerCase), neu khong SQL tim thay nhung cau trich / to mau lai sai cho.
    @Test
    void sqlAccentTablesMatchJava() {
        String lower = ChunkRepository.VI_LOWER;
        assertEquals(lower.length(), ChunkRepository.VI_UPPER.length());
        assertEquals(lower.length(), ChunkRepository.VI_PLAIN.length());
        assertEquals(lower, ChunkRepository.VI_UPPER.toLowerCase(Locale.ROOT));
        for (int i = 0; i < lower.length(); i++) {
            String accented = String.valueOf(lower.charAt(i));
            assertEquals(String.valueOf(ChunkRepository.VI_PLAIN.charAt(i)), VietnameseText.normalize(accented),
                    "Chu " + accented);
        }
        assertEquals(ChunkRepository.LINE_BREAKS.length(), ChunkRepository.LINE_BREAKS_AS_SPACE.length());
    }

    @Test
    void rejectsTooShortOrTooLongQueryWithoutTouchingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.searchDocuments(" a "));
        assertThrows(IllegalArgumentException.class, () -> service.searchDocuments("a".repeat(201)));
        assertThrows(IllegalArgumentException.class, () -> service.searchInDocument(1L, null));

        verify(chunkRepository, never()).searchDocuments(any(), anyBoolean());
        verify(chunkRepository, never()).searchChunksInDocument(anyLong(), any(), anyBoolean());
    }

    // Go khong dau, chu hoa, nhieu dau cach -> tim moi cach viet; % va _ duoc escape (ky tu '!').
    @Test
    void unaccentedQueryMatchesEverySpelling() {
        when(chunkRepository.searchDocuments(any(), anyBoolean())).thenReturn(List.of());

        service.searchDocuments("  Da   DAY 50%_x ");

        verify(chunkRepository).searchDocuments("da day 50!%!_x", false);
    }

    // Go co dau -> giu dau, chi doi chu thuong: "đau" khong ra "đầu".
    @Test
    void accentedQueryKeepsAccents() {
        when(chunkRepository.searchDocuments(any(), anyBoolean())).thenReturn(List.of());

        service.searchDocuments("  Đau   DẠ dày ");

        verify(chunkRepository).searchDocuments("đau dạ dày", true);
    }

    @Test
    void resultKeepsOrderFromSqlAndTakesTotalFromFirstRow() {
        when(chunkRepository.searchDocuments("da day", false)).thenReturn(List.of(
                row(2L, "Viêm loét", 5, "Bệnh dạ dày", 31),
                row(1L, "Trào ngược", 2, "Đau dạ dày", 31)));

        var result = service.searchDocuments("da day");

        assertEquals(31, result.totalDocuments());
        assertEquals(2, result.items().size());
        assertEquals(2L, result.items().get(0).documentId());
        assertEquals(5, result.items().get(0).matchCount());
    }

    @Test
    void noMatchMeansZeroTotal() {
        when(chunkRepository.searchDocuments("hp", false)).thenReturn(List.of());

        assertEquals(0, service.searchDocuments("hp").totalDocuments());
    }

    // Go khong dau van cat dung quanh cho khop, va cau trich giu nguyen dau cua tai lieu.
    @Test
    void snippetFindsUnaccentedQueryInAccentedText() {
        String content = "Mở đầu. ".repeat(40) + "Viêm DẠ DÀY mạn tính" + " Kết thúc.".repeat(40);

        String result = DocumentContentSearchService.snippet(content, DocumentContentSearchService.prepareQuery("da day"));

        assertTrue(result.startsWith("…"));
        assertTrue(result.endsWith("…"));
        assertTrue(result.contains("Viêm DẠ DÀY mạn tính"));
    }

    // File luu chu dang tach dau (NFD) van cat dung cho, vi da ghep lai NFC truoc khi tim.
    @Test
    void snippetHandlesDecomposedAccents() {
        String content = Normalizer.normalize("x".repeat(300) + " Đau dạ dày " + "y".repeat(300), Normalizer.Form.NFD);

        String result = DocumentContentSearchService.snippet(content,
                DocumentContentSearchService.prepareQuery("dau da day"));

        assertTrue(result.contains("Đau dạ dày"));
    }

    @Test
    void shortSnippetHasNoEllipsisAndJoinsLineBreaks() {
        assertEquals("Điều trị HP", DocumentContentSearchService.snippet("Điều trị\nHP",
                DocumentContentSearchService.prepareQuery("hp")));
    }

    // Go "đau" co dau: cau trich phai cat quanh "Đau bụng", khong phai "ĐẦU gối" o dau van ban.
    @Test
    void accentedSnippetSkipsOtherSpellings() {
        String content = "ĐẦU gối " + "x".repeat(300) + " Đau bụng " + "y".repeat(300);

        String result = DocumentContentSearchService.snippet(content, DocumentContentSearchService.prepareQuery("đau"));

        assertTrue(result.startsWith("…"));
        assertTrue(result.contains("Đau bụng"));
    }

    @Test
    void searchInDocumentReturnsMatchingChunksInOrder() {
        ChunkContentRow chunk = new ChunkContentRow() {
            @Override public Integer getChunkIndex() { return 3; }
            @Override public String getContent() { return "Phác đồ 4 thuốc"; }
        };
        when(chunkRepository.searchChunksInDocument(7L, "phác đồ", true)).thenReturn(List.of(chunk));

        var result = service.searchInDocument(7L, "Phác đồ");

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).chunkIndex());
        assertEquals("Phác đồ 4 thuốc", result.get(0).content());
    }
}