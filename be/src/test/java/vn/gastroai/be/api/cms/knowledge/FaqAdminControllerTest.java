package vn.gastroai.be.api.cms.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.knowledge.FaqAdminService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.knowledge.Faq;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(FaqAdminController.class)
@Import(SecurityConfig.class)
class FaqAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private FaqAdminService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private static CmsFaqResponse response(Long id, boolean published, String reviewer, Long version) {
        return new CmsFaqResponse(id, "Câu hỏi", "Câu trả lời", "Dạ dày", 10, published,
                reviewer, reviewer != null ? Instant.now() : null, Instant.now(), Instant.now(), version);
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listForbiddenForPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/faqs"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void listRejectedWithoutLogin() throws Exception {
        // SecurityConfig hiện không cấu hình AuthenticationEntryPoint riêng nên request ẩn danh nhận 403
        mockMvc.perform(get("/api/v1/cms/faqs"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listReturnsPageForDoctor() throws Exception {
        when(service.list(isNull(), isNull(), eq(true), eq(0), eq(20)))
                .thenReturn(new CmsFaqPageResponse(List.of(response(1L, true, "BS. CKII Trần B", 3L)), 1, 0, 20));

        mockMvc.perform(get("/api/v1/cms/faqs").param("published", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].medicalReviewer").value("BS. CKII Trần B"))
                .andExpect(jsonPath("$.items[0].version").value(3));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createReturnsCreatedForAdmin() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời", "Dạ dày", 10, false, null, null);
        when(service.create(any(CmsFaqRequest.class))).thenReturn(response(2L, false, null, 0L));

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void createForbiddenForPatient() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời", "Dạ dày", 10, false);

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createRejectsBlankQuestionWithBadRequest() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("   ", "Câu trả lời", "Dạ dày", 10, false);

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createRejectsTooLongQuestionWithBadRequest() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("a".repeat(501), "Câu trả lời", "Dạ dày", 10, false);

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createRejectsNegativeSortOrderWithBadRequest() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời", "Dạ dày", -1, false);

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void createPublishedWithoutReviewerReturnsBadRequest() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời", "Dạ dày", 10, true);
        when(service.create(any(CmsFaqRequest.class)))
                .thenThrow(new IllegalArgumentException("FAQ muốn xuất bản bắt buộc phải có bác sĩ kiểm duyệt"));

        mockMvc.perform(post("/api/v1/cms/faqs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void updateReturnsOkWithValidPayload() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời mới", "Dạ dày", 10, false, null, 2L);
        when(service.update(eq(5L), any(CmsFaqRequest.class))).thenReturn(response(5L, false, null, 3L));

        mockMvc.perform(put("/api/v1/cms/faqs/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void updateWithStaleVersionReturnsConflict() throws Exception {
        CmsFaqRequest request = new CmsFaqRequest("Câu hỏi", "Câu trả lời", "Dạ dày", 10, false, null, 1L);
        when(service.update(eq(5L), any(CmsFaqRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Faq.class, 5L));

        mockMvc.perform(put("/api/v1/cms/faqs/5")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void publishRejectsMissingPublishedFlagWithBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/cms/faqs/5/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalReviewer\":\"BS. A\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).setPublished(any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void publishRejectsTooLongReviewerWithBadRequest() throws Exception {
        CmsFaqPublishRequest request = new CmsFaqPublishRequest(true, "B".repeat(201), 0L);

        mockMvc.perform(patch("/api/v1/cms/faqs/5/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        verify(service, never()).setPublished(any(), any());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void publishWithReviewerReturnsOk() throws Exception {
        CmsFaqPublishRequest request = new CmsFaqPublishRequest(true, "BS. CKII Trần B", 0L);
        when(service.setPublished(eq(5L), any(CmsFaqPublishRequest.class)))
                .thenReturn(response(5L, true, "BS. CKII Trần B", 1L));

        mockMvc.perform(patch("/api/v1/cms/faqs/5/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.medicalReviewer").value("BS. CKII Trần B"));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void publishForbiddenForPatient() throws Exception {
        mockMvc.perform(patch("/api/v1/cms/faqs/5/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true,\"medicalReviewer\":\"BS. A\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void deleteReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/faqs/3").with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(3L);
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void deleteForbiddenForPatient() throws Exception {
        mockMvc.perform(delete("/api/v1/cms/faqs/3").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "8", roles = "DOCTOR")
    void listRejectsInvalidPageSizeWithBadRequest() throws Exception {
        when(service.list(any(), any(), any(), anyInt(), eq(500)))
                .thenThrow(new IllegalArgumentException("page >= 0 va size trong khoang 1..100"));

        mockMvc.perform(get("/api/v1/cms/faqs").param("size", "500"))
                .andExpect(status().isBadRequest());
    }
}
