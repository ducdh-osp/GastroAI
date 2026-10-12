package vn.gastroai.be.api.document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.document.DocumentStatusService;
import vn.gastroai.be.application.document.DocumentUploadService;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentUploadController {

    private final DocumentUploadService documentUploadService;
    private final DocumentStatusService documentStatusService;

    public DocumentUploadController(
            DocumentUploadService documentUploadService,
            DocumentStatusService documentStatusService
    ) {
        this.documentUploadService = documentUploadService;
        this.documentStatusService = documentStatusService;
    }

    @PostMapping
    public ResponseEntity<DocumentStatusResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceUrl", required = false) String sourceUrl
    ) {
        Document document =
                documentUploadService.upload(file, sourceUrl);

        return ResponseEntity.accepted().body(toResponse(document));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentStatusResponse> getStatus(
            @PathVariable Long documentId
    ) {
        return documentStatusService.findDocument(documentId)
                .map(document -> ResponseEntity.ok(toResponse(document)))
                .orElseGet(() -> ResponseEntity.notFound().build());
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

    public record DocumentStatusResponse(
            Long documentId,
            String title,
            DocumentStatus status,
            DocumentProcessingStage processingStage,
            String errorMessage
    ) {
    }
}
