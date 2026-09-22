package vn.gastroai.be.api.auth; import jakarta.validation.constraints.*; public record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank @Size(min=8) String newPassword){}
