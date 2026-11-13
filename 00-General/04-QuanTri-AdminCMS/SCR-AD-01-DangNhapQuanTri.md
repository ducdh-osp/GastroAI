# SCR-AD-01 — Đăng nhập quản trị

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0045` |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Lối vào riêng biệt cho đội ngũ nội bộ — tách bạch hoàn toàn khỏi cổng bệnh nhân để không ai nhầm lẫn 2 luồng | 
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần 1 cổng đăng nhập riêng, không dùng chung với bệnh nhân

Hai lý do nghiệp vụ, không chỉ là sở thích thiết kế:

- **An toàn**: nếu cùng 1 màn đăng nhập cho cả bệnh nhân lẫn Admin, bất kỳ lỗ hổng/nhầm lẫn phân quyền nào ở đó đều ảnh hưởng tới cả 2 nhóm — tách riêng giúp mỗi cổng có thể siết chặt độc lập mà không ảnh hưởng nhóm còn lại.
- **Tránh nhầm lẫn**: Admin/Bác sĩ cần biết chắc chắn mình đang ở "chế độ làm việc", không phải vô tình lạc vào trải nghiệm dành cho bệnh nhân.

## 2. Thiết kế màn hình

Dùng cùng khung bố cục 2 panel như cổng bệnh nhân (nhất quán hệ thống thiết kế), nhưng **tông màu khác hẳn** — tối, nghiêm túc hơn — để không ai nhầm 2 cổng dù nhìn thoáng qua.

```text
┌─────────────────────────┬───────────────────────────────┐
│  (nền tông tối, nghiêm    │   Đăng nhập quản trị              │
│   túc — khác hẳn màu      │                                    │
│   ngọc bích của cổng      │  ┌─────────────────────────────┐ │
│   bệnh nhân)               │  │ Email                          │ │
│                            │  │ [___________________________] │ │
│  "Cổng quản trị"           │  │ Mật khẩu                 [👁] │ │
│                            │  │ [___________________________] │ │
│                            │  │      [   Đăng nhập   ]          │ │
└─────────────────────────┴───────────────────────────────┘
```

Không có nút "Đăng ký" ở màn này — tài khoản Admin/Bác sĩ chỉ được **tạo bởi quản trị viên**, không ai tự đăng ký được (khác hẳn nguyên tắc tự phục vụ của cổng bệnh nhân).

## 3. Hành vi cần có

Áp dụng **y hệt** các quy tắc đã mô tả ở `01-XacThuc-TaiKhoan/00-TongQuan.md` (chính sách mật khẩu, khoá tạm thời sau nhiều lần sai, không tiết lộ email tồn tại hay không) — dùng chung 1 bộ quy tắc bảo mật cho cả 2 cổng để không tạo ra 1 cổng "yếu hơn" cổng kia một cách vô tình.

| Khác biệt duy nhất so với cổng bệnh nhân | Vì sao |
| :--- | :--- |
| Không có luồng "Đăng ký" | Tài khoản nội bộ do Admin cấp (`SCR-AD-02`), không tự phục vụ |
| Sau đăng nhập, luôn vào thẳng màn làm việc phù hợp vai trò | Admin và Bác sĩ có bộ màn hình nhìn thấy khác nhau ngay từ đầu (xem ma trận phân quyền ở từng màn) |

## 4. Tham chiếu

- Quy tắc mật khẩu & chống brute-force dùng chung: `01-XacThuc-TaiKhoan/00-TongQuan.md` §4.2, §4.3
- Tạo tài khoản mới: [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md) (cho bệnh nhân) / [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md) (cho nhân sự nội bộ)
