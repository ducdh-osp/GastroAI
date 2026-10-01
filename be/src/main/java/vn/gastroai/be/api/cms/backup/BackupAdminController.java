package vn.gastroai.be.api.cms.backup;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.infrastructure.backup.RestoreService;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/cms/backup")
public class BackupAdminController {

    private final RestoreService restoreService;

    public BackupAdminController(RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, String>> restore() {
        restoreService.restoreAll();
        return ResponseEntity.ok(Map.of(
                "message",
                "Da chay xong tien trinh restore - kiem tra file log ./logs/gastroai.log de biet ket qua tung CSDL"));
    }
}