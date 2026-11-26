package vn.gastroai.be.application.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Tra VỊ TRÍ ƯỚC TÍNH (quốc gia + tỉnh/bang) theo địa chỉ IP, hoàn toàn offline — dữ liệu DB-IP City
 * Lite (CC BY 4.0, xem resources/geoip/NOTICE.md) nằm sẵn trong classpath, nạp 1 lần lúc khởi động vào
 * mảng nguyên thuỷ, không gọi dịch vụ ngoài nào, không thư viện ngoài.
 *
 * <p>Định dạng 2 file {@code geoip/geoip-v4.bin.gz} và {@code geoip/geoip-v6.bin.gz} (big-endian, toàn
 * bộ file nén gzip):
 * <pre>
 *   magic   4 byte  "GIP4" | "GIP6"
 *   version 1 byte  = 1
 *   int32   nameCount; mỗi tên: uint16 độ dài + chuỗi UTF-8 dạng "MÃ_NƯỚC|Tỉnh"
 *                      (index 0 = "|" = không xác định)
 *   int32   entryCount
 *   entryCount giá trị start: IPv4 = uint32; IPv6 = 64 bit CAO của địa chỉ (long)
 *   entryCount giá trị uint16 nameIndex
 * </pre>
 * Tra cứu: tìm entry CUỐI CÙNG có start &lt;= ip (so sánh KHÔNG DẤU), lấy tên theo nameIndex. nameIndex 0
 * hoặc không có entry nào &lt;= ip = không xác định. IPv6 chỉ so 64 bit cao.
 *
 * <p>Vị trí chỉ là ƯỚC TÍNH: thường là nơi nhà mạng cấp IP, không phải vị trí thật của người dùng.
 */
@Service
public class IpLocationService {

