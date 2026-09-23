package vn.gastroai.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình cho UC0057/58 (backup/restore), đọc từ app.backup.* trong application.yml. */
@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
        String directory,
        DatabaseBackupProperties postgres,
        DatabaseBackupProperties mysql
) {

   // port đọc từ ${POSTGRES_PORT:5432}/${MYSQL_PORT:3306} trong application.yml — không
   // hardcode, vì mỗi máy trong nhóm có thể chạy Postgres/MySQL ở cổng khác nhau.
   public record DatabaseBackupProperties(
            String container,
            String username,
            String password,
            String database,
            int port
    ) {
    }
}