ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS unpublished_at TIMESTAMPTZ;

-- Chi ban DONE moi duoc la ban dang dung (truoc day ban 1 duoc tao san la hien hanh).
UPDATE documents SET is_current = FALSE WHERE status <> 'DONE';

-- Nhom da bi go truoc day: khong con ban nao hien hanh nhung co it nhat 1 ban DONE.
UPDATE documents r
SET unpublished_at = now()
WHERE r.root_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM documents x
      WHERE COALESCE(x.root_id, x.id) = r.id AND x.is_current)
  AND EXISTS (
      SELECT 1 FROM documents x
      WHERE COALESCE(x.root_id, x.id) = r.id AND x.status = 'DONE');