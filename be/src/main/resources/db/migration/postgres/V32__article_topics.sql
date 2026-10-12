-- UC0066 - Danh muc/chu de bai viet (dung de phan loai bai viet va, sau nay, FAQ).
-- knowledge_articles.category van giu nguyen la chuoi ten chu de (UC0026 dang doc theo cot nay),
-- bang nay la nguon quan ly danh sach chu de; doi ten chu de se cap nhat cac bai viet dang dung.
CREATE TABLE article_topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Khong cho trung ten ke ca khac hoa/thuong ("Da day" va "da day").
CREATE UNIQUE INDEX uq_article_topics_name_lower ON article_topics (lower(name));

-- Dua cac chu de dang co san trong bai viet vao bang, de admin thay ngay khi mo man hinh.
INSERT INTO article_topics (name, sort_order)
SELECT category, (ROW_NUMBER() OVER (ORDER BY category)) * 10
FROM (SELECT DISTINCT category FROM knowledge_articles) AS existing_categories;
