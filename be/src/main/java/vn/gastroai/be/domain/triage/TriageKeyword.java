package vn.gastroai.be.domain.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * UC0054 - 1 từ khóa/câu nói (đã chuẩn hóa thường, không dấu - xem VietnameseText.normalize())
 * thuộc 1 nhóm dấu hiệu cấp cứu cố định (TriageKeywordGroup). Thay cho danh sách hardcode của
 * HardcodedTriageKeywordSource từ GĐ3 - xem DatabaseTriageKeywordSource.
 */
@Entity
@Table(name = "triage_keywords")
@Getter
@Setter
@NoArgsConstructor
public class TriageKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_code", nullable = false, length = 50)
    private TriageKeywordGroup groupCode;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public TriageKeyword(TriageKeywordGroup groupCode, String keyword) {
        this.groupCode = groupCode;
        this.keyword = keyword;
    }
}
