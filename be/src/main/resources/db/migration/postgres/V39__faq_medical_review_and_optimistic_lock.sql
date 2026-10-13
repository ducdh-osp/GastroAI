-- UC0062: Kiem duyet y khoa bat buoc truoc khi xuat ban FAQ + optimistic locking
-- (ap dung dung pattern da fix cho knowledge_articles o V38)

-- 1. Optimistic locking (@Version)
ALTER TABLE faqs ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Thong tin bac si kiem duyet y khoa
ALTER TABLE faqs ADD COLUMN IF NOT EXISTS medical_reviewer VARCHAR(200);
ALTER TABLE faqs ADD COLUMN IF NOT EXISTS medical_reviewed_at TIMESTAMPTZ;

-- 3. FAQ moi mac dinh la ban nhap
ALTER TABLE faqs ALTER COLUMN published SET DEFAULT false;

-- 4. Cac FAQ dang published nhung chua co nguoi duyet that -> chuyen ve ban nhap.
-- Khong gan ten bac si gia lap: Admin/Bac si phai mo CMS, kiem duyet va xuat ban lai.
UPDATE faqs
SET published = false
WHERE published = true
  AND (medical_reviewer IS NULL OR btrim(medical_reviewer) = '' OR medical_reviewed_at IS NULL);

-- 5. Rang buoc DB: chi duoc published = true khi da co medical_reviewer va medical_reviewed_at
ALTER TABLE faqs
    DROP CONSTRAINT IF EXISTS chk_faqs_published_review;

ALTER TABLE faqs
    ADD CONSTRAINT chk_faqs_published_review
    CHECK (published = false
        OR (medical_reviewer IS NOT NULL AND btrim(medical_reviewer) <> '' AND medical_reviewed_at IS NOT NULL));
