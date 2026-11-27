package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.application.geo.IpLocationService;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminPasswordHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsAuthServiceCurrentAccountTest {

    @Mock private AdminRepository adminRepository;
    @Mock private AdminLoginHistoryRepository adminLoginHistoryRepository;
    @Mock private AdminPasswordHistoryRepository passwordHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CmsAuthService service;

    @BeforeEach
    void setUp() {
        service = new CmsAuthService(adminRepository, adminLoginHistoryRepository, passwordHistoryRepository,
                passwordEncoder, new LoginSecurityPolicy(5, 10, 90),
                new ClientRequestInfoResolver(""), mock(IpLocationService.class));
    }

    @Test
    void returnsRoleReadFreshFromDatabase() {
        Admin admin = new Admin();
        admin.setId(7L);
        admin.setEmail("ops@gastroai.vn");
        admin.setFullName("Nguyễn Văn A");
        admin.setRole(CmsRole.OPERATIONS);
        when(adminRepository.findById(7L)).thenReturn(Optional.of(admin));

        var result = service.currentAccount(7L);

        assertEquals(7L, result.userId());
        assertEquals("ops@gastroai.vn", result.email());
        assertEquals("OPERATIONS", result.userType());
    }

    @Test
    void deletedAccountIsTreatedAsLoggedOut() {
        when(adminRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.currentAccount(7L));
    }
}
