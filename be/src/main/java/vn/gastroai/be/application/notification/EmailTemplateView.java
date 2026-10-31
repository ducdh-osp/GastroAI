package vn.gastroai.be.application.notification;

import java.time.Instant;
import java.util.List;

public record EmailTemplateView(
        String code,
        String displayName,
        String subjectTemplate,
        String htmlBodyTemplate,
        String textBodyTemplate,
        List<String> allowedVariables,
        long version,
        Instant updatedAt,
        String updatedByType,
        Long updatedById) {
}
