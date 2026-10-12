package vn.gastroai.be.api.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.chat.ChatExportService;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.filestorage.ChatAttachmentStorage;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatAttachmentRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MessageRatingRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cung ly do @Import(SecurityConfig.class) nhu ChatControllerTest - @WebMvcTest khong tu nap
// SecurityConfig that cua app.
@WebMvcTest(ChatHistoryController.class)
@Import(SecurityConfig.class)
class ChatHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatHistoryService chatHistoryService;

    @MockitoBean
    private ChatExportService chatExportService;

    @MockitoBean
    private MessageRatingRepository ratingRepository;

    @MockitoBean
    private ChatAttachmentRepository attachmentRepository;

    @MockitoBean
    private ChatAttachmentStorage attachmentStorage;

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void exportPdfReturnsPdfWithAttachmentHeaderOnSuccess() throws Exception {
        byte[] fakePdf = "%PDF-fake".getBytes();
        when(chatExportService.exportSession(1L, 10L)).thenReturn(fakePdf);

        mockMvc.perform(get("/api/v1/chat/sessions/10/pdf").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("phien-chat-10-")))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void exportPdfReturnsNotFoundWhenSessionNotOwned() throws Exception {
        when(chatExportService.exportSession(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Khong tim thay phien chat hoac ban khong co quyen truy cap"));

        mockMvc.perform(get("/api/v1/chat/sessions/999/pdf").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void exportPdfForbiddenForAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/chat/sessions/10/pdf").with(csrf()))
                .andExpect(status().isForbidden());
    }
}