package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

@Service
public class DocumentUploadService {

    private static final int MAX_SOURCE_URL_LENGTH = 2000;

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

    public Document upload(MultipartFile file, String sourceUrl) {

        // 0. Validate URL nguồn TRƯỚC khi lưu file, để URL sai không để lại file mồ côi
        String normalizedSourceUrl = normalizeSourceUrl(sourceUrl);

        // 1. Validate + lưu file xuống storage
        Path storedPath = fileStorageService.store(file);

        try {
            // 2. Tạo document với trạng thái PENDING
            Document document = new Document();

            document.setTitle(file.getOriginalFilename());
            document.setSource(storedPath.toString());
            document.setSourceUrl(normalizedSourceUrl);
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

    /**
     * URL nguồn sẽ hiện thành link cho bệnh nhân, nên chỉ nhận http/https có host
     * (chặn javascript:, data:...). Rỗng/blank coi như không có URL.
     */
    private static String normalizeSourceUrl(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        String url = raw.trim();

        if (url.length() > MAX_SOURCE_URL_LENGTH) {
            throw new IllegalArgumentException(
                    "URL nguồn quá dài (tối đa " + MAX_SOURCE_URL_LENGTH + " ký tự)");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL nguồn không hợp lệ");
        }

        String scheme = uri.getScheme();
        boolean httpLike = scheme != null
                && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));

        if (!httpLike || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "URL nguồn phải là http:// hoặc https:// và có tên miền");
        }

        return url;
    }
    @Transactional
    public void delete(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy document: " + documentId));

        documentRepository.delete(document);

        if (document.getSource() != null) {
            fileStorageService.delete(Path.of(document.getSource()));
        }
    }
}