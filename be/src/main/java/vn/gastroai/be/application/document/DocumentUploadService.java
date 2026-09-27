package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;

import java.nio.file.Path;

@Service
public class DocumentUploadService {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingWorker documentProcessingWorker;

    public DocumentUploadService(
            FileStorageService fileStorageService,
            DocumentRepository documentRepository,
            DocumentProcessingWorker documentProcessingWorker
    ) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.documentProcessingWorker = documentProcessingWorker;
    }

    public Document upload(MultipartFile file) {

        // 1. Validate + lưu file xuống storage
        Path storedPath = fileStorageService.store(file);

        try {
            // 2. Tạo document với trạng thái PENDING
            Document document = new Document();

            document.setTitle(file.getOriginalFilename());
            document.setSource(storedPath.toString());
            document.setStatus(DocumentStatus.PENDING);

            // 3. Lưu document vào database
            document = documentRepository.save(document);

            // 4. Chạy xử lý document ở background
            documentProcessingWorker.processAsync(
                    document.getId(),
                    document.getSource()
            );

            // 5. Trả document cho Controller
            return document;

        } catch (RuntimeException e) {

            // DB insert thất bại → xóa file vừa lưu
            fileStorageService.delete(storedPath);

            throw e;
        }
    }
}