package vn.gastroai.be.application.chat;

import org.springframework.stereotype.Service;
import vn.gastroai.be.application.rag.RagQueryService;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.TriageResult;
import java.util.function.Consumer;

/**
 * Tang application cho UC0017 (Chat voi AI) - boc RagQueryService (UC0028/029) de
 * ChatController khong goi thang sang tinh nang RAG cua nguoi khac. Tu UC0034/035, goi them
 * TriageService TRUOC khi goi RagQueryService (re, khong goi API ngoai) de gan co canh bao
 * khan cap vao cau tra loi - khong chan/bo qua Gemini, chi bao them cho FE biet.
 */
@Service
public class ChatService {
    private final RagQueryService ragQueryService;
    private final TriageService triageService;

    public ChatService(RagQueryService ragQueryService, TriageService triageService) {
        this.ragQueryService = ragQueryService;
        this.triageService = triageService;
    }

    public ChatAnswer ask(String question) {
        return ask(question, triageResult -> { });
    }

    public ChatAnswer ask(String question, Consumer<TriageResult> onTriageChecked) {
        TriageResult triageResult = triageService.check(question);
        onTriageChecked.accept(triageResult);
        return new ChatAnswer(ragQueryService.answerWithSources(question), triageResult.emergency(), triageResult.matchedGroups());
    }
}