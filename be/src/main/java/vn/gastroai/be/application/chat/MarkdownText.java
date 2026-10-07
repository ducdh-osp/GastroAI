package vn.gastroai.be.application.chat;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Chuyển câu trả lời AI dạng Markdown (**đậm**, ## đề mục, - gạch đầu dòng, [chữ](link)...)
 * thành chữ thường, để in vào PDF (UC0027) mà không lẫn ký hiệu markdown vào nội dung.
 */
public final class MarkdownText {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s*(.*)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*]\\s+(.*)$");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");

    private MarkdownText() {
    }

    public static String toPlain(String markdown) {
        if (markdown == null) return "";
        String[] lines = markdown.split("\n", -1);
        return Stream.of(lines)
                .map(MarkdownText::convertLine)
                .collect(Collectors.joining("\n"));
    }

    private static String convertLine(String line) {
        var heading = HEADING.matcher(line);
        if (heading.matches()) {
            return stripInline(heading.group(2));
        }
        var bullet = BULLET.matcher(line);
        if (bullet.matches()) {
            return bullet.group(1) + "• " + stripInline(bullet.group(2));
        }
        return stripInline(line);
    }

    /** Xử lý ký hiệu markdown nằm trong 1 dòng (không phải tiêu đề/gạch đầu dòng ở đầu dòng). */
    private static String stripInline(String text) {
        String result = LINK.matcher(text).replaceAll("$1 ($2)");
        result = result.replace("**", "").replace("__", "");
        result = result.replace("`", "");
        return result;
    }
}