package vn.gastroai.be.application.notification;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        String safeHtml = Jsoup.clean(replace(content.htmlBodyTemplate(), variables, true), SAFE_EMAIL_HTML);
        String html = applyGastroAiLayout(code, safeHtml);
        String text = replace(content.textBodyTemplate(), variables, false);
        return new RenderedEmail(subject, html, text);
    }

    /**
     * Giao diện cố định cho email tài khoản. CSS chỉ được hệ thống thêm sau bước sanitize;
     * nội dung do CMS quản lý vẫn không được phép tự chèn style/script. Dùng table và inline
     * style để hiển thị ổn định trên Gmail desktop lẫn mobile.
     */
    private String applyGastroAiLayout(EmailTemplateCode code, String safeHtml) {
        if (code != EmailTemplateCode.ACCOUNT_VERIFICATION
                && code != EmailTemplateCode.PASSWORD_RESET) {
            return safeHtml;
        }

        Document content = Jsoup.parseBodyFragment(safeHtml);
        content.outputSettings().prettyPrint(false);
        content.select("h2").attr("style",
                "margin:0 0 18px;color:#0f172a;font-size:24px;line-height:1.3;font-weight:700;");
        content.select("p").attr("style",
                "margin:0 0 16px;color:#475569;font-size:15px;line-height:1.7;");
        content.select("strong").attr("style", "color:#0f172a;font-weight:700;");
        content.select("small").attr("style", "color:#64748b;font-size:13px;line-height:1.6;");
        content.select("a").attr("style",
                "display:inline-block;padding:12px 22px;background-color:#0f766e;color:#ffffff!important;"
                        + "font-size:15px;font-weight:700;line-height:1.2;text-decoration:none;border-radius:8px;");

        String preheader = code == EmailTemplateCode.ACCOUNT_VERIFICATION
                ? "Xác thực email để bắt đầu sử dụng tài khoản GastroAI."
                : "Sử dụng liên kết bảo mật để đặt lại mật khẩu GastroAI.";
        String typeLabel = code == EmailTemplateCode.ACCOUNT_VERIFICATION
                ? "XÁC THỰC TÀI KHOẢN"
                : "BẢO MẬT TÀI KHOẢN";

        return """
                <!doctype html>
                <html lang="vi">
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:Arial,Helvetica,sans-serif;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">%s</div>
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="width:100%%;background-color:#f1f5f9;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:600px;background-color:#ffffff;border:1px solid #e2e8f0;border-radius:14px;">
                          <tr>
                            <td style="padding:24px 28px;background-color:#0f766e;border-radius:14px 14px 0 0;">
                              <table role="presentation" cellspacing="0" cellpadding="0" border="0">
                                <tr>
                                  <td style="width:42px;height:42px;text-align:center;background-color:#ffffff;color:#0f766e;font-size:21px;font-weight:700;border-radius:10px;">G</td>
                                  <td style="padding-left:12px;color:#ffffff;">
                                    <div style="font-size:20px;font-weight:700;line-height:1.2;">GastroAI</div>
                                    <div style="margin-top:3px;font-size:11px;letter-spacing:1.2px;opacity:.9;">%s</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px 28px 24px;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:12px;line-height:1.6;">
                              Đây là email tự động từ GastroAI. Vui lòng không trả lời email này.<br>
                              Thông tin sức khỏe trên GastroAI chỉ mang tính tham khảo.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(preheader, typeLabel, content.body().html());
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
