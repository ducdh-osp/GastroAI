package vn.gastroai.be.domain.notification;

public enum NotificationType {
    JOURNAL_REMINDER,
    MEDICATION_REMINDER,
    SECURITY_ALERT,
    TRIAGE_ALERT,
    SYSTEM,
    /** SCR-AD-13 buoc 1: hoi benh nhan "Ban co on khong?" kem 2 nut [An toan] [Can ho tro]. */
    TRIAGE_CHECK_IN,
    /** SCR-AD-13 buoc 3/4: bao moi Admin dang truc co ca "Can can thiep" (hoac qua han). */
    TRIAGE_INTERVENTION
}
