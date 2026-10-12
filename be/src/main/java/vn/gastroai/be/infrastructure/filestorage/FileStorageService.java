package vn.gastroai.be.infrastructure.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf",
            ".docx"
    );

    // Chu ky dau file ("magic bytes") - khong the gia mao bang cach doi ten/duoi file hay
    // sua header Content-Type, vi ca hai thu do deu do nguoi gui tu khai bao.
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] DOCX_SIGNATURE = {0x50, 0x4B, 0x03, 0x04}; // PK.. (DOCX la file zip)

    private static final String DOCX_REQUIRED_ENTRY = "word/document.xml";

    private final Path storageDirectory;
    private final long maxFileSize;

    public FileStorageService(
            @Value("${app.storage.dir}") String storageDirectory,
            @Value("${app.storage.max-file-size}") long maxFileSize
    ) {
        this.storageDirectory = Paths.get(storageDirectory);
        this.maxFileSize = maxFileSize;
    }

    public Path store(MultipartFile file) {
        validate(file);

        String extension = getExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;
        Path targetPath;

        try {
            Files.createDirectories(storageDirectory);
            targetPath = storageDirectory.resolve(storedFileName);
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không thể lưu file",
                    e
            );
        }
        if (".docx".equals(extension)) {
            validateDocxStructure(targetPath);
        }

        return targetPath;
    }

    public void delete(Path filePath) {
        if (filePath == null) {
            return;
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Không làm hỏng flow chính nếu cleanup thất bại.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File không được để trống"
            );
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "File vượt quá giới hạn cho phép"
            );
        }

        String extension =
                getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Chỉ hỗ trợ file PDF hoặc DOCX"
            );
        }

        validateSignature(file, extension);
    }

    private void validateSignature(MultipartFile file, String extension) {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(5);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Không đọc được nội dung file",
                    e
            );
        }

        if (".pdf".equals(extension) && !startsWith(header, PDF_SIGNATURE)) {
            throw new IllegalArgumentException(
                    "Nội dung file không đúng định dạng PDF"
            );
        }

        if (".docx".equals(extension) && !startsWith(header, DOCX_SIGNATURE)) {
            throw new IllegalArgumentException(
                    "Nội dung file không đúng định dạng DOCX"
            );
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void validateDocxStructure(Path targetPath) {
        boolean valid;
        try (ZipFile zipFile = new ZipFile(targetPath.toFile())) {
            valid = zipFile.getEntry(DOCX_REQUIRED_ENTRY) != null;
        } catch (IOException e) {
            valid = false;
        }

        if (!valid) {
            delete(targetPath);
            throw new IllegalArgumentException(
                    "File DOCX không hợp lệ"
            );
        }
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên file không hợp lệ"
            );
        }

        int lastDot = filename.lastIndexOf('.');

        if (lastDot < 0) {
            throw new IllegalArgumentException(
                    "File không có extension"
            );
        }

        return filename
                .substring(lastDot)
                .toLowerCase(Locale.ROOT);
    }
}