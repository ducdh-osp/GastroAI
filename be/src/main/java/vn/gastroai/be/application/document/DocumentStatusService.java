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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStatusService.class);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private static final int SNIPPET_RADIUS = 120;

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

        // UC0049 - phien ban moi xu ly xong thi tro thanh ban dang dung, TRU KHI admin da
        // chu dong go tai lieu (dong goc co unpublished_at) - luc do chi danh dau DONE.
        // clearCurrent chay TRUOC, bat ban nay SAU, de khong bao gio co 2 ban cung la
        // hien hanh trong 1 nhom (index uk_documents_group_current).
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

        List<Document> all = documentRepository.findAllByOrderByUploadedAtDesc();

        // UC0049 - moi phien ban la 1 dong rieng, nen gom theo ma nhom de moi tai lieu
        // chi
        // hien 1 dong (ban moi nhat). LinkedHashMap giu thu tu "tai len moi nhat
        // truoc".
        Map<Long, List<Document>> groups = all.stream()
                .collect(Collectors.groupingBy(Document::groupId, LinkedHashMap::new, Collectors.toList()));

        List<Document> latestPerGroup = groups.values().stream()
                .map(versions -> versions.stream()
                        .max(Comparator.comparingInt(Document::getVersionNo))
                        .orElseThrow())
                .toList();

        List<Document> filtered = latestPerGroup.stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> normalizedQuery.isEmpty()
                        || (d.getTitle() != null && vn.gastroai.be.application.support.VietnameseText
                                .normalize(d.getTitle()).contains(normalizedQuery))
                        || (d.getSourceUrl() != null && vn.gastroai.be.application.support.VietnameseText
                                .normalize(d.getSourceUrl()).contains(normalizedQuery)))
                .toList();

        // Lấy số lượng chunks cho từng document
        java.util.Map<Long, Long> chunkCounts = chunkRepository.countByDocumentIdGrouped().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new vn.gastroai.be.api.document.DocumentPageResponse(List.of(), filtered.size(), page, size);
        }

        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());

        List<vn.gastroai.be.api.document.DocumentSummaryResponse> items = filtered.subList(start, end).stream()
                .map(d -> new vn.gastroai.be.api.document.DocumentSummaryResponse(
                        d.getId(),
                        d.getTitle(),
                        d.getSource(),
                        d.getSourceUrl(),
                        d.getStatus(),
                        d.getProcessingStage(),
                        d.getErrorMessage(),
                        chunkCounts.getOrDefault(d.getId(), 0L),
                        d.getUploadedAt(),
                        d.getUpdatedAt(),
                        d.getVersionNo(),
                        groups.get(d.groupId()).stream()
                                .filter(Document::isCurrentVersion)
                                .map(Document::getVersionNo)
                                .findFirst()
                                .orElse(null),
                        groups.get(d.groupId()).size(),
                        groups.get(d.groupId()).stream()
                                .anyMatch(v -> v.getRootId() == null && v.getUnpublishedAt() != null)))
                .toList();

        return new vn.gastroai.be.api.document.DocumentPageResponse(items, filtered.size(), page, size);
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
        documentRepository.clearCurrent(document.groupId());

        // Danh dau "go co chu dich" tren dong goc. clearCurrent la cau UPDATE hang loat nen
        // ban trong bo nho co the con ghi "dang dung" (cu) - dong bo lai truoc khi save, neu
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
    public List<DocumentSearchHit> searchContent(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new IllegalArgumentException("Nhập ít nhất 2 ký tự để tìm kiếm");
        }
        String escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return chunkRepository.searchContent(escaped).stream()
                .map(row -> new DocumentSearchHit(row.getDocumentId(), row.getTitle(),
                        row.getChunkIndex(), snippet(row.getContent(), q)))
                .toList();
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