package vn.gastroai.be.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.gastroai.be.application.auth.AccountLockedException;
import vn.gastroai.be.application.support.ResourceNotFoundException;

import java.util.Map;

/**
 * Bắt exception nghiệp vụ ném ra từ service, map sang HTTP status/JSON body thống nhất —
 * để controller không cần try/catch lặp lại ở từng endpoint. Dùng chung cho cả luồng
 * Bệnh nhân (AuthService) lẫn Admin/Bác sĩ (CmsAuthService) vì exception dùng chung.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // CmsAuthController tự kiểm tra "đã đăng nhập chưa" thủ công; MeController cũng giữ một
    // check phòng vệ, dù SecurityConfig đã bắt buộc ROLE_PATIENT cho /me/**. Không có handler
    // này, exception rơi về AccessDeniedHandler mặc định của Spring Security — response không
    // có field "message" mà FE đang mong đợi, hiện chữ chung chung vô nghĩa (vd session hết
    // hạn do BE restart nhưng FE vẫn tưởng còn đăng nhập).
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
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

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockConflict(
            ObjectOptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Du lieu da duoc cap nhat o noi khac. Vui long tai lai va thu lai."));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, String>> handleMailFailure(MailException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "code", "EMAIL_SERVICE_UNAVAILABLE",
                        "message", "Khong the gui email luc nay, vui long thu lai sau"));
    }

    // UC0017/UC0028/029 - GeminiEmbeddingClient/GeminiChatClient dung RestClient.retrieve()
    // khong .onStatus(...) rieng, nen loi HTTP tu Gemini (429 het quota, 503 qua tai...) nem ra
    // day nguyen dang, KHONG duoc bat o day thi FE se thay "Internal Server Error" chung chung
    // (da xac nhan thuc te: 429 "GenerateRequestsPerDayPerProjectPerModel-FreeTier" khi het
    // 20 luot/ngay cua tier free) - phai bao ro la loi tu dich vu AI, khong phai loi he thong.
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceFailure(RestClientResponseException exception) {
        log.error("Loi tu Gemini API: status={} body={}", exception.getStatusCode(),
                exception.getResponseBodyAsString(), exception);
        boolean quotaExceeded = exception.getStatusCode().value() == 429;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", quotaExceeded ? "AI_QUOTA_EXCEEDED" : "AI_SERVICE_UNAVAILABLE",
                "message", quotaExceeded
                        ? "Da het luot su dung tro ly AI hom nay, vui long thu lai sau"
                        : "Tro ly AI dang tam thoi khong phan hoi duoc, vui long thu lai sau it phut"));
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
