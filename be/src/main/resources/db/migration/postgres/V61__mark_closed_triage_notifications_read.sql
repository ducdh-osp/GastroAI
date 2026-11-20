-- SCR-AD-13: ca cap cuu da dong thi thong bao cua ca do khong con viec gi phai lam.
-- Tu nay BE tu danh dau da doc luc dong ca (AppNotificationService.markTriageAlertNotificationsRead);
-- migration nay xu ly cac ca DA dong truoc do.
-- Bang admins nam o MySQL nen khong doc duoc o day: lay id Admin tu du lieu Postgres co san
-- (Admin tung doc/xoa thong bao, hoac tung nhan/dong ca cap cuu).
INSERT INTO notification_states (notification_id, recipient_type, recipient_id, read_at)
SELECT n.id, 'ADMIN', a.admin_id, now()
FROM notifications n
JOIN triage_alerts t ON t.id = n.source_id AND t.status = 'RESOLVED'
CROSS JOIN (
    SELECT recipient_id AS admin_id FROM notification_states WHERE recipient_type = 'ADMIN'
    UNION
    SELECT claimed_by_id FROM triage_alerts WHERE claimed_by_type = 'ADMIN' AND claimed_by_id IS NOT NULL
    UNION
    SELECT resolved_by_id FROM triage_alerts WHERE resolved_by_type = 'ADMIN' AND resolved_by_id IS NOT NULL
) a
WHERE n.recipient_type = 'ADMIN' AND n.recipient_id IS NULL
  AND n.source_type = 'TRIAGE_ALERT'
UNION ALL
SELECT n.id, 'PATIENT', n.recipient_id, now()
FROM notifications n
JOIN triage_alerts t ON t.id = n.source_id AND t.status = 'RESOLVED'
WHERE n.recipient_type = 'PATIENT'
  AND n.source_type = 'TRIAGE_ALERT'
ON CONFLICT (notification_id, recipient_type, recipient_id)
DO UPDATE SET read_at = COALESCE(notification_states.read_at, EXCLUDED.read_at);
