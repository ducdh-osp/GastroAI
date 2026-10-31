package vn.gastroai.be.api.cms.dashboard;

public record TriageStatusMetric(
        String status,
        String label,
        long count,
        double percentage
) {}
