package vn.gastroai.be.api.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.gastroai.be.application.chat.ChatAttachmentProcessor;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.StreamingRagQueryService;
import vn.gastroai.be.application.triage.TriageAlertService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.triage.TriageResult;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerStreamingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatHistoryService chatHistoryService;

    @MockitoBean
    private StreamingRagQueryService streamingRagQueryService;

    @MockitoBean
    private TriageAlertService triageAlertService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockitoBean
    private ChatAttachmentProcessor attachmentProcessor;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void streamMessagePublishesTriageAlertWhenEmergencyDetected() throws Exception {
        stubStreamAnswer(true, List.of("DAU_BUNG_CAP_TINH"));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/chat/messages/stream")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Toi bi dau bung du doi qua\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        // UC0036/067(vá) - ChatController gio goi TriageAlertService.createAndPublish() thay vi
        // tu dung TriageAlertPublisher - sessionId/messageId van la null (chua doi
        // saveExchange() chay xong).
        verify(triageAlertService, timeout(2000)).createAndPublish(
                eq(1L), isNull(), isNull(),
                eq("Toi bi dau bung du doi qua"), eq(List.of("DAU_BUNG_CAP_TINH")));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void streamMessageDoesNotPublishWhenNoEmergencyDetected() throws Exception {
        stubStreamAnswer(false, List.of());

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/chat/messages/stream")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Lam sao de giam dau bung?\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        verify(triageAlertService, never()).createAndPublish(any(), any(), any(), any(), any());
    }


    private void stubStreamAnswer(boolean emergency, List<String> matchedGroups) {
        TriageResult triageResult = new TriageResult(emergency, matchedGroups);
        when(streamingRagQueryService.streamAnswer(anyString(), any(), any())).thenAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(1);
            Consumer<TriageResult> onTriageChecked = invocation.getArgument(2);

            onTriageChecked.accept(triageResult);
            onToken.accept("Ban nen theo doi trieu chung.");

            return new StreamingRagQueryService.StreamingResult(
                    "Ban nen theo doi trieu chung.",
                    List.of(),
                    List.of(),
                    emergency,
                    matchedGroups);
        });
    }
}