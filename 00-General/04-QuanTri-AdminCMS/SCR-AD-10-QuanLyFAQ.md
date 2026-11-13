# SCR-AD-10 — Quản lý FAQ

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0062` |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Soạn & duyệt nội dung hiển thị ở [SCR-PT-11-FAQ.md](../02-NguoiDung-BenhNhan/SCR-PT-11-FAQ.md) — nội dung tĩnh, cùng 1 câu trả lời cho mọi bệnh nhân |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao nội dung này cần con người duyệt, khác với câu trả lời AI

Câu trả lời AI ở [SCR-PT-08-ChatAI.md](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md) được sinh riêng cho từng ngữ cảnh, có thể kiểm chứng qua trích dẫn. FAQ thì ngược lại — **cùng 1 nội dung, hiển thị y hệt cho mọi bệnh nhân**, không có "ngữ cảnh" để tự điều chỉnh. Vì vậy nội dung này bắt buộc phải qua **duyệt của người có chuyên môn y tế** trước khi công khai — sai 1 câu FAQ ảnh hưởng tới mọi người đọc, không giới hạn ở 1 cuộc trò chuyện.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Quản lý FAQ                            [ + Thêm câu hỏi ]    │
│  Chủ đề: [Tất cả ▾]                                            │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Câu hỏi                    │ Chủ đề      │ Trạng thái │ Thao tác│
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Thang Bristol là gì?        │ Theo dõi    │ ✓ Đã duyệt │ [Sửa]   │
│ │ AI có thay thế bác sĩ không?│ Về ứng dụng │ ✓ Đã duyệt │ [Sửa]   │
│ │ ... (câu mới soạn) ...       │ Thuốc       │ ⏳ Chờ duyệt│ [Duyệt] │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Có bước "chờ duyệt" trước khi hiển thị cho bệnh nhân | Biên tập nội dung soạn → Bác sĩ/Admin duyệt → mới công khai | Tách vai trò soạn thảo và vai trò xác nhận chuyên môn — không để 1 người vừa viết vừa tự duyệt nội dung y tế công khai cho mọi người |
| Mỗi câu hỏi gắn 1 chủ đề | Dùng chung danh mục chủ đề với bài viết kiến thức ([SCR-AD-12](SCR-AD-12-ChuDeBaiViet.md)) | Nhất quán cách phân loại nội dung trong toàn hệ thống, tránh 2 bộ chủ đề khác nhau cho FAQ và bài viết gây rối |
| Sửa câu đã duyệt cần duyệt lại | Thay đổi nội dung quay về trạng thái "chờ duyệt" | Một câu trả lời đã duyệt có thể bị sửa sai nội dung — không nên tự động công khai lại mà không qua kiểm tra |

## 4. Tham chiếu

- Nơi hiển thị: `02-NguoiDung-BenhNhan/SCR-PT-11-FAQ.md`
- Chủ đề dùng chung: [SCR-AD-12-ChuDeBaiViet.md](SCR-AD-12-ChuDeBaiViet.md)
