package vn.gastroai.be.infrastructure.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/x");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /** Chạy filter, trả về requestId mà chain "nhìn thấy" trong MDC. */
    private String runAndCaptureMdc() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_REQUEST_ID));
        filter.doFilter(request, response, chain);
        return seen.get();
    }

    @Test
    void generatesIdWhenHeaderMissing() throws Exception {
        String id = runAndCaptureMdc();

        assertTrue(id.matches("[0-9a-f]{8}"), "ma tu sinh phai la 8 ky tu hex: " + id);
        assertEquals(id, response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void reusesValidIncomingId() throws Exception {
        request.addHeader(RequestIdFilter.HEADER, "client-Abc-123");

        String id = runAndCaptureMdc();

        assertEquals("client-Abc-123", id);
        assertEquals("client-Abc-123", response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void replacesIdWithNewlineOrSpecialCharacters() throws Exception {
        for (String forged : new String[]{"abc\n2026-01-01 INFO gia mao", "abc\n", "a b", "x;rm", "a".repeat(65)}) {
            MockHttpServletRequest forgedRequest = new MockHttpServletRequest("GET", "/api/v1/x");
            forgedRequest.addHeader(RequestIdFilter.HEADER, forged);
            MockHttpServletResponse forgedResponse = new MockHttpServletResponse();
            AtomicReference<String> seen = new AtomicReference<>();

            filter.doFilter(forgedRequest, forgedResponse,
                    (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_REQUEST_ID)));

            assertNotEquals(forged, seen.get());
            assertTrue(seen.get().matches("[0-9a-f]{8}"), "phai sinh ma moi thay cho: " + forged);
        }
    }

    @Test
    void mdcIsEmptyAfterFilterEvenWhenChainThrows() {
        FilterChain failingChain = (req, res) -> {
            MDC.put(RequestIdFilter.MDC_ACTOR, "PATIENT#12");
            throw new IllegalStateException("loi gia lap");
        };

        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, failingChain));

        assertNull(MDC.get(RequestIdFilter.MDC_REQUEST_ID));
        assertNull(MDC.get(RequestIdFilter.MDC_ACTOR));
    }
}
