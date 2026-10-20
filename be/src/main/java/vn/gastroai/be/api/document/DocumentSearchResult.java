package vn.gastroai.be.api.document;

import java.util.List;

/** Ket qua tim kiem noi dung (UC0055): toi da 50 dong; truncated = true khi con ket qua chua hien. */
public record DocumentSearchResult(List<DocumentSearchHit> items, boolean truncated) {
}