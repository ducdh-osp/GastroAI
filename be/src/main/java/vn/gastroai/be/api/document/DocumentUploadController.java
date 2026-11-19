package vn.gastroai.be.api.document;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import vn.gastroai.be.application.document.DocumentReviewService;
import vn.gastroai.be.application.document.DocumentStatusService;
import vn.gastroai.be.application.document.DocumentUploadService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentUploadController {

    private final DocumentUploadService documentUploadService;
    private final DocumentStatusService documentStatusService;
    private final AuditLogService auditLogService;
    private final DocumentReviewService documentReviewService;

    public DocumentUploadController(
            DocumentUploadService documentUploadService,
            DocumentStatusService documentStatusService,
            AuditLogService auditLogService,
            DocumentReviewService documentReviewService) {
        this.documentUploadService = documentUploadService;
        this.documentStatusService = documentStatusService;
        this.auditLogService = auditLogService;
        this.documentReviewService = documentReviewService;
    }

    @PostMapping
    public ResponseEntity<DocumentStatusResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
            @RequestParam(value = "title", required = false) String title) {
        Document document = documentUploadService.upload(file, sourceUrl, title);
        auditLogService.record(AuditAction.DOCUMENT_UPLOAD, "DOCUMENT", document.getId(),
                "Tải lên: " + document.getTitle());
        return ResponseEntity.accepted().body(toResponse(document));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getStatus(
            @PathVariable Long documentId) {
        return documentStatusService.findDocument(documentId)
                .map(document -> ResponseEntity.ok(toResponse(document)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<DocumentPageResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate updatedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(documentStatusService.list(q, status, updatedFrom, updatedTo, page, size));
    }

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<DocumentStatusResponse> retry(@PathVariable Long documentId) {
        Document document = documentStatusService.retry(documentId);
        return ResponseEntity.accepted().body(toResponse(document));
    }

    /**
     * UC0049 - tai len phien ban moi cho 1 tai lieu da co. Ban moi chua duoc dung
     * cho toi khi xu ly xong VA Bien tap xac nhan noi dung (xem approveContent).
     */
    @PostMapping("/{documentId}/versions")
    public ResponseEntity<DocumentStatusResponse> uploadVersion(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {
        Document document = documentUploadService.uploadNewVersion(documentId, file);
        auditLogService.record(AuditAction.DOCUMENT_NEW_VERSION, "DOCUMENT", documentId,
                "Phiên bản mới id=" + document.getId());
        return ResponseEntity.accepted().body(toResponse(document));
    }

    /** UC0049 - lich su moi phien ban cua tai lieu, ban moi nhat dung dau. */
    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentStatusService.listVersions(documentId));
    }
    @GetMapping("/{documentId}/review")
    public ResponseEntity<DocumentReviewResponse> review(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentReviewService.review(documentId));
    }

    /**
     * File gốc của 1 phiên bản, mở xem ngay trên trình duyệt (inline) chứ không tải về.
     * FE tải bằng axios rồi tạo đường dẫn blob: cho iframe, nên không cần nới X-Frame-Options.
     */
    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> originalFile(@PathVariable Long documentId) {
        DocumentReviewService.OriginalFile file = documentReviewService.originalFile(documentId);
        MediaType contentType = "PDF".equals(file.fileType())
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(file.path().getFileName().toString(), StandardCharsets.UTF_8).build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new FileSystemResource(file.path()));
    }

    /** UC0048 / SCR-AD-04 §3.1b - Biên tập xác nhận nội dung trước khi chatbot dùng (SecurityConfig chỉ cho CONTENT_EDITOR). */
    @PostMapping("/{documentId}/versions/{versionId}/approve")
    public ResponseEntity<Void> approveContent(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        documentStatusService.approveContent(documentId, versionId, auditLogService.currentActorEmail());
        auditLogService.record(AuditAction.DOCUMENT_APPROVE_CONTENT, "DOCUMENT", documentId,
                "Xác nhận nội dung phiên bản id=" + versionId);
        return ResponseEntity.noContent().build();
    }

    /** UC0049 - chuyen chatbot sang dung 1 phien ban cu (phai da xu ly xong). */
    @PostMapping("/{documentId}/versions/{versionId}/activate")
    public ResponseEntity<Void> activateVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        documentStatusService.activateVersion(documentId, versionId);
        auditLogService.record(AuditAction.DOCUMENT_ACTIVATE_VERSION, "DOCUMENT", documentId,
                "Kích hoạt phiên bản id=" + versionId);
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - go tai lieu khoi chatbot (giu du lieu). */
    @PostMapping("/{documentId}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable Long documentId) {
        documentStatusService.unpublish(documentId);
        auditLogService.record(AuditAction.DOCUMENT_UNPUBLISH, "DOCUMENT", documentId, null);
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - dang lai phien ban DONE moi nhat da duoc xac nhan noi dung. */
    @PostMapping("/{documentId}/publish")
    public ResponseEntity<Void> publish(@PathVariable Long documentId) {
        documentStatusService.publish(documentId);
        auditLogService.record(AuditAction.DOCUMENT_PUBLISH, "DOCUMENT", documentId, null);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{documentId}")
    public ResponseEntity<Void> updateInfo(
            @PathVariable Long documentId,
            @RequestBody UpdateDocumentInfoRequest request) {
        documentStatusService.updateInfo(documentId, request.title(), request.sourceUrl());
        auditLogService.record(AuditAction.DOCUMENT_UPDATE_INFO, "DOCUMENT", documentId,
                "Tên: " + request.title() + " | URL: " + request.sourceUrl());
        return ResponseEntity.noContent().build();
    }

    /** Xoa rieng mot phien ban (khong xoa ca tai lieu). */
    @DeleteMapping("/{documentId}/versions/{versionId}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        documentUploadService.deleteVersion(documentId, versionId);
        auditLogService.record(AuditAction.DOCUMENT_DELETE_VERSION, "DOCUMENT", documentId,
                "Xoá phiên bản id=" + versionId);
        return ResponseEntity.noContent().build();
    }

   @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Long documentId) {
        documentUploadService.delete(documentId);
        auditLogService.record(AuditAction.DOCUMENT_DELETE, "DOCUMENT", documentId, null);
        return ResponseEntity.noContent().build();
    }

    private DocumentStatusResponse toResponse(Document document) {
        return new DocumentStatusResponse(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                document.getProcessingStage(),
                document.getErrorMessage());
    }

    @GetMapping("/search")
    public ResponseEntity<DocumentSearchResult> search(@RequestParam("q") String q) {
        return ResponseEntity.ok(documentStatusService.searchContent(q));
    }

    public record DocumentStatusResponse(
            Long documentId,
            String title,
            DocumentStatus status,
            DocumentProcessingStage processingStage,
            String errorMessage) {
    }
}