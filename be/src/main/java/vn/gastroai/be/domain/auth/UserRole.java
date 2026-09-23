package vn.gastroai.be.domain.auth;

/**
 * Chỉ còn PATIENT — trước đây có cả DOCTOR nhưng đã gỡ (xem migration V6), vì Bác sĩ/Admin
 * đăng nhập qua bảng riêng bên MySQL (domain.admin), không dùng chung role với Bệnh nhân.
 * Giữ enum riêng (thay vì hardcode String) để Hibernate validate + tận dụng RBAC middleware
 * (hasRole("PATIENT") trong SecurityConfig) nếu sau này có thêm vai trò phía patients.
 */
public enum UserRole {
    PATIENT
}
