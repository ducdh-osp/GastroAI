package vn.gastroai.be.domain.triage;

/** Ket qua khi canh bao RESOLVED. */
public enum TriageAlertOutcome {
    /** Admin da lien he va dong ca. */
    HANDLED,
    FALSE_ALARM,
    /** Benh nhan tu bam [An toan] tren thong bao hoi tham. */
    PATIENT_SAFE
}
