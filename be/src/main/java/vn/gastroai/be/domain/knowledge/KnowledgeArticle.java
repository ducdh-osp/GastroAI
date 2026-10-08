package vn.gastroai.be.domain.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "knowledge_articles")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "reading_minutes", nullable = false)
    private Integer readingMinutes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_checked_at", nullable = false)
    private LocalDate sourceCheckedAt;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
