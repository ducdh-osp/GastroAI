ALTER TABLE food_diary_entries
    ADD COLUMN meal_type VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN symptoms_after_meal TEXT,
    ADD COLUMN symptom_onset_minutes INTEGER;

ALTER TABLE food_diary_entries
    ADD CONSTRAINT ck_food_diary_meal_type
        CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK', 'OTHER')),
    ADD CONSTRAINT ck_food_diary_symptom_onset
        CHECK (symptom_onset_minutes IS NULL OR symptom_onset_minutes >= 0);
