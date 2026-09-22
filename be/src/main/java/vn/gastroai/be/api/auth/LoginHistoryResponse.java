package vn.gastroai.be.api.auth;

import vn.gastroai.be.application.readmodel.LoginHistoryItem;
import java.util.List;

public record LoginHistoryResponse(
        List<LoginHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
