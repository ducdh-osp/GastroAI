package vn.gastroai.be.domain.admin;

/**
 * Vai trò phân quyền CMS; loại tài khoản đăng nhập được xác định riêng bởi CmsAccountType.
 * CONTENT_AUTHOR (Cộng tác viên nội dung) và OPERATIONS (Giám sát vận hành) thêm theo
 * SCR-AD-03 v1.2 (nâng từ 2 lên 4 vai trò) - cả 2 nằm ở bảng admins, không dùng bảng doctors.
 * Việc enforce ma trận quyền mới (SecurityConfig, cmsNav.ts) và dọn DOCTOR/bảng doctors là việc
 * riêng, CHƯA làm trong lần đổi này - xem ghi chú kỹ thuật ở SCR-AD-03 Section 4.
 */
public enum CmsRole {
    ADMIN,
    DOCTOR,
    CONTENT_EDITOR,
    CONTENT_AUTHOR,
    OPERATIONS
}

