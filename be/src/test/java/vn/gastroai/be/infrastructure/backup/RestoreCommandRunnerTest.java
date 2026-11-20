package vn.gastroai.be.infrastructure.backup;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.domain.audit.AuditAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestoreCommandRunnerTest {

    private final RestoreService restoreService = mock(RestoreService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final RestoreCommandRunner runner =
            new RestoreCommandRunner(restoreService, auditLogService, mock(ApplicationContext.class));

    @Test
    void successfulRestoreExitsWithZeroAndIsAudited() {
        when(restoreService.restoreAll()).thenReturn(new RestoreResult(true, null, true, null));

        assertEquals(0, runner.restore());

        verify(auditLogService).record(eq(AuditAction.SYSTEM_RESTORE), eq("SYSTEM"), isNull(),
                contains("postgresOk=true, mysqlOk=true"));
    }

    @Test
    void partialFailureExitsWithOneAndIsStillAudited() {
        when(restoreService.restoreAll()).thenReturn(new RestoreResult(true, null, false, "mysql: access denied"));

        assertEquals(1, runner.restore());

        verify(auditLogService).record(eq(AuditAction.SYSTEM_RESTORE), eq("SYSTEM"), isNull(),
                contains("mysqlOk=false"));
    }
}