package vn.gastroai.be.application.admin;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.api.cms.rbac.CreateCmsAccountRequest;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CmsRbacServiceTest {
    private final AdminRepository admins = mock(AdminRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private CmsRbacService service;

    @BeforeEach
    void setUp() {
        service = new CmsRbacService(admins, passwordEncoder);
    }

    @Test
    void assignsContentEditorToCmsAdminAccount() {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setEmail("editor@gastroai.vn");
        when(admins.findByIdForUpdate(9L)).thenReturn(Optional.of(admin));

        var result = service.assignRole(9L, CmsRole.CONTENT_EDITOR);

        assertEquals(CmsRole.CONTENT_EDITOR, admin.getRole());
        assertEquals(CmsRole.CONTENT_EDITOR, result.role());
        verify(admins).findByIdForUpdate(9L);
    }

    @Test
    void escapesLikeWildcardsBeforeSearchingAccounts() {
        String escaped = "100!%!_!\\!!";
        PageRequest pageable = PageRequest.of(0, 20);
        when(admins.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(escaped, escaped, pageable))
                .thenReturn(Page.empty(pageable));

        service.list("100%_\\!", pageable);

        verify(admins).findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(escaped, escaped, pageable);
    }

    @Test
    void assignsNewRolesToCmsAdminAccount() {
        Admin admin = new Admin();
        admin.setId(11L);
        when(admins.findByIdForUpdate(11L)).thenReturn(Optional.of(admin));

        var result = service.assignRole(11L, CmsRole.OPERATIONS);

        assertEquals(CmsRole.OPERATIONS, result.role());
    }

    @Test
    void createsAccountWithHashedPasswordAndDefaultUnlocked() {
        when(admins.findByEmail("new@gastroai.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("hashed");

        var result = service.createAccount(
                new CreateCmsAccountRequest("Lê Văn E", "new@gastroai.vn", CmsRole.CONTENT_AUTHOR, "Str0ng!Pass"));

        assertEquals(CmsRole.CONTENT_AUTHOR, result.role());
        assertFalse(result.adminLocked());
        verify(admins).save(argThat(a -> "hashed".equals(a.getPasswordHash())));
    }

    @Test
    void rejectsCreatingAccountWithDuplicateEmail() {
        when(admins.findByEmail("dup@gastroai.vn")).thenReturn(Optional.of(new Admin()));

        assertThrows(IllegalArgumentException.class, () -> service.createAccount(
                new CreateCmsAccountRequest("X", "dup@gastroai.vn", CmsRole.ADMIN, "Str0ng!Pass")));
        verify(admins, never()).save(any());
    }

    @Test
    void locksAccountAndBumpsSessionVersionOnlyOnce() {
        Admin admin = new Admin();
        admin.setId(5L);
        admin.setSessionVersion(2);
        when(admins.findByIdForUpdate(5L)).thenReturn(Optional.of(admin));

        var result = service.lock(5L);

        assertTrue(result.adminLocked());
        assertEquals(3, admin.getSessionVersion());

        // lock lai 1 ca da khoa thi khong tang them sessionVersion nua
        service.lock(5L);
        assertEquals(3, admin.getSessionVersion());
    }

    @Test
    void unlocksAccountWithoutChangingSessionVersion() {
        Admin admin = new Admin();
        admin.setId(5L);
        admin.setAdminLockedAt(Instant.now());
        admin.setSessionVersion(4);
        when(admins.findByIdForUpdate(5L)).thenReturn(Optional.of(admin));

        var result = service.unlock(5L);

        assertFalse(result.adminLocked());
        assertEquals(4, admin.getSessionVersion());
    }
}
