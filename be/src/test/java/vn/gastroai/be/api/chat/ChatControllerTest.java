package vn.gastroai.be.api.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.RagSource;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

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

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void sendMessageReturnsAssistantReplyMatchingFrontendContract() throws Exception {
        RagAnswer ragAnswer = new RagAnswer(
                "Ban nen theo doi trieu chung.",
                List.of(new RagSource("Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo.",null)),
                List.of("Trieu chung nay co nguy hiem khong?", "Khi nao nen di kham?"));
        stubChatServiceAsk(new ChatAnswer(ragAnswer, false, List.of()), TriageResult.safe());
        when(chatHistoryService.saveExchange(anyLong(), any(), anyString(), any(), anyBoolean(), any()))
                .thenReturn(new ChatHistoryService.SavedExchange(1L, 1L));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Lam sao de giam dau bung?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("assistant"))
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.content").value("Ban nen theo doi trieu chung."))
                .andExpect(jsonPath("$.sources[0].documentTitle").value("Cam nang tieu hoa"))
                .andExpect(jsonPath("$.sources[0].snippet").value("Uong nhieu nuoc va an nhieu chat xo."))
                .andExpect(jsonPath("$.relatedQuestions[0]").value("Trieu chung nay co nguy hiem khong?"))
                .andExpect(jsonPath("$.relatedQuestions[1]").value("Khi nao nen di kham?"))
                .andExpect(jsonPath("$.emergency").value(false))
                .andExpect(jsonPath("$.dbMessageId").value(1))
                .andExpect(jsonPath("$.sessionId").value(1));

        // UC0036 - khong co dau hieu khan cap thi KHONG duoc tao/gan canh bao nao ca.
        verify(triageAlertService, never()).createAndPublish(any(), any(), any(), any(), any());
        verify(triageAlertService, never()).linkConversation(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void sendMessageRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void sendMessageStillReturnsRealAnswerWhenEmergencyDetected() throws Exception {
        RagAnswer ragAnswer = new RagAnswer("Ban nen den co so y te ngay.", List.of(), List.of());
        stubChatServiceAsk(
                new ChatAnswer(ragAnswer, true, List.of("DAU_BUNG_CAP_TINH")),
                new TriageResult(true, List.of("DAU_BUNG_CAP_TINH")));
        when(triageAlertService.createAndPublish(
                eq(1L), isNull(), isNull(),
                eq("Toi bi dau bung du doi qua"), eq(List.of("DAU_BUNG_CAP_TINH"))))
                .thenReturn(42L);
        when(chatHistoryService.saveExchange(anyLong(), any(), anyString(), any(), anyBoolean(), any()))
                .thenReturn(new ChatHistoryService.SavedExchange(1L, 1L));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Toi bi dau bung du doi qua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Ban nen den co so y te ngay."))
                .andExpect(jsonPath("$.emergency").value(true))
                .andExpect(jsonPath("$.matchedGroups[0]").value("DAU_BUNG_CAP_TINH"));

        verify(triageAlertService).createAndPublish(
                eq(1L), isNull(), isNull(),
                eq("Toi bi dau bung du doi qua"), eq(List.of("DAU_BUNG_CAP_TINH")));
        verify(triageAlertService).linkConversation(eq(42L), eq(1L), eq(1L));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void sendMessageReportsQuotaExceededInsteadOfGenericServerError() throws Exception {
        when(chatService.ask(anyString(), any())).thenThrow(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                        HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Lam sao de giam dau bung?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_QUOTA_EXCEEDED"));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void sendMessageReportsGenericAiFailureForOtherGeminiErrors() throws Exception {
        when(chatService.ask(anyString(), any())).thenThrow(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Lam sao de giam dau bung?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
    }

 
    private void stubChatServiceAsk(ChatAnswer chatAnswer, TriageResult triageResult) {
        when(chatService.ask(anyString(), any())).thenAnswer(invocation -> {
            Consumer<TriageResult> onTriageChecked = invocation.getArgument(1);
            onTriageChecked.accept(triageResult);
            return chatAnswer;
        });
    }
}