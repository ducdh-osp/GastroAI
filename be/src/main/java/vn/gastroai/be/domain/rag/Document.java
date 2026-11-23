package vn.gastroai.be.domain.rag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_stage", nullable = false, length = 20)
    private DocumentProcessingStage processingStage = DocumentProcessingStage.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * SCR-AD-04 §3.2: null = dong chinh cua tai lieu. Co gia tri = day la BAN THAY THE dang cho
     * xu ly / cho xac nhan, tro ve dong chinh no se thay. Moi tai lieu co toi da 1 ban thay the.
     */
    @Column(name = "replaces_id")
    private Long replacesId;

    // Chi co y nghia tren dong chinh (replacesId == null). Co gia tri = admin chu dong go.
    @Column(name = "unpublished_at")
    private Instant unpublishedAt;

    @Column(name = "content_approved_at")
    private Instant contentApprovedAt;

    @Column(name = "content_approved_by", length = 255)
    private String contentApprovedBy;

    /** Ma tai lieu: id cua dong chinh (ban thay the dung chung ma voi dong no se thay). */
    public Long groupId() {
        return replacesId != null ? replacesId : id;
    }

    public boolean isReplacement() {
        return replacesId != null;
    }

    public boolean isInUse() {
        return contentApprovedAt != null && unpublishedAt == null;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}