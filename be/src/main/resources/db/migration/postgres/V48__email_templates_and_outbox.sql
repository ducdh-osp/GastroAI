-- UC0069/70/71/74: ha tang email dung chung, mau email sua duoc qua CMS va outbox retry.
-- Payload outbox duoc ma hoa o tang ung dung; DB khong luu token xac thuc/reset dang ro.
CREATE TABLE email_templates (
    code                  VARCHAR(60) PRIMARY KEY,
    display_name          VARCHAR(160) NOT NULL,
    subject_template      VARCHAR(255) NOT NULL,
    html_body_template    TEXT NOT NULL,
    text_body_template    TEXT NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_type       VARCHAR(20),
    updated_by_id         BIGINT,
    CONSTRAINT ck_email_template_code CHECK (code IN (
        'ACCOUNT_VERIFICATION', 'PASSWORD_RESET', 'MEDICATION_REMINDER_OFFLINE'
    ))
);

CREATE TABLE email_template_revisions (
    id                    BIGSERIAL PRIMARY KEY,
    template_code         VARCHAR(60) NOT NULL REFERENCES email_templates(code) ON DELETE CASCADE,
    version_number        BIGINT NOT NULL,
    subject_template      VARCHAR(255) NOT NULL,
    html_body_template    TEXT NOT NULL,
    text_body_template    TEXT NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by_type       VARCHAR(20),
    updated_by_id         BIGINT,
    CONSTRAINT uq_email_template_revision UNIQUE (template_code, version_number)
);

CREATE TABLE email_outbox (
    id                    BIGSERIAL PRIMARY KEY,
    template_code         VARCHAR(60) NOT NULL REFERENCES email_templates(code),
    recipient_email       VARCHAR(255) NOT NULL,
    encrypted_payload     TEXT NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    dedupe_key            VARCHAR(190) NOT NULL UNIQUE,
    patient_id            BIGINT REFERENCES patients(id) ON DELETE CASCADE,
    medication_reminder_id BIGINT REFERENCES medication_reminders(id) ON DELETE CASCADE,
    scheduled_date        DATE,
    scheduled_time        TIME,
    expected_token_hash   VARCHAR(64),
    attempt_count         INT NOT NULL DEFAULT 0,
    next_attempt_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error            VARCHAR(1000),
    sent_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_email_outbox_status CHECK (status IN (
        'PENDING', 'SENT', 'FAILED', 'CANCELLED', 'SKIPPED'
    )),
    CONSTRAINT ck_email_outbox_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX idx_email_outbox_due
    ON email_outbox(next_attempt_at, id)
    WHERE status = 'PENDING';

CREATE INDEX idx_email_outbox_patient_template
    ON email_outbox(patient_id, template_code, status)
    WHERE patient_id IS NOT NULL;

INSERT INTO email_templates (
    code, display_name, subject_template, html_body_template, text_body_template
) VALUES
(
    'ACCOUNT_VERIFICATION',
    'Xác thực tài khoản',
    'Xác thực tài khoản GastroAI',
    '<h2>Xác thực tài khoản GastroAI</h2><p>Chào {{userName}},</p><p>Vui lòng nhấn vào liên kết dưới đây để xác thực tài khoản:</p><p><a href="{{verificationUrl}}">Xác thực tài khoản</a></p><p>Liên kết có hiệu lực trong 24 giờ.</p>',
    'Chào {{userName}},\n\nVui lòng xác thực tài khoản tại: {{verificationUrl}}\n\nLiên kết có hiệu lực trong 24 giờ.'
),
(
    'PASSWORD_RESET',
    'Đặt lại mật khẩu',
    'Đặt lại mật khẩu GastroAI',
    '<h2>Đặt lại mật khẩu GastroAI</h2><p>Chào {{userName}},</p><p>Vui lòng nhấn vào liên kết dưới đây để đặt lại mật khẩu:</p><p><a href="{{resetUrl}}">Đặt lại mật khẩu</a></p><p>Liên kết có hiệu lực trong 30 phút. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p>',
    'Chào {{userName}},\n\nĐặt lại mật khẩu tại: {{resetUrl}}\n\nLiên kết có hiệu lực trong 30 phút. Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.'
),
(
    'MEDICATION_REMINDER_OFFLINE',
    'Nhắc uống thuốc khi offline',
    'Nhắc uống thuốc từ GastroAI',
    '<h2>Nhắc uống thuốc</h2><p>Đã đến giờ uống thuốc theo lịch bạn đã thiết lập.</p><p>Vui lòng mở <a href="{{applicationUrl}}">GastroAI</a> để kiểm tra và xác nhận sau khi uống.</p>',
    'Đã đến giờ uống thuốc theo lịch bạn đã thiết lập. Vui lòng mở GastroAI để kiểm tra và xác nhận sau khi uống: {{applicationUrl}}'
);

-- Luu ca ban seed de lich su phien ban day du ngay tu luc khoi tao.
INSERT INTO email_template_revisions (
    template_code, version_number, subject_template, html_body_template, text_body_template
)
SELECT code, version, subject_template, html_body_template, text_body_template
FROM email_templates;
