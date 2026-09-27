package vn.gastroai.be.infrastructure.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf",
            ".docx"
    );

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

        try {
            Files.createDirectories(storageDirectory);

            String extension =
                    getExtension(file.getOriginalFilename());

            String storedFileName =
                    UUID.randomUUID() + extension;

            Path targetPath =
                    storageDirectory.resolve(storedFileName);

            file.transferTo(targetPath);

            return targetPath;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không thể lưu file",
                    e
            );
        }
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