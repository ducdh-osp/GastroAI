package vn.gastroai.be.api.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * UC0017 - body cua POST /api/v1/chat/messages.
 * sessionId tuy chon: null = bat dau phien moi, co gia tri = tiep tuc phien hien tai.
 */
public record ChatMessageRequest(
        @NotBlank @Size(max = 1000, message = "noi dung toi da 1000 ky tu") String content,
        Long sessionId
) {
}
