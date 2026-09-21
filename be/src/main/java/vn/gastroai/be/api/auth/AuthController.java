package vn.gastroai.be.api.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.auth.AuthService;
import vn.gastroai.be.application.commands.LoginCommand;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/v1/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(new LoginCommand(request.email(), request.password()));
        return AuthResponse.from(result);
    }
}
