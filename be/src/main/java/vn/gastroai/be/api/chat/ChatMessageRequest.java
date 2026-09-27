package vn.gastroai.be.api.chat;

import jakarta.validation.constraints.NotBlank;

/**
 * UC0017 - body cua POST /api/v1/chat/messages.
 * sessionId tuy chon: null = bat dau phien moi, co gia tri = tiep tuc phien hien tai.
 */
public record ChatMessageRequest(
        @NotBlank String content,
        Long sessionId
) {
}