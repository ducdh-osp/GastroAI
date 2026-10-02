package vn.gastroai.be.infrastructure.filestorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service(Path storageDir) {
        return new FileStorageService(storageDir.toString(), 20_971_520L);
    }

    private byte[] zipBytes(String entryName, byte[] entryContent) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(entryContent);
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }

    @Test
    void rejectsPdfExtensionWhenContentIsNotRealPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", "hello".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service(tempDir).store(file));
    }

    @Test
    void storesRealPdfSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.pdf", "application/pdf", "%PDF-1.7\n...".getBytes());

        Path stored = service(tempDir).store(file);

        assertTrue(Files.exists(stored));
    }

    @Test
    void rejectsDocxExtensionWhenContentIsActuallyPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "%PDF-1.7\n...".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service(tempDir).store(file));
    }

    @Test
    void rejectsDocxWhenZipHasNoWordDocumentXmlAndCleansUpStoredFile() throws IOException {
        byte[] fakeZip = zipBytes("abc.txt", "khong phai docx".getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                fakeZip);

        assertThrows(IllegalArgumentException.class, () -> service(tempDir).store(file));

        try (var stream = Files.list(tempDir)) {
            assertEquals(0, stream.count());
        }
    }

    @Test
    void storesRealDocxSuccessfully() throws IOException {
        byte[] realDocx = zipBytes("word/document.xml", "<xml>noi dung docx gia</xml>".getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                realDocx);

        Path stored = service(tempDir).store(file);

        assertTrue(Files.exists(stored));
    }
}