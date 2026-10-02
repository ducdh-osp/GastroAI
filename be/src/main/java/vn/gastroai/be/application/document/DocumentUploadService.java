package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.filestorage.FileStorageService;
import vn.gastroai.be.infrastructure.persistence.postgres.DocumentRepository;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class DocumentUploadService {

    private static final int MAX_SOURCE_URL_LENGTH = 2000;

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    public DocumentUploadService(
            FileStorageService fileStorageService,
            DocumentRepository documentRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
    }

    public Document upload(MultipartFile file, String sourceUrl) {

        // 0. Validate URL nguồn TRƯỚC khi lưu file, để URL sai không để lại file mồ côi
        String normalizedSourceUrl = normalizeSourceUrl(sourceUrl);

        // 1. Validate + lưu file xuống storage
        Path storedPath = fileStorageService.store(file);

        try {
            // 1b. Tính mã băm nội dung rồi chặn trùng - tài liệu cũ đã ERROR vẫn cho
            // upload lại (thường là thử lại sau khi sửa lỗi nguồn).
            String contentHash = computeContentHash(storedPath);

            documentRepository.findFirstByContentHashAndStatusNot(contentHash, DocumentStatus.ERROR)
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Tài liệu này đã tồn tại (id = " + existing.getId() + ")");
                    });

            // 2. Tạo document với trạng thái PENDING
            Document document = new Document();

            document.setTitle(file.getOriginalFilename());
            document.setSource(storedPath.toString());
            document.setSourceUrl(normalizedSourceUrl);
            document.setStatus(DocumentStatus.PENDING);
            document.setContentHash(contentHash);

            // 3. Lưu document vào database với trạng thái PENDING - không tự gọi
            // worker xử lý ở đây nữa. DocumentProcessingScheduler sẽ định kỳ quét và
            // giành lấy document này để xử lý (xem file DocumentProcessingScheduler).
            document = documentRepository.save(document);

            // 4. Trả document cho Controller
            return document;

        } catch (RuntimeException e) {

            // DB insert thất bại, hoặc phát hiện trùng → xóa file vừa lưu
            fileStorageService.delete(storedPath);

            throw e;
        }
    }

    /** Mã băm SHA-256 (dạng hex, 64 ký tự) của nội dung file đã lưu - dùng để phát hiện upload trùng. */
    private static String computeContentHash(Path storedPath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(storedPath));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (IOException e) {
            throw new IllegalStateException("Không thể đọc nội dung file để tính mã băm", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Lỗi hệ thống: không hỗ trợ thuật toán SHA-256", e);
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

        if (document.getStatus() == DocumentStatus.PENDING
                || document.getStatus() == DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Tài liệu đang được xử lý, vui lòng thử lại sau");
        }

        documentRepository.delete(document);

        if (document.getSource() != null) {
            fileStorageService.delete(Path.of(document.getSource()));
        }
    }
}