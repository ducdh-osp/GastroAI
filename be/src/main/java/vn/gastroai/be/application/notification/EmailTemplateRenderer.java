package vn.gastroai.be.application.notification;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer nho, co whitelist placeholder. Khong dung SpEL/Thymeleaf cho noi dung lay tu DB
 * de Admin khong the vo tinh chen bieu thuc co kha nang thuc thi tren server.
 */
@Component
public class EmailTemplateRenderer {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9]*)\\s*}}", Pattern.MULTILINE);
    private static final Safelist SAFE_EMAIL_HTML = Safelist.relaxed()
            .removeProtocols("a", "href", "ftp", "mailto");
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_BODY_LENGTH = 100_000;

    public void validate(EmailTemplateCode code, EmailTemplateContent content) {
        if (content == null) {
            throw new IllegalArgumentException("Nội dung mẫu email không được để trống");
        }
        requireText(content.subjectTemplate(), "Tiêu đề email");
        requireText(content.htmlBodyTemplate(), "Nội dung HTML");
        requireText(content.textBodyTemplate(), "Nội dung text");
        if (content.subjectTemplate().length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Tiêu đề email tối đa 255 ký tự");
        }
        if (content.subjectTemplate().contains("\r") || content.subjectTemplate().contains("\n")) {
            throw new IllegalArgumentException("Tiêu đề email không được chứa ký tự xuống dòng");
        }
        if (content.htmlBodyTemplate().length() > MAX_BODY_LENGTH
                || content.textBodyTemplate().length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Nội dung email tối đa 100.000 ký tự");
        }

        validateVariables(code, "tiêu đề", content.subjectTemplate(), false);
        validateVariables(code, "nội dung HTML", content.htmlBodyTemplate(), true);
        validateVariables(code, "nội dung text", content.textBodyTemplate(), true);

        // Validate the rendered sample so URL placeholders contain a real safe protocol. Jsoup
        // parses character references before checking the safelist, which also blocks encoded
        // protocol bypasses such as javascript&#58; that a blacklist regex cannot detect.
        String sampleHtml = replace(content.htmlBodyTemplate(), code.sampleVariables(), true);
        if (!Jsoup.isValid(sampleHtml, SAFE_EMAIL_HTML)) {
            throw new IllegalArgumentException("Nội dung HTML chứa thẻ hoặc thuộc tính không an toàn");
        }
    }

    public RenderedEmail render(EmailTemplateCode code, EmailTemplateContent content,
                                Map<String, String> variables) {
        validate(code, content);
        Set<String> missingValues = new HashSet<>(variablesIn(content.subjectTemplate()));
        missingValues.addAll(variablesIn(content.htmlBodyTemplate()));
        missingValues.addAll(variablesIn(content.textBodyTemplate()));
        missingValues.removeAll(variables.keySet());
        if (!missingValues.isEmpty()) {
            throw new IllegalArgumentException("Thiếu giá trị cho placeholder: " + String.join(", ", missingValues));
        }

        String subject = replace(content.subjectTemplate(), variables, false)
                .replace('\r', ' ').replace('\n', ' ');
        // Always serialize the allowlisted output as a second line of defence, including for
        // templates already stored before allowlist validation was introduced.
        String html = Jsoup.clean(replace(content.htmlBodyTemplate(), variables, true), SAFE_EMAIL_HTML);
        String text = replace(content.textBodyTemplate(), variables, false);
        return new RenderedEmail(subject, html, text);
    }

    private void validateVariables(EmailTemplateCode code, String field, String template,
                                   boolean requireMandatoryVariables) {
        Set<String> variables = variablesIn(template);
        Set<String> unknown = new HashSet<>(variables);
        unknown.removeAll(code.allowedVariables());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Placeholder không hợp lệ trong " + field + ": "
                    + String.join(", ", unknown));
        }
        if (requireMandatoryVariables) {
            Set<String> missing = new HashSet<>(code.requiredVariables());
            missing.removeAll(variables);
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Thiếu placeholder bắt buộc trong " + field + ": "
                        + String.join(", ", missing));
            }
        }
    }

    private Set<String> variablesIn(String template) {
        Set<String> result = new HashSet<>();
        Matcher matcher = VARIABLE.matcher(template);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private String replace(String template, Map<String, String> variables, boolean escapeHtml) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = variables.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(escapeHtml ? escapeHtml(value) : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " không được để trống");
        }
    }
}
