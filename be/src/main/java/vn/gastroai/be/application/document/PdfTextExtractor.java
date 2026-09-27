package vn.gastroai.be.application.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class PdfTextExtractor implements TextExtractor {

    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public boolean supports(String fileExtension) {
        return ".pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public String extract(Path filePath) throws ExtractionException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(document);

            if (text == null || text.trim().length() < MIN_TEXT_LENGTH) {
                throw new ExtractionException(
                        "Không trích xuất được text — có thể là file scan/ảnh"
                );
            }

            return text;

        } catch (InvalidPasswordException e) {
            throw new ExtractionException(
                    "File PDF có mật khẩu, không thể xử lý",
                    e
            );

        } catch (IOException e) {
            throw new ExtractionException(
                    "Không thể đọc file PDF",
                    e
            );
        }
    }
}