package vn.gastroai.be.api.chat;

/** 1 file/ảnh bệnh nhân đã đính kèm - url trỏ về GET endpoint tải lại nội dung (xem ChatHistoryController). */
public record ChatAttachmentResponse(Long id, String originalFilename, String contentType, long sizeBytes, String url) {
}
