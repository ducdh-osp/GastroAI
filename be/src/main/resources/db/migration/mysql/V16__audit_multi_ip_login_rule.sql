UPDATE audit_anomaly_rules
SET name = 'Đăng nhập/thao tác từ nhiều địa chỉ IP',
    threshold = 4,
    window_minutes = 1440
WHERE code = 'MULTI_IP'
  AND updated_by IS NULL;