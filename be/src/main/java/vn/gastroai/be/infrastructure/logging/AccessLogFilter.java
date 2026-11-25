package vn.gastroai.be.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Ghi 1 dòng access log cho mỗi request: "{method} {path} -> {status} ({ms}ms) actor={actor}".
 * requestId không ghi lặp trong message vì đã có sẵn qua MDC (pattern/structured log).
 * CHỈ ghi path, KHÔNG ghi query string — query có thể chứa token (vd ?token= xác thực email).
 * Không đăng ký bằng @Component — xem LoggingFilterConfig.
 */
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    private static final int MAX_PATH_LENGTH = 200;
    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            "css", "js", "map", "png", "jpg", "jpeg", "gif", "svg", "ico", "webp",
            "woff", "woff2", "ttf", "eot", "html");

    private final long slowRequestMs;

    public AccessLogFilter(long slowRequestMs) {
        this.slowRequestMs = slowRequestMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            // Exception lọt ra khỏi chain (chưa ai xử lý): container sẽ trả 500, status trên response lúc
            // này chưa đổi nên ghi thẳng 500. Stack trace do nơi xử lý lỗi ghi, ở đây chỉ 1 dòng.
            write(request, 500, elapsedMs(start), false);
            throw exception;
        }
        write(request, response.getStatus(), elapsedMs(start), request.isAsyncStarted());
    }

    private void write(HttpServletRequest request, int status, long ms, boolean async) {
        String actor = MDC.get(RequestIdFilter.MDC_ACTOR);
        String path = LogSanitizer.truncate(pathOf(request), MAX_PATH_LENGTH);
        if (async) {
            // Chat streaming (SSE): lúc này mới mở luồng, chưa xong -> không xét chậm, kẻo mọi lượt chat đều WARN.
            log.info("{} {} -> {} ({}ms, async) actor={}", request.getMethod(), path, status, ms, actorOf(actor));
        } else if (status >= 500) {
            log.error("{} {} -> {} ({}ms) actor={}", request.getMethod(), path, status, ms, actorOf(actor));
        } else if (ms >= slowRequestMs) {
            log.warn("CHAM {} {} -> {} ({}ms) actor={}", request.getMethod(), path, status, ms, actorOf(actor));
        } else {
            log.info("{} {} -> {} ({}ms) actor={}", request.getMethod(), path, status, ms, actorOf(actor));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = pathOf(request);
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")
                || path.equals("/favicon.ico")) {
            return true;
        }
        // SockJS long-polling/heartbeat gọi /ws/** liên tục -> rác log, không có giá trị tra cứu.
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            return true;
        }
        return isStaticFile(path);
    }

    private static boolean isStaticFile(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot <= slash) {
            return false;
        }
        return STATIC_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String pathOf(HttpServletRequest request) {
        // getRequestURI() không bao gồm query string và chưa giải mã -> không có ký tự xuống dòng thật.
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri == null) {
            return "";
        }
        return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
    }

    private static String actorOf(String actor) {
        return actor == null ? "-" : actor;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
