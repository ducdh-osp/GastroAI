# SCR-AD-03 — Phân quyền nhân sự nội bộ (RBAC)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0047` |
| Actor | Admin |
| Mục tiêu nghiệp vụ | Gán đúng vai trò cho từng nhân sự nội bộ — nền tảng để mọi màn hình khác biết "ai được thấy gì, làm gì" |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với "Quản lý người dùng" ([SCR-AD-02](SCR-AD-02-QuanLyNguoiDung.md))

Màn kia quản lý **bệnh nhân** (chỉ có khoá/mở, không có khái niệm "vai trò"). Màn này quản lý **nhân sự nội bộ** (Admin, Biên tập chuyên môn y khoa) — khái niệm hoàn toàn khác: ở đây việc cần làm là xác định *mức độ quyền hạn*, không phải *cho phép truy cập hay không*.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Phân quyền nhân sự               [ 🔍 Tìm theo tên/email ]   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên              │ Email          │ Vai trò hiện tại      │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Phạm Thị D        │ d@gastroai.vn  │ [ Biên tập chuyên    │ │
│ │                    │                │   môn y khoa     ▾ ] │ │
│ │ Admin Chính        │ admin@...      │ [ Admin          ▾ ]  │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Khi đổi vai trò —
┌─────────────────────────────────┐
│  Đổi vai trò cho "Phạm Thị D"?       │
│  Từ: Biên tập chuyên môn y khoa →    │
│       Admin                           │
│  ⚠ Admin có toàn quyền hệ thống,     │
│    bao gồm cả quản lý tài khoản và   │
│    xem nhật ký hoạt động.             │
│       [Huỷ]  [  Xác nhận đổi  ]      │
└─────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Số vai trò tối thiểu, mỗi vai trò có ranh giới rõ | 2 vai trò: Admin, Biên tập chuyên môn y khoa — không tạo thêm vai trò "lưng chừng" không rõ ràng (đã bỏ "Bác sĩ" như 1 tài khoản vận hành riêng — AI đảm nhận phần tư vấn trực tiếp, xem `00-TongQuan.md` §0) | Càng nhiều vai trò mập mờ, càng khó kiểm soát và dễ xảy ra "quyền leo thang" không kiểm soát được theo thời gian |
| Cảnh báo rõ khi nâng quyền lên Admin | Xác nhận kèm giải thích cụ thể Admin có thể làm gì | Nâng quyền lên mức cao nhất là quyết định quan trọng — không nên để xảy ra "lỡ tay" chỉ vì thao tác dropdown quá dễ dàng |
| Mọi thay đổi vai trò đều ghi vào Audit Log | Tự động, không cần Admin tự nhớ ghi chú | Đổi phân quyền là 1 trong các thao tác nhạy cảm bắt buộc phải truy vết — xem `00-TongQuan.md` §5 |

## 4. Tham chiếu

- Quản lý tài khoản bệnh nhân (khác khái niệm): [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md)
- Lịch sử thay đổi phân quyền: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md)
