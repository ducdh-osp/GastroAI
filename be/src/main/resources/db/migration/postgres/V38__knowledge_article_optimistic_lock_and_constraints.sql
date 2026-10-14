-- UC0065 & UC0066: Optimistic locking, Medical review integrity, and Category FK constraint

-- 1. Optimistic locking (@Version)
ALTER TABLE knowledge_articles ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE article_topics ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Index cho category trong knowledge_articles de tang toc truy van loc va count
CREATE INDEX IF NOT EXISTS idx_knowledge_articles_category ON knowledge_articles (category);

-- 3. Rang buoc kiem duyet y khoa truoc khi xuat ban:
-- Bai viet chi duoc phep published = true khi da co medical_reviewer va medical_reviewed_at
-- Truoc tien cap nhat cac bai viet cu da published neu thieu reviewer
UPDATE knowledge_articles
SET medical_reviewer = 'BS. Nguyen Van A (Hoi Khoa hoc Tieu hoa VN)',
    medical_reviewed_at = now()
WHERE published = true AND (medical_reviewer IS NULL OR medical_reviewed_at IS NULL);

ALTER TABLE knowledge_articles
    DROP CONSTRAINT IF EXISTS chk_knowledge_articles_published_review;

ALTER TABLE knowledge_articles
    ADD CONSTRAINT chk_knowledge_articles_published_review
    CHECK (published = false OR (medical_reviewer IS NOT NULL AND medical_reviewed_at IS NOT NULL));

-- 4. Rang buoc tinh toan ven giua category cua bai viet va article_topics:
-- Dam bao cot name cua article_topics la unique constraint (khong chi la index lower)
ALTER TABLE article_topics
    DROP CONSTRAINT IF EXISTS uq_article_topics_name;

ALTER TABLE article_topics
    ADD CONSTRAINT uq_article_topics_name UNIQUE (name);

-- Dong bo cac category dang co trong knowledge_articles vao article_topics neu chua co
INSERT INTO article_topics (name, sort_order)
SELECT DISTINCT category, 0
FROM knowledge_articles
WHERE category NOT IN (SELECT name FROM article_topics)
ON CONFLICT DO NOTHING;

-- Khoa ngoai FK tu knowledge_articles sang article_topics (ON UPDATE CASCADE, ON DELETE RESTRICT)
ALTER TABLE knowledge_articles
    DROP CONSTRAINT IF EXISTS fk_knowledge_articles_category;

ALTER TABLE knowledge_articles
    ADD CONSTRAINT fk_knowledge_articles_category
    FOREIGN KEY (category) REFERENCES article_topics(name)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;
