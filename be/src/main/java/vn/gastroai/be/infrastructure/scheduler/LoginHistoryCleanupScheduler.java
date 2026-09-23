package vn.gastroai.be.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.auth.LoginHistoryCleanupService;

/** Chạy hằng đêm (mặc định 3h15 UTC) để xoá bớt lịch sử đăng nhập quá cũ, tránh bảng phình vô hạn. */
@Component
public class LoginHistoryCleanupScheduler {
    private final LoginHistoryCleanupService service;

    public LoginHistoryCleanupScheduler(LoginHistoryCleanupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.security.login.cleanup-cron:0 15 3 * * *}", zone = "UTC")
    public void cleanup() {
        service.cleanupPatients();
    }
}
