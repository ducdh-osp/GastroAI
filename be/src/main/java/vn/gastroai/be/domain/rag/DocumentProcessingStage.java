package vn.gastroai.be.domain.rag;

/** Current pipeline step; the overall lifecycle is tracked separately by DocumentStatus. */
public enum DocumentProcessingStage {
    PENDING,
    EXTRACTION,
    CHUNKING,
    EMBEDDING,
    DONE,
    ERROR
}
