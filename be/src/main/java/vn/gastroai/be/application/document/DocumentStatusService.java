package vn.gastroai.be.application.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.ChunkRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;
import vn.gastroai.be.api.document.DocumentSearchHit;
import vn.gastroai.be.api.document.DocumentVersionResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import vn.gastroai.be.api.document.DocumentSearchResult;
import vn.gastroai.be.api.document.DocumentStats;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentGroupRow;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentListRow;

@Service
public class DocumentStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStatusService.class);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private static final int SNIPPET_RADIUS = 120;
    private static final int MAX_QUERY_LENGTH = 200;
    private static final int MAX_SEARCH_RESULTS = 50;

    public DocumentStatusService(DocumentRepository documentRepository, ChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Document> findDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }

    @Transactional
    public void markProcessing(Long documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.PROCESSING);
        document.setProcessingStage(DocumentProcessingStage.EXTRACTION);
        document.setErrorMessage(null);

        documentRepository.save(document);
    }

    @Transactional
    public void markChunking(Long documentId) {
        Document document = getDocument(documentId);
        document.setProcessingStage(DocumentProcessingStage.CHUNKING);
        documentRepository.save(document);
    }

    @Transactional
    public void markEmbedding(Long documentId) {
        Document document = getDocument(documentId);
        document.setProcessingStage(DocumentProcessingStage.EMBEDDING);
        documentRepository.save(document);
    }

    @Transactional
    public void markDone(Long documentId) {
        Document document = getDocument(documentId);

        document.setStatus(DocumentStatus.DONE);
        document.setProcessingStage(DocumentProcessingStage.DONE);
        document.setErrorMessage(null);

        if (!document.isCurrentVersion() && rootOf(document).getUnpublishedAt() == null) {
            documentRepository.clearCurrent(document.groupId());
            document.setCurrentVersion(true);
        }

        documentRepository.save(document);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markError(Long documentId, String errorMessage) {
        Optional<Document> maybeDocument = documentRepository.findById(documentId);

        if (maybeDocument.isEmpty()) {
            log.warn("Document id={} khong con ton tai (co the da bi xoa) - bo qua markError", documentId);
            return;
        }

        Document document = maybeDocument.get();

        document.setStatus(DocumentStatus.ERROR);
        document.setProcessingStage(DocumentProcessingStage.ERROR);
        document.setErrorMessage(errorMessage);

        documentRepository.save(document);
        chunkRepository.deleteByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public vn.gastroai.be.api.document.DocumentPageResponse list(
            String query,
            DocumentStatus status,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }

        String normalizedQuery = query == null || query.isBlank()
                ? ""
                : vn.gastroai.be.application.support.VietnameseText.normalize(query.trim());

        // Moi tai lieu 1 dong (phien ban moi nhat), KHONG co full_text - nhe hon nhieu
        // so voi
        // tai moi phien ban cua moi tai lieu.
        List<DocumentListRow> latestPerGroup = documentRepository.findLatestVersionRows();

        // Thong tin theo nhom: so phien ban, ban dang dung, da go hay chua.
        Map<Long, DocumentGroupRow> groups = documentRepository.findGroupRows().stream()
                .collect(Collectors.toMap(DocumentGroupRow::getGroupId, g -> g));

        DocumentStats stats = new DocumentStats(
        latestPerGroup.size(),
        (int) groups.values().stream().filter(g -> g.getCurrentVersionNo() != null).count(),
        (int) latestPerGroup.stream()
                .filter(d -> d.getStatus() == DocumentStatus.PENDING || d.getStatus() == DocumentStatus.PROCESSING)
                .count(),
        (int) latestPerGroup.stream().filter(d -> d.getStatus() == DocumentStatus.ERROR).count());

        List<DocumentListRow> filtered = latestPerGroup.stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> normalizedQuery.isEmpty()
                        || (d.getTitle() != null && vn.gastroai.be.application.support.VietnameseText
                                .normalize(d.getTitle()).contains(normalizedQuery))
                        || (d.getSourceUrl() != null && vn.gastroai.be.application.support.VietnameseText
                                .normalize(d.getSourceUrl()).contains(normalizedQuery)))
                .toList();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new vn.gastroai.be.api.document.DocumentPageResponse(List.of(), filtered.size(), page, size, stats);
        }

        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());
        List<DocumentListRow> pageRows = filtered.subList(start, end);

        // Dem chunk CHI cho cac dong cua trang hien tai, theo ban dang dung (neu co),
        // khong co thi theo ban moi nhat.
        List<Long> chunkOwnerIds = pageRows.stream()
                .map(row -> chunkOwnerId(row, groups.get(row.getGroupId())))
                .distinct()
                .toList();
        Map<Long, Long> chunkCounts = chunkRepository.countByDocumentIdIn(chunkOwnerIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        List<vn.gastroai.be.api.document.DocumentSummaryResponse> items = pageRows.stream()
                .map(row -> {
                    DocumentGroupRow group = groups.get(row.getGroupId());
                    return new vn.gastroai.be.api.document.DocumentSummaryResponse(
                            row.getId(),
                            row.getTitle(),
                            row.getSource(),
                            row.getSourceUrl(),
                            row.getStatus(),
                            row.getProcessingStage(),
                            row.getErrorMessage(),
                            chunkCounts.getOrDefault(chunkOwnerId(row, group), 0L),
                            row.getUploadedAt(),
                            row.getUpdatedAt(),
                            row.getVersionNo(),
                            group == null ? null : group.getCurrentVersionNo(),
                            group == null ? 1 : group.getVersionCount().intValue(),
                            group != null && group.getUnpublishedAt() != null);
                })
                .toList();

        return new vn.gastroai.be.api.document.DocumentPageResponse(items, filtered.size(), page, size, stats);
    }

    /**
     * id cua dong ma so chunk duoc tinh: ban dang dung neu co, khong thi chinh dong
     * dang hien.
     */
    private static Long chunkOwnerId(DocumentListRow row, DocumentGroupRow group) {
        return group != null && group.getCurrentId() != null ? group.getCurrentId() : row.getId();
    }

    @Transactional
    public Document retry(Long documentId) {
        Document document = getDocument(documentId);
        if (document.getStatus() != DocumentStatus.ERROR) {
            throw new IllegalStateException("Chỉ có thể thử lại các tài liệu đang ở trạng thái lỗi (ERROR)");
        }
        document.setStatus(DocumentStatus.PENDING);
        document.setProcessingStage(DocumentProcessingStage.PENDING);
        document.setErrorMessage(null);
        return documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersions(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        return documentRepository.findGroup(document.groupId()).stream()
                .map(d -> new DocumentVersionResponse(d.getId(), d.getVersionNo(), d.getTitle(), d.getStatus(),
                        d.isCurrentVersion(), d.getUploadedAt(), d.getErrorMessage()))
                .toList();
    }

    @Transactional
    public void activateVersion(Long documentId, Long versionId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Document target = documentRepository.findById(versionId)
                .filter(v -> v.groupId().equals(document.groupId()))
                .orElseThrow(() -> new ResourceNotFoundException("Phiên bản không thuộc tài liệu này"));

        if (target.getStatus() != DocumentStatus.DONE) {
            throw new IllegalStateException("Chỉ dùng được phiên bản đã xử lý xong");
        }
        clearUnpublishedFlag(target);
        if (target.isCurrentVersion()) {
            return;
        }
        documentRepository.clearCurrent(target.groupId());
        target.setCurrentVersion(true);
        documentRepository.save(target);
    }

    /**
     * UC0050 - go tai lieu khoi chatbot nhung giu nguyen du lieu, co the dang lai
     * sau.
     */
    @Transactional
    public void unpublish(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));

        boolean hasDone = documentRepository.findGroup(document.groupId()).stream()
                .anyMatch(v -> v.getStatus() == DocumentStatus.DONE);
        if (!hasDone) {
            throw new IllegalStateException("Tài liệu chưa có phiên bản nào xử lý xong, chưa có gì để gỡ");
        }

        documentRepository.clearCurrent(document.groupId());

        // Danh dau "go co chu dich" tren dong goc. clearCurrent la cau UPDATE hang loat
        // nen
        // ban trong bo nho co the con ghi "dang dung" (cu) - dong bo lai truoc khi
        // save, neu
        // khong save(root) se ghi de is_current = true tro lai.
        Document root = rootOf(document);
        root.setCurrentVersion(false);
        root.setUnpublishedAt(Instant.now());
        documentRepository.save(root);
    }

    /** UC0050 - dang lai: bat phien ban DONE moi nhat cua nhom. */
    @Transactional
    public void publish(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Long groupId = document.groupId();
        Document latestDone = documentRepository.findGroup(groupId).stream()
                .filter(v -> v.getStatus() == DocumentStatus.DONE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa có phiên bản nào xử lý xong để đăng"));

        clearUnpublishedFlag(latestDone);
        if (latestDone.isCurrentVersion()) {
            return;
        }
        documentRepository.clearCurrent(groupId);
        latestDone.setCurrentVersion(true);
        documentRepository.save(latestDone);
    }

    /**
     * UC0055 - tim cum tu trong noi dung tai lieu dang dung, tra doan trich quanh
     * cho khop.
     */
    @Transactional(readOnly = true)
    public DocumentSearchResult searchContent(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new IllegalArgumentException("Nhập ít nhất 2 ký tự để tìm kiếm");
        }
        if (q.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Từ khóa quá dài (tối đa " + MAX_QUERY_LENGTH + " ký tự)");
        }
        String escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        // SQL lay toi da 51 dong: du 51 nghia la con ket qua chua hien -> truncated.
        var rows = chunkRepository.searchContent(escaped);
        boolean truncated = rows.size() > MAX_SEARCH_RESULTS;

        List<DocumentSearchHit> items = rows.stream()
                .limit(MAX_SEARCH_RESULTS)
                .map(row -> new DocumentSearchHit(row.getDocumentId(), row.getTitle(),
                        row.getChunkIndex(), snippet(row.getContent(), q)))
                .toList();
        return new DocumentSearchResult(items, truncated);
    }

    /** Cat ~120 ky tu truoc va sau vi tri khop dau tien, them "…" neu bi cat. */
    static String snippet(String content, String q) {
        String text = content.replaceAll("\\s+", " ").trim();
        int index = text.toLowerCase(Locale.ROOT).indexOf(q.toLowerCase(Locale.ROOT));
        if (index < 0) {
            index = 0;
        }
        int start = Math.max(0, index - SNIPPET_RADIUS);
        int end = Math.min(text.length(), index + q.length() + SNIPPET_RADIUS);
        return (start > 0 ? "…" : "") + text.substring(start, end) + (end < text.length() ? "…" : "");
    }

    /** Dong goc cua nhom chua phien ban nay (chinh no neu no la ban goc). */
    private Document rootOf(Document version) {
        Long groupId = version.groupId();
        if (groupId.equals(version.getId())) {
            return version;
        }
        return documentRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + groupId));
    }

    /** Dang lai / dung 1 ban cu thi bo co "da go" tren dong goc. */
    private void clearUnpublishedFlag(Document version) {
        Document root = rootOf(version);
        if (root.getUnpublishedAt() != null) {
            root.setUnpublishedAt(null);
            documentRepository.save(root);
        }
    }

    private Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy document: " + documentId));
    }

}