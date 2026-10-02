package vn.gastroai.be.infrastructure.filestorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAttachmentStorageTest {

    private ChatAttachmentStorage storage;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storage = new ChatAttachmentStorage(tempDir.toString());
    }

    @Test
    void storesJpegWhenMagicBytesMatchDeclaredType() throws IOException {
        byte[] jpeg = jpegBytes();
        ChatAttachmentStorage.StoredFile stored = storage.store(jpeg, "image/jpeg");

        assertEquals("image/jpeg", stored.contentType());
        assertTrue(stored.storedName().endsWith(".jpg"));
        assertTrue(Files.exists(stored.path()));
        assertTrue(Arrays.equals(jpeg, Files.readAllBytes(stored.path())));
    }

    @Test
    void storesPdfWhenMagicBytesMatchDeclaredType() {
        byte[] pdf = "%PDF-1.4 noi dung gia lap du dai de qua MIN_TEXT_LENGTH".getBytes(StandardCharsets.US_ASCII);
        ChatAttachmentStorage.StoredFile stored = storage.store(pdf, "application/pdf");

        assertEquals("application/pdf", stored.contentType());
        assertTrue(stored.storedName().endsWith(".pdf"));
    }

    @Test
    void rejectsFileWhenDeclaredTypeDoesNotMatchRealMagicBytes() {
        // Gia mao: doi content-type thanh pdf nhung noi dung thuc la JPEG - dung giong issue #44
        // (FileStorageService chi tin extension) nhung o day phai CHAN vi khong lap lai loi do.
        byte[] jpeg = jpegBytes();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storage.store(jpeg, "application/pdf"));
        assertTrue(exception.getMessage().contains("dinh dang"));
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] tooLarge = new byte[(int) ChatAttachmentStorage.maxFileSize() + 1];
        System.arraycopy(jpegBytes(), 0, tooLarge, 0, jpegBytes().length);

        assertThrows(IllegalArgumentException.class, () -> storage.store(tooLarge, "image/jpeg"));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> storage.store(new byte[0], "image/jpeg"));
    }

    @Test
    void rejectsUnsupportedFileType() {
        byte[] exe = {0x4D, 0x5A, 0x00, 0x00};
        assertThrows(IllegalArgumentException.class, () -> storage.store(exe, "application/octet-stream"));
    }

    private byte[] jpegBytes() {
        byte[] content = new byte[64];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        return content;
    }
}
