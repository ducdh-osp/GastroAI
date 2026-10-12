-- UC0020: thung rac (soft-delete) cho nhat ky an uong va bang Bristol. deleted_at = NULL
-- nghia la dong con hien thi binh thuong, khac NULL nghia la da bi xoa (vao thung rac).
-- Khong co DEFAULT va khong backfill: du lieu cu tu dong la NULL, van hien thi nhu truoc.
ALTER TABLE food_diary_entries
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE bristol_logs
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Index rieng, chi chua cac dong da xoa (WHERE deleted_at IS NOT NULL) nen rat nho -
-- phuc vu truy van thung rac (findTrash) va don dep dinh ky (purgeDeletedBefore).
CREATE INDEX IF NOT EXISTS idx_food_diary_entries_trash
    ON food_diary_entries(patient_id, deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bristol_logs_trash
    ON bristol_logs(patient_id, deleted_at)
    WHERE deleted_at IS NOT NULL;