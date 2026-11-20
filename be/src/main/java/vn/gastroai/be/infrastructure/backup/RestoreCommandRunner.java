package vn.gastroai.be.infrastructure.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.domain.audit.AuditAction;

/**
 * UC0058 - khôi phục dữ liệu từ bản sao lưu mới nhất. Không có API hay nút bấm trên web:
 * người vận hành dừng hệ thống rồi chạy trực tiếp trên máy chủ
 * {@code java -jar be.jar --app.restore.run=true --server.port=0}.
 * Chạy xong thì tự thoát, mã thoát 0 là khôi phục thành công cả hai cơ sở dữ liệu.
 */
@Component
@ConditionalOnProperty(name = "app.restore.run", havingValue = "true")
public class RestoreCommandRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RestoreCommandRunner.class);

    private final RestoreService restoreService;
    private final AuditLogService auditLogService;
    private final ApplicationContext context;

    public RestoreCommandRunner(RestoreService restoreService, AuditLogService auditLogService,
            ApplicationContext context) {
        this.restoreService = restoreService;
        this.auditLogService = auditLogService;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode = restore();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    /** Khôi phục cả hai cơ sở dữ liệu, ghi nhật ký và trả về mã thoát: 0 thành công, 1 có lỗi. */
    int restore() {
        log.warn("Che do khoi phuc: ghi de ca PostgreSQL va MySQL bang ban sao luu moi nhat.");
        RestoreResult result = restoreService.restoreAll();
        auditLogService.record(AuditAction.SYSTEM_RESTORE, "SYSTEM", null,
                "postgresOk=" + result.postgresOk() + ", mysqlOk=" + result.mysqlOk() + ", chay bang lenh tren may chu");

        if (result.allOk()) {
            log.info("Khoi phuc thanh cong ca 2 CSDL.");
            return 0;
        }
        log.error("Khoi phuc that bai mot phan. postgresError={}, mysqlError={}",
                result.postgresError(), result.mysqlError());
        return 1;
    }
}