package vn.gastroai.be.application.document;

/** Quy tac chung cho ten tai lieu - dung o ca luc upload lan doi ten. */
final class DocumentTitle {

    /** Khop cot documents.title VARCHAR(255). */
    static final int MAX_LENGTH = 255;

    private DocumentTitle() {
    }

    /** Bo khoang trang thua (2 dau va giua cac tu), bat buoc khong rong, toi da 255 ky tu. */
    static String normalize(String raw) {
        String title = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Tên tài liệu không được để trống");
        }
        if (title.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Tên tài liệu tối đa " + MAX_LENGTH + " ký tự");
        }
        return title;
    }
}