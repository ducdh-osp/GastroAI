package vn.gastroai.be.application.auth;

/** Thông tin thiết bị/IP đã xử lý xong (không phải HttpServletRequest thô) — xem ClientRequestInfoResolver. */
public record ClientRequestInfo(String ipAddress, String userAgent, String deviceLabel) {
}
