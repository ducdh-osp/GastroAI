package vn.gastroai.be.domain.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * UC0059 - cấu hình RAG/Triage chỉnh được qua màn hình admin CMS, thay cho giá trị cố định
 * trong application.yml trước đây (xem V33 - luôn chỉ 1 dòng, id=1). RagQueryService/
 * StreamingRagQueryService đọc ragTopK/ragSimilarityThreshold mỗi lần trả lời (không cache ở
 * mức bean) để admin đổi có hiệu lực ngay, không cần restart app.
 */
@Entity
@Table(name = "app_runtime_config")
@Getter
@Setter
@NoArgsConstructor
public class AppRuntimeConfig {

    @Id
    private Long id;

    @Column(name = "rag_top_k", nullable = false)
    private int ragTopK;

    /** null = không lọc theo similarity, giữ hành vi cũ (chỉ top-k, không cần đủ gần). */
    @Column(name = "rag_similarity_threshold")
    private Double ragSimilarityThreshold;

    @Column(name = "triage_realtime_alerts_enabled", nullable = false)
    private boolean triageRealtimeAlertsEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by_type", length = 20)
    private String updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
