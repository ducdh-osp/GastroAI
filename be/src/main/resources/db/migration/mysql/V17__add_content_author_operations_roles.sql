-- Mo rong RBAC tu 2 len 4 vai tro (SCR-AD-03 v1.2) - them CONTENT_AUTHOR (Cong tac vien noi
-- dung) va OPERATIONS (Giam sat van hanh) vao danh sach role hop le cua bang admins. Ca 2 vai
-- tro moi nam o bang admins (giong CONTENT_EDITOR), khong dung bang doctors - viec don
-- DOCTOR/bang doctors khoi he thong la viec rieng, CHUA lam trong migration nay (xem ghi chu
-- ky thuat o SCR-AD-03 Section 4 va SCR-AD-14 Section 4).
ALTER TABLE admins
    DROP CHECK chk_admins_role;

ALTER TABLE admins
    ADD CONSTRAINT chk_admins_role CHECK (role IN ('ADMIN', 'CONTENT_EDITOR', 'CONTENT_AUTHOR', 'OPERATIONS'));
