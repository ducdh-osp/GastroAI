package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.document.DocumentReviewResponse;
import vn.gastroai.be.api.document.DocumentSearchHit;
import vn.gastroai.be.api.document.DocumentSearchResult;
import vn.gastroai.be.application.support.SearchQueryEscaper;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkSearchRow;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;


@Service
public class DocumentContentSearchService {

    static final int MIN_QUERY_LENGTH = 2;
    static final int MAX_QUERY_LENGTH = 200;
    static final int SNIPPET_RADIUS = 120;

    private final ChunkRepository chunkRepository;

    public DocumentContentSearchService(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    /** Từ khoá đã chuẩn hoá để so trong SQL; keepAccents = người dùng có gõ dấu. */
    record PreparedQuery(String text, boolean keepAccents) {
    }

    /** Mỗi tài liệu 1 dòng, tài liệu khớp nhiều đoạn nhất lên đầu. */
    @Transactional(readOnly = true)
    public DocumentSearchResult searchDocuments(String query) {
        PreparedQuery prepared = prepareQuery(query);
        List<ChunkSearchRow> rows = chunkRepository.searchDocuments(
                SearchQueryEscaper.escapeLike(prepared.text()), prepared.keepAccents());

        long totalDocuments = rows.isEmpty() ? 0 : rows.get(0).getTotalDocuments();
        List<DocumentSearchHit> items = rows.stream()
                .map(row -> new DocumentSearchHit(row.getDocumentId(), row.getTitle(), row.getMatchCount(),
                        snippet(row.getFirstContent(), prepared)))
                .toList();
        return new DocumentSearchResult(items, totalDocuments);
    }

    /** Các đoạn khớp của 1 tài liệu, theo thứ tự trong tài liệu. */
    @Transactional(readOnly = true)
    public List<DocumentReviewResponse.Chunk> searchInDocument(Long documentId, String query) {
        PreparedQuery prepared = prepareQuery(query);
        return chunkRepository.searchChunksInDocument(documentId,
                        SearchQueryEscaper.escapeLike(prepared.text()), prepared.keepAccents()).stream()
                .map(row -> new DocumentReviewResponse.Chunk(row.getChunkIndex(), row.getContent()))
                .toList();
    }

    /**
     * Kiểm tra độ dài, gộp khoảng trắng, đưa về chữ thường; gõ không dấu thì bỏ dấu luôn.
     * Phải chuẩn hoá giống hệt cách SQL xử lý nội dung (ChunkRepository LOWERED_CONTENT / FOLDED_CONTENT).
     */
    static PreparedQuery prepareQuery(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException("Nhập ít nhất " + MIN_QUERY_LENGTH + " ký tự để tìm kiếm");
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Từ khóa quá dài (tối đa " + MAX_QUERY_LENGTH + " ký tự)");
        }
        String collapsed = Normalizer.normalize(q, Normalizer.Form.NFC).replaceAll("\\s+", " ");
        String lowered = collapsed.toLowerCase(Locale.ROOT);
        String folded = VietnameseText.normalize(collapsed);
        boolean keepAccents = !folded.equals(lowered);
        return new PreparedQuery(keepAccents ? lowered : folded, keepAccents);
    }

    /**
     * Cắt khoảng SNIPPET_RADIUS ký tự trước và sau chỗ khớp đầu tiên, thêm "…" nếu bị cắt.
     * Tìm vị trí trên bản đã chuẩn hoá (giống cách so trong SQL) nhưng cắt trên bản gốc, nên câu
     * trích vẫn giữ dấu và chữ hoa.
     */
    static String snippet(String content, PreparedQuery query) {
        if (content == null) {
            return "";
        }
        String text = Normalizer.normalize(content, Normalizer.Form.NFC).replaceAll("\\s+", " ").trim();
        String comparable = query.keepAccents() ? text.toLowerCase(Locale.ROOT) : VietnameseText.normalize(text);
        // Chữ thường / bỏ dấu mỗi chữ (đã ghép NFC) vẫn còn đúng 1 chữ, nên vị trí 2 bản trùng nhau;
        // gặp chữ đặc biệt làm lệch độ dài thì cắt từ đầu cho an toàn.
        int index = comparable.length() == text.length() ? comparable.indexOf(query.text()) : -1;
        if (index < 0) {
            index = 0;
        }
        int start = Math.max(0, index - SNIPPET_RADIUS);
        int end = Math.min(text.length(), index + query.text().length() + SNIPPET_RADIUS);
        return (start > 0 ? "…" : "") + text.substring(start, end) + (end < text.length() ? "…" : "");
    }
}