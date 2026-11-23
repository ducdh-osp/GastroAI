package vn.gastroai.be.application.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.gastroai.be.application.auth.ClientRequestInfoResolver;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.domain.audit.AuditLog;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AuditLogRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private AuditAnomalyDetector anomalyDetector;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, adminRepository,
                new ClientRequestInfoResolver(""), anomalyDetector);
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void loginAs(Long id) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("User-Agent", "JUnit-Browser");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CmsAuthService.AUTH_USER_ID, id);
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordTakesAdminFromSessionAndRequestInfo() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setEmail("admin@gastroai.vn");
        loginAs(1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(auditLogRepository.save(any(AuditLog.class))).then(returnsFirstArg());

        service.record(AuditAction.PATIENT_LOCK, "PATIENT", 7L, null);

        AuditLog saved = captureSaved();
        assertEquals("ADMIN", saved.getActorType());
        assertEquals("ADMIN", saved.getActorRole());
        assertEquals(1L, saved.getActorId());
        assertEquals("admin@gastroai.vn", saved.getActorEmail());
        assertEquals(AuditAction.PATIENT_LOCK, saved.getAction());
        assertEquals("PATIENT", saved.getTargetType());
        assertEquals("7", saved.getTargetId());
        assertEquals("10.0.0.5", saved.getIpAddress());
        assertEquals("JUnit-Browser", saved.getUserAgent());
        verify(anomalyDetector).check(saved);
    }

    @Test
    void contentEditorIsRecordedAsCmsAccountWithItsRole() {
        Admin editor = new Admin();
        editor.setId(2L);
        editor.setEmail("bientap@gastroai.vn");
        editor.setRole(CmsRole.CONTENT_EDITOR);
        loginAs(2L);
        when(adminRepository.findById(2L)).thenReturn(Optional.of(editor));
        when(auditLogRepository.save(any(AuditLog.class))).then(returnsFirstArg());

        service.record(AuditAction.PATIENT_DATA_VIEW, "PATIENT", 3L, "Xem lịch sử");

        AuditLog saved = captureSaved();
        assertEquals("ADMIN", saved.getActorType());
        assertEquals("CONTENT_EDITOR", saved.getActorRole());
        assertEquals(2L, saved.getActorId());
        assertEquals("bientap@gastroai.vn", saved.getActorEmail());
    }

    @Test
    void recordWithoutRequestIsSystemActor() {
        when(auditLogRepository.save(any(AuditLog.class))).then(returnsFirstArg());

        service.record(AuditAction.SYSTEM_RESTORE, "SYSTEM", null, null);

        AuditLog saved = captureSaved();
        assertEquals("SYSTEM", saved.getActorType());
        assertNull(saved.getActorId());
        assertNull(saved.getActorEmail());
        assertNull(saved.getActorRole());
        assertNull(saved.getTargetId());
        assertEquals("unknown", saved.getIpAddress());
    }

    @Test
    void longDetailIsCutTo1000Characters() {
        when(auditLogRepository.save(any(AuditLog.class))).then(returnsFirstArg());

        service.record(AuditAction.DOCUMENT_UPDATE_INFO, "DOCUMENT", 1L, "x".repeat(1500));

        assertEquals(1000, captureSaved().getDetail().length());
    }

    @Test
    void storageErrorNeverBreaksTheCaller() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("MySQL down"));

        assertDoesNotThrow(() -> service.record(AuditAction.PATIENT_LOCK, "PATIENT", 7L, null));
        verify(anomalyDetector, never()).check(any());
    }

    @Test
    void currentActorEmailReadsSessionOrReturnsNull() {
        assertNull(service.currentActorEmail());

        Admin admin = new Admin();
        admin.setEmail("admin@gastroai.vn");
        loginAs(1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertEquals("admin@gastroai.vn", service.currentActorEmail());
    }

    @Test
    void searchRejectsFromDateAfterToDate() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.search(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 1),
                        null, null, null, null, 0, 20));

        assertEquals("Từ ngày phải trước hoặc bằng Đến ngày", error.getMessage());
    }

    @Test
    void searchConvertsDatesToVietnamDayAndNormalisesKeyword() {
        when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 30),
                AuditAction.PATIENT_LOCK, "  ", 3L, "  ADMIN ", 0, 20);

        verify(auditLogRepository).search(
                eq(Instant.parse("2026-10-29T17:00:00Z")),
                eq(Instant.parse("2026-10-30T17:00:00Z")),
                eq(AuditAction.PATIENT_LOCK),
                isNull(),
                eq("ADMIN"),
                eq(3L),
                eq("%admin%"),
                eq("ADMIN"),
                any());
    }

    @Test
    void exportRefusesMoreThanLimitRows() {
        when(auditLogRepository.search(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, AuditLogService.MAX_EXPORT_ROWS),
                        AuditLogService.MAX_EXPORT_ROWS + 1L));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.findForExport(null, null, null, null, null, null));

        assertTrue(error.getMessage().contains("vượt quá giới hạn xuất"));
    }
}