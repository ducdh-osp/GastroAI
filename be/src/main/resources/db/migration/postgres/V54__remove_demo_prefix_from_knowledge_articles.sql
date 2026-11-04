-- V54: Loại bỏ tiền tố [DEMO] / (demo) trong tiêu đề các bài viết kiến thức y khoa
UPDATE knowledge_articles
SET title = TRIM(REGEXP_REPLACE(title, '^\s*(\[DEMO\]|\(demo\)|\[demo\]|\(DEMO\))\s*', '', 'i')),
    source_name = CASE WHEN source_name = 'GastroAI Demo Dataset' THEN 'GastroAI Y khoa' ELSE source_name END
WHERE title ~* '(\[DEMO\]|\(demo\)|\[demo\]|\(DEMO\))';

