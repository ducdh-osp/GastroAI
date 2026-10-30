package vn.gastroai.be.api.cms.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.triage.SymptomAssessmentHistoryService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SymptomAssessmentAdminController.class)
@Import(SecurityConfig.class)
class SymptomAssessmentAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SymptomAssessmentHistoryService historyService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminViewingPatientHistoryIsAudited() throws Exception {
        when(historyService.listForPatient(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cms/triage-assessments/patients/3"))
                .andExpect(status().isOk());

        verify(historyService).listForPatient(3L);
        verify(auditLogService).record(AuditAction.PATIENT_DATA_VIEW, "PATIENT", 3L,
                "Xem lịch sử đánh giá triệu chứng");
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void doctorViewingPatientHistoryIsAuditedToo() throws Exception {
        when(historyService.listForPatient(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cms/triage-assessments/patients/5"))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.PATIENT_DATA_VIEW, "PATIENT", 5L,
                "Xem lịch sử đánh giá triệu chứng");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void failedLookupIsNotAudited() throws Exception {
        when(historyService.listForPatient(999L))
                .thenThrow(new ResourceNotFoundException("Khong tim thay benh nhan"));

        mockMvc.perform(get("/api/v1/cms/triage-assessments/patients/999"))
                .andExpect(status().isNotFound());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "4", roles = "CONTENT_EDITOR")
    void contentEditorCannotViewPatientDataAndNothingIsAudited() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-assessments/patients/3"))
                .andExpect(status().isForbidden());

        verify(historyService, never()).listForPatient(any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientCannotUseStaffEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/cms/triage-assessments/patients/1"))
                .andExpect(status().isForbidden());

        verify(historyService, never()).listForPatient(any());
    }
}