package vn.gastroai.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình cho UC0057/58 (backup/restore), đọc từ app.backup.* trong application.yml. */
@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
        String directory,
        int retentionDays,
        DatabaseBackupProperties postgres,
        DatabaseBackupProperties mysql
) {

 
   public record DatabaseBackupProperties(
            String container,
            String username,
            String password,
            String database,
            int port
    ) {
    }
}