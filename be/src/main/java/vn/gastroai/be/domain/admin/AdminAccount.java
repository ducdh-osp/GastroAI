package vn.gastroai.be.domain.admin;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="admin_accounts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminAccount { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String email; @Column(name="password_hash",nullable=false) private String passwordHash; @Column(name="full_name") private String fullName; @Column(nullable=false) private boolean active; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt; }
