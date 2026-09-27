package vn.gastroai.be.api.document;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.document.DocumentUploadService;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentUploadController {

    private final DocumentUploadService documentUploadService;

    public DocumentUploadController(
            DocumentUploadService documentUploadService
    ) {
        this.documentUploadService = documentUploadService;
    }

    @PostMapping
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestParam("file") MultipartFile file
    ) {
        Document document =
                documentUploadService.upload(file);

        return ResponseEntity.accepted().body(
                new DocumentUploadResponse(
                        document.getId(),
                        document.getTitle(),
                        document.getStatus()
                )
        );
    }

    public record DocumentUploadResponse(
            Long documentId,
            String title,
            DocumentStatus status
    ) {
    }
}