package vn.gastroai.be.api.cms.dashboard;

public record TriageGroupMetric(
        String groupCode,
        String groupName,
        long count,
        double percentage
) {}
