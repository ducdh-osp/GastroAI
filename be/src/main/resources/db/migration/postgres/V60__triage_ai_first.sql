-- SCR-AD-13 (AI-first): hệ thống tự hỏi bệnh nhân trước, chỉ kéo Admin vào khi bệnh nhân im lặng.
-- 3 trạng thái: AUTO_HANDLING (hệ thống đang tự xử lý) -> NEEDS_INTERVENTION (cần can thiệp) -> RESOLVED.
ALTER TABLE triage_alerts
    ADD COLUMN needs_intervention_at TIMESTAMPTZ,
    ADD COLUMN acknowledged_at       TIMESTAMPTZ,
    ADD COLUMN failsafe_at           TIMESTAMPTZ,
    ADD COLUMN patient_response      VARCHAR(20),
    ADD COLUMN patient_responded_at  TIMESTAMPTZ;

-- Ca cũ còn mở đã quá mốc tự động từ lâu -> chuyển thẳng "Cần can thiệp". Ghi acknowledged_at để
-- failsafe không bắn hàng loạt cho dữ liệu cũ ngay lúc nâng cấp (ca cũ vốn đã hiện trên CMS từ trước).
UPDATE triage_alerts
SET status                = 'NEEDS_INTERVENTION',
    needs_intervention_at = occurred_at,
    acknowledged_at       = COALESCE(claimed_at, now())
WHERE status IN ('NEW', 'IN_PROGRESS');

ALTER TABLE triage_alerts ALTER COLUMN status SET DEFAULT 'AUTO_HANDLING';

ALTER TABLE triage_alerts
    ADD CONSTRAINT ck_triage_alerts_status
        CHECK (status IN ('AUTO_HANDLING', 'NEEDS_INTERVENTION', 'RESOLVED')),
    ADD CONSTRAINT ck_triage_alerts_patient_response
        CHECK (patient_response IS NULL OR patient_response IN ('SAFE', 'NEED_HELP'));

-- Thêm kết quả "bệnh nhân tự báo an toàn" (bấm [An toàn] trên thông báo hỏi thăm).
ALTER TABLE triage_alerts DROP CONSTRAINT ck_triage_alerts_outcome;
ALTER TABLE triage_alerts
    ADD CONSTRAINT ck_triage_alerts_outcome
        CHECK (outcome IN ('HANDLED', 'FALSE_ALARM', 'PATIENT_SAFE'));
