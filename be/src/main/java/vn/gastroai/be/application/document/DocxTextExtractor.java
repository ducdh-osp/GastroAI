package vn.gastroai.be.application.document;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocxTextExtractor implements TextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public boolean supports(String fileExtension) {
        return ".docx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extract(Path filePath) throws ExtractionException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();

                if (paragraphText != null && !paragraphText.isBlank()) {
                    text.append(paragraphText);
                    text.append(System.lineSeparator());
                }
            }

            String fullText = text.toString();

            if (fullText.trim().length() < MIN_TEXT_LENGTH) {
                throw new ExtractionException(
                        "Không trích xuất được text — có thể là file DOCX rỗng hoặc không có nội dung văn bản"
                );
            }

            return fullText;

        } catch (IOException e) {
            throw new ExtractionException(
                    "Không thể đọc file DOCX",
                    e
            );
        }
    }
}