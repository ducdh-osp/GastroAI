package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.document.DocumentProcessingWorker.Step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentProcessingWorkerMessageTest {

    @Test
    void fileReadingErrorKeepsItsOwnVietnameseMessage() {
        String message = DocumentProcessingWorker.userMessage(Step.EXTRACTION,
                new ExtractionException("File PDF có mật khẩu, không thể xử lý"));

        assertEquals("File PDF có mật khẩu, không thể xử lý", message);
    }

    @Test
    void technicalErrorIsNeverShownToUser() {
        String message = DocumentProcessingWorker.userMessage(Step.EMBEDDING,
                new IllegalStateException("429 Too Many Requests: RESOURCE_EXHAUSTED"));

        assertFalse(message.contains("429"));
        assertTrue(message.startsWith("Dịch vụ AI tạm thời không phản hồi"));
    }

    @Test
    void eachStepHasItsOwnExplanation() {
        RuntimeException error = new RuntimeException("java.lang.NullPointerException");

        assertTrue(DocumentProcessingWorker.userMessage(Step.EXTRACTION, error).startsWith("Không đọc được nội dung file"));
        assertTrue(DocumentProcessingWorker.userMessage(Step.CHUNKING, error).startsWith("Không chia được nội dung"));
        assertTrue(DocumentProcessingWorker.userMessage(Step.EMBEDDING, error).startsWith("Dịch vụ AI"));
    }
}