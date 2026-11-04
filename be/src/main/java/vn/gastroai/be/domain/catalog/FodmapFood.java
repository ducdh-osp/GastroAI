package vn.gastroai.be.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fodmap_foods")
@Getter
@Setter
@NoArgsConstructor
public class FodmapFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 120)
    private String normalizedName;

    @Column(length = 700)
    private String aliases;

    @Enumerated(EnumType.STRING)
    @Column(name = "fodmap_level", nullable = false, length = 10)
    private FodmapLevel level;

    @Column(name = "fodmap_groups", length = 100)
    private String groups;

    @Column(name = "serving_note", length = 100)
    private String servingNote;

    @Column(length = 500)
    private String note;

    @Column(length = 255)
    private String source;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public List<String> getAliasList() {
        return splitCsv(aliases);
    }

    public void setAliasList(List<String> list) {
        this.aliases = (list == null || list.isEmpty()) ? null : String.join(", ", list);
    }

    public List<String> getGroupList() {
        return splitCsv(groups);
    }

    public void setGroupList(List<String> list) {
        this.groups = (list == null || list.isEmpty()) ? null : String.join(",", list);
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }
}