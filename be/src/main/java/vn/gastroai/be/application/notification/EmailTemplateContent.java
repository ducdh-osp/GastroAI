package vn.gastroai.be.application.notification;

public record EmailTemplateContent(
        String subjectTemplate,
        String htmlBodyTemplate,
        String textBodyTemplate) {
}
