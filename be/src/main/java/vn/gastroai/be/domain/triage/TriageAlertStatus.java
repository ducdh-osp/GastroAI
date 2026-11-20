package vn.gastroai.be.domain.triage;

/**
 * SCR-AD-13 (AI-first) - 3 trạng thái rõ ràng, Admin chỉ cần quan tâm NEEDS_INTERVENTION.
 */
public enum TriageAlertStatus {
    /** Hệ thống đang tự xử lý: đã hỏi "Bạn có ổn không?", đang chờ bệnh nhân trả lời. */
    AUTO_HANDLING,
    /** Cần can thiệp: bệnh nhân im lặng quá mốc hoặc bấm [Cần hỗ trợ] - báo tới mọi Admin đang trực. */
    NEEDS_INTERVENTION,
    /** Đã xử lý (xem outcome để biết kết quả). */
    RESOLVED
}
