package vn.gastroai.be.domain.auth;

/**
 * Chỉ còn PATIENT — nhân sự CMS đăng nhập qua bảng admins bên MySQL (domain.admin), không dùng chung
 * role với Bệnh nhân.
 * Giữ enum riêng (thay vì hardcode String) để Hibernate validate + tận dụng RBAC middleware
 * (hasRole("PATIENT") trong SecurityConfig) nếu sau này có thêm vai trò phía patients.
 */
public enum UserRole {
    PATIENT
}
