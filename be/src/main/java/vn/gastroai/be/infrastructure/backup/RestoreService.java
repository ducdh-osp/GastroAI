package vn.gastroai.be.infrastructure.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.gastroai.be.config.BackupProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * UC0058 - Khôi phục dữ liệu từ bản backup mới nhất do BackupScheduler tạo ra. Có backup
 * mà chưa từng thử restore thì không chắc backup đó dùng được — service này tồn tại để
 * kiểm chứng, không chỉ chạy 1 chiều "cứ dump ra rồi thôi".
 */
@Service
public class RestoreService {

    private static final Logger log =
            LoggerFactory.getLogger(RestoreService.class);

    private final BackupProperties backupProperties;

    public RestoreService(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    public void restoreAll() {
        try {
            restorePostgres();
        } catch (Exception e) {
            log.error("PostgreSQL restore failed", e);
        }

        try {
            restoreMysql();
        } catch (Exception e) {
            log.error("MySQL restore failed", e);
        }

        log.info("Database restore process completed.");
    }

    // psql có sẵn flag -f để chạy trực tiếp 1 file .sql, và -v ON_ERROR_STOP=1 để dừng ngay
    // nếu 1 câu lệnh trong file lỗi (mặc định psql chạy tiếp các câu sau, dễ restore dở dang
    // mà không ai biết).
    private void restorePostgres() {
        Path backupFile = findLatestBackup("postgres_");

        if (backupFile == null) {
            log.info("No PostgreSQL backup found. Skip restore.");
            return;
        }

        var postgres = backupProperties.postgres();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "psql",
                "-h",
                "localhost",
                "-p",
                String.valueOf(postgres.port()),
                "-U",
                postgres.username(),
                "-d",
                postgres.database(),
                "-v",
                "ON_ERROR_STOP=1",
                "-f",
                backupFile.toString()
        );

        processBuilder.environment().put(
                "PGPASSWORD",
                postgres.password()
        );

        runProcess(
                processBuilder,
                "PostgreSQL",
                backupFile
        );
    }

    // mysql CLI không có flag "-f file.sql" như psql — cách chạy 1 script SQL là pipe nội
    // dung file vào stdin của process, nên phải đọc file vào bộ nhớ rồi ghi qua
    // process.getOutputStream() (đây là stdin của process con) thay vì dùng runProcess() chung.
    private void restoreMysql() {
        Path backupFile = findLatestBackup("mysql_");

        if (backupFile == null) {
            log.info("No MySQL backup found. Skip restore.");
            return;
        }

        var mysql = backupProperties.mysql();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysql",
                "-h",
                "localhost",
                "-P",
                String.valueOf(mysql.port()),
                "-u",
                mysql.username(),
                mysql.database()
        );

        processBuilder.environment().put(
                "MYSQL_PWD",
                mysql.password()
        );

        try {
            String sql = Files.readString(
                    backupFile,
                    StandardCharsets.UTF_8
            );

            processBuilder.redirectError(
                    ProcessBuilder.Redirect.PIPE
            );

            Process process = processBuilder.start();

            process.getOutputStream().write(
                    sql.getBytes(StandardCharsets.UTF_8)
            );

            process.getOutputStream().close();

            int exitCode = process.waitFor();

            String error = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "MySQL restore failed: " + error
                );
            }

            log.info(
                    "MySQL restore completed from: {}",
                    backupFile.toAbsolutePath()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "MySQL restore was interrupted",
                    e
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "MySQL restore failed",
                    e
            );
        }
    }

    // Tên file backup có dạng "postgres_yyyyMMdd_HHmmss.sql" (xem BackupScheduler) nên so
    // sánh chuỗi tên file theo thứ tự chữ cái cũng chính là so sánh theo thời gian — không
    // cần đọc timestamp thật của file trên đĩa.
    private Path findLatestBackup(String prefix) {
        try (var files = Files.list(
                Path.of(backupProperties.directory())
        )) {

            return files
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .startsWith(prefix)
                    )
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith(".sql")
                    )
                    .max((a, b) ->
                            a.getFileName()
                                    .toString()
                                    .compareTo(
                                            b.getFileName().toString()
                                    )
                    )
                    .orElse(null);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot find latest backup",
                    e
            );
        }
    }

    private void runProcess(
            ProcessBuilder processBuilder,
            String databaseName,
            Path backupFile) {

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            String error = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (exitCode != 0) {
                throw new IllegalStateException(
                        databaseName
                                + " restore failed: "
                                + error
                );
            }

            log.info(
                    "{} restore completed from: {}",
                    databaseName,
                    backupFile.toAbsolutePath()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    databaseName + " restore was interrupted",
                    e
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    databaseName + " restore failed",
                    e
            );
        }
    }
}