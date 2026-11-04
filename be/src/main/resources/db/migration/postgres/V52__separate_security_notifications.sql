UPDATE notifications
SET notification_type = 'SECURITY_ALERT'
WHERE recipient_type = 'PATIENT'
  AND notification_type = 'SYSTEM'
  AND source_type = 'LOGIN_SECURITY';
