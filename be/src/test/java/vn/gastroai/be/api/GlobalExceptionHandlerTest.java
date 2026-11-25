package vn.gastroai.be.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.infrastructure.logging.RequestIdFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String INTERNAL_DETAIL = "Connection refused: jdbc:postgresql://db-noi-bo:5432";

    @RestController
    static class FailingController {
        @GetMapping("/test/boom")
        String boom() {
            throw new UnsupportedOperationException(INTERNAL_DETAIL);
        }

        @PostMapping("/test/only-post")
        String onlyPost() {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void unexpectedExceptionReturns500WithRequestIdAndNoInternalMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/boom").header(RequestIdFilter.HEADER, "tra-cuu-01"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.UNEXPECTED_MESSAGE))
                .andExpect(jsonPath("$.requestId").value("tra-cuu-01"))
                .andExpect(header().string(RequestIdFilter.HEADER, "tra-cuu-01"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("jdbc"), body);
        assertFalse(body.contains("UnsupportedOperationException"), body);
    }

    @Test
    void springExceptionWithOwnStatusKeepsItsStatus() throws Exception {
        // HttpRequestMethodNotSupportedException phải giữ 405, không bị handleUnexpected biến thành 500.
        mockMvc.perform(get("/test/only-post"))
                .andExpect(status().isMethodNotAllowed());
    }
}
