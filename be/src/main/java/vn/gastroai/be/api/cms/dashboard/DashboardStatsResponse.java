package vn.gastroai.be.api.cms.dashboard;

import java.util.List;

public record DashboardStatsResponse(
        DashboardSummaryResponse summary,
        List<DailyChatMetric> dailyChats,
        List<SymptomMetric> topSymptoms,
        List<TriageGroupMetric> triageGroups,
        List<TriageStatusMetric> triageStatuses,
        int days,
        String fromDate,
        String toDate
) {}
