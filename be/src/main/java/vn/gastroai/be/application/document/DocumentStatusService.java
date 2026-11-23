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
import vn.gastroai.be.application.support.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import vn.gastroai.be.api.document.DocumentStats;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentGroupRow;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentListRow;

@Service
public class DocumentStatusService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStatusService.class);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentGroupPruner groupPruner;

    public DocumentStatusService(DocumentRepository documentRepository, ChunkRepository chunkRepository,
                                 DocumentGroupPruner groupPruner) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.groupPruner = groupPruner;
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
        // Xu ly lai thi noi dung co the khac, phai xac nhan lai.
        document.setContentApprovedAt(null);
        document.setContentApprovedBy(null);

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
        // SCR-AD-04 §3.1b - xu ly ky thuat xong chua phai la xong: cho Bien tap xac nhan noi dung
        // (approveContent) roi chatbot moi dung. Ban dang dung (neu co) van phuc vu trong luc cho.

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
        return list(query, status, null, null, page, size);
    }

    /**
     * UC0055 / SCR-AD-04 §3.4 - tìm theo tên, nguồn và thời điểm cập nhật.
     * Ngày lọc tính theo giờ Việt Nam, cả hai đầu đều tính trọn ngày.
     */
    @Transactional(readOnly = true)
    public vn.gastroai.be.api.document.DocumentPageResponse list(
            String query,
            DocumentStatus status,
            java.time.LocalDate updatedFrom,
            java.time.LocalDate updatedTo,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        if (updatedFrom != null && updatedTo != null && updatedFrom.isAfter(updatedTo)) {
            throw new IllegalArgumentException("Từ ngày phải trước hoặc bằng Đến ngày");
        }
        Instant updatedFromInclusive = updatedFrom == null ? null
                : updatedFrom.atStartOfDay(vn.gastroai.be.application.support.VietnamDateRange.ZONE).toInstant();
        Instant updatedToExclusive = updatedTo == null ? null
                : updatedTo.plusDays(1).atStartOfDay(vn.gastroai.be.application.support.VietnamDateRange.ZONE).toInstant();

        String normalizedQuery = query == null || query.isBlank()
                ? ""
                : vn.gastroai.be.application.support.VietnameseText.normalize(query.trim());

        // Moi tai lieu 1 dong (ban thay the neu dang co, khong thi dong chinh), KHONG co
        // full_text cho nhe.
        List<DocumentListRow> latestPerGroup = documentRepository.findDisplayRows();

        // Thong tin theo tai lieu: co ban thay the khong, dong chatbot dang dung, da go hay chua.
        Map<Long, DocumentGroupRow> groups = documentRepository.findGroupRows().stream()
                .collect(Collectors.toMap(DocumentGroupRow::getGroupId, g -> g));

        DocumentStats stats = new DocumentStats(
                latestPerGroup.size(),
                (int) groups.values().stream().filter(g -> g.getInUseId() != null).count(),
                (int) latestPerGroup.stream()
                        .filter(d -> d.getStatus() == DocumentStatus.PENDING
                                || d.getStatus() == DocumentStatus.PROCESSING)
                        .count(),
                (int) latestPerGroup.stream().filter(d -> d.getStatus() == DocumentStatus.ERROR).count());

        List<DocumentListRow> filtered = latestPerGroup.stream()
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> updatedFromInclusive == null
                        || (d.getUpdatedAt() != null && !d.getUpdatedAt().isBefore(updatedFromInclusive)))
                .filter(d -> updatedToExclusive == null
                        || (d.getUpdatedAt() != null && d.getUpdatedAt().isBefore(updatedToExclusive)))
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
                            group != null && group.getInUseId() != null,
                            group != null && group.getRowCount() > 1,
                            group != null && group.getUnpublishedAt() != null,
                            row.getContentApprovedAt() != null);
                })
                .toList();

        return new vn.gastroai.be.api.document.DocumentPageResponse(items, filtered.size(), page, size, stats);
    }

    /**
     * id cua dong ma so chunk duoc tinh: ban dang dung neu co, khong thi chinh dong
     * dang hien.
     */
    private static Long chunkOwnerId(DocumentListRow row, DocumentGroupRow group) {
        return group != null && group.getInUseId() != null ? group.getInUseId() : row.getId();
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

    /**
     * UC0048 / SCR-AD-04 §3.1b - Biên tập xác nhận đã đối chiếu nội dung với nguồn y khoa đã công bố.
     * Sau bước này chatbot mới dùng dòng này (trừ khi tài liệu đang bị gỡ).
     * <p>
     * §3.2: nếu đây là bản THAY THẾ, bản đang dùng bị xoá vĩnh viễn ngay lúc này
     * (document + chunk + embedding) - không giữ lịch sử, không khôi phục được.
     *
     * @return true nếu đã thay thế (xoá) bản cũ
     */
    @Transactional
    public boolean approveContent(Long documentId, String approverEmail) {
        Document target = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));

        if (target.getStatus() != DocumentStatus.DONE) {
            throw new IllegalStateException("Chỉ xác nhận được bản đã xử lý xong");
        }
        if (target.getContentApprovedAt() != null) {
            throw new IllegalStateException("Bản này đã được xác nhận nội dung");
        }
        boolean replacesOld = target.isReplacement();
        if (replacesOld) {
            // Lam TRUOC khi dong vao target: truy van findGroup se flush target, ma ban thay the
            // khong duoc mang co "da xac nhan" (ck_documents_replacement_pending).
            groupPruner.keepOnly(target, documentRepository.findGroup(target.groupId()));
        }

        // Toi day target la dong chinh. Chatbot dung no ngay, tru khi tai lieu dang bi go
        // (co "da go" da duoc chuyen sang tu dong chinh cu) - khi do cho toi luc dang lai.
        target.setContentApprovedAt(Instant.now());
        target.setContentApprovedBy(approverEmail);
        documentRepository.save(target);
        return replacesOld;
    }

    /** UC0050 - gỡ tài liệu khỏi chatbot nhưng giữ nguyên dữ liệu, có thể đăng lại sau. */
    @Transactional
    public void unpublish(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Document main = mainRowOf(document);
        if (!main.isInUse()) {
            throw new IllegalStateException("Chatbot đang không dùng tài liệu này nên không có gì để gỡ");
        }

        main.setUnpublishedAt(Instant.now());
        documentRepository.save(main);
    }

    /** UC0050 - đăng lại tài liệu đã gỡ: chatbot dùng lại bản đã được Biên tập xác nhận. */
    @Transactional
    public void publish(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Document main = mainRowOf(document);
        if (main.getUnpublishedAt() == null) {
            throw new IllegalStateException("Tài liệu chưa bị gỡ khỏi chatbot nên không cần đăng lại");
        }
        // Khong can kiem tra "da xac nhan chua": chi tai lieu da xac nhan moi go duoc
        // (ck_documents_unpublished_requires_approval).
        main.setUnpublishedAt(null);
        documentRepository.save(main);
    }

    @Transactional
    public void updateInfo(Long documentId, String rawTitle, String rawSourceUrl) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        String title = DocumentTitle.normalize(rawTitle);
        String sourceUrl = DocumentUploadService.normalizeSourceUrl(rawSourceUrl);
        documentRepository.updateGroupInfo(document.groupId(), title, sourceUrl, Instant.now());
    }

    /** Dong chinh cua tai lieu chua dong nay (chinh no neu no la dong chinh). */
    private Document mainRowOf(Document row) {
        Long groupId = row.groupId();
        if (groupId.equals(row.getId())) {
            return row;
        }
        return documentRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + groupId));
    }

    private Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy document: " + documentId));
    }

}