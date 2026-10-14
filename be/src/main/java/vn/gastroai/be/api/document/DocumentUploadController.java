package vn.gastroai.be.api.document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.document.DocumentStatusService;
import vn.gastroai.be.application.document.DocumentUploadService;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentUploadController {

    private final DocumentUploadService documentUploadService;
    private final DocumentStatusService documentStatusService;

    public DocumentUploadController(
            DocumentUploadService documentUploadService,
            DocumentStatusService documentStatusService) {
        this.documentUploadService = documentUploadService;
        this.documentStatusService = documentStatusService;
    }

    @PostMapping
    public ResponseEntity<DocumentStatusResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl) {
        Document document = documentUploadService.upload(file, sourceUrl);

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(documentStatusService.list(q, status, page, size));
    }

    @PostMapping("/{documentId}/retry")
    public ResponseEntity<DocumentStatusResponse> retry(@PathVariable Long documentId) {
        Document document = documentStatusService.retry(documentId);
        return ResponseEntity.accepted().body(toResponse(document));
    }

    /**
     * UC0049 - tai len phien ban moi cho 1 tai lieu da co. Ban moi chua duoc dung
     * cho
     * toi khi xu ly xong (xem DocumentStatusService.markDone).
     */
    @PostMapping("/{documentId}/versions")
    public ResponseEntity<DocumentStatusResponse> uploadVersion(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl) {
        Document document = documentUploadService.uploadNewVersion(documentId, file, sourceUrl);
        return ResponseEntity.accepted().body(toResponse(document));
    }

    /** UC0049 - lich su moi phien ban cua tai lieu, ban moi nhat dung dau. */
    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentStatusService.listVersions(documentId));
    }

    /** UC0049 - chuyen chatbot sang dung 1 phien ban cu (phai da xu ly xong). */
    @PostMapping("/{documentId}/versions/{versionId}/activate")
    public ResponseEntity<Void> activateVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        documentStatusService.activateVersion(documentId, versionId);
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - go tai lieu khoi chatbot (giu du lieu). */
    @PostMapping("/{documentId}/unpublish")
    public ResponseEntity<Void> unpublish(@PathVariable Long documentId) {
        documentStatusService.unpublish(documentId);
        return ResponseEntity.noContent().build();
    }

    /** UC0050 - dang lai phien ban DONE moi nhat. */
    @PostMapping("/{documentId}/publish")
    public ResponseEntity<Void> publish(@PathVariable Long documentId) {
        documentStatusService.publish(documentId);
        return ResponseEntity.noContent().build();
    }

    /** Xoa rieng mot phien ban (khong xoa ca tai lieu). */
    @DeleteMapping("/{documentId}/versions/{versionId}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable Long documentId,
            @PathVariable Long versionId) {
        documentUploadService.deleteVersion(documentId, versionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Long documentId) {
        documentUploadService.delete(documentId);
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
