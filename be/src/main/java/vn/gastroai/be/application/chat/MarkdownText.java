package vn.gastroai.be.application.chat;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Chuyen cau tra loi AI dang Markdown thanh chu thuong de in vao PDF (UC0027): bo ky hieu
 * dam/nghieng/de muc/gach dau dong/duong ke/bang, giu nguyen noi dung chu.
 */
public final class MarkdownText {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*+]\\s+(.*)$");
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("^\\s*(?:-{3,}|\\*{3,}|_{3,})\\s*$");
    private static final Pattern TABLE_SEPARATOR =
            Pattern.compile("^\\s*\\|?(\\s*:?-{3,}:?\\s*\\|)+\\s*(:?-{3,}:?\\s*)?$");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern ITALIC =
            Pattern.compile("(?<![\\p{L}\\p{N}*])\\*(?=\\S)([^*\\n]*?\\S)\\*(?![\\p{L}\\p{N}*])");
    private static final Pattern ESCAPED = Pattern.compile("\\\\([\\\\`*_#+\\-|\\[\\]()!.])");
    private static final char ESCAPE_BASE = '\uE000';

    private MarkdownText() {
    }

    public static String toPlain(String markdown) {
        if (markdown == null) {
            return "";
        }
        return Stream.of(markdown.split("\n", -1))
                .map(MarkdownText::convertLine)
                .collect(Collectors.joining("\n"));
    }

    private static String convertLine(String rawLine) {
        String line = hideEscapes(rawLine);
        String result;
        if (HORIZONTAL_RULE.matcher(line).matches() || TABLE_SEPARATOR.matcher(line).matches()) {
            result = "";
        } else if (line.trim().startsWith("|")) {
            result = convertTableRow(line);
        } else {
            Matcher heading = HEADING.matcher(line);
            Matcher bullet = BULLET.matcher(line);
            if (heading.matches()) {
                result = stripInline(heading.group(2));
            } else if (bullet.matches()) {
                result = bullet.group(1) + "• " + stripInline(bullet.group(2));
            } else {
                result = stripInline(line);
            }
        }
        return restoreEscapes(result);
    }

    /** "| a | **b** |" -> "a | b": bo 2 dau | ngoai cung, xu ly tung o roi noi lai. */
    private static String convertTableRow(String line) {
        String inner = line.trim().substring(1);
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        return Arrays.stream(inner.split("\\|", -1))
                .map(cell -> stripInline(cell.trim()))
                .collect(Collectors.joining(" | "));
    }

    /** Ky hieu markdown nam trong 1 dong: link, dam, nghieng, code. */
    private static String stripInline(String text) {
        String result = LINK.matcher(text).replaceAll("$1 ($2)");
        result = result.replace("**", "").replace("__", "");
        result = ITALIC.matcher(result).replaceAll("$1");
        result = result.replace("`", "");
        return result;
    }

    /** Doi "\*" thanh 1 ky tu tam (vung Unicode rieng) de cac regex phia sau khong dung toi. */
    private static String hideEscapes(String line) {
        Matcher matcher = ESCAPED.matcher(line);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            char hidden = (char) (ESCAPE_BASE + matcher.group(1).charAt(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(hidden)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Doi ky tu tam ve lai ky tu goc (khong con dau "\"). */
    private static String restoreEscapes(String line) {
        StringBuilder result = new StringBuilder(line.length());
        for (char c : line.toCharArray()) {
            boolean hidden = c >= ESCAPE_BASE && c < ESCAPE_BASE + 128;
            result.append(hidden ? (char) (c - ESCAPE_BASE) : c);
        }
        return result.toString();
    }
}