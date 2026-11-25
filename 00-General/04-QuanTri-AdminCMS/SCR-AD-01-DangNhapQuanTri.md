# SCR-AD-01 — Đăng nhập quản trị

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0045` |
| Actor | Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành (cả 4 vai trò nội bộ dùng chung đúng 1 cổng đăng nhập này) |
| Mục tiêu nghiệp vụ | Lối vào riêng biệt cho đội ngũ nội bộ — tách bạch hoàn toàn khỏi cổng bệnh nhân để không ai nhầm lẫn 2 luồng | 
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần 1 cổng đăng nhập riêng, không dùng chung với bệnh nhân

Hai lý do nghiệp vụ, không chỉ là sở thích thiết kế:

- **An toàn**: nếu cùng 1 màn đăng nhập cho cả bệnh nhân lẫn Admin, bất kỳ lỗ hổng/nhầm lẫn phân quyền nào ở đó đều ảnh hưởng tới cả 2 nhóm — tách riêng giúp mỗi cổng có thể siết chặt độc lập mà không ảnh hưởng nhóm còn lại.
- **Tránh nhầm lẫn**: nhân sự CMS cần biết chắc chắn mình đang ở "chế độ làm việc", không phải vô tình lạc vào trải nghiệm dành cho bệnh nhân.

## 2. Thiết kế màn hình — chỉ còn đăng nhập thuần, không chọn vai trò

**Nguyên tắc bắt buộc**: màn hình này chỉ hỏi email + mật khẩu. **Không có bất kỳ lựa chọn "Admin hay Biên tập chuyên môn y khoa" nào ở đây** — vai trò phải do hệ thống tự xác định từ tài khoản đã đăng nhập, không phải do người dùng tự khai. Để người dùng tự chọn vai trò của chính mình lúc đăng nhập là sai về bản chất: vai trò là thuộc tính của *tài khoản*, không phải một lựa chọn tại thời điểm phiên làm việc — tương tự như không ai thiết kế màn đăng nhập ngân hàng để khách tự chọn "tôi là khách VIP hay khách thường".

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

Dùng cùng khung bố cục 2 panel như cổng bệnh nhân (nhất quán hệ thống thiết kế), nhưng **tông màu khác hẳn** — tối, nghiêm túc hơn — để không ai nhầm 2 cổng dù nhìn thoáng qua.

Không có nút "Đăng ký" ở màn này — tài khoản nội bộ chỉ được **tạo bởi Admin** ([SCR-AD-14](SCR-AD-14-TaoTaiKhoanNhanSu.md)), không ai tự đăng ký được (khác hẳn nguyên tắc tự phục vụ của cổng bệnh nhân).

## 3. Hành vi cần có

Áp dụng **y hệt** các quy tắc đã mô tả ở `01-XacThuc-TaiKhoan/00-TongQuan.md` (chính sách mật khẩu, khoá tạm thời sau nhiều lần sai, không tiết lộ email tồn tại hay không) — dùng chung 1 bộ quy tắc bảo mật cho cả 2 cổng để không tạo ra 1 cổng "yếu hơn" cổng kia một cách vô tình.

| Khác biệt duy nhất so với cổng bệnh nhân | Vì sao |
| :--- | :--- |
| Không có luồng "Đăng ký" | Tài khoản nội bộ do Admin cấp (`SCR-AD-14`), không tự phục vụ |
| Sau đăng nhập, luôn vào Trang chủ CMS (`SCR-AD-00`) — hệ thống tự route theo vai trò đã lưu sẵn trong tài khoản, không hỏi lại người dùng | Cả 4 vai trò thấy bộ ô chức năng khác nhau trên cùng 1 Trang chủ (xem [SCR-AD-00](SCR-AD-00-TrangChu.md) §3 và ma trận phân quyền ở [SCR-AD-03](SCR-AD-03-PhanQuyenRBAC.md) §4), nhưng việc xác định "ai đang là ai" là trách nhiệm của hệ thống, không phải của người đăng nhập |

## 4. Tham chiếu

- Quy tắc mật khẩu & chống brute-force dùng chung: `01-XacThuc-TaiKhoan/00-TongQuan.md` §4.2, §4.3
- Tạo tài khoản mới: [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md) (cho bệnh nhân) / [SCR-AD-14-TaoTaiKhoanNhanSu.md](SCR-AD-14-TaoTaiKhoanNhanSu.md) (cho nhân sự nội bộ)
- Đổi vai trò tài khoản đã có: [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md)
