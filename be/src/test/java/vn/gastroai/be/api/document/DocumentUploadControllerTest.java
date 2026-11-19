package vn.gastroai.be.api.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.document.DocumentStatusService;
import vn.gastroai.be.application.document.DocumentUploadService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.rag.Document;
import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import java.util.Optional;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.domain.audit.AuditAction;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentUploadController.class)
@Import(SecurityConfig.class)
class DocumentUploadControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DocumentUploadService documentUploadService;

        @MockitoBean
        private DocumentStatusService documentStatusService;

        @MockitoBean
        private AuditLogService auditLogService;

        @MockitoBean
        private vn.gastroai.be.application.document.DocumentReviewService documentReviewService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private PatientRepository patientRepository;

        @MockitoBean
        private RevokedTokenRepository revokedTokenRepository;

        @Test
        @WithMockUser(roles = "ADMIN")
        void uploadResponseIncludesInitialStatusAndStage() throws Exception {
                when(documentUploadService.upload(any(), any(), any()))
                                .thenReturn(document(DocumentStatus.PENDING, DocumentProcessingStage.PENDING));
                MockMultipartFile file = new MockMultipartFile(
                                "file", "ibs.pdf", "application/pdf", new byte[] { 1 });

                mockMvc.perform(multipart("/api/v1/documents").file(file))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.documentId").value(42))
                                .andExpect(jsonPath("$.status").value("PENDING"))
                                .andExpect(jsonPath("$.processingStage").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getStatusReturnsLifecycleStageAndErrorDetails() throws Exception {
                Document document = document(DocumentStatus.ERROR, DocumentProcessingStage.ERROR);
                document.setErrorMessage("Embedding failed");
                when(documentStatusService.findDocument(42L)).thenReturn(Optional.of(document));

                mockMvc.perform(get("/api/v1/documents/42"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.documentId").value(42))
                                .andExpect(jsonPath("$.title").value("IBS guide"))
                                .andExpect(jsonPath("$.status").value("ERROR"))
                                .andExpect(jsonPath("$.processingStage").value("ERROR"))
                                .andExpect(jsonPath("$.errorMessage").value("Embedding failed"));
        }

        @Test
        @WithMockUser(roles = "CONTENT_EDITOR")
        void contentEditorsCanReadDocumentStatus() throws Exception {
                when(documentStatusService.findDocument(42L))
                                .thenReturn(Optional.of(
                                                document(DocumentStatus.PROCESSING, DocumentProcessingStage.CHUNKING)));

                mockMvc.perform(get("/api/v1/documents/42"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PROCESSING"))
                                .andExpect(jsonPath("$.processingStage").value("CHUNKING"));
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        void patientsCannotReadDocumentStatus() throws Exception {
                mockMvc.perform(get("/api/v1/documents/42"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void unknownDocumentReturnsNotFound() throws Exception {
                when(documentStatusService.findDocument(999L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/documents/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void listReturnsPagedDocuments() throws Exception {
                DocumentSummaryResponse docSummary = new DocumentSummaryResponse(
                                42L, "IBS guide", "/path", "https://example.com",
                                DocumentStatus.DONE, DocumentProcessingStage.DONE,
                                null, 10L, java.time.Instant.now(), java.time.Instant.now(),
                                                               1, 1, 1, false, true);
                when(documentStatusService.list(null, null, null, null, 0, 15))
                                .thenReturn(new DocumentPageResponse(java.util.List.of(docSummary), 1, 0, 15,
                                                new DocumentStats(1, 1, 0, 0)));

                mockMvc.perform(get("/api/v1/documents"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(1))
                                .andExpect(jsonPath("$.items[0].id").value(42))
                                .andExpect(jsonPath("$.items[0].chunkCount").value(10));
        }

        @Test
        @WithMockUser(roles = "CONTENT_EDITOR")
        void retryDelegatesToStatusService() throws Exception {
                Document retried = document(DocumentStatus.PENDING, DocumentProcessingStage.PENDING);
                when(documentStatusService.retry(42L)).thenReturn(retried);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/documents/42/retry"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void searchContentAllowedForAdmin() throws Exception {
                mockMvc.perform(get("/api/v1/documents/search").param("q", "HP"))
                                .andExpect(status().isOk());
        }

        // Benh nhan khong duoc vao CMS -> SecurityConfig chan o tang quyen, truoc khi
        // toi controller.
        @Test
        @WithMockUser(roles = "PATIENT")
        void searchContentForbiddenForPatient() throws Exception {
                mockMvc.perform(get("/api/v1/documents/search").param("q", "HP"))
                                .andExpect(status().isForbidden());
        }

        // Vai tro moi (merge tu dev): bien tap vien noi dung cung duoc tim tai lieu.
        @Test
        @WithMockUser(roles = "CONTENT_EDITOR")
        void searchContentAllowedForContentEditor() throws Exception {
                mockMvc.perform(get("/api/v1/documents/search").param("q", "HP"))
                                .andExpect(status().isOk());
        }

        private Document document(DocumentStatus status, DocumentProcessingStage stage) {
                Document document = new Document();
                document.setId(42L);
                document.setTitle("IBS guide");
                document.setStatus(status);
                document.setProcessingStage(stage);
                return document;
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void uploadVersionAcceptedForAdmin() throws Exception {
                when(documentUploadService.uploadNewVersion(eq(42L), any()))
                                .thenReturn(document(DocumentStatus.PENDING, DocumentProcessingStage.PENDING));
                MockMultipartFile file = new MockMultipartFile(
                                "file", "ibs-v2.pdf", "application/pdf", new byte[] { 1 });

                mockMvc.perform(multipart("/api/v1/documents/42/versions").file(file))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.documentId").value(42))
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void listVersionsOkForAdmin() throws Exception {
                when(documentStatusService.listVersions(42L)).thenReturn(java.util.List.of());

                mockMvc.perform(get("/api/v1/documents/42/versions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void activateVersionReturnsNoContentForAdmin() throws Exception {
                mockMvc.perform(post("/api/v1/documents/42/versions/7/activate"))
                                .andExpect(status().isNoContent());

                verify(documentStatusService).activateVersion(42L, 7L);
        }

        // SCR-AD-04 §3.1b - chi Bien tap duoc xac nhan noi dung, ghi lai ai xac nhan.
        @Test
        @WithMockUser(roles = "CONTENT_EDITOR")
        void contentEditorCanApproveDocumentContent() throws Exception {
                when(auditLogService.currentActorEmail()).thenReturn("bientap@gastroai.vn");

                mockMvc.perform(post("/api/v1/documents/42/versions/7/approve"))
                                .andExpect(status().isNoContent());

                verify(documentStatusService).approveContent(42L, 7L, "bientap@gastroai.vn");
                verify(auditLogService).record(eq(AuditAction.DOCUMENT_APPROVE_CONTENT), eq("DOCUMENT"), eq(42L), any());
        }

        // Admin tai len va xu ly ky thuat, nhung khong tu duyet noi dung y khoa.
        @Test
        @WithMockUser(roles = "CONTENT_EDITOR")
        void contentEditorCanOpenReviewOfAVersion() throws Exception {
                when(documentReviewService.review(42L)).thenReturn(new DocumentReviewResponse(
                                42L, "Phác đồ HP", 2, DocumentStatus.DONE, false, false, null, null,
                                "PDF", null, java.util.List.of(new DocumentReviewResponse.Chunk(0, "Đoạn 1"))));

                mockMvc.perform(get("/api/v1/documents/42/review"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.originalFileType").value("PDF"))
                                .andExpect(jsonPath("$.chunks[0].content").value("Đoạn 1"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void publishReturnsNoContentForAdmin() throws Exception {
                mockMvc.perform(post("/api/v1/documents/42/publish"))
                                .andExpect(status().isNoContent());

                verify(documentStatusService).publish(42L);
                verify(auditLogService).record(AuditAction.DOCUMENT_PUBLISH, "DOCUMENT", 42L, null);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void unpublishReturnsNoContentForAdmin() throws Exception {
                mockMvc.perform(post("/api/v1/documents/42/unpublish"))
                                .andExpect(status().isNoContent());

                verify(documentStatusService).unpublish(42L);
                verify(auditLogService).record(AuditAction.DOCUMENT_UNPUBLISH, "DOCUMENT", 42L, null);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteVersionReturnsNoContentForAdmin() throws Exception {
                mockMvc.perform(delete("/api/v1/documents/42/versions/7"))
                                .andExpect(status().isNoContent());

                verify(documentUploadService).deleteVersion(42L, 7L);
                verify(auditLogService).record(AuditAction.DOCUMENT_DELETE_VERSION, "DOCUMENT", 42L,
                                "Xoá phiên bản id=7");
        }

        // Benh nhan khong duoc go tai lieu: SecurityConfig chan o tang quyen, service
        // khong bi goi.
        @Test
        @WithMockUser(roles = "PATIENT")
        void unpublishForbiddenForPatient() throws Exception {
                mockMvc.perform(post("/api/v1/documents/42/unpublish"))
                                .andExpect(status().isForbidden());

                verify(documentStatusService, never()).unpublish(any());
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        void deleteVersionForbiddenForPatient() throws Exception {
                mockMvc.perform(delete("/api/v1/documents/42/versions/7"))
                                .andExpect(status().isForbidden());

                verify(documentUploadService, never()).deleteVersion(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateInfoReturnsNoContentForAdmin() throws Exception {
                mockMvc.perform(patch("/api/v1/documents/42")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Trào ngược dạ dày\",\"sourceUrl\":\"https://moh.gov.vn/gerd\"}"))
                                .andExpect(status().isNoContent());
                verify(documentStatusService).updateInfo(42L, "Trào ngược dạ dày", "https://moh.gov.vn/gerd");
                verify(auditLogService).record(AuditAction.DOCUMENT_UPDATE_INFO, "DOCUMENT", 42L,
                                "Tên: Trào ngược dạ dày | URL: https://moh.gov.vn/gerd");
        }

        @Test
        @WithMockUser(roles = "PATIENT")
        void updateInfoForbiddenForPatient() throws Exception {
                mockMvc.perform(patch("/api/v1/documents/42")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"x\",\"sourceUrl\":null}"))
                                .andExpect(status().isForbidden());
                verify(documentStatusService, never()).updateInfo(any(), any(), any());
                verify(auditLogService, never()).record(any(), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteDocumentIsAudited() throws Exception {
                mockMvc.perform(delete("/api/v1/documents/42"))
                                .andExpect(status().isNoContent());

                verify(documentUploadService).delete(42L);
                verify(auditLogService).record(AuditAction.DOCUMENT_DELETE, "DOCUMENT", 42L, null);
        }

}
