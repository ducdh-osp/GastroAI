package vn.gastroai.be.application.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.admin.*;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.api.auth.InvalidPasswordResetTokenException;
import vn.gastroai.be.infrastructure.persistence.mysql.*;
import java.time.Instant;

@Service
public class AdminLoginService {
    private final AdminAccountRepository admins; private final AdminLoginHistoryRepository history;
    private final PasswordEncoder encoder; private final LoginSecurityPolicy policy; private final EmailVerificationService mail;
    public AdminLoginService(AdminAccountRepository a,AdminLoginHistoryRepository h,PasswordEncoder e,LoginSecurityPolicy p,EmailVerificationService m){admins=a;history=h;encoder=e;policy=p;mail=m;}
    @Transactional(value="mysqlTransactionManager",noRollbackFor={BadCredentialsException.class,AccountLockedException.class})
    public AdminAccount login(String email,String password,ClientRequestInfo info){var found=admins.findByEmailForUpdate(email.trim().toLowerCase());if(found.isEmpty()){encoder.matches(password,"$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW");throw new BadCredentialsException("Email hoac mat khau khong dung");}AdminAccount a=found.get();Instant now=Instant.now();if(a.getLockedUntil()!=null&&a.getLockedUntil().isAfter(now)){record(a,LoginOutcome.BLOCKED,"ACCOUNT_LOCKED",info);throw new AccountLockedException(a.getLockedUntil());}if(a.getLockedUntil()!=null){a.setLockedUntil(null);a.setFailedLoginAttempts(0);}if(!a.isActive()){record(a,LoginOutcome.FAILURE,"INACTIVE_ACCOUNT",info);throw new BadCredentialsException("Email hoac mat khau khong dung");}if(!encoder.matches(password,a.getPasswordHash())){a.setFailedLoginAttempts(a.getFailedLoginAttempts()+1);boolean locked=a.getFailedLoginAttempts()>=policy.maxFailedAttempts();if(locked)a.setLockedUntil(now.plus(policy.lockDuration()));record(a,locked?LoginOutcome.BLOCKED:LoginOutcome.FAILURE,locked?"TOO_MANY_ATTEMPTS":"BAD_PASSWORD",info);if(locked)throw new AccountLockedException(a.getLockedUntil());throw new BadCredentialsException("Email hoac mat khau khong dung");}a.setFailedLoginAttempts(0);a.setLockedUntil(null);record(a,LoginOutcome.SUCCESS,null,info);return a;}
    @Transactional("mysqlTransactionManager")
    public boolean recordWrongPortalIfPresent(String email, String password, ClientRequestInfo info) {
        var found = admins.findByEmailForUpdate(email.trim().toLowerCase(java.util.Locale.ROOT));
        if (found.isEmpty()) return false;
        AdminAccount admin = found.get();
        // Thực hiện BCrypt để thời gian xử lý không phụ thuộc vào việc mật khẩu có đúng hay không.
        encoder.matches(password, admin.getPasswordHash());
        record(admin, LoginOutcome.FAILURE, "WRONG_PORTAL", info);
        return true;
    }
    @Transactional("mysqlTransactionManager") public boolean requestPasswordReset(String email){AdminAccount a=admins.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).orElse(null);if(a==null){encoder.matches(java.util.UUID.randomUUID().toString(),"$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW");return false;}String raw=java.util.UUID.randomUUID()+java.util.UUID.randomUUID().toString();a.setPasswordResetTokenHash(hash(raw));a.setPasswordResetTokenExpiresAt(java.time.Instant.now().plus(30,java.time.temporal.ChronoUnit.MINUTES));mail.sendPasswordReset(a.getEmail(),raw);return true;}
    @Transactional("mysqlTransactionManager") public void resetPassword(String token,String newPassword){AdminAccount a=admins.findByPasswordResetTokenHash(hash(token)).orElseThrow(InvalidPasswordResetTokenException::new);if(a.getPasswordResetTokenExpiresAt()==null||a.getPasswordResetTokenExpiresAt().isBefore(java.time.Instant.now()))throw new IllegalArgumentException("Token dat lai mat khau da het han");a.setPasswordHash(encoder.encode(newPassword));a.setPasswordResetTokenHash(null);a.setPasswordResetTokenExpiresAt(null);a.setFailedLoginAttempts(0);a.setLockedUntil(null);}
 public org.springframework.data.domain.Page<AdminLoginHistory> history(Long id,org.springframework.data.domain.Pageable p){return history.findByAdminId(id,p);}
    private void record(AdminAccount a,LoginOutcome o,String reason,ClientRequestInfo i){history.save(new AdminLoginHistory(a,Instant.now(),o,reason,i.ipAddress(),i.userAgent(),i.deviceLabel()));}
    private static String hash(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
