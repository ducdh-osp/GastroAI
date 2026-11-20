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
import vn.gastroai.be.api.document.DocumentVersionResponse;
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
                            row.getVersionNo(),
                            group == null ? null : group.getCurrentVersionNo(),
                            group == null ? 1 : group.getVersionCount().intValue(),
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
                        d.isCurrentVersion(), d.getUploadedAt(), d.getErrorMessage(),
                        d.getContentApprovedAt() != null, d.getContentApprovedBy(), d.getContentApprovedAt()))
                .toList();
    }

    @Transactional
    public void activateVersion(Long documentId, Long versionId) {
        Document target = versionInGroup(documentId, versionId);

        if (target.getStatus() != DocumentStatus.DONE) {
            throw new IllegalStateException("Chỉ dùng được phiên bản đã xử lý xong");
        }
        if (target.getContentApprovedAt() == null) {
            throw new IllegalStateException("Phiên bản này chưa được Biên tập xác nhận nội dung nên chưa dùng cho AI được");
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
     * UC0048 / SCR-AD-04 §3.1b - Biên tập xác nhận đã đối chiếu nội dung với nguồn y khoa đã công bố.
     * Sau bước này chatbot mới dùng phiên bản: nó thành bản đang dùng nếu mới hơn bản hiện tại
     * và tài liệu không bị gỡ.
     */
    @Transactional
    public void approveContent(Long documentId, Long versionId, String approverEmail) {
        Document target = versionInGroup(documentId, versionId);

        if (target.getStatus() != DocumentStatus.DONE) {
            throw new IllegalStateException("Chỉ xác nhận được phiên bản đã xử lý xong");
        }
        if (target.getContentApprovedAt() != null) {
            throw new IllegalStateException("Phiên bản này đã được xác nhận nội dung");
        }
        target.setContentApprovedAt(Instant.now());
        target.setContentApprovedBy(approverEmail);

        boolean newerThanCurrent = documentRepository.findGroup(target.groupId()).stream()
                .filter(Document::isCurrentVersion)
                .findFirst()
                .map(current -> target.getVersionNo() > current.getVersionNo())
                .orElse(true);
        if (!target.isCurrentVersion() && newerThanCurrent && rootOf(target).getUnpublishedAt() == null) {
            documentRepository.clearCurrent(target.groupId());
            target.setCurrentVersion(true);
        }
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

    /** UC0050 - dang lai: bat phien ban DONE moi nhat da duoc Bien tap xac nhan noi dung. */
    @Transactional
    public void publish(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Long groupId = document.groupId();
        Document latestDone = documentRepository.findGroup(groupId).stream()
                .filter(v -> v.getStatus() == DocumentStatus.DONE && v.getContentApprovedAt() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chưa có phiên bản nào được xác nhận nội dung để đăng"));

        clearUnpublishedFlag(latestDone);
        if (latestDone.isCurrentVersion()) {
            return;
        }
        documentRepository.clearCurrent(groupId);
        latestDone.setCurrentVersion(true);
        documentRepository.save(latestDone);
    }

    @Transactional
    public void updateInfo(Long documentId, String rawTitle, String rawSourceUrl) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        String title = DocumentTitle.normalize(rawTitle);
        String sourceUrl = DocumentUploadService.normalizeSourceUrl(rawSourceUrl);
        documentRepository.updateGroupInfo(document.groupId(), title, sourceUrl, Instant.now());
    }

    /** Phiên bản versionId phải thuộc cùng tài liệu với documentId, nếu không coi như không tìm thấy. */
    private Document versionInGroup(Long documentId, Long versionId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        return documentRepository.findById(versionId)
                .filter(v -> v.groupId().equals(document.groupId()))
                .orElseThrow(() -> new ResourceNotFoundException("Phiên bản không thuộc tài liệu này"));
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