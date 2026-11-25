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
import vn.gastroai.be.application.document.DocumentContentSearchService;
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
    private final DocumentContentSearchService documentContentSearchService;

    public DocumentUploadController(
            DocumentUploadService documentUploadService,
            DocumentStatusService documentStatusService,
            AuditLogService auditLogService,
            DocumentReviewService documentReviewService,
            DocumentContentSearchService documentContentSearchService) {
        this.documentUploadService = documentUploadService;
        this.documentStatusService = documentStatusService;
        this.auditLogService = auditLogService;
        this.documentReviewService = documentReviewService;
        this.documentContentSearchService = documentContentSearchService;
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
     * UC0049 / SCR-AD-04 §3.2 - tai ban thay the cho 1 tai lieu da co. Ban cu van phuc vu
     * chatbot cho toi khi ban thay the xu ly xong VA Bien tap xac nhan noi dung (approveContent),
     * luc do ban cu bi xoa vinh vien.
     */
    @PostMapping("/{documentId}/replacement")
    public ResponseEntity<DocumentStatusResponse> uploadReplacement(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file) {
        Document document = documentUploadService.uploadReplacement(documentId, file);
        auditLogService.record(AuditAction.DOCUMENT_REPLACE, "DOCUMENT", documentId,
                "Bản thay thế id=" + document.getId());
        return ResponseEntity.accepted().body(toResponse(document));
    }

    @GetMapping("/{documentId}/review")
    public ResponseEntity<DocumentReviewResponse> review(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentReviewService.review(documentId));
    }

    /**
     * File gốc của 1 bản, mở xem ngay trên trình duyệt (inline) chứ không tải về.
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
    @PostMapping("/{documentId}/approve")
    public ResponseEntity<Void> approveContent(@PathVariable Long documentId) {
        boolean replaced = documentStatusService.approveContent(documentId, auditLogService.currentActorEmail());
        auditLogService.record(AuditAction.DOCUMENT_APPROVE_CONTENT, "DOCUMENT", documentId,
                replaced ? "Xác nhận bản thay thế · đã xoá vĩnh viễn bản cũ" : "Xác nhận nội dung");
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - go tai lieu khoi chatbot (giu du lieu). */
    @PostMapping("/{documentId}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable Long documentId) {
        documentStatusService.unpublish(documentId);
        auditLogService.record(AuditAction.DOCUMENT_UNPUBLISH, "DOCUMENT", documentId, null);
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - dang lai ban da duoc xac nhan noi dung. */
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

        /** UC0055 - tìm trong nội dung: mỗi tài liệu 1 dòng, khớp nhiều nhất lên đầu. */
    @GetMapping("/search")
    public ResponseEntity<DocumentSearchResult> search(@RequestParam("q") String q) {
        return ResponseEntity.ok(documentContentSearchService.searchDocuments(q));
    }

    /** Các đoạn khớp từ khoá trong 1 tài liệu - FE gọi khi bấm "Xem các đoạn". */
    @GetMapping("/{documentId}/search")
    public ResponseEntity<List<DocumentReviewResponse.Chunk>> searchInDocument(
            @PathVariable Long documentId, @RequestParam("q") String q) {
        return ResponseEntity.ok(documentContentSearchService.searchInDocument(documentId, q));
    }
    
    public record DocumentStatusResponse(
            Long documentId,
            String title,
            DocumentStatus status,
            DocumentProcessingStage processingStage,
            String errorMessage) {
    }
}