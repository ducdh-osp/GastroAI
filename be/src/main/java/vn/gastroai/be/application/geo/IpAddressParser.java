package vn.gastroai.be.application.geo;

import java.util.Locale;

/**
 * Phân tích chuỗi IP (IPv4 dạng a.b.c.d, IPv6 kể cả "::" rút gọn, IPv4 nhúng cuối và zone "%eth0")
 * thành mảng byte — tự viết, KHÔNG dùng InetAddress.getByName để chuỗi tuỳ ý (vd tên miền) không bao
 * giờ gây ra 1 lần tra DNS. Chuỗi không hợp lệ -> null.
 */
final class IpAddressParser {

    private static final int MAX_LENGTH = 64;

    private IpAddressParser() {
    }

    static byte[] parse(String value) {
        if (value == null) {
            return null;
        }
        String ip = value.trim();
        if (ip.isEmpty() || ip.length() > MAX_LENGTH || ip.equalsIgnoreCase("unknown")) {
            return null;
        }
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        return ip.indexOf(':') >= 0 ? parseV6(ip) : parseV4(ip);
    }

    static byte[] parseV4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            int number = 0;
            for (int c = 0; c < part.length(); c++) {
                char ch = part.charAt(c);
                if (ch < '0' || ch > '9') {
                    return null;
                }
                number = number * 10 + (ch - '0');
            }
            if (number > 255) {
                return null;
            }
            bytes[i] = (byte) number;
        }
        return bytes;
    }

    static byte[] parseV6(String input) {
        String ip = input;
        int zone = ip.indexOf('%');
        if (zone >= 0) {
            ip = ip.substring(0, zone);
        }
        int doubleColon = ip.indexOf("::");
        if (doubleColon >= 0 && ip.indexOf("::", doubleColon + 1) >= 0) {
            return null;
        }
        String head = doubleColon >= 0 ? ip.substring(0, doubleColon) : ip;
        String tail = doubleColon >= 0 ? ip.substring(doubleColon + 2) : "";

        int[] headGroups = groups(head, doubleColon < 0);
        int[] tailGroups = doubleColon >= 0 ? groups(tail, true) : new int[0];
        if (headGroups == null || tailGroups == null) {
            return null;
        }
        int total = headGroups.length + tailGroups.length;
        if (doubleColon < 0 ? total != 8 : total > 7) {
            return null;
        }

        byte[] bytes = new byte[16];
        for (int i = 0; i < headGroups.length; i++) {
            bytes[i * 2] = (byte) (headGroups[i] >>> 8);
            bytes[i * 2 + 1] = (byte) headGroups[i];
        }
        int offset = 16 - tailGroups.length * 2;
        for (int i = 0; i < tailGroups.length; i++) {
            bytes[offset + i * 2] = (byte) (tailGroups[i] >>> 8);
            bytes[offset + i * 2 + 1] = (byte) tailGroups[i];
        }
        return bytes;
    }

    /**
     * Tách phần IPv6 (giữa các dấu ":") thành các nhóm 16 bit. {@code allowIpv4Suffix}: nhóm cuối được
     * phép là IPv4 a.b.c.d (tính là 2 nhóm). Chuỗi rỗng -> 0 nhóm.
     */
    private static int[] groups(String part, boolean allowIpv4Suffix) {
        if (part.isEmpty()) {
            return new int[0];
        }
        String[] tokens = part.split(":", -1);
        boolean ipv4Suffix = allowIpv4Suffix && tokens[tokens.length - 1].indexOf('.') >= 0;
        int count = tokens.length + (ipv4Suffix ? 1 : 0);
        int[] result = new int[count];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (ipv4Suffix && i == tokens.length - 1) {
                byte[] v4 = parseV4(token);
                if (v4 == null) {
                    return null;
                }
                result[i] = ((v4[0] & 0xFF) << 8) | (v4[1] & 0xFF);
                result[i + 1] = ((v4[2] & 0xFF) << 8) | (v4[3] & 0xFF);
                continue;
            }
            if (token.isEmpty() || token.length() > 4) {
                return null;
            }
            int value = 0;
            for (int c = 0; c < token.length(); c++) {
                int digit = Character.digit(token.toLowerCase(Locale.ROOT).charAt(c), 16);
                if (digit < 0) {
                    return null;
                }
                value = (value << 4) | digit;
            }
            result[i] = value;
        }
        return result;
    }

    /** ::ffff:a.b.c.d — byte 0..9 = 0, byte 10..11 = 0xFF. */
    static boolean isIpv4Mapped(byte[] v6) {
        for (int i = 0; i < 10; i++) {
            if (v6[i] != 0) {
                return false;
            }
        }
        return (v6[10] & 0xFF) == 0xFF && (v6[11] & 0xFF) == 0xFF;
    }

    /**
     * IP nội bộ: IPv4 127/8, 10/8, 172.16/12, 192.168/16, 169.254/16; IPv6 ::1, fc00::/7 (ULA), fe80::/10.
     */
    static boolean isLocal(byte[] address) {
        if (address.length == 4) {
            int a = address[0] & 0xFF;
            int b = address[1] & 0xFF;
            return a == 127 || a == 10
                    || (a == 172 && b >= 16 && b <= 31)
                    || (a == 192 && b == 168)
                    || (a == 169 && b == 254);
        }
        boolean loopback = true;
        for (int i = 0; i < 15; i++) {
            if (address[i] != 0) {
                loopback = false;
                break;
            }
        }
        if (loopback && address[15] == 1) {
            return true;
        }
        int first = address[0] & 0xFF;
        int second = address[1] & 0xFF;
        return (first & 0xFE) == 0xFC || (first == 0xFE && (second & 0xC0) == 0x80);
    }
}
