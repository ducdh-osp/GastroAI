package vn.gastroai.be.api.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastAnnouncementRequest(
        @NotBlank(message = "Tieu de khong duoc de trong")
        @Size(max = 160, message = "Tieu de khong duoc vuot qua 160 ky tu")
        String title,

        @NotBlank(message = "Noi dung khong duoc de trong")
        String message,

        String targetGroup,

        @Size(max = 255, message = "actionUrl khong duoc vuot qua 255 ky tu")
        String actionUrl,

        Boolean sendWebSocket
) {
}
