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
import java.util.List;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.gastroai.be.application.support.ResourceNotFoundException;

@Service
public class DocumentUploadService {

    private static final int MAX_SOURCE_URL_LENGTH = 2000;

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    public DocumentUploadService(
            FileStorageService fileStorageService,
            DocumentRepository documentRepository) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
    }

    public Document upload(MultipartFile file, String sourceUrl) {
        return storeNewDocument(file, sourceUrl, null, 1, false);
    }

    @Transactional("postgresTransactionManager")
    public Document uploadNewVersion(Long documentId, MultipartFile file, String sourceUrl) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        Long groupId = document.groupId();
        Document latest = documentRepository.findGroup(groupId).get(0);

        if (latest.getStatus() == DocumentStatus.PENDING || latest.getStatus() == DocumentStatus.PROCESSING) {
            throw new IllegalStateException("Phiên bản trước đang được xử lý, vui lòng chờ xong rồi tải bản mới");
        }
        return storeNewDocument(file, sourceUrl, groupId, latest.getVersionNo() + 1, false);
    }

    private Document storeNewDocument(MultipartFile file, String sourceUrl, Long rootId, int versionNo,
            boolean currentVersion) {

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
            document.setRootId(rootId);
            document.setVersionNo(versionNo);
            document.setCurrentVersion(currentVersion);
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

    /**
     * Mã băm SHA-256 (dạng hex, 64 ký tự) của nội dung file đã lưu - dùng để phát
     * hiện upload trùng.
     */
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

    @Transactional("postgresTransactionManager")
    public void delete(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài liệu: " + documentId));
        Long groupId = document.groupId();
        List<Document> versions = documentRepository.findGroup(groupId);

        boolean anyProcessing = versions.stream()
                .anyMatch(v -> v.getStatus() == DocumentStatus.PENDING || v.getStatus() == DocumentStatus.PROCESSING);
        if (anyProcessing) {
            throw new IllegalStateException(
                    "Tài liệu có phiên bản đang được xử lý, vui lòng thử lại sau");
        }

        // Gom duong dan file TRUOC khi xoa DB - sau deleteGroup cac dong khong con de
        // doc.
        List<Path> files = versions.stream()
                .map(Document::getSource)
                .filter(source -> source != null)
                .map(Path::of)
                .toList();

        documentRepository.deleteGroup(groupId);
        files.forEach(this::deleteFileAfterCommit);
    }

    @Transactional("postgresTransactionManager")
    public void deleteVersion(Long documentId, Long versionId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài liệu: " + documentId));
        Long groupId = document.groupId();
        Document target = documentRepository.findById(versionId)
                .filter(v -> v.groupId().equals(groupId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phiên bản không thuộc tài liệu này"));

        // Nhom chi con 1 ban: khong con gi de giu lai, xoa ca tai lieu.
        if (documentRepository.findGroup(groupId).size() == 1) {
            delete(documentId);
            return;
        }

        if (target.getStatus() == DocumentStatus.PENDING
                || target.getStatus() == DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Phiên bản đang được xử lý, vui lòng thử lại sau");
        }
        if (target.isCurrentVersion()) {
            throw new IllegalStateException(
                    "Không xóa được phiên bản đang dùng. Hãy chọn dùng phiên bản khác trước");
        }
        if (target.getRootId() == null) {
            throw new IllegalStateException(
                    "Đây là phiên bản gốc, không xóa riêng được khi tài liệu còn phiên bản khác. "
                            + "Hãy xóa cả tài liệu");
        }

        documentRepository.delete(target);
        if (target.getSource() != null) {
            deleteFileAfterCommit(Path.of(target.getSource()));
        }
    }

    /**
     * Chi xoa file tren dia SAU KHI DB da commit xong viec xoa - commit loi thi giu
     * file.
     */
    private void deleteFileAfterCommit(Path file) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.delete(file);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(file);
            }
        });
    }
}