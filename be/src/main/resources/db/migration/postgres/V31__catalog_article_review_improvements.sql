ALTER TABLE catalog_items
    ADD COLUMN triage_code VARCHAR(80);

UPDATE catalog_items
SET code = 'BLACK_TARRY_STOOL',
    name = 'Phân đen như hắc ín',
    triage_code = 'BLACK_OR_BLOODY_STOOL'
WHERE type = 'SYMPTOM' AND code = 'BLOOD_IN_STOOL';

INSERT INTO catalog_items(type, code, name, triage_code, sort_order) VALUES
('SYMPTOM', 'FRESH_RED_BLOOD_IN_STOOL', 'Máu đỏ tươi trong phân', 'BLACK_OR_BLOODY_STOOL', 90),
('SYMPTOM', 'BLOOD_IN_VOMIT', 'Nôn ra máu hoặc chất nôn như bã cà phê', 'BLOOD_IN_VOMIT', 100),
('CONDITION', 'HELICOBACTER_PYLORI', 'Nhiễm Helicobacter pylori (H. pylori)', NULL, 70),
('CONDITION', 'HEMORRHOIDS', 'Trĩ', NULL, 80),
('CONDITION', 'HEPATITIS_B', 'Viêm gan B', NULL, 90),
('CONDITION', 'HEPATITIS_C', 'Viêm gan C', NULL, 100),
('MEDICATION', 'ESOMEPRAZOLE', 'Esomeprazole', NULL, 70),
('MEDICATION', 'LANSOPRAZOLE', 'Lansoprazole', NULL, 80),
('MEDICATION', 'RABEPRAZOLE', 'Rabeprazole', NULL, 90),
('MEDICATION', 'LOPERAMIDE', 'Loperamide', NULL, 100),
('MEDICATION', 'BISMUTH_SUBSALICYLATE', 'Bismuth subsalicylate', NULL, 110),
('MEDICATION', 'PROBIOTIC', 'Men vi sinh (probiotic)', NULL, 120);

UPDATE catalog_items SET triage_code = code
WHERE type = 'SYMPTOM' AND code IN (
    'ABDOMINAL_PAIN', 'BLOATING', 'CONSTIPATION', 'DIARRHEA',
    'HEARTBURN', 'NAUSEA', 'VOMITING'
);

ALTER TABLE knowledge_articles
    ADD COLUMN medical_reviewer VARCHAR(200),
    ADD COLUMN medical_reviewed_at TIMESTAMPTZ;

ALTER TABLE knowledge_articles ALTER COLUMN published SET DEFAULT FALSE;

UPDATE knowledge_articles
SET content = replace(content,
    'nôn ra máu/dịch giống bã cà phê, phân đen như hắc ín hoặc sụt cân không rõ nguyên nhân cần được nhân viên y tế đánh giá.',
    'nôn ra máu/dịch giống bã cà phê, phân đen như hắc ín hoặc ngất là dấu hiệu cần cấp cứu ngay: gọi 115. Sụt cân không rõ nguyên nhân cần được nhân viên y tế đánh giá sớm.'),
    source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/acid-reflux-ger-gerd-adults'
WHERE slug = 'trao-nguoc-da-day-thuc-quan';

UPDATE knowledge_articles
SET content = replace(content,
    'Đi ngoài ra máu, phân đen, đau dữ dội, nôn kéo dài hoặc sụt cân không chủ ý cần được kiểm tra sớm vì có thể liên quan nguyên nhân khác.',
    'Nôn ra máu/dịch giống bã cà phê, phân đen như hắc ín, đi ngoài ra máu hoặc ngất cần cấp cứu ngay: gọi 115. Đau dữ dội, nôn kéo dài hoặc sụt cân không chủ ý cần sớm được nhân viên y tế đánh giá vì có thể do nguyên nhân khác.'),
    source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/irritable-bowel-syndrome'
WHERE slug = 'hoi-chung-ruot-kich-thich';

UPDATE knowledge_articles
SET content = replace(content,
    'Nôn ra máu hoặc chất giống bã cà phê, phân đen như hắc ín, choáng/ngất hay đau bụng dữ dội cần được chăm sóc y tế khẩn cấp.',
    'Nôn ra máu hoặc chất giống bã cà phê, phân đen như hắc ín, choáng/ngất hay đau bụng dữ dội cần cấp cứu ngay: gọi 115.'),
    source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/gastritis-gastropathy'
