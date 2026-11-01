INSERT INTO audit_anomaly_rules (code, name, actions, metric, threshold, window_minutes) VALUES
    ('MASS_CONTENT_DELETE',    'Xoá nội dung hàng loạt', 'CONTENT_DELETE',    'ACTIONS', 5, 10),
    ('MASS_CONTENT_UNPUBLISH', 'Gỡ nội dung hàng loạt',  'CONTENT_UNPUBLISH', 'ACTIONS', 5, 10);