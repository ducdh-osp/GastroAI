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
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.RagSource;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest bo qua cac @Configuration thuong (khong tu nap SecurityConfig cua app) nen
// phai @Import thu cong - neu khong Spring Boot se dung security mac dinh (CSRF bat, user
// sinh ngau nhien) thay vi rule that (CSRF tat, permitAll/hasRole("PATIENT")) -> test se
// khong con phan anh dung hanh vi endpoint that su chay production.
@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    // SecurityConfig đăng ký JwtAuthenticationFilter cho mọi request (kể cả trong slice test
    // này) -> filter cần 3 bean này để khởi tạo được, dù test không có Authorization header
    // (đã đăng nhập sẵn qua @WithMockUser nên filter không thực sự dùng tới).
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(roles = "PATIENT")
    void sendMessageReturnsAssistantReplyMatchingFrontendContract() throws Exception {
        RagAnswer ragAnswer = new RagAnswer(
                "Ban nen theo doi trieu chung.",
                List.of(new RagSource("Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo.")),
                List.of("Trieu chung nay co nguy hiem khong?", "Khi nao nen di kham?"));
        when(chatService.ask(anyString())).thenReturn(new ChatAnswer(ragAnswer, false));

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
                .andExpect(jsonPath("$.emergency").value(false));
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void sendMessageRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void sendMessageStillReturnsRealAnswerWhenEmergencyDetected() throws Exception {
        // UC0034/035 - emergency=true KHONG duoc chan Gemini, chi la co bao them de FE hien thi
        // canh bao noi bat - benh nhan van phai nhan duoc cau tra loi that.
        RagAnswer ragAnswer = new RagAnswer("Ban nen den co so y te ngay.", List.of(), List.of());
        when(chatService.ask(anyString())).thenReturn(new ChatAnswer(ragAnswer, true));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Toi bi dau bung du doi qua\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Ban nen den co so y te ngay."))
                .andExpect(jsonPath("$.emergency").value(true));
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void sendMessageReportsQuotaExceededInsteadOfGenericServerError() throws Exception {
        // Xac nhan thuc te tu log: Gemini free tier tra 429 khi het 20 luot generateContent/ngay.
        when(chatService.ask(anyString())).thenThrow(
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
    @WithMockUser(roles = "PATIENT")
    void sendMessageReportsGenericAiFailureForOtherGeminiErrors() throws Exception {
        when(chatService.ask(anyString())).thenThrow(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                        HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/api/v1/chat/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Lam sao de giam dau bung?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
    }
}
