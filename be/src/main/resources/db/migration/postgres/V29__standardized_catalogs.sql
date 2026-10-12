CREATE TABLE catalog_items (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(24) NOT NULL CHECK (type IN ('SYMPTOM', 'CONDITION', 'MEDICATION')),
    code VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_catalog_items_type_code UNIQUE (type, code)
);

CREATE INDEX idx_catalog_items_active_type_order
    ON catalog_items(type, sort_order, name)
    WHERE active = TRUE;

INSERT INTO catalog_items(type, code, name, sort_order) VALUES
('SYMPTOM', 'ABDOMINAL_PAIN', 'Đau bụng', 10),
('SYMPTOM', 'BLOATING', 'Đầy hơi, chướng bụng', 20),
('SYMPTOM', 'CONSTIPATION', 'Táo bón', 30),
('SYMPTOM', 'DIARRHEA', 'Tiêu chảy', 40),
('SYMPTOM', 'HEARTBURN', 'Ợ nóng, trào ngược', 50),
('SYMPTOM', 'NAUSEA', 'Buồn nôn', 60),
('SYMPTOM', 'VOMITING', 'Nôn', 70),
('SYMPTOM', 'BLOOD_IN_STOOL', 'Đi ngoài ra máu', 80),
('CONDITION', 'GERD', 'Trào ngược dạ dày thực quản', 10),
('CONDITION', 'GASTRITIS', 'Viêm dạ dày', 20),
('CONDITION', 'PEPTIC_ULCER', 'Loét dạ dày tá tràng', 30),
('CONDITION', 'IRRITABLE_BOWEL_SYNDROME', 'Hội chứng ruột kích thích', 40),
('CONDITION', 'INFLAMMATORY_BOWEL_DISEASE', 'Bệnh viêm ruột', 50),
('CONDITION', 'CELIAC_DISEASE', 'Bệnh Celiac', 60),
('MEDICATION', 'OMEPRAZOLE', 'Omeprazole', 10),
('MEDICATION', 'PANTOPRAZOLE', 'Pantoprazole', 20),
('MEDICATION', 'FAMOTIDINE', 'Famotidine', 30),
('MEDICATION', 'ANTACID', 'Thuốc kháng acid', 40),
('MEDICATION', 'SIMETHICONE', 'Simethicone', 50),
('MEDICATION', 'PSYLLIUM', 'Psyllium (chất xơ hòa tan)', 60);
