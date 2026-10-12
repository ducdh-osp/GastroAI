package vn.gastroai.be.domain.patient;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;

/** UC0013/UC0014 - 1 lan ghi nhan tinh trang tieu hoa theo thang Bristol (1-7). */
@Entity
@Table(name = "bristol_logs")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class BristolLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "bristol_type", nullable = false)
    private Integer bristolType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public BristolLog(Patient patient, Instant loggedAt, Integer bristolType, String notes) {
        this.patient = patient;
        this.loggedAt = loggedAt;
        this.bristolType = bristolType;
        this.notes = notes;
    }
}