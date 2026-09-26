package vn.gastroai.be.infrastructure.rag;

/**
 * UC0028 - 1 kết quả truy xuất ngữ cảnh. distance là cosine distance từ pgvector (toán tử
 * "&lt;=&gt;") — CÀNG NHỎ càng giống câu hỏi, khác với độ tương đồng (similarity) thường thấy
 * là càng LỚN càng giống. Đừng nhầm 2 chiều này khi hiển thị/so sánh ngưỡng.
 */
public record SimilarChunk(Long chunkId, Long documentId, String documentTitle, String content, double distance) {
}
