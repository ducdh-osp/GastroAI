package vn.gastroai.be.domain.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Hash của một mật khẩu bệnh nhân đã dùng, phục vụ ngăn tái sử dụng mật khẩu cũ. */
@Entity
@Table(name = "patient_password_history")
@Getter
@NoArgsConstructor
public class PatientPasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PatientPasswordHistory(Patient patient, String passwordHash, Instant createdAt) {
        this.patient = patient;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }
}
