-- Bỏ hẳn vai trò Bác sĩ (DOCTOR) khỏi CMS. Từ SCR-AD-03 v1.2, nhân sự CMS gồm 4 vai trò và đều là tài khoản
-- ở bảng admins; tài khoản bảng doctors đã không đăng nhập CMS được nữa (bảng lịch sử đăng nhập của bác sĩ
-- đã xoá ở V13). Xoá bảng kèm các tài khoản bác sĩ mẫu của V2/V6/V9.
-- Không còn khoá ngoại nào trỏ tới doctors (doctor_login_history đã xoá ở V13).
DROP TABLE IF EXISTS doctors;
