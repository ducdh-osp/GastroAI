package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.patient.BristolLogService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BristolLogController.class)
@Import(SecurityConfig.class)
class BristolLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BristolLogService bristolLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void createRejectsBristolTypeOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/patient/bristol-logs")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"loggedAt\":\"2026-01-01T08:00:00Z\",\"bristolType\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void trendRejectsDaysAboveMax() throws Exception {
        mockMvc.perform(get("/api/v1/patient/bristol-logs/trend").with(csrf()).param("days", "9999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void createRejectsFutureLoggedAt() throws Exception {
        mockMvc.perform(post("/api/v1/patient/bristol-logs")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"loggedAt\":\"2999-01-01T08:00:00Z\",\"bristolType\":4}"))
                .andExpect(status().isBadRequest());
    }
}
