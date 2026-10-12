-- UC0009/UC0010: bo sung medical_profiles cho "hoan chinh" hon mot ho so y te that -
-- thong tin co ban (ngay sinh/gioi tinh/chieu cao/can nang) de AI tu van sat tuoi/gioi,
-- benh nen + tien su phau thuat tach rieng dang danh sach (thay vi gop chung vao
-- medical_history tu do), va che do an dac biet/khong dung nap - lien quan truc tiep
-- app tieu hoa.
--
-- chronic_conditions/past_surgeries/dietary_restrictions luu JSON array of strings,
-- cung quy uoc voi allergies/current_medications da co tu V13.
ALTER TABLE medical_profiles
    ADD COLUMN date_of_birth        DATE,
    ADD COLUMN gender               VARCHAR(10),
    ADD COLUMN height_cm            INTEGER,
    ADD COLUMN weight_kg            INTEGER,
    ADD COLUMN chronic_conditions   TEXT,
    ADD COLUMN past_surgeries       TEXT,
    ADD COLUMN dietary_restrictions TEXT;
