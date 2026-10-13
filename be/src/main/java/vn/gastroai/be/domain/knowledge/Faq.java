package vn.gastroai.be.domain.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** UC0062 & UC0021 - Câu hỏi thường gặp (FAQ) trong kho tri thức y khoa. */
@Entity
@Table(name = "faqs")
@Getter
@Setter
@NoArgsConstructor
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /** Mặc định là bản nháp - chỉ được công khai sau khi có bác sĩ kiểm duyệt y khoa. */
    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "medical_reviewer", length = 200)
    private String medicalReviewer;

    @Column(name = "medical_reviewed_at")
    private Instant medicalReviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Faq(String question, String answer, String category, int sortOrder, boolean published) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.sortOrder = sortOrder;
        this.published = published;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
