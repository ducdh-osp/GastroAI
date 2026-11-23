package vn.gastroai.be.domain.audit;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_anomaly_rules")
public class AuditAnomalyRule {

    public enum Metric {
        ACTIONS,
        DISTINCT_TARGETS,
        DISTINCT_IPS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /**
     * Các hành động quy tắc này theo dõi - bảng con audit_anomaly_rule_actions, mỗi dòng 1 hành động
     * (1NF: không gộp nhiều giá trị vào 1 ô). Rỗng với quy tắc đếm theo IP (MULTI_IP).
     * EAGER vì quy tắc nào cũng chỉ có vài hành động và luôn cần ngay khi kiểm tra bất thường.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "audit_anomaly_rule_actions", joinColumns = @JoinColumn(name = "rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    @OrderBy
    private List<AuditAction> actions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Metric metric;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "window_minutes", nullable = false)
    private int windowMinutes;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    protected AuditAnomalyRule() {
    }

    public List<AuditAction> getActionList() {
        return List.copyOf(actions);
    }

    public boolean appliesTo(AuditAction action) {
        if (metric == Metric.DISTINCT_IPS) {
            return true;
        }
        return getActionList().contains(action);
    }

    public void update(int threshold, int windowMinutes, boolean enabled, String updatedBy) {
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
        this.enabled = enabled;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Metric getMetric() { return metric; }
    public int getThreshold() { return threshold; }
    public int getWindowMinutes() { return windowMinutes; }
    public boolean isEnabled() { return enabled; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}