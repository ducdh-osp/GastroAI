# SCR-AD-10 — Quản lý FAQ

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0062` |
| Actor | Admin hoặc Biên tập chuyên môn y khoa (soạn), Biên tập chuyên môn y khoa (duyệt) |
| Mục tiêu nghiệp vụ | Soạn & duyệt nội dung hiển thị ở [SCR-PT-11-FAQ.md](../02-NguoiDung-BenhNhan/SCR-PT-11-FAQ.md) — nội dung tĩnh, cùng 1 câu trả lời cho mọi bệnh nhân |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao nội dung này cần con người duyệt, khác với câu trả lời AI

Câu trả lời AI ở [SCR-PT-08-ChatAI.md](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md) được sinh riêng cho từng ngữ cảnh, có thể kiểm chứng qua trích dẫn. FAQ thì ngược lại — **cùng 1 nội dung, hiển thị y hệt cho mọi bệnh nhân, và mang nhãn "đã duyệt" nên được tin gần như tuyệt đối** — không có "ngữ cảnh" giảm nhẹ như 1 câu trả lời chat đơn lẻ. Sai 1 câu FAQ ảnh hưởng tới mọi người đọc cùng lúc, có thể nằm im rất lâu trước khi ai đó tình cờ phát hiện (ví dụ thực tế: 1 FAQ về dùng thuốc ức chế axit dạ dày lâu dài mà thiếu cảnh báo nguy cơ giảm hấp thu vi chất ở người lớn tuổi). Vì vậy nội dung này bắt buộc phải qua **duyệt, đối chiếu với tài liệu y khoa đã công bố** trước khi công khai — đây là mức độ rủi ro cao hơn cả kho tài liệu RAG ([SCR-AD-04](SCR-AD-04-KhoTaiLieuYKhoa.md) §3.1b), nên không được bỏ qua bước này dù nghe có vẻ chỉ là "vài câu hỏi-đáp ngắn".

> **Trung thực học thuật**: "duyệt" ở đây nghĩa là Biên tập chuyên môn y khoa đối chiếu nội dung với tài liệu y khoa đã công bố (phác đồ, hướng dẫn điều trị chính thức) — **không phải bác sĩ hành nghề thẩm định**. Không dùng ngôn từ ngụ ý có bác sĩ xác nhận nếu thực tế không có.

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
| Có bước "chờ duyệt" trước khi hiển thị cho bệnh nhân, tách biệt rõ với bước soạn | Người soạn (Admin hoặc Biên tập chuyên môn y khoa) viết xong → bắt buộc chuyển trạng thái "chờ duyệt" → Biên tập chuyên môn y khoa đối chiếu nguồn và bấm duyệt riêng, không tự động công khai ngay lúc lưu | Tách bước soạn thảo và bước xác nhận — kể cả khi 1 cùng người vừa soạn vừa duyệt (team nhỏ khó tránh), việc phải dừng lại ở 1 bước riêng, có chủ đích đối chiếu nguồn, vẫn tốt hơn hẳn công khai ngay không qua kiểm tra lại |
| Mỗi câu hỏi gắn 1 chủ đề | Dùng chung danh mục chủ đề với bài viết kiến thức ([SCR-AD-12](SCR-AD-12-ChuDeBaiViet.md)) | Nhất quán cách phân loại nội dung trong toàn hệ thống, tránh 2 bộ chủ đề khác nhau cho FAQ và bài viết gây rối |
| Sửa câu đã duyệt cần duyệt lại | Thay đổi nội dung quay về trạng thái "chờ duyệt" | Một câu trả lời đã duyệt có thể bị sửa sai nội dung — không nên tự động công khai lại mà không qua kiểm tra |

## 4. Tham chiếu

- Nơi hiển thị: `02-NguoiDung-BenhNhan/SCR-PT-11-FAQ.md`
- Chủ đề dùng chung: [SCR-AD-12-ChuDeBaiViet.md](SCR-AD-12-ChuDeBaiViet.md)
