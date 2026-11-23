-- SCR-AD-03/SCR-AD-14: 15 tai khoan test cho MOI vai trong 4 vai tro CMS (60 dong) - phuc vu
-- demo/test ma tran quyen moi (SCR-AD-03 Section 4) ngay ca khi man tao tai khoan that (SCR-AD-14)
-- chua duoc code. Mat khau dung chung "1" (giong quy uoc V2/V6 - de nho luc test, KHONG dung mat
-- khau nay cho moi truong that). CONTENT_AUTHOR/OPERATIONS can V17 chay truoc (mo rong CHECK
-- constraint + enum CmsRole) moi insert duoc.

INSERT INTO admins (email, password_hash, full_name, role, created_at, updated_at)
VALUES
    ('admin01@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 01', 'ADMIN', NOW(), NOW()),
    ('admin02@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 02', 'ADMIN', NOW(), NOW()),
    ('admin03@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 03', 'ADMIN', NOW(), NOW()),
    ('admin04@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 04', 'ADMIN', NOW(), NOW()),
    ('admin05@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 05', 'ADMIN', NOW(), NOW()),
    ('admin06@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 06', 'ADMIN', NOW(), NOW()),
    ('admin07@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 07', 'ADMIN', NOW(), NOW()),
    ('admin08@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 08', 'ADMIN', NOW(), NOW()),
    ('admin09@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 09', 'ADMIN', NOW(), NOW()),
    ('admin10@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 10', 'ADMIN', NOW(), NOW()),
    ('admin11@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 11', 'ADMIN', NOW(), NOW()),
    ('admin12@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 12', 'ADMIN', NOW(), NOW()),
    ('admin13@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 13', 'ADMIN', NOW(), NOW()),
    ('admin14@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 14', 'ADMIN', NOW(), NOW()),
    ('admin15@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Admin 15', 'ADMIN', NOW(), NOW());

INSERT INTO admins (email, password_hash, full_name, role, created_at, updated_at)
VALUES
    ('bientap01@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 01', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap02@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 02', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap03@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 03', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap04@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 04', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap05@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 05', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap06@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 06', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap07@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 07', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap08@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 08', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap09@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 09', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap10@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 10', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap11@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 11', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap12@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 12', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap13@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 13', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap14@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 14', 'CONTENT_EDITOR', NOW(), NOW()),
    ('bientap15@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Bien tap chuyen mon y khoa 15', 'CONTENT_EDITOR', NOW(), NOW());

INSERT INTO admins (email, password_hash, full_name, role, created_at, updated_at)
VALUES
    ('congtacvien01@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 01', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien02@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 02', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien03@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 03', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien04@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 04', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien05@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 05', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien06@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 06', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien07@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 07', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien08@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 08', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien09@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 09', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien10@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 10', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien11@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 11', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien12@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 12', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien13@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 13', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien14@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 14', 'CONTENT_AUTHOR', NOW(), NOW()),
    ('congtacvien15@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Cong tac vien noi dung 15', 'CONTENT_AUTHOR', NOW(), NOW());

INSERT INTO admins (email, password_hash, full_name, role, created_at, updated_at)
VALUES
    ('giamsat01@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 01', 'OPERATIONS', NOW(), NOW()),
    ('giamsat02@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 02', 'OPERATIONS', NOW(), NOW()),
    ('giamsat03@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 03', 'OPERATIONS', NOW(), NOW()),
    ('giamsat04@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 04', 'OPERATIONS', NOW(), NOW()),
    ('giamsat05@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 05', 'OPERATIONS', NOW(), NOW()),
    ('giamsat06@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 06', 'OPERATIONS', NOW(), NOW()),
    ('giamsat07@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 07', 'OPERATIONS', NOW(), NOW()),
    ('giamsat08@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 08', 'OPERATIONS', NOW(), NOW()),
    ('giamsat09@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 09', 'OPERATIONS', NOW(), NOW()),
    ('giamsat10@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 10', 'OPERATIONS', NOW(), NOW()),
    ('giamsat11@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 11', 'OPERATIONS', NOW(), NOW()),
    ('giamsat12@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 12', 'OPERATIONS', NOW(), NOW()),
    ('giamsat13@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 13', 'OPERATIONS', NOW(), NOW()),
    ('giamsat14@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 14', 'OPERATIONS', NOW(), NOW()),
    ('giamsat15@gastroai.vn', '$2a$10$I68nVLFYTMmW3MmzbsDBvOxqoOXWaTiC85N4Az/3UGKU3uapLI9NG', 'Giam sat van hanh 15', 'OPERATIONS', NOW(), NOW());

