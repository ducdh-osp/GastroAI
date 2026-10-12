package vn.gastroai.be.domain.triage;

public enum TriageAlertStatus {
    /** Moi phat sinh, chua ai tiep nhan. */
    NEW,
    /** Da co 1 admin/bac si bam "tiep nhan" (claim). */
    IN_PROGRESS,
    /** Da xu ly xong. */
    RESOLVED
}