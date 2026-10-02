package vn.gastroai.be.infrastructure.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lưu file/ảnh bệnh nhân đính kèm khi chat - khác FileStorageService (chỉ pdf/docx, dành cho
 * tài liệu CMS) vì ở đây còn nhận cả ảnh, và người gửi là BỆNH NHÂN (không phải Doctor/Admin)
 * nên không tin tưởng content-type/extension client khai báo.
 *
 * Validate bằng magic bytes (vài byte đầu file) thay vì chỉ tin đuôi file/content-type do
 * client gửi - xem issue #44 (FileStorageService chỉ check đuôi file, giả mạo được 100%).
 * Không lặp lại lỗi đó ở code mới này.
 */
@Service
public class ChatAttachmentStorage {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILES_PER_MESSAGE = 5;

    /** Map content-type khai báo (đã validate magic bytes khớp) -> extension lưu trên đĩa. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

    private final Path storageDirectory;

    public ChatAttachmentStorage(@Value("${app.storage.dir}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).resolve("chat-attachments");
    }

    public static long maxFileSize() {
        return MAX_FILE_SIZE;
    }

    public static int maxFilesPerMessage() {
        return MAX_FILES_PER_MESSAGE;
    }

    /** contentType la gia tri client khai bao - chi dung de CHON extension, khong tin de bo qua kiem tra magic bytes. */
    public StoredFile store(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File dinh kem khong duoc de trong");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Moi file dinh kem khong duoc vuot qua 10MB");
        }
        String detectedType = detectRealContentType(content);
        if (detectedType == null || !detectedType.equals(normalize(contentType))) {
            throw new IllegalArgumentException(
                    "File khong dung dinh dang anh (jpg/png/webp) hoac tai lieu (pdf/docx) nhu da khai bao");
        }

        try {
            Files.createDirectories(storageDirectory);
            String storedName = UUID.randomUUID() + ALLOWED_TYPES.get(detectedType);
            Path target = storageDirectory.resolve(storedName);
            Files.write(target, content);
            return new StoredFile(storedName, target, detectedType);
        } catch (IOException e) {
            throw new IllegalStateException("Khong the luu file dinh kem", e);
        }
    }

    public Path resolve(String storedName) {
        return storageDirectory.resolve(storedName);
    }

    private String normalize(String contentType) {
        return contentType == null ? null : contentType.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Doc vai byte dau (magic bytes) de xac dinh dung loai file THAT, khong tin content-type
     * client gui. JPEG: FF D8 FF. PNG: 89 50 4E 47. WEBP: "RIFF"....["WEBP"] o byte 8-11.
     * PDF: "%PDF". DOCX (OOXML/ZIP): "PK\x03\x04" - khong phan biet duoc docx voi zip/xlsx/pptx
     * chi bang magic bytes (deu la ZIP), nhung day van la lop phong thu manh hon chi check
     * duoi file - ket hop voi gioi han kich thuoc + Gemini/extractor tu bo qua noi dung doc
     * khong hop le (xem ExtractionException) la chap nhan duoc cho pham vi chat dinh kem.
     */
    private String detectRealContentType(byte[] content) {
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47)) return "image/png";
        if (content.length >= 12 && startsWith(content, 'R', 'I', 'F', 'F')
                && Arrays.equals(Arrays.copyOfRange(content, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        if (startsWith(content, '%', 'P', 'D', 'F')) return "application/pdf";
        if (startsWith(content, 0x50, 0x4B, 0x03, 0x04)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return null;
    }

    private boolean startsWith(byte[] content, int... expectedBytes) {
        if (content.length < expectedBytes.length) return false;
        for (int i = 0; i < expectedBytes.length; i++) {
            if ((content[i] & 0xFF) != (expectedBytes[i] & 0xFF)) return false;
        }
        return true;
    }

    public record StoredFile(String storedName, Path path, String contentType) {
    }
}
