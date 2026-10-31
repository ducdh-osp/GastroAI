-- UC0038 / UC0054: Bổ sung các cụm từ khóa cảnh báo cấp cứu đau bụng quặn và chướng căng cứng
-- để đảm bảo độ nhạy Triage (Sensitivity >= 95%) cho các biến thể diễn đạt thông dụng của bệnh nhân.

INSERT INTO triage_keywords (group_code, keyword) VALUES
    ('DAU_BUNG_CAP_TINH', 'dau quan tung con'),
    ('DAU_BUNG_CAP_TINH', 'dau quan du doi'),
    ('DAU_BUNG_CAP_TINH', 'bung chuong cang cung'),
    ('DAU_BUNG_CAP_TINH', 'dau nhoi du doi')
ON CONFLICT (group_code, keyword) DO NOTHING;
