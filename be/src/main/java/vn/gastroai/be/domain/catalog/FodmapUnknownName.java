package vn.gastroai.be.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fodmap_unknown_names")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FodmapUnknownName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(name = "sample_name", nullable = false, length = 120)
    private String sampleName;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    @Column(nullable = false)
    private boolean dismissed;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public FodmapUnknownName(String normalizedName, String sampleName) {
        Instant now = Instant.now();
        this.normalizedName = normalizedName;
        this.sampleName = sampleName;
        this.hitCount = 1;
        this.dismissed = false;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
    }

    public void recordHit() {
        this.hitCount++;
        this.lastSeenAt = Instant.now();
    }

    public void dismiss() {
        this.dismissed = true;
    }
}