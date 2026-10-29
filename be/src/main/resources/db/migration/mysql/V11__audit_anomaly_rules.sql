CREATE TABLE audit_anomaly_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    actions VARCHAR(500),
    metric VARCHAR(20) NOT NULL,
    threshold INT NOT NULL,
    window_minutes INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),

    PRIMARY KEY (id),
    CONSTRAINT uk_audit_anomaly_rules_code UNIQUE (code)
);

INSERT INTO audit_anomaly_rules (code, name, actions, metric, threshold, window_minutes) VALUES
    ('MASS_PATIENT_VIEW',       'Xem dữ liệu nhiều bệnh nhân',  'PATIENT_DATA_VIEW',                       'DISTINCT_TARGETS', 20, 10),
    ('MASS_DOCUMENT_DELETE',    'Xoá tài liệu hàng loạt',       'DOCUMENT_DELETE,DOCUMENT_DELETE_VERSION', 'ACTIONS',           5, 10),
    ('MASS_DOCUMENT_UNPUBLISH', 'Gỡ tài liệu hàng loạt',        'DOCUMENT_UNPUBLISH',                      'ACTIONS',           5, 10),
    ('MASS_PATIENT_LOCK',       'Khoá/mở khoá nhiều bệnh nhân', 'PATIENT_LOCK,PATIENT_UNLOCK',             'DISTINCT_TARGETS', 10, 10),
    ('MASS_ROLE_CHANGE',        'Đổi vai trò nhiều tài khoản',  'RBAC_ROLE_CHANGE',                        'DISTINCT_TARGETS',  5, 10),
    ('MULTI_IP',                'Thao tác từ nhiều địa chỉ IP', NULL,                                      'DISTINCT_IPS',      3, 30);