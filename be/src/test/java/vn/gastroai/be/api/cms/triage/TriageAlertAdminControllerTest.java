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
import vn.gastroai.be.application.triage.TriageAlertDetailResponse;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.triage.TriageContactResult;
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
                10L, 1L, "Nguyen Van A", "0901234567", null,
                "Toi bi dau bung du doi", List.of("DAU_BUNG_CAP_TINH"), status,
                claimedById, claimedByType,
                claimedById == null ? null : Instant.parse("2026-09-30T00:05:00Z"),
                resolvedById == null ? null : Instant.parse("2026-09-30T00:10:00Z"),
                resolvedById, resolvedByType,
                Instant.parse("2026-09-30T00:00:00Z"),
                "RESOLVED".equals(status) ? "HANDLED" : null, null, 0, null, null, 0, 0, 0,
                "AUTO_HANDLING".equals(status) ? null : Instant.parse("2026-09-30T00:03:00Z"),
                claimedById == null ? null : Instant.parse("2026-09-30T00:05:00Z"),
                null, null, null);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void listReturnsOpenPageByDefault() throws Exception {
        when(triageAlertService.list("OPEN", 0, 20))
                .thenReturn(new PageImpl<>(List.of(alert("AUTO_HANDLING", null, null, null, null))));

        mockMvc.perform(get("/api/v1/cms/triage-alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].patientFullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.items[0].status").value("AUTO_HANDLING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void listPassesViewAndPaging() throws Exception {
        when(triageAlertService.list("CLOSED", 2, 10)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/cms/triage-alerts")
                        .param("view", "CLOSED").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(triageAlertService).list("CLOSED", 2, 10);
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void listRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-alerts").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void detailReturnsAlertActionsAndMessages() throws Exception {
        when(triageAlertService.getDetail(10L, 9L, "ADMIN")).thenReturn(new TriageAlertDetailResponse(
                alert("NEEDS_INTERVENTION", 9L, "ADMIN", null, null),
                List.of(new TriageAlertDetailResponse.ActionItem(1L, "CREATED", null, null, null,
                        Instant.parse("2026-09-30T00:00:00Z"))),
                List.of()));

        mockMvc.perform(get("/api/v1/cms/triage-alerts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alert.id").value(10))
                .andExpect(jsonPath("$.actions[0].actionType").value("CREATED"));

        verify(triageAlertService).getDetail(10L, 9L, "ADMIN");
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void detailPassesAdminAsViewer() throws Exception {
        when(triageAlertService.getDetail(10L, 7L, "ADMIN")).thenReturn(new TriageAlertDetailResponse(
                alert("NEEDS_INTERVENTION", 9L, "ADMIN", null, null), List.of(), List.of()));

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
                .thenReturn(alert("NEEDS_INTERVENTION", 7L, "ADMIN", null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/claim").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INTERVENTION"))
                .andExpect(jsonPath("$.claimedById").value(7))
                .andExpect(jsonPath("$.claimedByType").value("ADMIN"));

        verify(triageAlertService).claim(eq(10L), eq(7L), eq("ADMIN"));
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_CLAIM, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Tôi đang liên hệ");
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void claimRejectedBecauseAlreadyTakenIsNotAudited() throws Exception {
        when(triageAlertService.claim(10L, 9L, "ADMIN"))
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
                "Bệnh nhân #1 · → Đã xử lý");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminOverrideIsVisibleInAuditDetail() throws Exception {
        when(triageAlertService.resolve(10L, 7L, "ADMIN", null))
                .thenReturn(alert("RESOLVED", 9L, "ADMIN", 7L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RESOLVE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · → Đã xử lý (người đang liên hệ: ADMIN #9)");
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
    @WithMockUser(username = "9", roles = "ADMIN")
    void resolveWithoutStaffMessageIsConflictAndNotAudited() throws Exception {
        when(triageAlertService.resolve(10L, 9L, "ADMIN", null)).thenThrow(new IllegalStateException(
                "Cần gửi ít nhất 1 tin nhắn cho bệnh nhân trước khi đánh dấu xử lý xong"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/resolve").with(csrf()))
                .andExpect(status().isConflict());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void releaseIsAudited() throws Exception {
        when(triageAlertService.release(10L, 9L, "ADMIN")).thenReturn(alert("NEEDS_INTERVENTION", null, null, null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/release").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INTERVENTION"));

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_RELEASE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Trả lại, failsafe tính lại từ lúc này");
    }

    @Test
    @WithMockUser(username = "5", roles = "ADMIN")
    void releaseByOtherAdminIsConflictAndNotAudited() throws Exception {
        when(triageAlertService.release(10L, 5L, "ADMIN")).thenThrow(new IllegalStateException("x"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/release").with(csrf()))
                .andExpect(status().isConflict());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void falseAlarmRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/false-alarm").with(csrf())
                        .contentType("application/json").content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(triageAlertService, never()).markFalseAlarm(any(), any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "ADMIN")
    void falseAlarmIsAuditedWithoutReasonText() throws Exception {
        when(triageAlertService.markFalseAlarm(10L, 9L, "ADMIN", "Benh nhan thu he thong"))
                .thenReturn(alert("RESOLVED", 9L, "ADMIN", 9L, "ADMIN"));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/false-alarm").with(csrf())
                        .contentType("application/json").content("{\"reason\":\"Benh nhan thu he thong\"}"))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_FALSE_ALARM, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · → Báo nhầm/Spam");
    }

    @Test
    void describeResolveNamesClaimerOnlyWhenSomeoneElseClosed() {
        assertEquals("Bệnh nhân #1 · → Đã xử lý",
                TriageAlertAdminController.describeResolve(
                        alert("RESOLVED", 7L, "ADMIN", 7L, "ADMIN"), 7L, "ADMIN"));
        assertEquals("Bệnh nhân #1 · → Đã xử lý (người đang liên hệ: ADMIN #9)",
                TriageAlertAdminController.describeResolve(
                        alert("RESOLVED", 9L, "ADMIN", 7L, "ADMIN"), 7L, "ADMIN"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void recordContactPassesResultAndAuditsLabelButNotNote() throws Exception {
        when(triageAlertService.recordContact(10L, 7L, "ADMIN", TriageContactResult.RELATIVE_REACHED,
                "Chong 0909xxx se ve ngay"))
                .thenReturn(alert("NEEDS_INTERVENTION", 7L, "ADMIN", null, null));

        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/contact").with(csrf())
                        .contentType("application/json")
                        .content("{\"result\":\"RELATIVE_REACHED\",\"note\":\"Chong 0909xxx se ve ngay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INTERVENTION"));

        // Ghi chu co the chua so dien thoai / thong tin y te -> khong vao nhat ky.
        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_CONTACT, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Đã gọi người thân - người thân sẽ kiểm tra");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void recordContactRequiresResult() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/contact").with(csrf())
                        .contentType("application/json").content("{\"note\":\"abc\"}"))
                .andExpect(status().isBadRequest());

        verify(triageAlertService, never()).recordContact(any(), any(), any(), any(), any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientCannotRecordContact() throws Exception {
        mockMvc.perform(post("/api/v1/cms/triage-alerts/10/contact").with(csrf())
                        .contentType("application/json").content("{\"result\":\"CALLED_EMERGENCY\"}"))
                .andExpect(status().isForbidden());
    }
}
