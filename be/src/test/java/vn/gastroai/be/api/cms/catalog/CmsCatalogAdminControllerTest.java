package vn.gastroai.be.api.cms.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.catalog.CmsCatalogAdminService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsCatalogAdminController.class)
@Import(SecurityConfig.class)
class CmsCatalogAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsCatalogAdminService service;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static CmsCatalogItemResponse item(String name, String triageCode, boolean active) {
        return new CmsCatalogItemResponse(4L, CatalogType.SYMPTOM, "DAU_BUNG", name, triageCode, 10, active);
    }

    private static final String VALID_BODY = """
            {"type":"SYMPTOM","code":"DAU_BUNG","name":"Đau bụng dữ dội",
             "triageCode":"DAU_BUNG_CAP_TINH","sortOrder":10,"active":true}
            """;

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listIsNotAudited() throws Exception {
        when(service.list(null, null, null)).thenReturn(List.of(item("Đau bụng", null, true)));

        mockMvc.perform(get("/api/v1/cms/catalogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DAU_BUNG"));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createIsAuditedWithTypeCodeAndName() throws Exception {
        when(service.create(any(CmsCatalogItemRequest.class))).thenReturn(item("Đau bụng dữ dội", "DAU_BUNG_CAP_TINH", true));

        mockMvc.perform(post("/api/v1/cms/catalogs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        verify(auditLogService).record(AuditAction.CONTENT_CREATE, "CATALOG", 4L,
                "[SYMPTOM] DAU_BUNG – Đau bụng dữ dội");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void invalidCreateIsRejectedAndNotAudited() throws Exception {
        mockMvc.perform(post("/api/v1/cms/catalogs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SYMPTOM\",\"code\":\"\",\"name\":\"x\"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void updateAuditsTriageCodeChange() throws Exception {
        when(service.getById(4L)).thenReturn(item("Đau bụng", null, true));
        when(service.update(eq(4L), any(CmsCatalogItemRequest.class)))
                .thenReturn(item("Đau bụng dữ dội", "DAU_BUNG_CAP_TINH", true));

        mockMvc.perform(put("/api/v1/cms/catalogs/4")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.CONTENT_UPDATE, "CATALOG", 4L,
                "[SYMPTOM] DAU_BUNG – Đau bụng dữ dội · Tên: «Đau bụng» → «Đau bụng dữ dội»; "
                        + "Mã Triage: null → DAU_BUNG_CAP_TINH");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void toggleActiveAuditsDirection() throws Exception {
        when(service.toggleActive(4L)).thenReturn(item("Đau bụng", null, false));

        mockMvc.perform(patch("/api/v1/cms/catalogs/4/toggle-active").with(csrf()))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.CONTENT_TOGGLE_ACTIVE, "CATALOG", 4L,
                "[SYMPTOM] DAU_BUNG – Đau bụng · bật → tắt");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteKeepsItemLabelInAudit() throws Exception {
        when(service.getById(4L)).thenReturn(item("Đau bụng", null, true));

        mockMvc.perform(delete("/api/v1/cms/catalogs/4").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(4L);
        verify(auditLogService).record(AuditAction.CONTENT_DELETE, "CATALOG", 4L,
                "[SYMPTOM] DAU_BUNG – Đau bụng");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientCannotModifyCatalogAndNothingIsAudited() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/catalogs/4").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/cms/catalogs/4/toggle-active").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void describeUpdateWithoutChangesOnlyShowsLabel() {
        assertEquals("[SYMPTOM] DAU_BUNG – Đau bụng",
                CmsCatalogAdminController.describeUpdate(item("Đau bụng", null, true), item("Đau bụng", null, true)));
    }
}