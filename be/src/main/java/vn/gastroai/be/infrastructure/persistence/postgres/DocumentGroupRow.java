package vn.gastroai.be.infrastructure.persistence.postgres;

import java.time.Instant;

/** 1 dong tong hop theo nhom phien ban cua mot tai lieu (ket qua GROUP BY). */
public interface DocumentGroupRow {
    /** Ma nhom = id cua dong goc. */
    Long getGroupId();

    /** Tong so phien ban cua tai lieu. */
    Long getVersionCount();

    /** So hieu (v?) cua ban dang dung, null neu khong ban nao dang dung. */
    Integer getCurrentVersionNo();

    /** id dong cua ban dang dung, null neu khong ban nao dang dung. */
    Long getCurrentId();

    /** Thoi diem admin go tai lieu (tren dong goc), null neu khong bi go. */
    Instant getUnpublishedAt();
}