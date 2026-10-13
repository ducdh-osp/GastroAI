package vn.gastroai.be.api.document;

public record DocumentSearchHit(Long documentId, String title, int chunkIndex, String snippet) {
}