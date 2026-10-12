package vn.gastroai.be.api.cms.backup;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.infrastructure.backup.RestoreResult;
import vn.gastroai.be.infrastructure.backup.RestoreService;

import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/cms/backup")
public class BackupAdminController {

    private final RestoreService restoreService;

    public BackupAdminController(RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restore() {
        RestoreResult result = restoreService.restoreAll();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("postgresOk", result.postgresOk());
        body.put("mysqlOk", result.mysqlOk());
        if (result.postgresError() != null) {
            body.put("postgresError", result.postgresError());
        }
        if (result.mysqlError() != null) {
            body.put("mysqlError", result.mysqlError());
        }
        body.put(
                "message",
                result.allOk()
                        ? "Restore thanh cong ca 2 CSDL."
                        : "Restore that bai mot phan - xem postgresError/mysqlError o tren hoac file log ./logs/gastroai.log de biet chi tiet."
        );

        if (result.allOk()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}