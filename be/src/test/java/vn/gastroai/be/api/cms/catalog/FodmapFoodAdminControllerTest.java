package vn.gastroai.be.api.cms.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.catalog.CmsFodmapAdminService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.catalog.FodmapLevel;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

@WebMvcTest(FodmapFoodAdminController.class)
@Import(SecurityConfig.class)
class FodmapFoodAdminControllerTest {

    private static final String URL = "/api/v1/cms/fodmap-foods";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsFodmapAdminService service;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static final String VALID_BODY = """
            {"name":"Rau muống","aliases":["water spinach"],"level":"LOW","groups":[],
             "servingNote":null,"note":null,"source":"Monash University FODMAP Diet App"}
            """;

    private static FodmapFoodResponse food(Long id, String name, FodmapLevel level,
                                           List<String> groups, boolean active) {
        return new FodmapFoodResponse(id, name, List.of(), level, level.getLabel(), groups,
                null, null, "Monash", active, Instant.parse("2026-11-04T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listPassesFiltersAndIsNotAudited() throws Exception {
        when(service.list("hanh", FodmapLevel.HIGH, true))
                .thenReturn(List.of(food(1L, "Hành tây", FodmapLevel.HIGH, List.of("FRUCTAN"), true)));

        mockMvc.perform(get(URL).param("q", "hanh").param("level", "HIGH").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hành tây"))
                .andExpect(jsonPath("$[0].levelLabel").value("Cao"))
                .andExpect(jsonPath("$[0].groups[0]").value("FRUCTAN"));

        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void unknownNamesRouteIsNotTreatedAsId() throws Exception {
        when(service.listUnknown()).thenReturn(List.of(new FodmapUnknownNameResponse(
                5L, "Rau muống", 3, Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-04T00:00:00Z"))));

        mockMvc.perform(get(URL + "/unknown-names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rau muống"))
                .andExpect(jsonPath("$[0].hitCount").value(3));

        verify(service, never()).getById(anyLong());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createIsAudited() throws Exception {
        when(service.create(any(FodmapFoodRequest.class)))
                .thenReturn(food(99L, "Rau muống", FodmapLevel.LOW, List.of(), true));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));

        verify(auditLogService).record(eq(AuditAction.CONTENT_CREATE), eq("FODMAP_FOOD"), eq(99L),
                eq("«Rau muống» · Thấp"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createWithoutSourceIs400AndNotAudited() throws Exception {
        String body = """
                {"name":"Rau muống","level":"LOW","source":"  "}
                """;

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void duplicateNameIs409AndNotAudited() throws Exception {
        when(service.create(any(FodmapFoodRequest.class)))
                .thenThrow(new IllegalStateException("Tên «Rau muống» đã được dùng cho món «Rau muống»"));

        mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());

        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void updateAuditsChanges() throws Exception {
        when(service.getById(1L)).thenReturn(food(1L, "Hành tây", FodmapLevel.LOW, List.of(), true));
        when(service.update(eq(1L), any(FodmapFoodRequest.class)))
                .thenReturn(food(1L, "Hành tây", FodmapLevel.HIGH, List.of("FRUCTAN"), true));

        mockMvc.perform(put(URL + "/1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        verify(auditLogService).record(eq(AuditAction.CONTENT_UPDATE), eq("FODMAP_FOOD"), eq(1L),
                eq("«Hành tây» · Cao · Mức: Thấp → Cao; Nhóm: (trống) → FRUCTAN"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void toggleActiveIsAudited() throws Exception {
        when(service.toggleActive(1L)).thenReturn(food(1L, "Hành tây", FodmapLevel.HIGH, List.of(), false));

        mockMvc.perform(patch(URL + "/1/toggle-active").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(auditLogService).record(eq(AuditAction.CONTENT_TOGGLE_ACTIVE), eq("FODMAP_FOOD"), eq(1L),
                eq("«Hành tây» · Cao · bật → tắt"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteReadsBeforeAndIsAudited() throws Exception {
        when(service.getById(2L)).thenReturn(food(2L, "Thịt bò", FodmapLevel.LOW, List.of(), true));

        mockMvc.perform(delete(URL + "/2").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(2L);
        verify(auditLogService).record(eq(AuditAction.CONTENT_DELETE), eq("FODMAP_FOOD"), eq(2L),
                eq("«Thịt bò» · Thấp"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void dismissUnknownIsAuditedWithOwnTarget() throws Exception {
        when(service.dismissUnknown(5L)).thenReturn(new FodmapUnknownNameResponse(
                5L, "Quế", 3, Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-11-04T00:00:00Z")));

        mockMvc.perform(patch(URL + "/unknown-names/5/dismiss").with(csrf()))
                .andExpect(status().isOk());

        verify(auditLogService).record(eq(AuditAction.CONTENT_UPDATE), eq("FODMAP_UNKNOWN"), eq(5L),
                eq("«Quế» · bỏ qua (gặp 3 lần)"));
    }

    @Test
    @WithMockUser(username = "9", roles = "CONTENT_EDITOR")
    void contentEditorIsForbidden() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, auditLogService);
    }

    @Test
    @WithMockUser(username = "3", roles = "PATIENT")
    void patientIsForbidden() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service, auditLogService);
    }

    @Test
    void describeUpdateWithoutChangesIsLabelOnly() {
        FodmapFoodResponse same = food(1L, "Hành tây", FodmapLevel.HIGH, List.of("FRUCTAN"), true);

        assertEquals("«Hành tây» · Cao", FodmapFoodAdminController.describeUpdate(same, same));
    }
}