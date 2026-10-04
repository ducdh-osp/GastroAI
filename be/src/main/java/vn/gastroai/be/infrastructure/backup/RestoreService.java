package vn.gastroai.be.infrastructure.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.gastroai.be.config.BackupProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RestoreService {

    private static final Logger log =
            LoggerFactory.getLogger(RestoreService.class);

    private final BackupProperties backupProperties;
    private final ProcessExecutor processExecutor;

    public RestoreService(BackupProperties backupProperties, ProcessExecutor processExecutor) {
        this.backupProperties = backupProperties;
        this.processExecutor = processExecutor;
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
                "-q",
                "-v", "ON_ERROR_STOP=1",
                "-f", backupFile.toString()
        );
        processBuilder.environment().put(
                "PGPASSWORD",
                postgres.password()
        );

        processExecutor.run(processBuilder, "PostgreSQL restore", null);
        log.info("PostgreSQL restore completed from {}", backupFile.getFileName());
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

        byte[] sql;
        try {
            sql = Files.readAllBytes(backupFile);
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc file backup MySQL: " + backupFile, e);
        }

        processExecutor.run(processBuilder, "MySQL restore", sql);
        log.info("MySQL restore completed from {}", backupFile.getFileName());
    }


    private Path findLatestBackup(String prefix) {
        try (var files = Files.list(
                Path.of(backupProperties.directory())
        )) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .max(Path::compareTo)
                    .orElse(null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot list backup directory",
                    e
            );
        }
    }
}