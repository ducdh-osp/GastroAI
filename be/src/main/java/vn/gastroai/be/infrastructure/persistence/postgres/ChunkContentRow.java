package vn.gastroai.be.infrastructure.persistence.postgres;

/** 1 đoạn khớp trong 1 tài liệu (UC0055) - Spring tự khớp theo tên cột AS. */
public interface ChunkContentRow {
    Integer getChunkIndex();
    String getContent();
}