package vn.gastroai.be.application.geo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Dùng file dữ liệu thật trong resources/geoip — nạp 1 lần cho cả lớp test. */
class IpLocationServiceTest {

    private static IpLocationService service;

    @BeforeAll
    static void loadRealData() {
        service = new IpLocationService();
    }

    @Test
    void vietnameseIpv4ResolvesToProvinceAndCountry() {
        assertEquals("Thanh Hoa Province, Việt Nam", service.locate("171.253.91.42"));
        assertEquals("Da Nang City, Việt Nam", service.locate("1.52.1.130"));
    }

    @Test
    void foreignIpv4ResolvesToItsRegion() {
        assertContains(service.locate("8.8.8.8"), "California");
        assertContains(service.locate("1.0.1.1"), "Fujian");
    }

    @Test
    void ipv6UsesHigh64Bits() {
        assertContains(service.locate("2402:800:631c:96b3::1"), "Ho Chi Minh");
    }

    @Test
    void ipv4MappedIpv6IsTreatedAsIpv4() {
        assertEquals(service.locate("8.8.8.8"), service.locate("::ffff:8.8.8.8"));
        assertEquals(service.locate("8.8.8.8"), service.locate("::ffff:0808:0808"));
    }

    @Test
    void privateLoopbackAndLinkLocalAddressesAreLocalNetwork() {
        for (String ip : new String[] {"192.168.1.5", "::1", "127.0.0.1", "10.1.2.3", "172.16.0.1",
                "172.31.255.255", "169.254.10.20", "fc00::1", "fd12:3456::1", "fe80::1%eth0", "::ffff:192.168.0.1"}) {
            assertEquals(IpLocationService.LOCAL_NETWORK, service.locate(ip), ip);
        }
        // Ngay ngoài dải 172.16/12 thì không còn là nội bộ.
        assertTrue(!IpLocationService.LOCAL_NETWORK.equals(service.locate("172.32.0.1")));
    }

    @Test
    void nonIpInputReturnsNullWithoutDnsLookup() {
        for (String value : new String[] {null, "", "unknown", "UNKNOWN", "khong-phai-ip", "google.com",
                "localhost", "1.2.3", "1.2.3.4.5", "256.1.1.1", "1.2.3.-4", "12345::1::2", "gggg::1",
                "1:2:3:4:5:6:7:8:9", "1.2.3.4 ; DROP TABLE"}) {
            assertNull(service.locate(value), String.valueOf(value));
        }
    }

    @Test
    void boundaryAddressesDoNotThrow() {
        assertDoesNotThrow(() -> service.locate("0.0.0.0"));
        assertDoesNotThrow(() -> service.locate("255.255.255.255"));
        assertDoesNotThrow(() -> service.locate("::"));
        assertDoesNotThrow(() -> service.locate("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
        assertNotNull(service.locate("0.0.0.0"));
    }

    @Test
    void missingDataFilesReturnNullWithoutThrowing() {
        IpLocationService withoutData = assertDoesNotThrow(
                () -> new IpLocationService("geoip/khong-ton-tai-v4.bin.gz", "geoip/khong-ton-tai-v6.bin.gz"));

        assertNull(withoutData.locate("8.8.8.8"));
        assertNull(withoutData.locate("2402:800:631c:96b3::1"));
        // Mạng nội bộ không cần dữ liệu.
        assertEquals(IpLocationService.LOCAL_NETWORK, withoutData.locate("192.168.1.5"));
    }

    @Test
    void wrongMagicIsRejectedWithoutThrowing() {
        // File IPv6 đọc như IPv4 -> magic "GIP6" khác "GIP4" -> bỏ, không ném lỗi.
        IpLocationService swapped = assertDoesNotThrow(
                () -> new IpLocationService(IpLocationService.DEFAULT_V6_RESOURCE, IpLocationService.DEFAULT_V4_RESOURCE));

        assertNull(swapped.locate("8.8.8.8"));
        assertNull(swapped.locate("2402:800:631c:96b3::1"));
    }

    private static void assertContains(String actual, String expected) {
        assertNotNull(actual);
        assertTrue(actual.contains(expected), "\"" + actual + "\" phai chua \"" + expected + "\"");
    }
}
