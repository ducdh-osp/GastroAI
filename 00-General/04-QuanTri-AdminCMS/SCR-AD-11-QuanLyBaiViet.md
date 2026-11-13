# SCR-AD-11 — Quản lý bài viết kiến thức

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0065` |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Soạn nội dung giáo dục chiều sâu hơn FAQ, hiển thị ở [SCR-PT-12-ThuVienKienThuc.md](../02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md) |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với FAQ ([SCR-AD-10](SCR-AD-10-QuanLyFAQ.md))

FAQ trả lời 1 câu hỏi ngắn gọn. Bài viết kiến thức là nội dung **dài hơn, có chiều sâu**, viết theo dạng bài đọc hoàn chỉnh (không phải hỏi-đáp) — phù hợp để giải thích 1 chủ đề đầy đủ (vd "Hiểu đúng về trào ngược dạ dày-thực quản") thay vì trả lời 1 câu hỏi cụ thể.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Quản lý bài viết kiến thức              [ + Viết bài mới ]   │
│  Chủ đề: [Tất cả ▾]   Trạng thái: [Tất cả ▾]                  │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tiêu đề                   │ Chủ đề  │ Trạng thái │ Thao tác  │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Hiểu đúng về trào ngược    │ Dạ dày  │ ✓ Đã đăng  │ [Sửa][Ẩn] │ │
│ │ dạ dày-thực quản            │         │            │           │ │
│ │ Chế độ ăn low-FODMAP...     │ Chế độ ăn│⏳ Bản nháp│ [Sửa][Xoá]│ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Trình soạn thảo bài viết —
┌───────────────────────────────────────────────────────────┐
│  Tiêu đề        [___________________________________]        │
│  Chủ đề         [ Dạ dày ▾ ]                                   │
│  Ảnh minh hoạ    [ Tải ảnh lên ]                                │
│  Nội dung                                                       │
│  [___________________________________________________]        │
│  [___________________________________________________]        │
│       [Lưu nháp]              [    Đăng bài    ]              │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Có trạng thái "bản nháp" tách biệt với "đã đăng" | Lưu nháp không công khai ngay | Bài viết dài cần thời gian soạn, có thể cần nhiều lần chỉnh sửa trước khi sẵn sàng công khai — không nên ép xong 1 lần |
| Có thể "ẩn" bài đã đăng mà không xoá hẳn | Nút "Ẩn" riêng với "Xoá" | Khi nội dung cần rà soát lại (vd phác đồ điều trị thay đổi), ẩn tạm thời trong lúc cập nhật tốt hơn xoá hẳn rồi mất nội dung gốc để tham khảo khi viết lại |
| Ảnh minh hoạ nên khuyến khích, không bắt buộc | Có nhưng không chặn đăng bài nếu thiếu | Bài có ảnh minh hoạ hấp dẫn hơn với bệnh nhân khi duyệt thư viện ([SCR-PT-12](../02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md)), nhưng không nên là rào cản khiến người biên soạn trì hoãn đăng nội dung quan trọng chỉ vì chưa tìm được ảnh phù hợp |

## 4. Tham chiếu

- Nơi hiển thị: `02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md`
- Quản lý chủ đề: [SCR-AD-12-ChuDeBaiViet.md](SCR-AD-12-ChuDeBaiViet.md)
