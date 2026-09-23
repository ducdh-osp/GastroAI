package vn.gastroai.be.application.commands;

/** Input cho AuthService.login() — tách khỏi LoginRequest (tầng api) để application không phụ thuộc ngược lên api. */
public record LoginCommand(String email, String password) {
}
