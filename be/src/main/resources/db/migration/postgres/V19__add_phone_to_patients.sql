-- UC0036/067(vá) - them so dien thoai lien lac cho benh nhan, de canh bao Triage
-- (TriageAlertEvent) co du thong tin cho bac si/admin hanh dong ngay khi can (goi dien truc
-- tiep) thay vi chi co ten. Nullable vi tai khoan cu chua co du lieu nay, benh nhan tu cap
-- nhat sau (lien quan UC0009/010) - khong bat buoc NOT NULL de tranh chan dang ky/dang nhap
-- benh nhan cu chua tung nhap SDT.
ALTER TABLE patients
    ADD COLUMN phone VARCHAR(20)