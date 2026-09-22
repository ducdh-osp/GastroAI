package vn.gastroai.be.application.auth;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.AuthResult;
import vn.gastroai.be.domain.auth.*;
import vn.gastroai.be.infrastructure.persistence.postgres.*;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminAccountRepository;
import vn.gastroai.be.infrastructure.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
 private final PatientRepository patients; private final AdminAccountRepository admins; private final PatientLoginHistoryRepository history; private final RevokedTokenRepository revoked; private final PasswordEncoder encoder; private final JwtService jwt; private final EmailVerificationService mail; private final LoginSecurityPolicy policy;
 public AuthService(PatientRepository p,AdminAccountRepository a,PatientLoginHistoryRepository h,RevokedTokenRepository r,PasswordEncoder e,JwtService j,EmailVerificationService m,LoginSecurityPolicy policy){patients=p;admins=a;history=h;revoked=r;encoder=e;jwt=j;mail=m;this.policy=policy;}
 @Transactional("postgresTransactionManager") public void register(String email,String password,String name){String n=normalize(email);if(patients.findByEmail(n).isPresent()||admins.findByEmail(n).isPresent())throw new IllegalStateException("Email da duoc su dung");String raw=UUID.randomUUID()+UUID.randomUUID().toString();Patient p=new Patient();p.setEmail(n);p.setPasswordHash(encoder.encode(password));p.setFullName(name.trim());p.setRole(UserRole.PATIENT);p.setEmailVerified(false);p.setVerificationTokenHash(hash(raw));p.setVerificationTokenExpiresAt(Instant.now().plus(24,java.time.temporal.ChronoUnit.HOURS));patients.saveAndFlush(p);mail.send(n,raw);}
 @Transactional("postgresTransactionManager") public void verify(String token){Patient p=patients.findByVerificationTokenHash(hash(token)).orElseThrow(()->new IllegalArgumentException("Token xac thuc khong hop le"));if(p.getVerificationTokenExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("Token xac thuc da het han");p.setEmailVerified(true);p.setVerificationTokenHash(null);p.setVerificationTokenExpiresAt(null);}
 @Transactional("postgresTransactionManager") public void requestPasswordReset(String email){String normalized=normalize(email);Patient p=patients.findByEmail(normalized).orElse(null);if(p==null){encoder.matches(UUID.randomUUID().toString(),"$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW");return;}String raw=UUID.randomUUID()+UUID.randomUUID().toString();p.setPasswordResetTokenHash(hash(raw));p.setPasswordResetTokenExpiresAt(Instant.now().plus(30,java.time.temporal.ChronoUnit.MINUTES));mail.sendPasswordReset(p.getEmail(),raw);}
 @Transactional("postgresTransactionManager") public void resetPassword(String token,String newPassword){Patient p=patients.findByPasswordResetTokenHash(hash(token)).orElseThrow(()->new IllegalArgumentException("Token dat lai mat khau khong hop le"));if(p.getPasswordResetTokenExpiresAt()==null||p.getPasswordResetTokenExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("Token dat lai mat khau da het han");p.setPasswordHash(encoder.encode(newPassword));p.setPasswordResetTokenHash(null);p.setPasswordResetTokenExpiresAt(null);p.setFailedLoginAttempts(0);p.setLockedUntil(null);p.setTokenVersion(p.getTokenVersion()+1);}
 @Transactional(value="postgresTransactionManager",noRollbackFor={BadCredentialsException.class,AccountLockedException.class,IllegalStateException.class}) public AuthResult login(LoginCommand c,ClientRequestInfo info,boolean staff){String email=normalize(c.email());Optional<Patient> found=patients.findByEmailForUpdate(email);if(found.isEmpty()){encoder.matches(c.password(),"$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW");throw new BadCredentialsException("Email hoac mat khau khong dung");}Patient p=found.get();Instant now=Instant.now();if(p.getLockedUntil()!=null&&p.getLockedUntil().isAfter(now)){record(p,LoginOutcome.BLOCKED,"ACCOUNT_LOCKED",info);throw new AccountLockedException(p.getLockedUntil());}if(p.getLockedUntil()!=null){p.setLockedUntil(null);p.setFailedLoginAttempts(0);}
  // Kiểm tra đúng cổng trước khi kiểm tra mật khẩu. Một tài khoản dùng nhầm
  // cổng vẫn được ghi nhận, nhưng không bị tăng bộ đếm chống brute-force.
  boolean portalMatches = staff ? p.getRole() == UserRole.DOCTOR : p.getRole() == UserRole.PATIENT;
  if (!portalMatches) {
   encoder.matches(c.password(), p.getPasswordHash()); // giữ thời gian xử lý tương tự đăng nhập bình thường
   record(p, LoginOutcome.FAILURE, "WRONG_PORTAL", info);
   throw new BadCredentialsException("Tài khoản này phải đăng nhập tại cổng phù hợp");
  }
  if(!encoder.matches(c.password(),p.getPasswordHash())){p.setFailedLoginAttempts(p.getFailedLoginAttempts()+1);boolean locked=p.getFailedLoginAttempts()>=policy.maxFailedAttempts();if(locked)p.setLockedUntil(now.plus(policy.lockDuration()));record(p,locked?LoginOutcome.BLOCKED:LoginOutcome.FAILURE,locked?"TOO_MANY_ATTEMPTS":"BAD_PASSWORD",info);if(locked)throw new AccountLockedException(p.getLockedUntil());throw new BadCredentialsException("Email hoac mat khau khong dung");}if(!p.isEmailVerified()){record(p,LoginOutcome.FAILURE,"EMAIL_NOT_VERIFIED",info);throw new IllegalStateException("Email chua duoc xac thuc");}p.setFailedLoginAttempts(0);p.setLockedUntil(null);record(p,LoginOutcome.SUCCESS,null,info);String token=jwt.generateToken(p);return new AuthResult(token,p.getId(),p.getEmail(),p.getFullName(),p.getRole().name(),jwt.expiration(token));}
 public AuthResult login(LoginCommand c){return login(c,new ClientRequestInfo("unknown","unknown","unknown"),false);}
 @Transactional("postgresTransactionManager") public void logoutSafely(String token){Claims claims;try{claims=jwt.parse(token);}catch(io.jsonwebtoken.JwtException|IllegalArgumentException ignored){return;}revoked.save(new RevokedToken(claims.getId(),claims.getExpiration().toInstant(),Instant.now()));}
 @Transactional("postgresTransactionManager") public void changePassword(Long id,String oldPass,String newPass){Patient p=patients.findById(id).orElseThrow(()->new BadCredentialsException("Tai khoan khong ton tai"));if(!encoder.matches(oldPass,p.getPasswordHash()))throw new BadCredentialsException("Mat khau hien tai khong dung");if(encoder.matches(newPass,p.getPasswordHash()))throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");p.setPasswordHash(encoder.encode(newPass));p.setTokenVersion(p.getTokenVersion()+1);}
 public org.springframework.data.domain.Page<PatientLoginHistory> history(Long id,org.springframework.data.domain.Pageable p){return history.findByPatientId(id,p);} private void record(Patient p,LoginOutcome o,String reason,ClientRequestInfo i){history.save(new PatientLoginHistory(p,Instant.now(),o,reason,i.ipAddress(),i.userAgent(),i.deviceLabel()));} private static String normalize(String e){return e.trim().toLowerCase(Locale.ROOT);} private static String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
