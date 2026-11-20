package vn.gastroai.be.application.triage;

import java.time.Instant;
import java.util.List;


public record TriageAlertResponse(
        Long id,
        Long patientId,
        String patientFullName,
        String patientPhone,
        Long sessionId,
        Long messageId,
        String messageContent,
        List<String> matchedGroups,
        String status,
        Long claimedById,
        String claimedByType,
        Instant claimedAt,
        Instant resolvedAt,
        Long resolvedById,
        String resolvedByType,
        Instant occurredAt,
        String outcome,
        String resolutionNote,
        int repeatCount,
        String lastMessageContent,
        Instant lastRepeatedAt,
        long unreadPatientMessages,
        long staffMessageCount,
        long patientFalseAlarmCount,
        /** Luc chuyen "Can can thiep" - FE dem nguoc failsafe tu moc nay. */
        Instant needsInterventionAt,
        /** Admin dau tien ra tay; co gia tri thi failsafe khong chay. */
        Instant acknowledgedAt,
        /** Failsafe da chay (qua han ma khong Admin nao xu ly). */
        Instant failsafeAt,
        /** SAFE / NEED_HELP - cau tra loi cua benh nhan cho "Ban co on khong?". */
        String patientResponse,
        Instant patientRespondedAt) {
}