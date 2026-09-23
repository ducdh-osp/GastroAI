package vn.gastroai.be.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.gastroai.be.application.auth.AccountLockedException;

import java.util.Map;

/**
 * Bắt exception nghiệp vụ ném ra từ service, map sang HTTP status/JSON body thống nhất —
 * để controller không cần try/catch lặp lại ở từng endpoint. Dùng chung cho cả luồng
 * Bệnh nhân (AuthService) lẫn Admin/Bác sĩ (CmsAuthService) vì exception dùng chung.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // MeController và CmsAuthController tự kiểm tra "đã đăng nhập chưa" thủ công (không qua
    // anyRequest().authenticated() vì /me/** và /cms/auth/** không được Spring Security tự
    // gác). Không có handler này, exception rơi về AccessDeniedHandler mặc định của Spring
    // Security — response không có field "message" mà FE đang mong đợi, hiện chữ chung chung
    // vô nghĩa (vd session hết hạn do BE restart nhưng FE vẫn tưởng còn đăng nhập).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", exception.getMessage()));
    }

    // 423 Locked kèm lockedUntil để FE hiển thị chính xác "còn khoá đến mấy giờ" (UC0008).
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockedAccount(AccountLockedException exception) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(Map.of(
                "code", "ACCOUNT_TEMPORARILY_LOCKED",
                "message", exception.getMessage(),
                "lockedUntil", exception.getLockedUntil()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    // Lưới an toàn cuối cùng cho race condition (2 request cùng lúc lọt qua check
    // "email đã tồn tại chưa" rồi cùng insert) — DB tự chặn nhờ ràng buộc UNIQUE,
    // Hibernate ném exception này, map về 409 giống IllegalStateException phía trên.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Du lieu da ton tai"));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, String>> handleMailFailure(MailException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "code", "EMAIL_SERVICE_UNAVAILABLE",
                        "message", "Khong the gui email luc nay, vui long thu lai sau"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Du lieu khong hop le");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
