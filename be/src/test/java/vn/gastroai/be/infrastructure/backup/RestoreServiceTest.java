package vn.gastroai.be.infrastructure.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import vn.gastroai.be.config.BackupProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RestoreServiceTest {

    private final ProcessExecutor processExecutor = mock(ProcessExecutor.class);

    private BackupProperties backupProperties(Path backupDir) {
        return new BackupProperties(
                backupDir.toString(),
                14,
                new BackupProperties.DatabaseBackupProperties(null, "gastroai", "gastroai-pw", "gastroai", 5432),
                new BackupProperties.DatabaseBackupProperties(null, "gastroai", "gastroai-admin-pw", "gastroai_admin", 3306));
    }

    @Test
    void restoreAllDoesNothingAndReturnsOkWhenNoBackupFilesExist(@TempDir Path backupDir) {
        RestoreService service = new RestoreService(backupProperties(backupDir), processExecutor);

        RestoreResult result = service.restoreAll();

        assertTrue(result.postgresOk());
        assertTrue(result.mysqlOk());
        verify(processExecutor, never()).run(any(), anyString(), any());
    }

    @Test
    void restoreAllPicksLatestPostgresBackupAndBuildsCorrectCommand(@TempDir Path backupDir) throws IOException {
        Files.writeString(backupDir.resolve("postgres_20260101_000000.sql"), "-- cu");
        Files.writeString(backupDir.resolve("postgres_20260301_000000.sql"), "-- moi");
        RestoreService service = new RestoreService(backupProperties(backupDir), processExecutor);

        service.restoreAll();

        ArgumentCaptor<ProcessBuilder> captor = ArgumentCaptor.forClass(ProcessBuilder.class);
        verify(processExecutor).run(captor.capture(), eq("PostgreSQL restore"), isNull());

        ProcessBuilder captured = captor.getValue();
        assertTrue(captured.command().contains("-q"));
        assertTrue(captured.command().contains("ON_ERROR_STOP=1"));
        assertTrue(captured.command().contains("gastroai"));
        assertTrue(captured.command().contains("5432"));
        // "-f" gio tro toi file TAM da loc EXTENSION, khong con giu nguyen ten file goc nua -
        // chi con kiem tra duoc dung extension ".sql", xem test rieng ben duoi ve noi dung loc.
        assertTrue(captured.command().stream().anyMatch(arg -> arg.endsWith(".sql")));
        assertEquals("gastroai-pw", captured.environment().get("PGPASSWORD"));
    }

    @Test
    void restorePostgresSkipsExtensionStatementsButKeepsOtherCommandsAndCleansUpTempFile(@TempDir Path backupDir) throws IOException {
        String backupContent = String.join("\n",
                "DROP EXTENSION IF EXISTS vector;",
                "CREATE EXTENSION IF NOT EXISTS vector;",
                "COMMENT ON EXTENSION vector IS 'vector data type and ivfflat access method';",
                "CREATE TABLE documents (id bigint);",
                "INSERT INTO documents VALUES (1);");
        Files.writeString(backupDir.resolve("postgres_20260301_000000.sql"), backupContent);

        // Phai doc noi dung file tam NGAY trong thenAnswer, vi RestoreService xoa file tam o
        // finally ngay sau khi processExecutor.run(...) tra ve - doc sau do se khong con gi.
        List<String> capturedContent = new ArrayList<>();
        List<Path> capturedPath = new ArrayList<>();
        doAnswer(invocation -> {
            ProcessBuilder processBuilder = invocation.getArgument(0);
            String lastArg = processBuilder.command().get(processBuilder.command().size() - 1);
            Path filteredFile = Path.of(lastArg);
            capturedPath.add(filteredFile);
            capturedContent.add(Files.readString(filteredFile, StandardCharsets.UTF_8));
            return null;
        }).when(processExecutor).run(any(), eq("PostgreSQL restore"), any());

        RestoreService service = new RestoreService(backupProperties(backupDir), processExecutor);
        service.restoreAll();

        String filtered = capturedContent.get(0);
        assertFalse(filtered.contains("DROP EXTENSION"));
        assertFalse(filtered.contains("CREATE EXTENSION"));
        assertFalse(filtered.contains("COMMENT ON EXTENSION"));
        assertTrue(filtered.contains("CREATE TABLE documents (id bigint);"));
        assertTrue(filtered.contains("INSERT INTO documents VALUES (1);"));

        // File tam phai bi xoa sau khi restore xong (finally trong RestoreService.restorePostgres).
        assertFalse(Files.exists(capturedPath.get(0)));
    }

    @Test
    void restoreAllSendsMysqlBackupFileContentThroughStdin(@TempDir Path backupDir) throws IOException {
        Files.writeString(backupDir.resolve("mysql_20260301_000000.sql"), "INSERT INTO x VALUES (1);");
        RestoreService service = new RestoreService(backupProperties(backupDir), processExecutor);

        service.restoreAll();

        ArgumentCaptor<byte[]> stdinCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(processExecutor).run(any(), eq("MySQL restore"), stdinCaptor.capture());
        assertEquals("INSERT INTO x VALUES (1);", new String(stdinCaptor.getValue(), StandardCharsets.UTF_8));
    }

    @Test
    void restoreAllStillRunsMysqlAndKeepsRealErrorWhenPostgresFails(@TempDir Path backupDir) throws IOException {
        Files.writeString(backupDir.resolve("postgres_20260301_000000.sql"), "-- loi");
        Files.writeString(backupDir.resolve("mysql_20260301_000000.sql"), "-- ok");
        doThrow(new IllegalStateException("PostgreSQL restore loi, exit code 3: cot khong ton tai"))
                .when(processExecutor).run(any(), eq("PostgreSQL restore"), any());

        RestoreService service = new RestoreService(backupProperties(backupDir), processExecutor);

        RestoreResult result = service.restoreAll();

        assertFalse(result.postgresOk());
        assertTrue(result.postgresError().contains("cot khong ton tai"));
        assertTrue(result.mysqlOk());
        verify(processExecutor).run(any(), eq("MySQL restore"), any());
    }
}