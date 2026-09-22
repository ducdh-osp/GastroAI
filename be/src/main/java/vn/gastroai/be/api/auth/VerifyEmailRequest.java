package vn.gastroai.be.api.auth; import jakarta.validation.constraints.NotBlank; public record VerifyEmailRequest(@NotBlank String token){}
