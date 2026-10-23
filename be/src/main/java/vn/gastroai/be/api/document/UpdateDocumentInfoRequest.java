package vn.gastroai.be.api.document;

/** Body cua PATCH /documents/{id}: ten tai lieu + URL nguon cong khai (de trong = xoa URL). */
public record UpdateDocumentInfoRequest(String title, String sourceUrl) {
}