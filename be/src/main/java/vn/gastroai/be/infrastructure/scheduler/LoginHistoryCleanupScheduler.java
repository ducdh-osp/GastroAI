package vn.gastroai.be.infrastructure.scheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.auth.LoginHistoryCleanupService;
@Component public class LoginHistoryCleanupScheduler {
 private final LoginHistoryCleanupService service; public LoginHistoryCleanupScheduler(LoginHistoryCleanupService s){service=s;}
 @Scheduled(cron="${app.security.login.cleanup-cron:0 15 3 * * *}",zone="UTC") public void cleanup(){service.cleanupPatients();service.cleanupAdmins();}
}
