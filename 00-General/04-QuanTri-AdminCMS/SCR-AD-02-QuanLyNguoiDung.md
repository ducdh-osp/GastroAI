# SCR-AD-02 — Quản lý người dùng (bệnh nhân)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0046` |
| Actor | Admin (chỉ Admin — Biên tập chuyên môn y khoa không có quyền ở màn này) |
| Mục tiêu nghiệp vụ | Cho Admin xử lý các tình huống cần can thiệp vào tài khoản bệnh nhân (vi phạm, yêu cầu hỗ trợ, nghi ngờ gian lận) |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao chỉ Admin

Khoá/mở tài khoản là quyết định **quản trị hệ thống**, không phải quyết định chuyên môn y tế — người biên tập nội dung y khoa giỏi không nhất thiết nên có quyền khoá tài khoản người dùng. Tách rõ ranh giới này tránh tình trạng cấp quyền "tiện thể" rồi dần dần vai trò nội dung có toàn quyền hệ thống ngoài ý muốn.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Quản lý người dùng              [ 🔍 Tìm theo tên/email ]    │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên            │ Email            │ Trạng thái │ Thao tác │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Nguyễn Văn A    │ a@gmail.com      │ ● Hoạt động│ [Khoá]   │ │
│ │ Trần Thị B      │ b@gmail.com      │ ⊘ Đã khoá  │ [Mở khoá]│ │
│ └─────────────────────────────────────────────────────────┘ │
│                                              ‹ 1 2 3 ... 20 › │
└───────────────────────────────────────────────────────────┘

— Khi bấm "Khoá" —
┌─────────────────────────────────┐
│  Khoá tài khoản này?                 │
│  Bệnh nhân sẽ không thể đăng nhập    │
│  cho tới khi được mở khoá lại.       │
│  Lý do (ghi vào nhật ký hoạt động):  │
│  [___________________________]       │
│       [Huỷ]  [  Xác nhận khoá  ]     │
└─────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hạn chế thông tin hiển thị | Chỉ hiện tên, email, trạng thái tài khoản — **không** hiện trực tiếp nhật ký sức khoẻ/hồ sơ bệnh lý ở bảng danh sách này | Đây là màn quản lý *tài khoản*, không phải màn xem hồ sơ y tế — tối thiểu hoá hiển thị dữ liệu nhạy cảm đúng với mục đích sử dụng |
| Bắt buộc ghi lý do khi khoá | Ô nhập lý do không được bỏ trống | Khoá tài khoản ảnh hưởng trực tiếp quyền truy cập của bệnh nhân vào dữ liệu sức khoẻ của chính họ — quyết định này cần có căn cứ truy vết lại được (ghi vào Audit Log, `SCR-AD-06`) |
| Khoá không xoá dữ liệu | Tài khoản bị khoá vẫn giữ nguyên toàn bộ nhật ký/hồ sơ, chỉ chặn đăng nhập | Khoá là biện pháp tạm thời/kỷ luật, không phải xoá bỏ — bệnh nhân khi được mở khoá lại cần thấy dữ liệu của mình còn nguyên vẹn |

## 4. Tham chiếu

- Phân quyền hệ thống (khác với quản lý tài khoản bệnh nhân): [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md)
- Mọi thao tác khoá/mở đều ghi nhận ở: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md)
