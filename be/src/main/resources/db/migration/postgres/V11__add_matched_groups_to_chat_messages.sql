-- UC0035: luu nhom dau hieu Triage da khop (vd XUAT_HUYET_TIEU_HOA) de FE hien banner canh
-- bao cu the thay vi mot cau chung chung. JSON array of strings, giong cach sources/
-- related_questions dang luu (xem V8) - null cho tin nhan patient va cho ban ghi cu truoc
-- migration nay (FE tu xu ly truong hop rong/null, khong loi).
ALTER TABLE chat_messages
    ADD COLUMN matched_groups TEXT;
