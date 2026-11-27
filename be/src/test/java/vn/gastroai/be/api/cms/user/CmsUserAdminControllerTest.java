package vn.gastroai.be.api.cms.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.admin.AdminPatientManagementService;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsUserAdminController.class)
@Import(SecurityConfig.class)
class CmsUserAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminPatientManagementService service;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private JwtService jwt;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private RevokedTokenRepository revoked;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanLockPatientAndAuditNormalizedReason() throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  Khóa theo yêu cầu hỗ trợ của bệnh nhân  \"}"))
                .andExpect(status().isOk());
        verify(service).lock(7L);
        verify(auditLogService).record(AuditAction.PATIENT_LOCK, "PATIENT", 7L,
                "Khóa theo yêu cầu hỗ trợ của bệnh nhân");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reason\":null}", "{\"reason\":\"\"}", "{\"reason\":\"  \\t\\n  \"}"})
    @WithMockUser(roles = "ADMIN")
    void rejectsMissingOrBlankReasonBeforeLocking(String body) throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, auditLogService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMissingRequestBodyBeforeLocking() throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, auditLogService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsReasonExceedingAuditCapacity() throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, auditLogService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void acceptsReasonAtAuditCapacityWithoutTruncation() throws Exception {
        String reason = "a".repeat(1000);
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
        verify(auditLogService).record(AuditAction.PATIENT_LOCK, "PATIENT", 7L, reason);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void failedLockDoesNotProduceSuccessAudit() throws Exception {
        when(service.lock(7L)).thenThrow(new ResourceNotFoundException("Không tìm thấy bệnh nhân"));
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Yêu cầu hỗ trợ\"}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUnlockIsAudited() throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/unlock"))
                .andExpect(status().isOk());
        verify(service).unlock(7L);
        verify(auditLogService).record(eq(AuditAction.PATIENT_UNLOCK), eq("PATIENT"), eq(7L), isNull());
    }

    @Test
    @WithMockUser(roles = "CONTENT_EDITOR")
    void contentEditorCannotManagePatients() throws Exception {
        mockMvc.perform(get("/api/v1/cms/users")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/cms/users/7/lock")).andExpect(status().isForbidden());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotAccessAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/cms/users")).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_EDITOR", "CONTENT_AUTHOR", "OPERATIONS", "PATIENT"})
    void nonAdminsCannotLockOrUnlockEvenWithValidReason(String role) throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock")
                        .with(user("non-admin").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Yêu cầu hỗ trợ\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/cms/users/7/unlock").with(user("non-admin").roles(role)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service, auditLogService);
    }
}
