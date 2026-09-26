package vn.gastroai.be.api.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageResponseTest {

    @Test
    void assistantReplyBuildsMessageMatchingFrontendContract() {
        List<ChatSourceResponse> sources =
                List.of(new ChatSourceResponse("Cam nang tieu hoa", "Uong nhieu nuoc va an nhieu chat xo."));
        List<String> relatedQuestions = List.of("Trieu chung nay co nguy hiem khong?");

        ChatMessageResponse response = ChatMessageResponse.assistantReply("Xin chao", sources, relatedQuestions, false);

        assertNotNull(response.id());
        assertEquals("assistant", response.sender());
        assertEquals("Xin chao", response.content());
        assertNotNull(response.createdAt());
        assertEquals("sent", response.status());
        assertEquals(sources, response.sources());
        assertEquals(relatedQuestions, response.relatedQuestions());
        assertFalse(response.emergency());
    }

    @Test
    void assistantReplyCarriesEmergencyFlagWhenTrue() {
        ChatMessageResponse response = ChatMessageResponse.assistantReply("Ban nen den benh vien ngay.", List.of(), List.of(), true);

        assertTrue(response.emergency());
    }
}
