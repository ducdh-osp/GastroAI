package vn.gastroai.be.application.config;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.config.AppRuntimeConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.AppRuntimeConfigRepository;

/**
 * UC0059 - đọc/sửa cấu hình RAG (top-k, ngưỡng similarity) và cảnh báo Triage realtime, thay cho
 * giá trị cố định trong application.yml. Luôn có đúng 1 dòng (id=1, seed ở V33) - getConfig()
 * không auto-tạo nếu thiếu vì đó là lỗi migration, không phải trạng thái hợp lệ cần tự vá.
 *
 * ragTopK giới hạn 1..20: EmbeddingStore.findTopK dùng trực tiếp làm LIMIT trong SQL, để quá lớn
 * vừa chậm vừa đưa ngữ cảnh không liên quan vào prompt. ragSimilarityThreshold giới hạn 0..2 vì
 * đó là miền giá trị cosine distance của pgvector "<=>' (0 = giống hệt, 2 = đối lập hoàn toàn -
 * xem SimilarChunk).
 */
@Service
public class AppRuntimeConfigService {

    private static final long CONFIG_ID = 1L;
    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 20;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.0;
    private static final double MAX_SIMILARITY_THRESHOLD = 2.0;

    private final AppRuntimeConfigRepository repository;

    public AppRuntimeConfigService(AppRuntimeConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AppRuntimeConfigSnapshot getConfig() {
        return toSnapshot(findConfig());
    }

    @Transactional
    public AppRuntimeConfigSnapshot updateConfig(UpdateAppRuntimeConfigRequest request,
            String updatedByType, Long updatedById) {
        validate(request);

        AppRuntimeConfig config = findConfig();
        config.setRagTopK(request.ragTopK());
        config.setRagSimilarityThreshold(request.ragSimilarityThreshold());
        config.setTriageRealtimeAlertsEnabled(request.triageRealtimeAlertsEnabled());
        config.setUpdatedByType(updatedByType);
        config.setUpdatedById(updatedById);

        return toSnapshot(repository.save(config));
    }

    private AppRuntimeConfig findConfig() {
        return repository.findById(CONFIG_ID).orElseThrow(() -> new IllegalStateException(
                "Thieu dong cau hinh runtime (id=1) - kiem tra migration V33 da chay chua"));
    }

    private void validate(UpdateAppRuntimeConfigRequest request) {
        if (request.ragTopK() == null) {
            throw new IllegalArgumentException("ragTopK khong duoc de trong");
        }
        if (request.ragTopK() < MIN_TOP_K || request.ragTopK() > MAX_TOP_K) {
            throw new IllegalArgumentException("ragTopK phai trong khoang " + MIN_TOP_K + ".." + MAX_TOP_K);
        }
        if (request.triageRealtimeAlertsEnabled() == null) {
            throw new IllegalArgumentException("triageRealtimeAlertsEnabled khong duoc de trong");
        }
        Double threshold = request.ragSimilarityThreshold();
        if (threshold != null && (threshold < MIN_SIMILARITY_THRESHOLD || threshold > MAX_SIMILARITY_THRESHOLD)) {
            throw new IllegalArgumentException(
                    "ragSimilarityThreshold phai trong khoang " + MIN_SIMILARITY_THRESHOLD + ".." + MAX_SIMILARITY_THRESHOLD
                            + " hoac de trong de tat loc theo similarity");
        }
    }

    private static AppRuntimeConfigSnapshot toSnapshot(AppRuntimeConfig config) {
        return new AppRuntimeConfigSnapshot(
                config.getRagTopK(),
                config.getRagSimilarityThreshold(),
                config.isTriageRealtimeAlertsEnabled(),
                config.getUpdatedByType(),
                config.getUpdatedById());
    }
}
