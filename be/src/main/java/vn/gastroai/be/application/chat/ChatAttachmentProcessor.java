package vn.gastroai.be.application.chat;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.gastroai.be.application.document.DocumentExtractionService;
import vn.gastroai.be.application.document.ExtractionException;
import vn.gastroai.be.infrastructure.ai.ImagePart;
import vn.gastroai.be.infrastructure.filestorage.ChatAttachmentStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Xu ly file benh nhan dinh kem khi chat (UC chat dinh kem): luu file (ChatAttachmentStorage,
 * co kiem tra magic bytes), roi tuy loai:
 *  - Anh (jpg/png/webp) -> ma hoa base64, gui thang cho Gemini qua ImagePart (vision thuc su,
 *    khong qua OCR/mo ta trung gian).
 *  - Tai lieu (pdf/docx) -> trich xuat text bang DocumentExtractionService (dung lai pipeline
 *    da co cho UC0031/032, khong viet lai), noi vao prompt nhu 1 doan ngu canh.
 * Loi trich xuat 1 file (vd PDF scan khong co text) KHONG lam hong ca tin nhan - chi ghi chu
 * "khong doc duoc" cho file do, cac file khac/cau tra loi chinh van tiep tuc binh thuong.
 */
@Service
public class ChatAttachmentProcessor {

    private final ChatAttachmentStorage storage;
    private final DocumentExtractionService extractionService;

    public ChatAttachmentProcessor(ChatAttachmentStorage storage, DocumentExtractionService extractionService) {
        this.storage = storage;
        this.extractionService = extractionService;
    }

    public ProcessedAttachments process(List<MultipartFile> files) {
        if (files.size() > ChatAttachmentStorage.maxFilesPerMessage()) {
            throw new IllegalArgumentException(
                    "Toi da " + ChatAttachmentStorage.maxFilesPerMessage() + " file dinh kem moi tin nhan");
        }

        List<AttachmentResult> results = new ArrayList<>();
        List<ImagePart> images = new ArrayList<>();
        StringBuilder documentText = new StringBuilder();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            byte[] content;
            try {
                content = file.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Khong doc duoc file dinh kem", e);
            }

            ChatAttachmentStorage.StoredFile stored = storage.store(content, file.getContentType());
            String originalFilename = truncate(file.getOriginalFilename());
            results.add(new AttachmentResult(stored.storedName(), originalFilename, stored.contentType(), content.length));

            if (stored.contentType().startsWith("image/")) {
                images.add(new ImagePart(stored.contentType(), Base64.getEncoder().encodeToString(content)));
            } else {
                try {
                    String text = extractionService.extract(stored.path());
                    documentText.append("[Tai lieu dinh kem \"").append(originalFilename).append("\"]:\n")
                            .append(text).append("\n\n");
                } catch (ExtractionException e) {
                    documentText.append("[Khong doc duoc noi dung tep \"").append(originalFilename)
                            .append("\" - co the la file scan/anh khong co text]\n\n");
                }
            }
        }

        return new ProcessedAttachments(results, images, documentText.toString());
    }

    private String truncate(String filename) {
        String safe = (filename == null || filename.isBlank()) ? "tep-dinh-kem" : filename;
        return safe.length() > 255 ? safe.substring(0, 255) : safe;
    }

    public record AttachmentResult(String storedName, String originalFilename, String contentType, long sizeBytes) {
    }

    public record ProcessedAttachments(List<AttachmentResult> attachments, List<ImagePart> images, String documentText) {
    }
}
