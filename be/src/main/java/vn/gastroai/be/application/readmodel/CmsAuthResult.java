package vn.gastroai.be.application.readmodel;

public record CmsAuthResult(
        Long userId,
        String email,
        String fullName,
        String userType
) {
}