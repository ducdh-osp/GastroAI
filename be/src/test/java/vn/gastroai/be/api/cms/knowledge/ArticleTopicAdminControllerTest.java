package vn.gastroai.be.api.cms.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.knowledge.ArticleTopicService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleTopicAdminController.class)
@Import(SecurityConfig.class)
class ArticleTopicAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ArticleTopicService service;

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
    void listForbiddenForPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/article-topics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listReturnsTopicsForDoctor() throws Exception {
        when(service.list()).thenReturn(List.of(
                new ArticleTopicResponse(1L, "Dạ dày", 10, 5L, 0L)
        ));

        mockMvc.perform(get("/api/v1/cms/article-topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Dạ dày"))
                .andExpect(jsonPath("$[0].articleCount").value(5));

        verify(service).list();
        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "10", roles = "CONTENT_EDITOR")
    void listReturnsTopicsForContentEditor() throws Exception {
        when(service.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cms/article-topics"))
                .andExpect(status().isOk());

        verify(service).list();
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createReturnsCreatedForAdminAndIsAudited() throws Exception {
        ArticleTopicRequest request = new ArticleTopicRequest("Đại tràng", 20, null);
        when(service.create(any(ArticleTopicRequest.class)))
                .thenReturn(new ArticleTopicResponse(2L, "Đại tràng", 20, 0L, 0L));

        mockMvc.perform(post("/api/v1/cms/article-topics")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Đại tràng"));

        verify(service).create(any(ArticleTopicRequest.class));
        verify(auditLogService).record(AuditAction.CONTENT_CREATE, "ARTICLE_TOPIC", 2L, "«Đại tràng»");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createRejectsBlankNameWithBadRequest() throws Exception {
        ArticleTopicRequest request = new ArticleTopicRequest("   ", 20, null);

        mockMvc.perform(post("/api/v1/cms/article-topics")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auditLogService);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createRejectsNegativeSortOrderWithBadRequest() throws Exception {
        ArticleTopicRequest request = new ArticleTopicRequest("Đại tràng", -10, null);

        mockMvc.perform(post("/api/v1/cms/article-topics")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void updateReturnsOkAndAuditsNameAndOrderChange() throws Exception {
        ArticleTopicRequest request = new ArticleTopicRequest("Đại tràng & trực tràng", 25, 0L);
        when(service.list()).thenReturn(List.of(new ArticleTopicResponse(2L, "Đại tràng", 20, 3L, 0L)));
        when(service.update(eq(2L), any(ArticleTopicRequest.class)))
                .thenReturn(new ArticleTopicResponse(2L, "Đại tràng & trực tràng", 25, 3L, 1L));

        mockMvc.perform(put("/api/v1/cms/article-topics/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Đại tràng & trực tràng"));

        verify(service).update(eq(2L), any(ArticleTopicRequest.class));
        verify(auditLogService).record(AuditAction.CONTENT_UPDATE, "ARTICLE_TOPIC", 2L,
                "Tên: «Đại tràng» → «Đại tràng & trực tràng»; Thứ tự: 20 → 25");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteReturnsNoContentAndKeepsNameInAudit() throws Exception {
        when(service.list()).thenReturn(List.of(new ArticleTopicResponse(3L, "Gan mật", 30, 0L, 0L)));

        mockMvc.perform(delete("/api/v1/cms/article-topics/3").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(3L);
        verify(auditLogService).record(AuditAction.CONTENT_DELETE, "ARTICLE_TOPIC", 3L, "«Gan mật»");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void deleteForbiddenForPatient() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/article-topics/3").with(csrf()))
                .andExpect(status().isForbidden());

        verify(service, never()).delete(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void describeUpdateOnlyListsChangedFields() {
        ArticleTopicResponse before = new ArticleTopicResponse(2L, "Dạ dày", 10, 0L, 0L);

        assertEquals("Thứ tự: 10 → 5", ArticleTopicAdminController.describeUpdate(before,
                new ArticleTopicResponse(2L, "Dạ dày", 5, 0L, 1L)));
        assertEquals("«Dạ dày»", ArticleTopicAdminController.describeUpdate(before,
                new ArticleTopicResponse(2L, "Dạ dày", 10, 0L, 1L)));
    }
}