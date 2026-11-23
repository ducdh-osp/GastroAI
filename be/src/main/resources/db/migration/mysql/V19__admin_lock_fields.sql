-- SCR-AD-03/SCR-AD-14: cho Admin khoa/mo tai khoan nhan su CMS (bang admins) - mirror dung field
-- admin_locked/admin_locked_at da dung cho benh nhan (Patient.java/patients table ben Postgres).
-- Chi ap dung cho bang admins (4 vai tro moi); KHONG dong toi bang doctors (legacy, dang ngung dung).
ALTER TABLE admins
    ADD COLUMN admin_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN admin_locked_at DATETIME NULL;
