# SCR-NT-01 — Trung tâm thông báo

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0073` (cài đặt), `UC0076` (trung tâm thông báo) |
| Actor | Bệnh nhân, Admin (dùng chung 1 kiểu màn hình, dữ liệu riêng biệt từng người) |
| Mục tiêu nghiệp vụ | Nơi lưu vết lại mọi cảnh báo/nhắc nhở đã từng gửi — vì thông báo đẩy (popup) chỉ xuất hiện 1 lần rồi biến mất, cần 1 nơi xem lại |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần màn hình này dù đã có thông báo đẩy

Thông báo dạng popup (vd nhắc thuốc ở `02-NguoiDung-BenhNhan/SCR-PT-07-NhacThuoc.md`) chỉ xuất hiện đúng lúc đó — nếu bệnh nhân không để ý hoặc vô tình đóng đi, thông tin mất luôn. Màn hình này là "hộp thư đến" lưu lại toàn bộ, để xem lại bất cứ lúc nào.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Thông báo                    [⚙ Cài đặt]  [Đánh dấu đã đọc]  │
│  Lọc: [Tất cả ▾]  ☐ Chỉ hiện chưa đọc                          │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ● 💊 Đã tới giờ uống Omeprazole        14:00 · Chưa đọc     │ │
│ │ ○ 🛡 Đăng nhập mới từ thiết bị lạ        hôm qua              │ │
│ │ ○ 📋 Bạn chưa ghi nhật ký hôm nay        hôm qua              │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Mở "Cài đặt" —
┌─────────────────────────────────┐
│  Cài đặt thông báo                   │
│  ☑ Nhắc uống thuốc                   │
│  ☑ Nhắc ghi nhật ký                  │
│  ☑ Cảnh báo bảo mật                  │
│  ☑ Thông báo hệ thống                │
│  ⓘ Cảnh báo an toàn khẩn cấp luôn     │
│    được gửi, không thể tắt.           │
└─────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Cho tắt từng loại thông báo riêng lẻ | Không phải "tắt tất cả hoặc không gì cả" | Một người có thể muốn tắt nhắc ghi nhật ký (thấy phiền) nhưng vẫn muốn giữ cảnh báo bảo mật |
| Cảnh báo an toàn khẩn cấp (Triage) không thể tắt | Loại trừ khỏi danh sách có thể tắt, nói rõ lý do | Đây là thông báo liên quan an toàn tính mạng — không nên để tuỳ chọn cá nhân vô tình làm mất đi lớp bảo vệ quan trọng nhất |
| Phân biệt rõ đã đọc/chưa đọc | Chấm tròn + đổi màu nền dòng chưa đọc | Giúp nhận ra ngay thông báo nào còn mới, tránh bỏ sót |
| Admin và bệnh nhân dùng chung giao diện này nhưng dữ liệu hoàn toàn tách biệt | Admin không bao giờ thấy thông báo của bệnh nhân và ngược lại | Nhất quán trải nghiệm trong toàn hệ thống không có nghĩa là trộn lẫn dữ liệu của 2 nhóm người dùng khác nhau |

## 4. Tham chiếu

- Các nguồn phát sinh thông báo: xem [00-TongQuan.md](00-TongQuan.md) §1 (bảng UC không có màn riêng)
