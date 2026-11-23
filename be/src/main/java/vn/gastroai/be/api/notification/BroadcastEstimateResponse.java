package vn.gastroai.be.api.notification;

public record BroadcastEstimateResponse(
        String targetGroup,
        int estimatedRecipients,
        int candidateCount
) {
}

