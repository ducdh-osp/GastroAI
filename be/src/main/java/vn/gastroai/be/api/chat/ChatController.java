package vn.gastroai.be.api.chat;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;

import java.util.List;

/** REST API cho UC0017 - Benh nhan gui cau hoi, nhan cau tra loi tu Gemini/RAG. */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        ChatAnswer chatAnswer = chatService.ask(request.content());
        RagAnswer ragAnswer = chatAnswer.ragAnswer();
        List<ChatSourceResponse> sources = ragAnswer.sources().stream()
                .map(source -> new ChatSourceResponse(source.documentTitle(), source.snippet()))
                .toList();
        return ChatMessageResponse.assistantReply(
                ragAnswer.answer(), sources, ragAnswer.relatedQuestions(), chatAnswer.emergency());
    }
}
