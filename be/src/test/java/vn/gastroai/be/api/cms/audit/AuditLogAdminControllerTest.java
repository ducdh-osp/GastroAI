package vn.gastroai.be.api.cms.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditAnomalyRuleService;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditAnomalyRule;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogAdminController.class)
@Import(SecurityConfig.class)
class AuditLogAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private AuditAnomalyRuleService ruleService;
    @MockitoBean private JwtService jwt;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private RevokedTokenRepository revoked;

    private AuditAnomalyRuleResponse sampleRule() {
        return new AuditAnomalyRuleResponse("MASS_PATIENT_VIEW", "Xem dữ liệu nhiều bệnh nhân",
                List.of(AuditAction.PATIENT_DATA_VIEW), AuditAnomalyRule.Metric.DISTINCT_TARGETS,
                20, 10, true, Instant.parse("2026-10-29T00:00:00Z"), null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanSearchLogsWithFilters() throws Exception {
        when(auditLogService.search(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new AuditLogPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/cms/audit-logs")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-30")
                        .param("action", "PATIENT_LOCK")
                        .param("q", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(auditLogService).search(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30),
                AuditAction.PATIENT_LOCK, null, "admin", 0, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidPagingOrActionReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cms/audit-logs").param("size", "500"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/cms/audit-logs").param("action", "KHONG_CO"))
                .andExpect(status().isBadRequest());

        verify(auditLogService, never()).search(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListRules() throws Exception {
        when(ruleService.list()).thenReturn(List.of(sampleRule()));

        mockMvc.perform(get("/api/v1/cms/audit-logs/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MASS_PATIENT_VIEW"))
                .andExpect(jsonPath("$[0].actions[0]").value("PATIENT_DATA_VIEW"))
                .andExpect(jsonPath("$[0].metric").value("DISTINCT_TARGETS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateRule() throws Exception {
        when(ruleService.update(eq("MASS_PATIENT_VIEW"), any())).thenReturn(sampleRule());

        mockMvc.perform(put("/api/v1/cms/audit-logs/rules/MASS_PATIENT_VIEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\":15,\"windowMinutes\":10,\"enabled\":true}"))
                .andExpect(status().isOk());

        verify(ruleService).update("MASS_PATIENT_VIEW", new UpdateAuditAnomalyRuleRequest(15, 10, true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRuleValuesReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/cms/audit-logs/rules/MASS_PATIENT_VIEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\":0,\"windowMinutes\":10,\"enabled\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/cms/audit-logs/rules/MASS_PATIENT_VIEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\":5,\"windowMinutes\":2000,\"enabled\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/cms/audit-logs/rules/MASS_PATIENT_VIEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\":5,\"windowMinutes\":10}"))
                .andExpect(status().isBadRequest());

        verify(ruleService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotReadLogsOrRules() throws Exception {
        mockMvc.perform(get("/api/v1/cms/audit-logs")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/cms/audit-logs/rules")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/cms/audit-logs/rules/MASS_PATIENT_VIEW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threshold\":1000,\"windowMinutes\":10,\"enabled\":false}"))
                .andExpect(status().isForbidden());

        verify(ruleService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "CONTENT_EDITOR")
    void contentEditorCannotReadLogs() throws Exception {
        mockMvc.perform(get("/api/v1/cms/audit-logs")).andExpect(status().isForbidden());
    }
}