WHERE slug = 'viem-da-day-va-benh-ly-niem-mac';

UPDATE knowledge_articles
SET content = replace(content,
    'Hãy đến cơ sở cấp cứu ngay.',
    'Hãy gọi 115 hoặc đến cơ sở cấp cứu ngay.'),
    source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/peptic-ulcers-stomach-ulcers'
WHERE slug = 'loet-da-day-ta-trang';

UPDATE knowledge_articles
SET source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/celiac-disease'
WHERE slug = 'benh-celiac';

UPDATE knowledge_articles
SET content = replace(content,
    'Lú lẫn hoặc li bì, nôn nhiều, đau bụng/trực tràng dữ dội, dấu hiệu mất nước, phân có máu hoặc đen cần được đánh giá sớm.',
    'Lú lẫn hoặc li bì, nôn nhiều, đau bụng/trực tràng dữ dội, mất nước nặng, nôn ra máu, phân đen như hắc ín hoặc có máu cần cấp cứu ngay: gọi 115.'),
    source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/diarrhea/symptoms-causes'
WHERE slug = 'tieu-chay-va-phong-mat-nuoc';

UPDATE knowledge_articles
SET content = replace(content,
    'sốt cao hoặc đi phân lỏng từ sáu lần/ngày.',
    'sốt từ 38,9 °C (102 °F) trở lên hoặc đi phân lỏng từ sáu lần/ngày.')
WHERE slug = 'tieu-chay-va-phong-mat-nuoc';

UPDATE knowledge_articles
SET source_url = 'https://www.niddk.nih.gov/health-information/digestive-diseases/constipation/symptoms-causes'
WHERE slug = 'tao-bon';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Triệu chứng và nguyên nhân GERD](https://www.niddk.nih.gov/health-information/digestive-diseases/acid-reflux-ger-gerd-adults/symptoms-causes)\n- [NIDDK: Điều trị GERD](https://www.niddk.nih.gov/health-information/digestive-diseases/acid-reflux-ger-gerd-adults/treatment)'
WHERE slug = 'trao-nguoc-da-day-thuc-quan';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Điều trị IBS](https://www.niddk.nih.gov/health-information/digestive-diseases/irritable-bowel-syndrome/treatment)\n- [NIDDK: Ăn uống và dinh dưỡng khi mắc IBS](https://www.niddk.nih.gov/health-information/digestive-diseases/irritable-bowel-syndrome/eating-diet-nutrition)'
WHERE slug = 'hoi-chung-ruot-kich-thich';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Triệu chứng và nguyên nhân viêm dạ dày/gastropathy](https://www.niddk.nih.gov/health-information/digestive-diseases/gastritis-gastropathy/symptoms-causes)\n- [NIDDK: Chẩn đoán viêm dạ dày/gastropathy](https://www.niddk.nih.gov/health-information/digestive-diseases/gastritis-gastropathy/diagnosis)'
WHERE slug = 'viem-da-day-va-benh-ly-niem-mac';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Chẩn đoán loét dạ dày-tá tràng](https://www.niddk.nih.gov/health-information/digestive-diseases/peptic-ulcers-stomach-ulcers/diagnosis)\n- [NIDDK: Điều trị loét dạ dày-tá tràng](https://www.niddk.nih.gov/health-information/digestive-diseases/peptic-ulcers-stomach-ulcers/treatment)'
WHERE slug = 'loet-da-day-ta-trang';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Điều trị Celiac](https://www.niddk.nih.gov/health-information/digestive-diseases/celiac-disease/treatment)'
WHERE slug = 'benh-celiac';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Điều trị tiêu chảy](https://www.niddk.nih.gov/health-information/digestive-diseases/diarrhea/treatment)'
WHERE slug = 'tieu-chay-va-phong-mat-nuoc';

UPDATE knowledge_articles SET content = content || E'\n\n### Nguồn đọc thêm\n\n- [NIDDK: Điều trị táo bón](https://www.niddk.nih.gov/health-information/digestive-diseases/constipation/treatment)'
WHERE slug = 'tao-bon';

UPDATE knowledge_articles SET source_checked_at = DATE '2026-10-11';
