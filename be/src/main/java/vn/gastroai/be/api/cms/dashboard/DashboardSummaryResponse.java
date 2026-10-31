package vn.gastroai.be.api.cms.dashboard;

public record DashboardSummaryResponse(
        long totalPatients,
        long totalSessions,
        long totalMessages,
        long todayMessages,
        long totalAlerts,
        long newAlerts,
        long inProgressAlerts,
        long resolvedAlerts,
        long totalAssessments,
        long emergencyAssessments,
        long helpfulRatings,
        long unhelpfulRatings,
        double satisfactionRate
) {}
