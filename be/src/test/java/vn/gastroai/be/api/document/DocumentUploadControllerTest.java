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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadResponseIncludesInitialStatusAndStage() throws Exception {
        when(documentUploadService.upload(any()))
                .thenReturn(document(DocumentStatus.PENDING, DocumentProcessingStage.PENDING));
        MockMultipartFile file = new MockMultipartFile(
                "file", "ibs.pdf", "application/pdf", new byte[]{1});

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
    @WithMockUser(roles = "DOCTOR")
    void doctorsCanReadDocumentStatus() throws Exception {
        when(documentStatusService.findDocument(42L))
                .thenReturn(Optional.of(document(DocumentStatus.PROCESSING, DocumentProcessingStage.CHUNKING)));

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

    private Document document(DocumentStatus status, DocumentProcessingStage stage) {
        Document document = new Document();
        document.setId(42L);
        document.setTitle("IBS guide");
        document.setStatus(status);
        document.setProcessingStage(stage);
        return document;
    }
}
