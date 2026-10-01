package vn.gastroai.be.infrastructure.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.gastroai.be.config.BackupProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


@Service
public class RestoreService {

    private static final Logger log =
            LoggerFactory.getLogger(RestoreService.class);

    private final BackupProperties backupProperties;

    public RestoreService(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    public RestoreResult restoreAll() {
        boolean postgresOk = true;
        String postgresError = null;
        try {
            restorePostgres();
        } catch (Exception e) {
            log.error("PostgreSQL restore failed", e);
            postgresOk = false;
            postgresError = e.getMessage();
        }

        boolean mysqlOk = true;
        String mysqlError = null;
        try {
            restoreMysql();
        } catch (Exception e) {
            log.error("MySQL restore failed", e);
            mysqlOk = false;
            mysqlError = e.getMessage();
        }

        log.info("Database restore process completed.");

        return new RestoreResult(postgresOk, postgresError, mysqlOk, mysqlError);
    }


    private void restorePostgres() {
        Path backupFile = findLatestBackup("postgres_");

        if (backupFile == null) {
            log.info("No PostgreSQL backup found. Skip restore.");
            return;
        }

        var postgres = backupProperties.postgres();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "psql",
                "-h", "localhost",
                "-p", postgres.port() + "",
                "-U", postgres.username(),
                "-d", postgres.database(),
                "-v", "ON_ERROR_STOP=1",
                "-f", backupFile.toString()
        );
        processBuilder.environment().put(
                "PGPASSWORD",
                postgres.password()
        );

        runProcess(
                processBuilder,
                "PostgreSQL"
        );
    }


    private void restoreMysql() {
        Path backupFile = findLatestBackup("mysql_");

        if (backupFile == null) {
            log.info("No MySQL backup found. Skip restore.");
            return;
        }

        var mysql = backupProperties.mysql();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysql",
                "-h", "localhost",
                "-P", mysql.port() + "",
                "-u", mysql.username(),
                mysql.database()
        );

        processBuilder.environment().put(
                "MYSQL_PWD",
                mysql.password());

        try {
            String sql = Files.readString(
                    backupFile,
                    StandardCharsets.UTF_8
            );

            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            
            StringBuilder errorOutput = new StringBuilder();
            Thread errorReader = new Thread(() -> {
                try {
                    errorOutput.append(new String(
                            process.getErrorStream().readAllBytes(),
                            StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                    
                }
            });
            errorReader.start();

            process.getOutputStream().write(
                    sql.getBytes(StandardCharsets.UTF_8)
            );

            process.getOutputStream().close();

            int exitCode = process.waitFor();
            errorReader.join();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "MySQL restore failed: " + errorOutput
                );
            }

            log.info("MySQL restore completed from {}", backupFile.getFileName());

        } catch (Exception e) {
            throw new IllegalStateException(
                    "MySQL restore failed",
                    e
            );
        }
    }


    private Path findLatestBackup(String prefix) {
        try (var files = Files.list(
                Path.of(backupProperties.directory())
        )) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .max(Path::compareTo)
                    .orElse(null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot list backup directory",
                    e
            );
        }
    }


    private void runProcess(
            ProcessBuilder processBuilder,
            String databaseName
    ) {
        processBuilder.redirectErrorStream(false);

        try {
            Process process = processBuilder.start();

            StringBuilder errorOutput = new StringBuilder();
            Thread errorReader = new Thread(() -> {
                try {
                    errorOutput.append(new String(
                            process.getErrorStream().readAllBytes(),
                            StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                    // Bo qua loi doc stderr - loi restore that (neu co) van duoc phat hien
                    // qua exitCode ben duoi.
                }
            });
            errorReader.start();

            int exitCode = process.waitFor();
            errorReader.join();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        databaseName
                                + " restore failed: "
                                + errorOutput
                );
            }

            log.info("{} restore completed.", databaseName);

        } catch (Exception e) {
            throw new IllegalStateException(
                    databaseName + " restore failed",
                    e
            );
        }
    }
}