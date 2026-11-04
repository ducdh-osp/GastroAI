package vn.gastroai.be.domain.triage;

/** Cac moc tren timeline cua 1 canh bao (bang triage_alert_actions). */
public enum TriageAlertActionType {
    CREATED,
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
