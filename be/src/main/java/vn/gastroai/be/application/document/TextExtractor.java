package vn.gastroai.be.application.document;
import java.nio.file.Path;

public interface TextExtractor {


    boolean supports(String fileExtension);

    String extract(Path filePath) throws ExtractionException;
}
