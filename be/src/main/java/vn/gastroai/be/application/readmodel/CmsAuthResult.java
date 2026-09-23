package vn.gastroai.be.application.readmodel;

/** Kết quả CmsAuthService.login() — không có token vì CMS dùng HttpSession, không dùng JWT. */
public record CmsAuthResult(
        Long userId,
        String email,
        String fullName,
        String userType
) {
}