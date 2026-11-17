# SCR-AD-11 — Quản lý bài viết kiến thức

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0065` |
| Actor | Admin hoặc Biên tập chuyên môn y khoa (soạn), Biên tập chuyên môn y khoa (duyệt trước khi đăng) |
| Mục tiêu nghiệp vụ | Soạn nội dung giáo dục chiều sâu hơn FAQ, hiển thị ở [SCR-PT-12-ThuVienKienThuc.md](../02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md) |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

> **Trung thực học thuật** (giống [SCR-AD-10](SCR-AD-10-QuanLyFAQ.md)): "duyệt" nghĩa là đối chiếu với tài liệu y khoa đã công bố, không phải bác sĩ hành nghề thẩm định — không dùng ngôn từ ngụ ý khác với thực tế.

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
│ │ Tương tác thuốc PPI cần biết │ Thuốc   │🔎 Chờ duyệt│[Duyệt] │ │
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
│       [Lưu nháp]              [    Gửi duyệt    ]              │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Có trạng thái "bản nháp" tách biệt với "đã đăng", và **"Đăng bài" chỉ là gửi duyệt, không công khai ngay** | Lưu nháp → gửi duyệt → Biên tập chuyên môn y khoa đối chiếu nguồn, bấm duyệt riêng → mới thật sự hiển thị cho bệnh nhân | Bài dài, chiều sâu hơn FAQ — rủi ro nội dung sai còn cao hơn (xem [SCR-AD-10](SCR-AD-10-QuanLyFAQ.md) §1), không nên để tác giả tự xuất bản thẳng mà thiếu bước đối chiếu nguồn riêng |
| Có thể "ẩn" bài đã đăng mà không xoá hẳn | Nút "Ẩn" riêng với "Xoá" | Khi nội dung cần rà soát lại (vd phác đồ điều trị thay đổi), ẩn tạm thời trong lúc cập nhật tốt hơn xoá hẳn rồi mất nội dung gốc để tham khảo khi viết lại |
| Ảnh minh hoạ nên khuyến khích, không bắt buộc | Có nhưng không chặn đăng bài nếu thiếu | Bài có ảnh minh hoạ hấp dẫn hơn với bệnh nhân khi duyệt thư viện ([SCR-PT-12](../02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md)), nhưng không nên là rào cản khiến người biên soạn trì hoãn đăng nội dung quan trọng chỉ vì chưa tìm được ảnh phù hợp |

## 4. Tham chiếu

- Nơi hiển thị: `02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md`
- Quản lý chủ đề: [SCR-AD-12-ChuDeBaiViet.md](SCR-AD-12-ChuDeBaiViet.md)
