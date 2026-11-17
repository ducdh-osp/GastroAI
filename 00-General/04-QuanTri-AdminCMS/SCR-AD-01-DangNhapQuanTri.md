# SCR-AD-01 — Đăng nhập quản trị

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0045` |
| Actor | Admin, Biên tập chuyên môn y khoa |
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

Không có nút "Đăng ký" ở màn này — tài khoản nội bộ chỉ được **tạo bởi Admin** ([SCR-AD-03](SCR-AD-03-PhanQuyenRBAC.md)), không ai tự đăng ký được (khác hẳn nguyên tắc tự phục vụ của cổng bệnh nhân).

## 3. Hành vi cần có

Áp dụng **y hệt** các quy tắc đã mô tả ở `01-XacThuc-TaiKhoan/00-TongQuan.md` (chính sách mật khẩu, khoá tạm thời sau nhiều lần sai, không tiết lộ email tồn tại hay không) — dùng chung 1 bộ quy tắc bảo mật cho cả 2 cổng để không tạo ra 1 cổng "yếu hơn" cổng kia một cách vô tình.

| Khác biệt duy nhất so với cổng bệnh nhân | Vì sao |
| :--- | :--- |
| Không có luồng "Đăng ký" | Tài khoản nội bộ do Admin cấp (`SCR-AD-03`), không tự phục vụ |
| Sau đăng nhập, hệ thống tự route theo vai trò đã lưu sẵn trong tài khoản — không hỏi lại người dùng | Admin và Biên tập chuyên môn y khoa thấy bộ menu khác nhau ngay từ đầu (xem ma trận phân quyền ở từng màn), nhưng việc xác định "ai đang là ai" là trách nhiệm của hệ thống, không phải của người đăng nhập |

> **Ghi chú khoảng cách với code hiện tại**: bản dựng hiện tại ở `CmsLoginPage.tsx` có radio chọn "Admin"/"Bác sĩ" ngay tại màn đăng nhập (người dùng tự khai vai trò, gửi lên server để server biết tra bảng nào). Đây là thiết kế cần sửa độc lập với việc bỏ Doctor — kể cả khi còn 2 loại tài khoản, hệ thống vẫn nên tự xác định được loại tài khoản từ email nhập vào, không cần hỏi người dùng. Khi bỏ hẳn bảng Doctor riêng, vấn đề này tự nhiên biến mất vì chỉ còn 1 nơi để tra.

## 4. Tham chiếu

- Quy tắc mật khẩu & chống brute-force dùng chung: `01-XacThuc-TaiKhoan/00-TongQuan.md` §4.2, §4.3
- Tạo tài khoản mới: [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md) (cho bệnh nhân) / [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md) (cho nhân sự nội bộ)
