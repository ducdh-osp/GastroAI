package vn.gastroai.be.application.config;

/** UC0059 - payload PUT /api/v1/cms/runtime-config. ragSimilarityThreshold=null tắt lọc theo
 * similarity (giữ hành vi chỉ lấy top-k, không quan tâm độ gần). */
public record UpdateAppRuntimeConfigRequest(
        Integer ragTopK,
        Double ragSimilarityThreshold,
        Boolean triageRealtimeAlertsEnabled) {
}
