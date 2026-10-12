package vn.gastroai.be.api.cms.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.triage.TriageKeywordRequest;
import vn.gastroai.be.application.triage.TriageKeywordResponse;
import vn.gastroai.be.application.triage.TriageKeywordService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TriageKeywordAdminController.class)
@Import(SecurityConfig.class)
class TriageKeywordAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageKeywordService triageKeywordService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static TriageKeywordResponse response() {
        return new TriageKeywordResponse(1L, "TAC_RUOT", "Tắc ruột", "khong xi hoi duoc",
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void listIsAllowedForAdmin() throws Exception {
        when(triageKeywordService.list()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/cms/triage-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("khong xi hoi duoc"));
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void listIsAllowedForDoctorToo() throws Exception {
        when(triageKeywordService.list()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/cms/triage-keywords"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listRejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-keywords"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createPassesRequestToService() throws Exception {
        TriageKeywordRequest request = new TriageKeywordRequest("TAC_RUOT", "khong xi hoi duoc");
        when(triageKeywordService.create(request)).thenReturn(response());

        mockMvc.perform(post("/api/v1/cms/triage-keywords")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"groupCode\":\"TAC_RUOT\",\"keyword\":\"khong xi hoi duoc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("khong xi hoi duoc"));

        verify(triageKeywordService).create(eq(request));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void updateReturns200WithUpdatedKeyword() throws Exception {
        TriageKeywordRequest request = new TriageKeywordRequest("TAC_RUOT", "bi tac ruot nang");
        when(triageKeywordService.update(1L, request)).thenReturn(response());

        mockMvc.perform(put("/api/v1/cms/triage-keywords/1")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"groupCode\":\"TAC_RUOT\",\"keyword\":\"bi tac ruot nang\"}"))
                .andExpect(status().isOk());

        verify(triageKeywordService).update(eq(1L), eq(request));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/triage-keywords/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(triageKeywordService).delete(1L);
    }
}
