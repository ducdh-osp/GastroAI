package vn.gastroai.be.application.chat;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.RagQueryService;
import vn.gastroai.be.application.rag.RagSource;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.TriageResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    @Test
    void askDelegatesToRagQueryServiceAndReturnsRagAnswerUnchanged() {
        RagQueryService ragQueryService = mock(RagQueryService.class);
        TriageService triageService = mock(TriageService.class);
        RagAnswer expectedRagAnswer = new RagAnswer(
                "Ban nen theo doi trieu chung.",
                List.of(new RagSource("Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo.")),
                List.of("Trieu chung nay co nguy hiem khong?"));
        when(triageService.check("Lam sao de giam dau bung?")).thenReturn(TriageResult.safe());
        when(ragQueryService.answerWithSources("Lam sao de giam dau bung?")).thenReturn(expectedRagAnswer);

        ChatService chatService = new ChatService(ragQueryService, triageService);

        ChatAnswer result = chatService.ask("Lam sao de giam dau bung?");

        assertEquals(expectedRagAnswer, result.ragAnswer());
        assertFalse(result.emergency());
    }

    @Test
    void askFlagsEmergencyButStillReturnsRealGeminiAnswer() {
        RagQueryService ragQueryService = mock(RagQueryService.class);
        TriageService triageService = mock(TriageService.class);
        RagAnswer expectedRagAnswer = new RagAnswer("Ban nen den benh vien ngay.", List.of(), List.of());
        // UC0034 - vi du y het TriageServiceTest: "dau bung du doi" khop nhom DAU_BUNG_CAP_TINH.
        when(triageService.check("Toi bi dau bung du doi qua, khong dung thang duoc"))
                .thenReturn(new TriageResult(true, List.of("DAU_BUNG_CAP_TINH")));
        when(ragQueryService.answerWithSources("Toi bi dau bung du doi qua, khong dung thang duoc"))
                .thenReturn(expectedRagAnswer);

        ChatService chatService = new ChatService(ragQueryService, triageService);

        ChatAnswer result = chatService.ask("Toi bi dau bung du doi qua, khong dung thang duoc");

        // Emergency=true KHONG duoc chan/thay the cau tra loi that cua Gemini - chi la co bao them.
        assertTrue(result.emergency());
        assertEquals(expectedRagAnswer, result.ragAnswer());
    }
}
