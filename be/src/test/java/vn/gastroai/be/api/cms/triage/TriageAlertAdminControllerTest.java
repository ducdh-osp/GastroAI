package vn.gastroai.be.api.cms.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertConversationService;
import vn.gastroai.be.application.triage.TriageAlertDetailResponse;
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
    private TriageAlertConversationService conversationService;

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
                Instant.parse("2026-09-30T00:00:00Z"),
                "RESOLVED".equals(status) ? "HANDLED" : null, null, 0, null, null, 0, 0, 0);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void listReturnsOpenPageByDefault() throws Exception {
        when(triageAlertService.list("OPEN", 0, 20))
                .thenReturn(new PageImpl<>(List.of(alert("NEW", null, null, null, null))));

        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].patientFullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.items[0].status").value("NEW"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void listPassesViewAndPaging() throws Exception {
        when(triageAlertService.list("CLOSED", 2, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/cms/triage-alerts")
                        .param("view", "CLOSED").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(triageAlertService).list("CLOSED", 2, 10);
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void listRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-alerts").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void detailReturnsAlertActionsAndMessages() throws Exception {
        when(triageAlertService.getDetail(10L, 9L, "DOCTOR")).thenReturn(new TriageAlertDetailResponse(
                alert("IN_PROGRESS", 9L, "DOCTOR", null, null),
                List.of(new TriageAlertDetailResponse.ActionItem(1L, "CREATED", null, null, null,
                        Instant.parse("2026-09-30T00:00:00Z"))),
                List.of()));

        mockMvc.perform(get("/api/v1/cms/triage-alerts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alert.id").value(10))
                .andExpect(jsonPath("$.actions[0].actionType").value("CREATED"));

        verify(triageAlertService).getDetail(10L, 9L, "DOCTOR");
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void detailPassesAdminAsViewer() throws Exception {
        when(triageAlertService.getDetail(10L, 7L, "ADMIN")).thenReturn(new TriageAlertDetailResponse(
                alert("IN_PROGRESS", 9L, "DOCTOR", null, null), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/cms/triage-alerts/10"))
                .andExpect(status().isOk());

        verify(triageAlertService).getDetail(10L, 7L, "ADMIN");
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
        when(triageAlertService.resolve(10L, 7L, "ADMIN", null))
                .thenReturn(alert("RESOLVED", 7L, "ADMIN", 7L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedById").value(7))
                .andExpect(jsonPath("$.resolvedByType").value("ADMIN"));

        verify(triageAlertService).resolve(eq(10L), eq(7L), eq("ADMIN"), eq(null));
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RESOLVE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đang xử lý → Đã xử lý");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminOverrideIsVisibleInAuditDetail() throws Exception {
        when(triageAlertService.resolve(10L, 7L, "ADMIN", null))
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

        verify(triageAlertService, never()).resolve(any(), any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void resolveWithNotePassesNoteToService() throws Exception {
        when(triageAlertService.resolve(10L, 7L, "ADMIN", "Da huong dan"))
                .thenReturn(alert("RESOLVED", 7L, "ADMIN", 7L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf())
                        .contentType("application/json").content("{\"note\":\"Da huong dan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("HANDLED"));
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void resolveWithoutStaffMessageIsConflictAndNotAudited() throws Exception {
        when(triageAlertService.resolve(10L, 9L, "DOCTOR", null)).thenThrow(new IllegalStateException(
                "Cần gửi ít nhất 1 tin nhắn cho bệnh nhân trước khi đánh dấu xử lý xong"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isConflict());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void releaseIsAudited() throws Exception {
        when(triageAlertService.release(10L, 9L, "DOCTOR")).thenReturn(alert("NEW", null, null, null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/release").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RELEASE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đang xử lý → Mới");
    }

    @Test
    @WithMockUser(username = "5", roles = "DOCTOR")
    void releaseByOtherDoctorIsConflictAndNotAudited() throws Exception {
        when(triageAlertService.release(10L, 5L, "DOCTOR")).thenThrow(new IllegalStateException("x"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/release").with(csrf()))
                .andExpect(status().isConflict());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void sendMessageAuditsLengthButNotContent() throws Exception {
        when(conversationService.sendStaffMessage(10L, 9L, "DOCTOR", "Ban hay den vien ngay"))
                .thenReturn(new TriageAlertConversationService.StaffMessageResult(10L, 1L,
                        new TriageAlertDetailResponse.MessageItem(77L, "DOCTOR", 9L, "Ban hay den vien ngay",
                                null, Instant.parse("2026-09-30T00:06:00Z"))));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"Ban hay den vien ngay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77));

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_MESSAGE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · 21 ký tự");
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void sendMessageRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).sendStaffMessage(any(), any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void falseAlarmRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/false-alarm").with(csrf())
                        .contentType("application/json").content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(triageAlertService, never()).markFalseAlarm(any(), any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void falseAlarmIsAuditedWithoutReasonText() throws Exception {
        when(triageAlertService.markFalseAlarm(10L, 9L, "DOCTOR", "Benh nhan thu he thong"))
                .thenReturn(alert("RESOLVED", 9L, "DOCTOR", 9L, "DOCTOR"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/false-alarm").with(csrf())
                        .contentType("application/json").content("{\"reason\":\"Benh nhan thu he thong\"}"))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_FALSE_ALARM, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đang xử lý → Báo nhầm/Spam");
    }

    @Test
    void describeResolveComparesBothIdAndAccountType() {
        assertEquals("Bệnh nhân #1 · Đang xử lý → Đã xử lý (Admin đóng hộ, người tiếp nhận: DOCTOR #7)",
                TriageAlertAdminController.describeResolve(
                        alert("RESOLVED", 7L, "DOCTOR", 7L, "ADMIN"), 7L, "ADMIN"));
    }
}