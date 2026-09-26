package vn.gastroai.be.api.chat;

import jakarta.validation.constraints.NotBlank;

/** UC0017 - body cua POST /api/v1/chat/messages. */
public record ChatMessageRequest(@NotBlank String content) {
}
