-- SCR-AD-04 v1.x (UC0049 "Thay the tai lieu"): bo lich su phien ban. Moi tai lieu chi con
--   (a) ban dang dung / da xac nhan noi dung ("live"), va
--   (b) toi da 1 ban thay the moi hon dang cho xu ly / cho xac nhan.
-- Moi ban cu khac bi XOA VINH VIEN (chunk + embedding xoa theo ON DELETE CASCADE).
-- Ban giu lai duoc dua len lam dong goc (root_id = NULL), ban thay the tro root_id ve no.
-- File goc tren dia cua cac ban bi xoa KHONG xoa duoc tu SQL - con lai trong thu muc storage.
-- Cuoi file: root_id -> replaces_id; bo version_no (khong con so phien ban) va is_current
-- (suy ra duoc: chatbot dung dong da xac nhan noi dung va khong bi go).

-- 1. Don du lieu ve dang "1 dong chinh + toi da 1 ban thay the".
DO $$
DECLARE
    grp           RECORD;
    old_unpub     TIMESTAMPTZ;
    live_id       BIGINT;
    live_version  INT;
    pending_id    BIGINT;
    keep_root     BIGINT;
BEGIN
    FOR grp IN SELECT DISTINCT COALESCE(root_id, id) AS gid FROM documents LOOP
        live_id := NULL;
        pending_id := NULL;

        SELECT unpublished_at INTO old_unpub FROM documents WHERE id = grp.gid;

        -- Ban "live": ban dang dung; neu tai lieu dang bi go thi lay ban da xac nhan moi nhat.
        SELECT id, version_no INTO live_id, live_version
        FROM documents
        WHERE COALESCE(root_id, id) = grp.gid AND is_current
        LIMIT 1;
        IF live_id IS NULL THEN
            SELECT id, version_no INTO live_id, live_version
            FROM documents
            WHERE COALESCE(root_id, id) = grp.gid
              AND status = 'DONE' AND content_approved_at IS NOT NULL
            ORDER BY version_no DESC
            LIMIT 1;
        END IF;

        IF live_id IS NOT NULL THEN
            -- Ban thay the: ban CHUA xac nhan, moi hon ban live (lay ban moi nhat).
            SELECT id INTO pending_id
            FROM documents
            WHERE COALESCE(root_id, id) = grp.gid
              AND content_approved_at IS NULL AND version_no > live_version
            ORDER BY version_no DESC
            LIMIT 1;
            keep_root := live_id;
        ELSE
            -- Chua tung co ban nao dung cho AI: chi giu ban moi nhat.
            SELECT id INTO keep_root
            FROM documents
            WHERE COALESCE(root_id, id) = grp.gid
            ORDER BY version_no DESC
            LIMIT 1;
        END IF;

        -- Tach ban giu lai khoi dong goc cu TRUOC khi xoa (xoa dong goc cu se CASCADE).
        UPDATE documents
        SET root_id = NULL,
            unpublished_at = COALESCE(unpublished_at, old_unpub)
        WHERE id = keep_root AND root_id IS NOT NULL;

        IF pending_id IS NOT NULL THEN
            UPDATE documents SET root_id = keep_root WHERE id = pending_id;
        END IF;

        DELETE FROM documents
        WHERE COALESCE(root_id, id) = grp.gid
          AND id <> keep_root
          AND id <> COALESCE(pending_id, -1);
    END LOOP;
END $$;


-- 2. Chuan hoa "da go" de bo duoc is_current.
--    Tu gio "chatbot dang dung" = content_approved_at IS NOT NULL AND unpublished_at IS NULL.
--    a) Chua tung duoc xac nhan thi khong co gi de go (V43 tung danh dau go ca tai lieu chua dung).
UPDATE documents SET unpublished_at = NULL WHERE content_approved_at IS NULL;
--    b) Da xac nhan nhung chatbot dang khong dung -> ghi nhan la da go, giu nguyen hanh vi.
UPDATE documents SET unpublished_at = now()
WHERE content_approved_at IS NOT NULL AND NOT is_current AND unpublished_at IS NULL;

-- 3. Doi ten / bo cot.
--    replaces_id: NULL = dong chinh; co gia tri = ban thay the, tro ve dong chinh no se thay.
DROP INDEX uk_documents_group_version;
DROP INDEX uk_documents_group_current;

ALTER TABLE documents RENAME COLUMN root_id TO replaces_id;
ALTER TABLE documents RENAME CONSTRAINT documents_root_id_fkey TO documents_replaces_id_fkey;
ALTER TABLE documents DROP COLUMN version_no;
ALTER TABLE documents DROP COLUMN is_current;

-- 4. Rang buoc: DB tu chan moi trang thai sai, khong chi trong cay vao code.

-- Moi tai lieu co TOI DA 1 ban thay the (2 nguoi cung tai -> nguoi sau bi tu choi).
CREATE UNIQUE INDEX uk_documents_one_replacement
    ON documents (replaces_id)
    WHERE replaces_id IS NOT NULL;

ALTER TABLE documents
    -- Chi xac nhan duoc ban da xu ly xong (§3.1b).
    ADD CONSTRAINT ck_documents_approved_requires_done
        CHECK (content_approved_at IS NULL OR status = 'DONE'),
    -- Ban thay the chua duoc xac nhan: xac nhan xong no thanh dong chinh. Vi vay chi dong chinh
    -- moi co the dang duoc chatbot dung -> moi tai lieu toi da 1 dong dang dung.
    ADD CONSTRAINT ck_documents_replacement_pending
        CHECK (replaces_id IS NULL OR content_approved_at IS NULL),
    -- Chi go duoc tai lieu da xac nhan (tuc la da tung duoc chatbot dung).
    ADD CONSTRAINT ck_documents_unpublished_requires_approval
        CHECK (unpublished_at IS NULL OR content_approved_at IS NOT NULL),
    ADD CONSTRAINT ck_documents_not_replacing_itself
        CHECK (replaces_id IS NULL OR replaces_id <> id);