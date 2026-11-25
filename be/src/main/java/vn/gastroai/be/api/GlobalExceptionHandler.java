package vn.gastroai.be.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.gastroai.be.application.auth.AccountLockedException;
import vn.gastroai.be.application.auth.AccountDisabledException;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.infrastructure.logging.LogSanitizer;
import vn.gastroai.be.infrastructure.logging.RequestIdFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt exception nghiệp vụ ném ra từ service, map sang HTTP status/JSON body thống nhất —
 * để controller không cần try/catch lặp lại ở từng endpoint. Dùng chung cho cả luồng
 * Bệnh nhân (AuthService) lẫn Admin/Bác sĩ (CmsAuthService) vì exception dùng chung.
 * Mọi body lỗi đều qua errorBody(): giữ key "message" (FE đang đọc) + "requestId" để tra log.
 * Log: lỗi phía người dùng -> WARN 1 dòng, không stack trace; lỗi hệ thống -> ERROR kèm exception.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String UNEXPECTED_MESSAGE = "Đã có lỗi hệ thống. Vui lòng thử lại sau.";
    private static final int MAX_LOGGED_MESSAGE = 300;

    // CmsAuthController tự kiểm tra "đã đăng nhập chưa" thủ công. Không có handler này,
    // exception rơi về AccessDeniedHandler mặc định của Spring Security — response không
    // có field "message" mà FE đang mong đợi, hiện chữ chung chung vô nghĩa (vd session hết
    // hạn do BE restart nhưng FE vẫn tưởng còn đăng nhập).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
        return clientError(HttpStatus.UNAUTHORIZED, exception, errorBody(exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException exception) {
        return clientError(HttpStatus.UNAUTHORIZED, exception, errorBody(exception.getMessage()));
    }

    // 423 Locked kèm lockedUntil để FE hiển thị chính xác "còn khoá đến mấy giờ" (UC0008).
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockedAccount(AccountLockedException exception) {
        Map<String, Object> body = errorBody("ACCOUNT_TEMPORARILY_LOCKED", exception.getMessage());
        body.put("lockedUntil", exception.getLockedUntil());
        return clientError(HttpStatus.LOCKED, exception, body);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledAccount(AccountDisabledException exception) {
        return clientError(HttpStatus.LOCKED, exception, errorBody("ACCOUNT_DISABLED", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return clientError(HttpStatus.BAD_REQUEST, exception, errorBody(exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException exception) {
        return clientError(HttpStatus.NOT_FOUND, exception, errorBody(exception.getMessage()));
    }

    // Trả 409 (xung đột trạng thái nghiệp vụ) -> lỗi phía người dùng, WARN.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        return clientError(HttpStatus.CONFLICT, exception, errorBody(exception.getMessage()));
    }

    // Lưới an toàn cuối cùng cho race condition (2 request cùng lúc lọt qua check
    // "email đã tồn tại chưa" rồi cùng insert) — DB tự chặn nhờ ràng buộc UNIQUE,
    // Hibernate ném exception này, map về 409 giống IllegalStateException phía trên.
    // KHÔNG log message gốc: chi tiết của driver chứa giá trị vi phạm (vd email) -> chỉ ghi loại lỗi.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataConflict(DataIntegrityViolationException exception) {
        log.warn("Loi phia nguoi dung status=409 loai={} (vi pham rang buoc du lieu)",
                exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody("Du lieu da ton tai"));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockConflict(
            ObjectOptimisticLockingFailureException exception) {
        log.warn("Loi phia nguoi dung status=409 loai={} (du lieu da bi cap nhat dong thoi)",
                exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("Du lieu da duoc cap nhat o noi khac. Vui long tai lai va thu lai."));
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, Object>> handleMailFailure(MailException exception) {
        log.error("Loi he thong: khong gui duoc email", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorBody("EMAIL_SERVICE_UNAVAILABLE", "Khong the gui email luc nay, vui long thu lai sau"));
    }

    // UC0017/UC0028/029 - GeminiEmbeddingClient/GeminiChatClient dung RestClient.retrieve()
    // khong .onStatus(...) rieng, nen loi HTTP tu Gemini (429 het quota, 503 qua tai...) nem ra
    // day nguyen dang, KHONG duoc bat o day thi FE se thay "Internal Server Error" chung chung
    // (da xac nhan thuc te: 429 "GenerateRequestsPerDayPerProjectPerModel-FreeTier" khi het
    // 20 luot/ngay cua tier free) - phai bao ro la loi tu dich vu AI, khong phai loi he thong.
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceFailure(RestClientResponseException exception) {
        // Body Gemini có thể lặp lại câu hỏi của bệnh nhân -> cắt ngắn. getMessage() của exception
        // gốc cũng chứa nguyên body, nên stack trace được ghi qua 1 exception thay thế không mang body.
        RuntimeException withoutBody = new RuntimeException(
                exception.getClass().getSimpleName() + ": Gemini tra ve HTTP " + exception.getStatusCode().value());
        withoutBody.setStackTrace(exception.getStackTrace());
        log.error("Loi tu Gemini API: status={} body={}", exception.getStatusCode(),
                LogSanitizer.truncate(exception.getResponseBodyAsString(), 300), withoutBody);
        boolean quotaExceeded = exception.getStatusCode().value() == 429;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(
                quotaExceeded ? "AI_QUOTA_EXCEEDED" : "AI_SERVICE_UNAVAILABLE",
                quotaExceeded
                        ? "Da het luot su dung tro ly AI hom nay, vui long thu lai sau"
                        : "Tro ly AI dang tam thoi khong phan hoi duoc, vui long thu lai sau it phut"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Du lieu khong hop le");
        // Chỉ ghi tên field + thông báo ràng buộc, không ghi giá trị người dùng nhập.
        log.warn("Loi phia nguoi dung status=400 loai=Validation message={}", message);
        return ResponseEntity.badRequest().body(errorBody(message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        log.warn("Loi phia nguoi dung status=413 loai={}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(errorBody("File vuot qua gioi han 20MB cho phep"));
    }

    /**
     * Lưới an toàn cuối: exception chưa có handler riêng -> 500 với câu chung, KHÔNG lộ
     * getMessage() nội bộ ra client; người dùng báo lại requestId để tra stack trace trong log.
     * Exception mà Spring MVC/Security đã có mã riêng (405, 415, 404 NoResourceFound, 400 đọc body
     * lỗi, @ResponseStatus, ResponseStatusException, AuthenticationException...) được ném lại
     * nguyên vẹn để resolver mặc định xử lý như trước — không bị biến thành 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, HttpServletRequest request)
            throws Exception {
        if (isHandledByFramework(exception)) {
            throw exception;
        }
        log.error("Loi khong mong doi khi xu ly {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(UNEXPECTED_MESSAGE));
    }

    static boolean isHandledByFramework(Exception exception) {
        return exception instanceof ErrorResponse
                || exception instanceof AuthenticationException
                || exception instanceof TypeMismatchException
                || exception instanceof HttpMessageNotReadableException
                || exception instanceof BindException
                || exception instanceof AsyncRequestNotUsableException
                // Client đóng kết nối giữa chừng (vd rời trang khi chat streaming) — không phải lỗi hệ thống.
                || DisconnectedClientHelper.isClientDisconnectedException(exception)
                || AnnotatedElementUtils.hasAnnotation(exception.getClass(), ResponseStatus.class);
    }

    /** Body lỗi chuẩn: "message" + "requestId" (bỏ requestId nếu ngoài request, vd test không có filter). */
    static Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        String requestId = RequestIdFilter.currentRequestId();
        if (requestId != null) {
            body.put("requestId", requestId);
        }
        return body;
    }

    private static Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.putAll(errorBody(message));
        return body;
    }

    private static ResponseEntity<Map<String, Object>> clientError(HttpStatus status, Exception exception,
                                                                   Map<String, Object> body) {
        log.warn("Loi phia nguoi dung status={} loai={} message={}", status.value(),
                exception.getClass().getSimpleName(),
                LogSanitizer.truncate(exception.getMessage(), MAX_LOGGED_MESSAGE));
        return ResponseEntity.status(status).body(body);
    }
}
