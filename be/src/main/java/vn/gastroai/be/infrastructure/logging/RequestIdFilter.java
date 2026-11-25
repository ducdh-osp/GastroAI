package vn.gastroai.be.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Gắn 1 mã tra cứu (requestId) cho mỗi request: đưa vào MDC để MỌI dòng log của request đó tự
 * mang mã (xem logging.pattern.level / structured log), và trả lại qua header X-Request-Id để
 * FE hiện cho người dùng báo lỗi. Không đăng ký bằng @Component — xem LoggingFilterConfig.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_ACTOR = "actor";

    // Giữ cùng 1 mã cho các lần dispatch ASYNC/ERROR của cùng request (vd chat streaming SSE).
    private static final String REQUEST_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    // Dùng matches() (khớp TOÀN BỘ chuỗi) — "$" với find() vẫn khớp trước 1 ký tự xuống dòng ở
    // cuối, đủ để kẻ xấu chèn dòng log giả qua header.
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
        String requestId = existing instanceof String id ? id : resolve(request.getHeader(HEADER));
        request.setAttribute(REQUEST_ATTRIBUTE, requestId);

        MDC.put(MDC_REQUEST_ID, requestId);
        if (!response.isCommitted()) {
            response.setHeader(HEADER, requestId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Thread của Tomcat được tái sử dụng: không dọn thì request sau "thừa kế" mã/actor cũ.
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_ACTOR);
        }
    }

    // Chạy cả ở lần dispatch ASYNC/ERROR để log ở các lần đó vẫn có requestId (mã lấy lại từ attribute).
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    /** Mã của request đang xử lý trên thread hiện tại, null nếu ngoài 1 request (vd job chạy nền). */
    public static String currentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }

    static String resolve(String headerValue) {
        if (headerValue != null && VALID_REQUEST_ID.matcher(headerValue).matches()) {
            return headerValue;
        }
        return newRequestId();
    }

    private static String newRequestId() {
        // Mã tra cứu, không phải bí mật -> không cần SecureRandom.
        return String.format("%08x", ThreadLocalRandom.current().nextInt());
    }
}
