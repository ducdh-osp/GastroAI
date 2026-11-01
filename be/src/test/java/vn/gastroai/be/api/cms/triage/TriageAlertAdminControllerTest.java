package vn.gastroai.be.api.cms.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static TriageAlertResponse alert(String status, Long claimedById, String claimedByType,
                                             Long resolvedById, String resolvedByType) {
        return new TriageAlertResponse(
                10L, 1L, "Nguyen Van A", "0901234567", null, null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), status,
                claimedById, claimedByType,
                claimedById == null ? null : Instant.parse("2026-09-30T00:05:00Z"),
                resolvedById == null ? null : Instant.parse("2026-09-30T00:10:00Z"),
                resolvedById, resolvedByType,
                Instant.parse("2026-09-30T00:00:00Z"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void listReturnsRecentAlertsForAdmin() throws Exception {
        when(triageAlertService.listRecent()).thenReturn(List.of(alert("NEW", null, null, null, null)));

        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].patientFullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].status").value("NEW"));

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listRejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void claimPassesAuthenticatedAdminIdAndTypeAndIsAudited() throws Exception {
        when(triageAlertService.claim(10L, 7L, "ADMIN"))
                .thenReturn(alert("IN_PROGRESS", 7L, "ADMIN", null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.claimedById").value(7))
                .andExpect(jsonPath("$.claimedByType").value("ADMIN"));

        verify(triageAlertService).claim(eq(10L), eq(7L), eq("ADMIN"));
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_CLAIM, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Mới → Đang xử lý");
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void claimResolvesClaimerTypeAsDoctorFromAuthorities() throws Exception {
        when(triageAlertService.claim(10L, 9L, "DOCTOR"))
                .thenReturn(alert("IN_PROGRESS", 9L, "DOCTOR", null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isOk());

        verify(triageAlertService).claim(eq(10L), eq(9L), eq("DOCTOR"));
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_CLAIM, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Mới → Đang xử lý");
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void claimRejectedBecauseAlreadyTakenIsNotAudited() throws Exception {
        when(triageAlertService.claim(10L, 9L, "DOCTOR"))
                .thenThrow(new IllegalStateException("Cảnh báo đã được Admin #7 tiếp nhận"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isConflict());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void resolveByClaimerIsAudited() throws Exception {
        when(triageAlertService.resolve(10L, 7L, "ADMIN"))
                .thenReturn(alert("RESOLVED", 7L, "ADMIN", 7L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedById").value(7))
                .andExpect(jsonPath("$.resolvedByType").value("ADMIN"));

        verify(triageAlertService).resolve(eq(10L), eq(7L), eq("ADMIN"));
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RESOLVE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đang xử lý → Đã xử lý");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminOverrideIsVisibleInAuditDetail() throws Exception {
        when(triageAlertService.resolve(10L, 7L, "ADMIN"))
                .thenReturn(alert("RESOLVED", 9L, "DOCTOR", 7L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RESOLVE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đang xử lý → Đã xử lý (Admin đóng hộ, người tiếp nhận: DOCTOR #9)");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientCannotResolveAndNothingIsAudited() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isForbidden());

        verify(triageAlertService, never()).resolve(any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void describeResolveComparesBothIdAndAccountType() {
        assertEquals("Bệnh nhân #1 · Đang xử lý → Đã xử lý (Admin đóng hộ, người tiếp nhận: DOCTOR #7)",
                TriageAlertAdminController.describeResolve(
                        alert("RESOLVED", 7L, "DOCTOR", 7L, "ADMIN"), 7L, "ADMIN"));
    }
}