    private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);

    static final String DEFAULT_V4_RESOURCE = "geoip/geoip-v4.bin.gz";
    static final String DEFAULT_V6_RESOURCE = "geoip/geoip-v6.bin.gz";

    public static final String LOCAL_NETWORK = "Mạng nội bộ";
    public static final String UNKNOWN = "Không xác định";

    private static final int FORMAT_VERSION = 1;
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");

    // null = file đó không nạp được -> locate() trả null cho họ địa chỉ tương ứng.
    private final String[] v4Names;
    private final int[] v4Starts;
    private final short[] v4NameIndexes;
    private final String[] v6Names;
    private final long[] v6Starts;
    private final short[] v6NameIndexes;

    private final Map<String, String> countryNames = new ConcurrentHashMap<>();

    public IpLocationService() {
        this(DEFAULT_V4_RESOURCE, DEFAULT_V6_RESOURCE);
    }

    /** Cho test: trỏ tới resource khác (vd không tồn tại) để kiểm tra trường hợp thiếu file. */
    IpLocationService(String v4Resource, String v6Resource) {
        long startNanos = System.nanoTime();
        Table v4 = load(v4Resource, "GIP4", false);
        Table v6 = load(v6Resource, "GIP6", true);

        v4Names = v4 == null ? null : v4.names();
        v4Starts = v4 == null ? null : v4.intStarts();
        v4NameIndexes = v4 == null ? null : v4.nameIndexes();
        v6Names = v6 == null ? null : v6.names();
        v6Starts = v6 == null ? null : v6.longStarts();
        v6NameIndexes = v6 == null ? null : v6.nameIndexes();

        if (v4 != null || v6 != null) {
            log.info("Da nap du lieu vi tri IP: {} dai IPv4, {} dai IPv6, {} ten vung trong {}ms",
                    v4Starts == null ? 0 : v4Starts.length,
                    v6Starts == null ? 0 : v6Starts.length,
                    (v4Names == null ? 0 : v4Names.length) + (v6Names == null ? 0 : v6Names.length),
                    (System.nanoTime() - startNanos) / 1_000_000);
        }
    }

    /**
     * Chuỗi hiển thị vị trí ước tính của 1 IP: "Mạng nội bộ" | "&lt;Tỉnh&gt;, &lt;Tên nước&gt;" | "&lt;Tên nước&gt;"
     * | "Không xác định". Trả null khi ip null / "unknown" / không phải IP hợp lệ, hoặc dữ liệu không nạp được.
     */
    public String locate(String ip) {
        byte[] address = IpAddressParser.parse(ip);
        if (address == null) {
            return null;
        }
        if (address.length == 16 && IpAddressParser.isIpv4Mapped(address)) {
            address = new byte[] {address[12], address[13], address[14], address[15]};
        }
        if (IpAddressParser.isLocal(address)) {
            return LOCAL_NETWORK;
        }
        String name = address.length == 4 ? lookupV4(toInt(address)) : lookupV6(toHigh64(address));
        return name == null ? null : display(name);
    }

    private String lookupV4(int ip) {
        if (v4Starts == null) {
            return null;
        }
        int lo = 0;
        int hi = v4Starts.length - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (Integer.compareUnsigned(v4Starts[mid], ip) <= 0) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return nameAt(v4Names, v4NameIndexes, found);
    }

    private String lookupV6(long high64) {
        if (v6Starts == null) {
            return null;
        }
        int lo = 0;
        int hi = v6Starts.length - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (Long.compareUnsigned(v6Starts[mid], high64) <= 0) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return nameAt(v6Names, v6NameIndexes, found);
    }

    /** "" = không xác định (không có entry nào &lt;= ip, nameIndex 0, hoặc index hỏng). */
    private static String nameAt(String[] names, short[] nameIndexes, int entry) {
        if (entry < 0) {
            return "";
        }
        int index = nameIndexes[entry] & 0xFFFF;
        return index == 0 || index >= names.length ? "" : names[index];
    }

    private String display(String name) {
        int bar = name.indexOf('|');
        String code = (bar < 0 ? name : name.substring(0, bar)).trim();
        String region = bar < 0 ? "" : name.substring(bar + 1).trim();
        if (code.isEmpty()) {
            return UNKNOWN;
        }
        String country = countryNames.computeIfAbsent(code, IpLocationService::countryName);
        return region.isEmpty() ? country : region + ", " + country;
    }

    private static String countryName(String code) {
        String name = Locale.of("", code).getDisplayCountry(VIETNAMESE);
        return name.isEmpty() || name.equalsIgnoreCase(code) ? code : name;
    }

    private static int toInt(byte[] v4) {
        return ((v4[0] & 0xFF) << 24) | ((v4[1] & 0xFF) << 16) | ((v4[2] & 0xFF) << 8) | (v4[3] & 0xFF);
    }

    private static long toHigh64(byte[] v6) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (v6[i] & 0xFF);
        }
        return value;
    }

    private static Table load(String resource, String expectedMagic, boolean ipv6) {
        InputStream raw = IpLocationService.class.getClassLoader().getResourceAsStream(resource);
        if (raw == null) {
            log.warn("Khong tim thay du lieu vi tri IP {} - se khong hien vi tri cho {}", resource,
                    ipv6 ? "IPv6" : "IPv4");
            return null;
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(raw, 1 << 16), 1 << 16))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            int version = in.readUnsignedByte();
            if (!expectedMagic.equals(new String(magic, StandardCharsets.US_ASCII)) || version != FORMAT_VERSION) {
                log.warn("Du lieu vi tri IP {} sai dinh dang (magic/version) - se khong hien vi tri", resource);
                return null;
            }

            int nameCount = in.readInt();
            if (nameCount < 0) {
                throw new IOException("nameCount am: " + nameCount);
            }
            String[] names = new String[nameCount];
            for (int i = 0; i < nameCount; i++) {
                byte[] bytes = new byte[in.readUnsignedShort()];
                in.readFully(bytes);
                names[i] = new String(bytes, StandardCharsets.UTF_8);
            }

            int entryCount = in.readInt();
            if (entryCount < 0) {
                throw new IOException("entryCount am: " + entryCount);
            }
            int[] intStarts = null;
            long[] longStarts = null;
            if (ipv6) {
                longStarts = new long[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    longStarts[i] = in.readLong();
                }
            } else {
                intStarts = new int[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    intStarts[i] = in.readInt();
                }
            }
            short[] nameIndexes = new short[entryCount];
            for (int i = 0; i < entryCount; i++) {
                nameIndexes[i] = in.readShort();
            }
            return new Table(names, intStarts, longStarts, nameIndexes);
        } catch (IOException | RuntimeException exception) {
            log.warn("Khong doc duoc du lieu vi tri IP {} - se khong hien vi tri: {}", resource, exception.toString());
            return null;
        }
    }

    private record Table(String[] names, int[] intStarts, long[] longStarts, short[] nameIndexes) {
    }
}
