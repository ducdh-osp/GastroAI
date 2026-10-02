package vn.gastroai.be.application.chat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.gastroai.be.application.document.DocumentExtractionService;
import vn.gastroai.be.application.document.ExtractionException;
import vn.gastroai.be.infrastructure.filestorage.ChatAttachmentStorage;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAttachmentProcessorTest {

    private final ChatAttachmentStorage storage = mock(ChatAttachmentStorage.class);
    private final DocumentExtractionService extractionService = mock(DocumentExtractionService.class);
    private final ChatAttachmentProcessor processor = new ChatAttachmentProcessor(storage, extractionService);

    @Test
    void imageAttachmentBecomesImagePartNotDocumentText() {
        byte[] bytes = {1, 2, 3, 4};
        MockMultipartFile file = new MockMultipartFile("files", "anh.jpg", "image/jpeg", bytes);
        when(storage.store(bytes, "image/jpeg"))
                .thenReturn(new ChatAttachmentStorage.StoredFile("uuid.jpg", Path.of("/tmp/uuid.jpg"), "image/jpeg"));

        ChatAttachmentProcessor.ProcessedAttachments result = processor.process(List.of(file));

        assertEquals(1, result.images().size());
        assertEquals("image/jpeg", result.images().get(0).mimeType());
        assertEquals(Base64.getEncoder().encodeToString(bytes), result.images().get(0).base64Data());
        assertTrue(result.documentText().isBlank());
        assertEquals(1, result.attachments().size());
        assertEquals("anh.jpg", result.attachments().get(0).originalFilename());
    }

    @Test
    void pdfAttachmentExtractsTextIntoDocumentContext() {
        byte[] bytes = "noi dung pdf gia lap".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", "ho-so.pdf", "application/pdf", bytes);
        Path storedPath = Path.of("/tmp/uuid.pdf");
        when(storage.store(bytes, "application/pdf"))
                .thenReturn(new ChatAttachmentStorage.StoredFile("uuid.pdf", storedPath, "application/pdf"));
        when(extractionService.extract(storedPath)).thenReturn("Benh nhan co tien su dau da day.");

        ChatAttachmentProcessor.ProcessedAttachments result = processor.process(List.of(file));

        assertTrue(result.images().isEmpty());
        assertTrue(result.documentText().contains("ho-so.pdf"));
        assertTrue(result.documentText().contains("Benh nhan co tien su dau da day."));
    }

    @Test
    void extractionFailureAddsNoticeInsteadOfThrowing() {
        byte[] bytes = "pdf scan khong co text".getBytes();
        MockMultipartFile file = new MockMultipartFile("files", "scan.pdf", "application/pdf", bytes);
        Path storedPath = Path.of("/tmp/uuid2.pdf");
        when(storage.store(bytes, "application/pdf"))
                .thenReturn(new ChatAttachmentStorage.StoredFile("uuid2.pdf", storedPath, "application/pdf"));
        when(extractionService.extract(storedPath)).thenThrow(new ExtractionException("khong co text"));

        ChatAttachmentProcessor.ProcessedAttachments result = processor.process(List.of(file));

        assertTrue(result.documentText().contains("Khong doc duoc noi dung"));
        assertEquals(1, result.attachments().size());
    }

    @Test
    void rejectsMoreThanFiveFilesPerMessage() {
        MockMultipartFile file = new MockMultipartFile("files", "x.jpg", "image/jpeg", new byte[]{1});
        List<MockMultipartFile> sixFiles = List.of(file, file, file, file, file, file);

        assertThrows(IllegalArgumentException.class, () -> processor.process(List.copyOf(sixFiles)));
    }

    @Test
    void skipsEmptyFilesSilently() {
        MockMultipartFile empty = new MockMultipartFile("files", "", "application/octet-stream", new byte[0]);

        ChatAttachmentProcessor.ProcessedAttachments result = processor.process(List.of(empty));

        assertTrue(result.attachments().isEmpty());
        assertTrue(result.images().isEmpty());
    }
}
