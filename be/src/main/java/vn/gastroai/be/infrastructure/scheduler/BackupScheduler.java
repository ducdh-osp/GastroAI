package vn.gastroai.be.infrastructure.scheduler;

import org.springframework.stereotype.Component;
import vn.gastroai.be.config.BackupProperties;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BackupScheduler {

    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final BackupProperties backupProperties;

    public BackupScheduler(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Ho_Chi_Minh") // Run daily at 2 AM
    public void backupAll() {
        ensureBackupDirExists();

        System.out.println("Starting database backup...");

        try {
            backupPostgres();
        } catch (Exception e) {
            System.err.println("PostgreSQL backup failed:");
            e.printStackTrace();
        }

        try {
            backupMysql();
        } catch (Exception e) {
            System.err.println("MySQL backup failed:");
            e.printStackTrace();
        }

        System.out.println("Database backup process completed.");
    }

    private Path getBackupDir() {
        return Path.of(backupProperties.directory());
    }

    private void ensureBackupDirExists() {
        try {
            Files.createDirectories(getBackupDir());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot create backup directory",
                    e);
        }
    }

    private void backupPostgres() {
        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);

        Path outputFile = getBackupDir()
                .resolve("postgres_" + timestamp + ".sql");

        var postgres = backupProperties.postgres();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_dump",
                "-h",
                "localhost",
                "-p",
                "5432",
                "-U",
                postgres.username(),
                "-d",
                postgres.database());

        runBackupProcess(
                processBuilder,
                outputFile,
                "PostgreSQL");
    }

    private void backupMysql() {
        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);

        Path outputFile = getBackupDir()
                .resolve("mysql_" + timestamp + ".sql");

        var mysql = backupProperties.mysql();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump",
                "-h",
                "localhost",
                "-P",
                "3306",
                "-u",
                mysql.username(),
                "--no-tablespaces",
                mysql.database());

        runBackupProcess(
                processBuilder,
                outputFile,
                "MySQL");
    }

    private void runBackupProcess(
            ProcessBuilder processBuilder,
            Path outputFile,
            String databaseName) {

        Process process = null;

        try {
            process = processBuilder.start();

            Process currentProcess = process;

            Thread errorReader = new Thread(() -> {
                try {
                    currentProcess.getErrorStream().transferTo(
                            System.err);
                } catch (Exception ignored) {
                    // Error stream will be handled after process completion.
                }
            });

            errorReader.start();

            try (InputStream input = process.getInputStream()) {
                Files.copy(input, outputFile);
            }

            int exitCode = process.waitFor();

            errorReader.join();

            if (exitCode != 0) {
                deleteBackupFile(outputFile);

                throw new IllegalStateException(
                        databaseName
                                + " backup failed with exit code "
                                + exitCode);
            }

            System.out.println(
                    databaseName
                            + " backup completed: "
                            + outputFile.toAbsolutePath());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            deleteBackupFile(outputFile);

            throw new IllegalStateException(
                    databaseName + " backup was interrupted",
                    e);

        } catch (Exception e) {
            deleteBackupFile(outputFile);

            throw new IllegalStateException(
                    databaseName + " backup failed",
                    e);
        }
    }

    private void deleteBackupFile(Path outputFile) {
        try {
            Files.deleteIfExists(outputFile);
        } catch (Exception e) {
            System.err.println(
                    "Cannot delete failed backup file: "
                            + outputFile);
        }
    }
}