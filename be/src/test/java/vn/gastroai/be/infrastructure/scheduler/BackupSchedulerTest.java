package vn.gastroai.be.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.gastroai.be.config.BackupProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupSchedulerTest {

    private BackupProperties backupProperties(Path backupDir, int retentionDays) {
        return new BackupProperties(
                backupDir.toString(),
                retentionDays,
                new BackupProperties.DatabaseBackupProperties(null, "gastroai", "gastroai-pw", "gastroai", 5432),
                new BackupProperties.DatabaseBackupProperties(null, "gastroai", "gastroai-admin-pw", "gastroai_admin", 3306));
    }

    // Instant.now() that "age" dua tren, de test khong phu thuoc thoi diem chay test thuc te.
    private static final Instant NOW = Instant.parse("2026-10-04T00:00:00Z");

    private void setAge(Path file, int daysOld) throws IOException {
        Files.setLastModifiedTime(
                file,
                java.nio.file.attribute.FileTime.from(NOW.minus(daysOld, ChronoUnit.DAYS)));
    }

    @Test
    void deletesBackupFileOlderThanRetention(@TempDir Path backupDir) throws Exception {
        Path oldFile = backupDir.resolve("postgres_20260101_000000.sql");
        Files.writeString(oldFile, "-- cu");
        setAge(oldFile, 20);

        BackupScheduler scheduler = new BackupScheduler(backupProperties(backupDir, 14));
        scheduler.cleanupOldBackups(NOW);

        assertFalse(Files.exists(oldFile));
    }

    @Test
    void keepsBackupFileWithinRetention(@TempDir Path backupDir) throws Exception {
        Path recentFile = backupDir.resolve("postgres_20261001_000000.sql");
        Files.writeString(recentFile, "-- moi");
        setAge(recentFile, 3);

        BackupScheduler scheduler = new BackupScheduler(backupProperties(backupDir, 14));
        scheduler.cleanupOldBackups(NOW);

        assertTrue(Files.exists(recentFile));
    }

    @Test
    void keepsNonBackupSqlFileRegardlessOfAge(@TempDir Path backupDir) throws Exception {
        Path noteFile = backupDir.resolve("ghi-chu.sql");
        Files.writeString(noteFile, "-- khong phai file backup");
        setAge(noteFile, 20);

        BackupScheduler scheduler = new BackupScheduler(backupProperties(backupDir, 14));
        scheduler.cleanupOldBackups(NOW);

        assertTrue(Files.exists(noteFile));
    }

    @Test
    void doesNothingWhenBackupDirDoesNotExist(@TempDir Path backupDir) {
        Path missingDir = backupDir.resolve("khong-ton-tai");
        BackupScheduler scheduler = new BackupScheduler(backupProperties(missingDir, 14));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> scheduler.cleanupOldBackups(NOW));
    }

    @Test
    void deletesNothingWhenRetentionDaysIsZero(@TempDir Path backupDir) throws Exception {
        Path oldFile = backupDir.resolve("postgres_20260101_000000.sql");
        Files.writeString(oldFile, "-- cu");
        setAge(oldFile, 20);

        BackupScheduler scheduler = new BackupScheduler(backupProperties(backupDir, 0));
        scheduler.cleanupOldBackups(NOW);

        assertTrue(Files.exists(oldFile));
    }
}