package vn.gastroai.be.application.commands;

/** Input cho CmsAuthService.login(). */
public record CmsLoginCommand(
        String email,
        String password
) {
}