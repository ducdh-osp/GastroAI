package vn.gastroai.be.api.notification;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record MedicationConfirmationRequest(@NotNull LocalTime scheduledTime) {
}
