-- UC0062 & UC0066: Dong bo chu de FAQ voi bang dung chung article_topics va rang buoc khoa ngoai.

-- 1. Gop cac ten chu de FAQ seed cu bi lech ten ve dung chu de bai viet tuong ung (neu chu de dich ton tai).
UPDATE faqs
SET category = 'Trào ngược dạ dày'
WHERE lower(btrim(category)) = lower('Trào ngược dạ dày thực quản')
  AND EXISTS (SELECT 1 FROM article_topics WHERE name = 'Trào ngược dạ dày');

UPDATE faqs
SET category = 'Dạ dày'
WHERE lower(btrim(category)) = lower('Bệnh dạ dày - tá tràng')
  AND EXISTS (SELECT 1 FROM article_topics WHERE name = 'Dạ dày');

UPDATE faqs
SET category = 'Đại tràng và ruột'
WHERE lower(btrim(category)) = lower('Bệnh lý đại tràng')
  AND EXISTS (SELECT 1 FROM article_topics WHERE name = 'Đại tràng và ruột');

UPDATE faqs
SET category = 'Dinh dưỡng và hấp thu'
WHERE lower(btrim(category)) = lower('Dinh dưỡng')
  AND EXISTS (SELECT 1 FROM article_topics WHERE name = 'Dinh dưỡng và hấp thu');

-- 2. Chuan hoa khoang trang va chu hoa/thuong cua faqs.category theo ten chu de chuan trong article_topics.
UPDATE faqs f
SET category = t.name
FROM article_topics t
WHERE lower(btrim(f.category)) = lower(t.name)
  AND f.category IS DISTINCT FROM t.name;

-- 3. Bo sung cac chu de con lai dang co trong faqs (vd: Theo doi suc khoe, Canh bao nguy hiem) vao article_topics.
WITH base_order AS (
    SELECT COALESCE(MAX(sort_order), 0) AS max_order FROM article_topics
),
missing_categories AS (
    SELECT DISTINCT ON (lower(btrim(f.category)))
           btrim(f.category) AS name
    FROM faqs f
    WHERE f.category IS NOT NULL
      AND btrim(f.category) <> ''
      AND NOT EXISTS (
          SELECT 1 FROM article_topics t WHERE lower(t.name) = lower(btrim(f.category))
      )
    ORDER BY lower(btrim(f.category)), btrim(f.category)
)
INSERT INTO article_topics (name, sort_order)
SELECT m.name,
       base_order.max_order + (ROW_NUMBER() OVER (ORDER BY m.name)) * 10
FROM missing_categories m
CROSS JOIN base_order
ON CONFLICT DO NOTHING;

UPDATE faqs f
SET category = t.name
FROM article_topics t
WHERE lower(btrim(f.category)) = lower(t.name)
  AND f.category IS DISTINCT FROM t.name;

-- 4. Rang buoc khoa ngoai tu faqs.category sang article_topics(name) (tuong tu V38 cho knowledge_articles).
ALTER TABLE faqs
    DROP CONSTRAINT IF EXISTS fk_faqs_category;

ALTER TABLE faqs
    ADD CONSTRAINT fk_faqs_category
    FOREIGN KEY (category) REFERENCES article_topics(name)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;

