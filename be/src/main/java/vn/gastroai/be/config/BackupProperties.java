package vn.gastroai.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
        String directory,
        DatabaseBackupProperties postgres,
        DatabaseBackupProperties mysql
) {

    public record DatabaseBackupProperties(
            String container,
            String username,
            String password,
            String database
    ) {
    }
}