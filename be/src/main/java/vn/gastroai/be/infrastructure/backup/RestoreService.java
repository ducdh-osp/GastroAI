package vn.gastroai.be.infrastructure.backup;

import org.springframework.stereotype.Service;
import vn.gastroai.be.config.BackupProperties;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RestoreService {

    private final BackupProperties backupProperties;

    public RestoreService(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    public void restoreAll() {
        try {
            restorePostgres();
        } catch (Exception e) {
            System.err.println("PostgreSQL restore failed:");
            e.printStackTrace();
        }

        try {
            restoreMysql();
        } catch (Exception e) {
            System.err.println("MySQL restore failed:");
            e.printStackTrace();
        }

        System.out.println("Database restore process completed.");
    }

    private void restorePostgres() {
        Path backupFile = findLatestBackup("postgres_");

        if (backupFile == null) {
            System.out.println("No PostgreSQL backup found. Skip restore.");
            return;
        }

        var postgres = backupProperties.postgres();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "psql",
                "-h",
                "localhost",
                "-p",
                "5432",
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

        runProcess(processBuilder, "PostgreSQL", backupFile);
    }

    private void restoreMysql() {
        Path backupFile = findLatestBackup("mysql_");

        if (backupFile == null) {
            System.out.println("No MySQL backup found. Skip restore.");
            return;
        }

        var mysql = backupProperties.mysql();

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysql",
                "-h",
                "localhost",
                "-P",
                "3306",
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

            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);

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

            System.out.println(
                    "MySQL restore completed from: "
                            + backupFile.toAbsolutePath()
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

            System.out.println(
                    databaseName
                            + " restore completed from: "
                            + backupFile.toAbsolutePath()
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