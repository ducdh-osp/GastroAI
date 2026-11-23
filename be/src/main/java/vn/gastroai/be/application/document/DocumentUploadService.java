package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
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
import vn.gastroai.be.application.support.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class DocumentUploadService {

    private static final int MAX_SOURCE_URL_LENGTH = 2000;

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final DocumentGroupPruner groupPruner;

    public DocumentUploadService(
            FileStorageService fileStorageService,
            DocumentRepository documentRepository,
            DocumentGroupPruner groupPruner) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.groupPruner = groupPruner;
    }

    public Document upload(MultipartFile file, String sourceUrl, String title) {
        String resolvedTitle = DocumentTitle.normalize(
                title == null || title.isBlank() ? file.getOriginalFilename() : title);
        return storeNewDocument(file, sourceUrl, resolvedTitle, null);
    }

    /**
     * UC0049 / SCR-AD-04 §3.2 - tai ban thay the cho 1 tai lieu da co. Khong giu lich su:
     * <ul>
     *   <li>Tai lieu dang dung cho AI: ban dang dung van phuc vu binh thuong cho toi khi Bien tap
     *       xac nhan ban thay the (luc do moi xoa vinh vien ban cu - xem
     *       {@link DocumentStatusService#approveContent}). Ban thay the cu chua duoc xac nhan (neu co)
     *       bi bo, chi giu ban moi nhat.</li>
     *   <li>Tai lieu chua tung duoc dung cho AI (loi xu ly / dang cho xac nhan): thay luon, xoa ban cu.</li>
     * </ul>
     */
    @Transactional("postgresTransactionManager")
    public Document uploadReplacement(Long documentId, MultipartFile file) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài liệu: " + documentId));
        List<Document> group = documentRepository.findGroup(document.groupId());

        boolean processing = group.stream().anyMatch(v ->
                v.getStatus() == DocumentStatus.PENDING || v.getStatus() == DocumentStatus.PROCESSING);
        if (processing) {
            throw new IllegalStateException("Bản trước đang được xử lý, vui lòng chờ xong rồi tải bản thay thế");
        }

        Document latest = group.get(0);
        Document live = group.stream()
                .filter(v -> v.getStatus() == DocumentStatus.DONE && v.getContentApprovedAt() != null)
                .findFirst()
                .orElse(null);

        if (live != null) {
            groupPruner.keepOnly(live, group);
            // Ban thay the giu ten va URL hien tai cua tai lieu - muon doi thi dung "Sua thong tin".
            return storeNewDocument(file, live.getSourceUrl(), live.getTitle(), live.getId());
        }
        groupPruner.deleteAll(group);
        return storeNewDocument(file, latest.getSourceUrl(), latest.getTitle(), null);
    }

    /**
     * Luu file + tao dong PENDING. {@code replacesId} = null: tai lieu moi;
     * co gia tri: ban thay the cho dong chinh co id nay. Dong moi chua duoc chatbot dung
     * cho toi khi Bien tap xac nhan noi dung.
     */
    private Document storeNewDocument(MultipartFile file, String sourceUrl, String title, Long replacesId) {

        // 0. Validate URL nguồn TRƯỚC khi lưu file, để URL sai không để lại file mồ côi
        String normalizedSourceUrl = normalizeSourceUrl(sourceUrl);

        // 1. Validate + lưu file xuống storage
        Path storedPath = fileStorageService.store(file);

        try {
            // 1b. Tính mã băm nội dung rồi chặn trùng - tài liệu cũ đã ERROR vẫn cho
            // upload lại (thường là thử lại sau khi sửa lỗi nguồn).
            String contentHash = computeContentHash(storedPath);

            List<Document> duplicates = documentRepository.findByContentHashAndProcessingStageNot(contentHash,
                    DocumentProcessingStage.ERROR);

            if (replacesId == null) {
                // Tai lieu moi: trung voi bat ky tai lieu nao (chua ERROR) deu bi chan.
                if (!duplicates.isEmpty()) {
                    Document existing = duplicates.get(0);
                    throw new IllegalStateException(
                            "Nội dung file này trùng với tài liệu \"" + existing.getTitle()
                                    + "\" (id " + existing.groupId() + ")");
                }
            } else {
                // Ban thay the: chi chan khi trung voi ban dang dung cua CHINH tai lieu nay.
                duplicates.stream()
                        .filter(d -> d.groupId().equals(replacesId))
                        .findFirst()
                        .ifPresent(sameGroup -> {
                            throw new IllegalStateException(
                                    "Nội dung file này trùng với bản đang dùng của chính tài liệu này");
                        });
            }

            // 2. Tạo document với trạng thái PENDING
            Document document = new Document();

            document.setTitle(title);
            document.setSource(storedPath.toString());
            document.setSourceUrl(normalizedSourceUrl);
            document.setContentHash(contentHash);
            document.setReplacesId(replacesId);
            // 3. Lưu document vào database với trạng thái PENDING - không tự gọi
            // worker xử lý ở đây nữa. DocumentProcessingScheduler sẽ định kỳ quét và
            // giành lấy document này để xử lý (xem file DocumentProcessingScheduler).
            try {
                // saveAndFlush: ghi xuong DB ngay de bat loi "da co ban thay the" (uk_documents_one_replacement)
                // tai day, khong de no no o cuoi giao dich voi thong bao kho hieu.
                document = documentRepository.saveAndFlush(document);
            } catch (DataIntegrityViolationException e) {
                if (replacesId != null) {
                    throw new IllegalStateException(
                            "Vừa có người khác tải bản thay thế cho tài liệu này, "
                                    + "vui lòng tải lại trang rồi thử lại",
                            e);
                }
                throw e;
            }

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
    static String normalizeSourceUrl(String raw) {

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

    /** UC0050 / SCR-AD-04 §3.3 - xoá vĩnh viễn cả tài liệu (dòng chính + bản thay thế nếu có). */
    @Transactional("postgresTransactionManager")
    public void delete(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài liệu: " + documentId));
        List<Document> group = documentRepository.findGroup(document.groupId());

        boolean processing = group.stream()
                .anyMatch(v -> v.getStatus() == DocumentStatus.PENDING || v.getStatus() == DocumentStatus.PROCESSING);
        if (processing) {
            throw new IllegalStateException("Tài liệu đang được xử lý, vui lòng thử lại sau");
        }
        groupPruner.deleteAll(group);
    }
}