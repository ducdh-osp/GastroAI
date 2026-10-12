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

/** UC0066 - 1 chu de/danh muc dung de phan loai bai viet kien thuc. */
@Entity
@Table(name = "article_topics")
@Getter
@Setter
@NoArgsConstructor
public class ArticleTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ArticleTopic(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
