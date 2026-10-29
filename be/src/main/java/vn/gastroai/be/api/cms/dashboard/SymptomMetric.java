package vn.gastroai.be.api.cms.dashboard;

public record SymptomMetric(
        String symptomCode,
        String symptomName,
        long totalCount,
        long emergencyCount,
        double percentage
) {}
