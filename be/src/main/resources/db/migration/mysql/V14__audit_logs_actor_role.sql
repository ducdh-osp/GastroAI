-- UC0052: ghi vai tro cua nguoi thuc hien tai dung thoi diem thao tac (ADMIN, CONTENT_EDITOR).
-- actor_type + actor_id van la danh tinh de dem bat thuong; actor_role chi de hien thi.
-- Dong cu de trong, man hinh lui ve nhan theo actor_type.
ALTER TABLE audit_logs
    ADD COLUMN actor_role VARCHAR(30) NULL AFTER actor_type;