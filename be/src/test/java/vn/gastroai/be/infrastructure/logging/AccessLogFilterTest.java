package vn.gastroai.be.infrastructure.logging;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class AccessLogFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void logsMethodPathStatusAndActorButNotQueryString(CapturedOutput output) throws Exception {
        AccessLogFilter filter = new AccessLogFilter(60_000);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/patient/food-diary");
        request.setQueryString("token=bi-mat-khong-duoc-log");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            MDC.put(RequestIdFilter.MDC_ACTOR, "PATIENT#12");
            ((HttpServletResponse) res).setStatus(201);
        });

        assertTrue(output.getOut().contains("POST /api/v1/patient/food-diary -> 201 ("), output.getOut());
        assertTrue(output.getOut().contains("actor=PATIENT#12"), output.getOut());
        assertFalse(output.getOut().contains("bi-mat-khong-duoc-log"), "khong duoc log query string");
    }

    @Test
    void slowRequestIsLoggedAsWarn(CapturedOutput output) throws Exception {
        // Ngưỡng 0ms: mọi request đều "chậm".
        AccessLogFilter filter = new AccessLogFilter(0);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cms/dashboard/stats");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertTrue(output.getOut().contains("CHAM GET /api/v1/cms/dashboard/stats -> 200"), output.getOut());
        assertTrue(output.getOut().contains("WARN"), output.getOut());
        assertTrue(output.getOut().contains("actor=-"), output.getOut());
    }

    @Test
    void skipsStaticFilesAndWebSocketPaths(CapturedOutput output) throws Exception {
        AccessLogFilter filter = new AccessLogFilter(0);
        for (String path : new String[]{"/assets/index-BO6B8UWv.js", "/ws/info", "/ws/123/abc/xhr_streaming",
                "/favicon.ico", "/actuator/health"}) {
            filter.doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), (req, res) -> { });
        }
        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/v1/patient/profile"),
                new MockHttpServletResponse(), (req, res) -> { });

        assertFalse(output.getOut().contains("index-BO6B8UWv.js"), output.getOut());
        assertFalse(output.getOut().contains("/ws/"), output.getOut());
        assertFalse(output.getOut().contains("/favicon.ico"), output.getOut());
        assertFalse(output.getOut().contains("/actuator/health"), output.getOut());
        assertFalse(output.getOut().contains("OPTIONS /api/v1/patient/profile"), output.getOut());
    }
}
