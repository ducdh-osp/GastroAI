package vn.gastroai.be.api.support;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.security.Principal;

/** Shared controller boundary checks for requests made by an authenticated patient. */
public final class AuthenticatedRequest {

    private AuthenticatedRequest() {
    }

    public static void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Chua dang nhap");
        }
    }

    public static Long patientId(Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        if (principal == null) {
            throw new AccessDeniedException("Chua dang nhap");
        }
        return Long.valueOf(principal.getName());
    }
}
