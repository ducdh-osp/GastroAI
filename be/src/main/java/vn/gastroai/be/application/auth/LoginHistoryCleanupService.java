package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import java.time.Instant;

@Service
public class LoginHistoryCleanupService {
 private final PatientLoginHistoryRepository patients; private final AdminLoginHistoryRepository admins; private final LoginSecurityPolicy policy;
 public LoginHistoryCleanupService(PatientLoginHistoryRepository p,AdminLoginHistoryRepository a,LoginSecurityPolicy policy){patients=p;admins=a;this.policy=policy;}
 @Transactional("postgresTransactionManager") public long cleanupPatients(){return patients.deleteByAttemptedAtBefore(Instant.now().minus(java.time.Duration.ofDays(policy.historyRetentionDays())));}
 @Transactional("mysqlTransactionManager") public long cleanupAdmins(){return admins.deleteByAttemptedAtBefore(Instant.now().minus(java.time.Duration.ofDays(policy.historyRetentionDays())));}
}
