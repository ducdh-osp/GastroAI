package vn.gastroai.be.api.auth;

import vn.gastroai.be.application.readmodel.LoginHistoryItem;
import java.util.List;

/** UC0007 - response phân trang cho GET /me/login-history. */
public record LoginHistoryResponse(
        List<LoginHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
