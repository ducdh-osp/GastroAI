package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.triage.TriagePatientResponse;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientTriageAlertController.class)
@Import(SecurityConfig.class)
class PatientTriageAlertControllerTest {

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

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void checkInSafeIsRecordedAndAudited() throws Exception {
        when(triageAlertService.respondToCheckIn(1L, 10L, TriagePatientResponse.SAFE))
                .thenReturn(new TriageAlertService.CheckInResult(10L, "SAFE", false));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"SAFE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertId").value(10))
                .andExpect(jsonPath("$.response").value("SAFE"))
                .andExpect(jsonPath("$.alreadyClosed").value(false));

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_PATIENT_RESPONSE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · An toàn");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void checkInNeedHelpPassesResponse() throws Exception {
        when(triageAlertService.respondToCheckIn(1L, 10L, TriagePatientResponse.NEED_HELP))
                .thenReturn(new TriageAlertService.CheckInResult(10L, "NEED_HELP", false));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"NEED_HELP\"}"))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.TRIAGE_ALERT_PATIENT_RESPONSE, "TRIAGE_ALERT", 10L,
                "Bệnh nhân #1 · Cần hỗ trợ");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void answeringAClosedAlertIsNotAudited() throws Exception {
        when(triageAlertService.respondToCheckIn(1L, 10L, TriagePatientResponse.SAFE))
                .thenReturn(new TriageAlertService.CheckInResult(10L, "SAFE", true));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"SAFE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyClosed").value(true));

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void otherPatientsAlertIsNotFound() throws Exception {
        when(triageAlertService.respondToCheckIn(1L, 99L, TriagePatientResponse.SAFE))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cảnh báo"));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/99/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"SAFE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void checkInRequiresResponse() throws Exception {
        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());

        verify(triageAlertService, never()).respondToCheckIn(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminCannotAnswerOnBehalfOfPatient() throws Exception {
        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"SAFE\"}"))
                .andExpect(status().isForbidden());

        verify(triageAlertService, never()).respondToCheckIn(any(), any(), any());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        // SecurityConfig hien tra 403 cho request chua dang nhap.
        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/check-in").with(csrf())
                        .contentType("application/json").content("{\"response\":\"SAFE\"}"))
                .andExpect(status().isForbidden());
    }
}
