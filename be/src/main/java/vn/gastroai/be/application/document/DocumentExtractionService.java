package vn.gastroai.be.application.document;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class DocumentExtractionService {

    private final List<TextExtractor> extractors;

    public DocumentExtractionService(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }

    public String extract(Path filePath) {
        String extension = getExtension(filePath);

        TextExtractor extractor = extractors.stream()
                .filter(item -> item.supports(extension))
                .findFirst()
                .orElseThrow(() -> new ExtractionException(
                        "Không có bộ xử lý cho file " + extension
                ));

        return extractor.extract(filePath);
    }

    private String getExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();

        int lastDot = fileName.lastIndexOf('.');

        if (lastDot < 0) {
            throw new ExtractionException(
                    "File không có extension"
            );
        }

        return fileName
                .substring(lastDot)
                .toLowerCase(Locale.ROOT);
    }
}