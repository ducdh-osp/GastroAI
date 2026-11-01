package vn.gastroai.be.api.cms.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.knowledge.KnowledgeArticleAdminService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeArticleAdminController.class)
@Import(SecurityConfig.class)
class KnowledgeArticleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private KnowledgeArticleAdminService service;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private CmsArticleDetailResponse sampleDetail(Long id) {
        return sampleDetail(id, "Trào ngược dạ dày", true);
    }

    private CmsArticleDetailResponse sampleDetail(Long id, String title, boolean published) {
        return new CmsArticleDetailResponse(
                id,
                "trao-nguoc",
                title,
                "Tóm tắt",
                "Dạ dày",
                "Nội dung chi tiết",
                "Nguồn NIDDK",
                "https://example.com/gerd",
                LocalDate.now(),
                published,
                "BS. Nguyễn Văn A",
                Instant.now(),
                10,
                Instant.now(),
                Instant.now(),
                0L
        );
    }

    private CmsArticleRequest validRequest(Long version) {
        return new CmsArticleRequest(
                "Tiêu đề sửa", null, "Tóm tắt sửa", "Dạ dày",
                "Nội dung sửa", "Nguồn", "https://example.com/updated", null, false, null, 5, version
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/api/v1/cms/articles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listReturnsPagedArticlesForDoctor() throws Exception {
        CmsArticleSummaryResponse item = new CmsArticleSummaryResponse(
                1L, "trao-nguoc", "Trào ngược", "Tóm tắt", "Dạ dày", true, "BS. A", Instant.now(), 10, Instant.now(), 0L
        );
        when(service.list(eq("trao"), eq("Da day"), eq(true), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/cms/articles")
                        .param("q", "trao")
                        .param("category", "Da day")
                        .param("published", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Trào ngược"));

        verify(service).list("trao", "Da day", true, 0, 20);
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void getByIdReturnsArticleForAdmin() throws Exception {
        when(service.getById(5L)).thenReturn(sampleDetail(5L));

        mockMvc.perform(get("/api/v1/cms/articles/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Trào ngược dạ dày"));
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void createReturnsCreatedForDoctorAndIsAudited() throws Exception {
        CmsArticleRequest req = new CmsArticleRequest(
                "Tiêu đề bài viết", null, "Tóm tắt bài viết", "Dạ dày",
                "Nội dung bài viết", "Nguồn", "https://example.com/guide", null, false, null, 10, null
        );
        when(service.create(any(CmsArticleRequest.class))).thenReturn(sampleDetail(1L, "Tiêu đề bài viết", false));

        mockMvc.perform(post("/api/v1/cms/articles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).create(any(CmsArticleRequest.class));
        verify(auditLogService).record(AuditAction.CONTENT_CREATE, "ARTICLE", 1L, "«Tiêu đề bài viết» · bản nháp");
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void createRejectsBlankTitleWithBadRequest() throws Exception {
        CmsArticleRequest invalid = new CmsArticleRequest(
                "", null, "Tóm tắt", "Dạ dày",
                "Nội dung", "Nguồn", "https://example.com", null, false, null, 10, null
        );

        mockMvc.perform(post("/api/v1/cms/articles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void createRejectsInvalidSourceUrlWithBadRequest() throws Exception {
        CmsArticleRequest invalid = new CmsArticleRequest(
                "Tiêu đề", null, "Tóm tắt", "Dạ dày",
                "Nội dung", "Nguồn", "not-a-valid-url", null, false, null, 10, null
        );

        mockMvc.perform(post("/api/v1/cms/articles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void createRejectsNegativeSortOrderWithBadRequest() throws Exception {
        CmsArticleRequest invalid = new CmsArticleRequest(
                "Tiêu đề", null, "Tóm tắt", "Dạ dày",
                "Nội dung", "Nguồn", "https://example.com", null, false, null, -5, null
        );

        mockMvc.perform(post("/api/v1/cms/articles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void updateReturnsOkAndAuditsTitleChange() throws Exception {
        when(service.getById(1L)).thenReturn(sampleDetail(1L, "Tiêu đề cũ", false));
        when(service.update(eq(1L), any(CmsArticleRequest.class))).thenReturn(sampleDetail(1L, "Tiêu đề sửa", false));

        mockMvc.perform(put("/api/v1/cms/articles/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(0L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).update(eq(1L), any(CmsArticleRequest.class));
        verify(auditLogService).record(AuditAction.CONTENT_UPDATE, "ARTICLE", 1L,
                "Tiêu đề: «Tiêu đề cũ» → «Tiêu đề sửa»");
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void unpublishIsAuditedAsUnpublish() throws Exception {
        CmsArticlePublishRequest unpublishReq = new CmsArticlePublishRequest(false, null, 0L);
        when(service.setPublished(eq(1L), any(CmsArticlePublishRequest.class)))
                .thenReturn(sampleDetail(1L, "Trào ngược dạ dày", false));

        mockMvc.perform(patch("/api/v1/cms/articles/1/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unpublishReq)))
                .andExpect(status().isOk());

        verify(service).setPublished(eq(1L), any(CmsArticlePublishRequest.class));
        verify(auditLogService).record(AuditAction.CONTENT_UNPUBLISH, "ARTICLE", 1L, "«Trào ngược dạ dày»");
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void publishIsAuditedWithMedicalReviewer() throws Exception {
        CmsArticlePublishRequest publishReq = new CmsArticlePublishRequest(true, "BS. Nguyễn Văn A", 0L);
        when(service.setPublished(eq(1L), any(CmsArticlePublishRequest.class))).thenReturn(sampleDetail(1L));

        mockMvc.perform(patch("/api/v1/cms/articles/1/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishReq)))
                .andExpect(status().isOk());

        verify(auditLogService).record(AuditAction.CONTENT_PUBLISH, "ARTICLE", 1L,
                "«Trào ngược dạ dày» · người duyệt: BS. Nguyễn Văn A");
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteReturnsNoContentAndKeepsTitleInAudit() throws Exception {
        when(service.getById(10L)).thenReturn(sampleDetail(10L, "Viêm dạ dày", true));

        mockMvc.perform(delete("/api/v1/cms/articles/10").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(10L);
        verify(auditLogService).record(AuditAction.CONTENT_DELETE, "ARTICLE", 10L, "«Viêm dạ dày»");
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void deleteForbiddenForPatient() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/articles/10").with(csrf()))
                .andExpect(status().isForbidden());

        verify(service, never()).delete(any());
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void describeUpdateFlagsAutoUnpublish() {
        assertEquals("«Trào ngược dạ dày» · tự gỡ khỏi bệnh nhân (sửa nội dung chưa có người duyệt)",
                KnowledgeArticleAdminController.describeUpdate(
                        sampleDetail(1L, "Trào ngược dạ dày", true),
                        sampleDetail(1L, "Trào ngược dạ dày", false)));
        assertEquals("«Trào ngược dạ dày»",
                KnowledgeArticleAdminController.describeUpdate(
                        sampleDetail(1L, "Trào ngược dạ dày", true),
                        sampleDetail(1L, "Trào ngược dạ dày", true)));
    }
}