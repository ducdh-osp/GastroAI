package vn.gastroai.be.infrastructure.scheduler;

import org.springframework.stereotype.Component;
import vn.gastroai.be.config.BackupProperties;
import org.springframework.scheduling.annotation.Scheduled;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class BackupScheduler {

    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Logger logger = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupProperties backupProperties;

    public BackupScheduler(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    // 1 lỗi ở CSDL này không được làm hỏng backup của CSDL kia — mỗi nhánh try/catch riêng.
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Ho_Chi_Minh") // Run daily at 2 AM
    public void backupAll() {
        ensureBackupDirExists();

        logger.info("Starting database backup...");

        try {
            backupPostgres();
        } catch (Exception e) {
            logger.error("PostgreSQL backup failed:", e);
            e.printStackTrace();
        }

        try {
            backupMysql();
        } catch (Exception e) {
            logger.error("MySQL backup failed:", e);
            e.printStackTrace();
        }

        // Bug fix (audit) - bo sung buoc don file backup cu, tranh tich luy vo han theo thoi
        // gian co the lam day o dia neu chay lau dai. Chay rieng try/catch - loi khi don dep
        // khong duoc anh huong toi backup vua tao thanh cong o tren.
        try {
            cleanupOldBackups();
        } catch (Exception e) {
            logger.error("Backup cleanup failed:", e);
        }

        logger.info("Database backup process completed.");
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
                postgres.port() + "",
                "-U",
                postgres.username(),
                "-d",
                postgres.database());
        // pg_dump không có flag nhập mật khẩu trực tiếp — cách chuẩn là set biến môi trường
        // PGPASSWORD cho riêng process con này (không set ở env hệ thống, tránh lộ mật khẩu
        // cho các process khác/khi liệt kê env toàn hệ thống).
        processBuilder.environment().put(
        "PGPASSWORD",
        postgres.password()
        );

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
                mysql.port() + "",
                "-u",
                mysql.username(),
                "--no-tablespaces",
                mysql.database());
        // Tương tự PGPASSWORD ở trên nhưng cho mysqldump (biến MYSQL_PWD).
        processBuilder.environment().put(
        "MYSQL_PWD",
        mysql.password());
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
                // Lệnh dump thất bại giữa chừng vẫn có thể để lại file .sql dở dang trên
                // đĩa — xoá đi để không nhầm lẫn với 1 bản backup hợp lệ khi restore sau này.
                deleteBackupFile(outputFile);

                throw new IllegalStateException(
                        databaseName
                                + " backup failed with exit code "
                                + exitCode);
            }

            logger.info(
                    "{} backup completed: {}",
                    databaseName,
                    outputFile.toAbsolutePath());

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
            logger.error(
            "Cannot delete failed backup file: {}",
            outputFile, e);
        }
    }

 
    private void cleanupOldBackups() throws Exception {
        Instant cutoff = Instant.now().minus(backupProperties.retentionDays(), ChronoUnit.DAYS);

        try (var files = Files.list(getBackupDir())) {
            files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .forEach(path -> {
                        try {
                            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                            if (lastModified.isBefore(cutoff)) {
                                Files.delete(path);
                                logger.info("Deleted old backup file: {}", path.toAbsolutePath());
                            }
                        } catch (Exception e) {
                            logger.error("Cannot check/delete old backup file: {}", path, e);
                        }
                    });
        }
    }
}