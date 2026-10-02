package vn.gastroai.be.infrastructure.ai;

/** 1 ảnh gửi kèm cho Gemini qua inlineData - base64Data KHÔNG có prefix "data:image/...;base64,". */
public record ImagePart(String mimeType, String base64Data) {
}
