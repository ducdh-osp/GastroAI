package vn.gastroai.be.api.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body của POST /api/v1/chat/messages/{id}/rating. */
public record RateMessageRequest(
        @NotBlank
        @Pattern(regexp = "HELPFUL|UNHELPFUL", message = "rating phải là HELPFUL hoặc UNHELPFUL")
        String rating
) {}
