package vn.gastroai.be.api.cms.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TriageAlertAdminController.class)
@Import(SecurityConfig.class)
class TriageAlertAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageAlertService triageAlertService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void listReturnsRecentAlertsForAdmin() throws Exception {
        when(triageAlertService.listRecent()).thenReturn(List.of(new TriageAlertResponse(
                10L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), "NEW",
                null, null, null, null, Instant.parse("2026-09-30T00:00:00Z"))));

        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].patientFullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listRejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void claimPassesAuthenticatedAdminIdAndTypeToService() throws Exception {
        when(triageAlertService.claim(10L, 7L, "ADMIN")).thenReturn(new TriageAlertResponse(
                10L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), "IN_PROGRESS",
                7L, "ADMIN", Instant.parse("2026-09-30T00:05:00Z"), null,
                Instant.parse("2026-09-30T00:00:00Z")));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.claimedById").value(7))
                .andExpect(jsonPath("$.claimedByType").value("ADMIN"));

        verify(triageAlertService).claim(eq(10L), eq(7L), eq("ADMIN"));
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void claimResolvesClaimerTypeAsDoctorFromAuthorities() throws Exception {
        when(triageAlertService.claim(10L, 9L, "DOCTOR")).thenReturn(new TriageAlertResponse(
                10L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), "IN_PROGRESS",
                9L, "DOCTOR", Instant.parse("2026-09-30T00:05:00Z"), null,
                Instant.parse("2026-09-30T00:00:00Z")));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isOk());

        verify(triageAlertService).claim(eq(10L), eq(9L), eq("DOCTOR"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void resolveMarksAlertResolved() throws Exception {
        when(triageAlertService.resolve(10L)).thenReturn(new TriageAlertResponse(
                10L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), "RESOLVED",
                7L, "ADMIN", Instant.parse("2026-09-30T00:05:00Z"),
                Instant.parse("2026-09-30T00:10:00Z"), Instant.parse("2026-09-30T00:00:00Z")));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(triageAlertService).resolve(10L);
    }
}