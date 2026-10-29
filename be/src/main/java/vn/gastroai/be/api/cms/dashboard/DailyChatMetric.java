package vn.gastroai.be.api.cms.dashboard;

public record DailyChatMetric(
        String day,
        long totalMessages,
        long emergencyMessages,
        long patientMessages,
        long botMessages
) {}
