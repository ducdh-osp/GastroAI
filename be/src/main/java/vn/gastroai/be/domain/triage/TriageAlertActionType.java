package vn.gastroai.be.domain.triage;

/** Cac moc tren timeline cua 1 canh bao (bang triage_alert_actions). */
public enum TriageAlertActionType {
    CREATED,
    /** He thong gui thong bao "Ban co on khong?" cho benh nhan. */
    CHECK_IN_SENT,
    PATIENT_SAFE,
    PATIENT_NEEDS_HELP,
    /** He thong chuyen "Can can thiep" vi benh nhan im lang qua moc. */
    ESCALATED,
    /** Failsafe: khong Admin nao xac nhan trong thoi han. */
    FAILSAFE,
    /** Admin ghi lai ket qua lien he (goi nguoi than / benh nhan / 115). */
    CONTACT_RECORDED,
    REPEATED,
    REPEATED_AFTER_FALSE_ALARM,
    CLAIMED,
    RELEASED,
    MESSAGE_SENT,
    PATIENT_REPLIED,
    PATIENT_READ,
    RESOLVED,
    FALSE_ALARM
}
