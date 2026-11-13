# SCR-AD-12 — Quản lý chủ đề bài viết

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0066` |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Duy trì 1 bộ chủ đề gọn gàng dùng chung cho cả bài viết lẫn FAQ — tránh phân loại nội dung lộn xộn theo thời gian |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao tách riêng màn này, không để tự tạo chủ đề ngay trong lúc viết bài

Nếu cho tự gõ tên chủ đề mới ngay trong màn soạn bài ([SCR-AD-11](SCR-AD-11-QuanLyBaiViet.md)), theo thời gian sẽ xuất hiện các biến thể trùng nghĩa ("Dạ dày", "Bệnh dạ dày", "Về dạ dày") làm bệnh nhân lọc theo chủ đề ở [SCR-PT-12-ThuVienKienThuc.md](../02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md) bị rối, bỏ sót bài liên quan nằm ở chủ đề gần giống. Quản lý tập trung ở 1 màn riêng giữ cho bộ chủ đề luôn gọn, có chủ đích.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Chủ đề bài viết & FAQ                    [ + Thêm chủ đề ]   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên chủ đề      │ Số bài viết đang dùng   │ Thao tác        │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Dạ dày          │ 12                      │ [Sửa][Xoá]      │ │
│ │ Đại tràng       │ 7                       │ [Sửa][Xoá]      │ │
│ │ Chế độ ăn       │ 9                       │ [Sửa][Xoá]      │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hiện số bài đang dùng trước khi xoá | "12 bài viết đang dùng" | Xoá 1 chủ đề đang có nhiều bài sẽ khiến các bài đó "mồ côi" chủ đề — cần biết phạm vi ảnh hưởng trước |
| Số lượng chủ đề nên gọn, có chủ đích thiết kế trước | Không khuyến khích tạo tràn lan — nên xuất phát từ các nhóm bệnh lý tiêu hoá chính (dạ dày, đại tràng, chế độ ăn, thuốc...) | Quá nhiều chủ đề nhỏ lẻ làm bộ lọc ở màn bệnh nhân kém hữu ích (quá nhiều lựa chọn gây rối thay vì giúp tìm nhanh) |

## 4. Tham chiếu

- Dùng chung cho: [SCR-AD-10-QuanLyFAQ.md](SCR-AD-10-QuanLyFAQ.md), [SCR-AD-11-QuanLyBaiViet.md](SCR-AD-11-QuanLyBaiViet.md)
- Nơi bệnh nhân lọc theo chủ đề: `02-NguoiDung-BenhNhan/SCR-PT-12-ThuVienKienThuc.md`
