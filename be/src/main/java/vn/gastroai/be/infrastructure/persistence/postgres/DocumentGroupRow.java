package vn.gastroai.be.infrastructure.persistence.postgres;

import java.time.Instant;

/** 1 dong tong hop theo tai lieu (dong chinh + ban thay the neu co) - ket qua GROUP BY. */
public interface DocumentGroupRow {

    /** Ma tai lieu = id cua dong chinh. */
    Long getGroupId();

    /** So dong cua tai lieu: 1, hoac 2 khi dang co ban thay the. */
    Long getRowCount();

    /** id dong chatbot dang dung, null neu chatbot chua/khong dung tai lieu nay. */
    Long getInUseId();

    /** Thoi diem admin go tai lieu (tren dong chinh), null neu khong bi go. */
    Instant getUnpublishedAt();
}