UPDATE email_templates
SET subject_template = 'Xác thực tài khoản GastroAI',
    html_body_template = '<h2>Chào mừng bạn đến với GastroAI</h2><p>Xin chào <strong>{{userName}}</strong>,</p><p>Cảm ơn bạn đã đăng ký tài khoản. Hãy xác thực địa chỉ email để bắt đầu sử dụng các tính năng theo dõi sức khỏe tiêu hóa.</p><p><a href="{{verificationUrl}}">Xác thực tài khoản</a></p><p><small>Liên kết có hiệu lực trong 24 giờ và chỉ sử dụng được một lần.</small></p><p>Nếu bạn không tạo tài khoản GastroAI, bạn có thể bỏ qua email này.</p>',
    text_body_template = E'Chào {{userName}},\n\nCảm ơn bạn đã đăng ký GastroAI. Xác thực tài khoản tại:\n{{verificationUrl}}\n\nLiên kết có hiệu lực trong 24 giờ và chỉ sử dụng được một lần. Nếu bạn không tạo tài khoản này, hãy bỏ qua email.',
    version = version + 1,
    updated_at = now(),
    updated_by_type = 'SYSTEM',
    updated_by_id = NULL
WHERE code = 'ACCOUNT_VERIFICATION';

UPDATE email_templates
SET subject_template = 'Yêu cầu đặt lại mật khẩu GastroAI',
    html_body_template = '<h2>Đặt lại mật khẩu</h2><p>Xin chào <strong>{{userName}}</strong>,</p><p>GastroAI nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Nhấn nút bên dưới để tạo mật khẩu mới.</p><p><a href="{{resetUrl}}">Đặt lại mật khẩu</a></p><p><small>Liên kết có hiệu lực trong 30 phút và chỉ sử dụng được một lần.</small></p><p>Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. Mật khẩu hiện tại của bạn vẫn được giữ nguyên.</p>',
    text_body_template = E'Chào {{userName}},\n\nGastroAI nhận được yêu cầu đặt lại mật khẩu. Tạo mật khẩu mới tại:\n{{resetUrl}}\n\nLiên kết có hiệu lực trong 30 phút và chỉ sử dụng được một lần. Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.',
    version = version + 1,
    updated_at = now(),
    updated_by_type = 'SYSTEM',
    updated_by_id = NULL
WHERE code = 'PASSWORD_RESET';

INSERT INTO email_template_revisions (
    template_code, version_number, subject_template, html_body_template,
    text_body_template, updated_at, updated_by_type, updated_by_id
)
SELECT code, version, subject_template, html_body_template,
       text_body_template, updated_at, updated_by_type, updated_by_id
FROM email_templates
WHERE code IN ('ACCOUNT_VERIFICATION', 'PASSWORD_RESET')
ON CONFLICT (template_code, version_number) DO NOTHING;
