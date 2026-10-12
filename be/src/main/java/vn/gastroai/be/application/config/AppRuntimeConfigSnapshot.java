package vn.gastroai.be.application.config;

/** UC0059 - bản chụp read-only của cấu hình runtime, tách khỏi entity JPA để service RAG/Triage
 * dùng mà không giữ tham chiếu tới entity ngoài transaction. */
public record AppRuntimeConfigSnapshot(
        int ragTopK,
        Double ragSimilarityThreshold,
        boolean triageRealtimeAlertsEnabled,
        String updatedByType,
        Long updatedById) {
}
