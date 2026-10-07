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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    // ===== UC0020 - thung rac (soft-delete) =====

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void trashReturnsOk() throws Exception {
        when(bristolLogService.listTrash(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/patient/bristol-logs/trash").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void restoreReturnsOkWhenWithinRetentionWindow() throws Exception {
        when(bristolLogService.restore(1L, 99L)).thenReturn(
                new BristolLogResponse(99L, java.time.Instant.now(), 4, null));

        mockMvc.perform(post("/api/v1/patient/bristol-logs/99/restore").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void restoreReturnsConflictWhenPastRetentionWindow() throws Exception {
        when(bristolLogService.restore(anyLong(), anyLong()))
                .thenThrow(new IllegalStateException("Mục này đã nằm trong thùng rác quá 30 ngày nên không thể khôi phục."));

        mockMvc.perform(post("/api/v1/patient/bristol-logs/99/restore").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Mục này đã nằm trong thùng rác quá 30 ngày nên không thể khôi phục."));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void trashRejectsAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/patient/bristol-logs/trash").with(csrf()))
                .andExpect(status().isForbidden());
    }
}