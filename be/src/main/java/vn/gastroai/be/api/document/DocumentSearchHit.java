package vn.gastroai.be.api.document;

/** 1 tài liệu khớp từ khoá: số đoạn khớp và câu trích quanh chỗ khớp đầu tiên. */
public record DocumentSearchHit(Long documentId, String title, long matchCount, String snippet) {
}