package vn.gastroai.be.infrastructure.persistence.postgres;

/** 1 dong ket qua tim kiem trong noi dung tai lieu (UC0055) - Spring tu khop theo ten cot AS. */
public interface ChunkSearchRow {
    Long getDocumentId();
    String getTitle();
    Integer getChunkIndex();
    String getContent();
}