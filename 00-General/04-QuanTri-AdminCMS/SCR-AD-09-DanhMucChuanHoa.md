# SCR-AD-09 — Danh mục chuẩn hoá (triệu chứng, thuốc)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0060` |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Tạo ra danh sách chuẩn để bệnh nhân chọn thay vì gõ tự do ở những nơi dữ liệu sạch quan trọng hơn linh hoạt diễn đạt |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần danh mục chuẩn, dù đã để nhiều nơi khác nhập tự do

[SCR-PT-02-NhatKyAnUong.md](../02-NguoiDung-BenhNhan/SCR-PT-02-NhatKyAnUong.md) cố tình để nhập món ăn tự do (vì ẩm thực quá đa dạng, không thể liệt kê hết). Nhưng với **triệu chứng và thuốc dùng trong hồ sơ bệnh lý** ([SCR-PT-01](../02-NguoiDung-BenhNhan/SCR-PT-01-HoSoBenhLy.md)), việc để nhập tự do hoàn toàn sẽ gây hậu quả khác: cùng 1 triệu chứng, người viết "đau dạ dày", người viết "đau bao tử", người viết "viêm dạ dày" — dữ liệu không đồng nhất, khó dùng để khớp với từ khoá Triage hay thống kê chính xác. Danh mục chuẩn giải quyết đúng vấn đề này cho những nơi cần **dữ liệu sạch** hơn là sự linh hoạt diễn đạt.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Danh mục chuẩn hoá                                            │
│ ┌──────────────┬──────────┐                                   │
│ │ Bệnh lý/      │ Thuốc    │                                   │
│ │ Triệu chứng   │ thường dùng│                                   │
│ └──────────────┴──────────┘                                   │
│                                      [ + Thêm mục mới ]         │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên chuẩn hoá            │ Đang dùng ở        │ Thao tác     │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Viêm dạ dày              │ 128 hồ sơ           │ [Sửa][Xoá]   │ │
│ │ Hội chứng ruột kích thích│ 45 hồ sơ            │ [Sửa][Xoá]   │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Phải seed sẵn danh mục cơ bản trước khi bệnh nhân cần dùng | Danh mục phải có sẵn **trước** khi [SCR-PT-01](../02-NguoiDung-BenhNhan/SCR-PT-01-HoSoBenhLy.md) ra mắt, không thể để dropdown trống rồi bệnh nhân là người đầu tiên "khai phá" | Trải nghiệm chọn từ 1 dropdown rỗng là trải nghiệm hỏng — phân hệ bệnh nhân phụ thuộc trực tiếp vào phân hệ này có dữ liệu sẵn |
| Hiện số lượng đang dùng trước khi xoá | "Đang dùng ở N hồ sơ" | Xoá 1 mục danh mục đang được hàng trăm hồ sơ bệnh nhân tham chiếu là quyết định cần cân nhắc kỹ — phải biết rõ phạm vi ảnh hưởng trước khi xoá |
| Gộp/đổi tên thay vì xoá khi phát hiện trùng lặp (vd "đau dạ dày" và "viêm dạ dày" nên là 1 mục) | Có chức năng gộp 2 mục thành 1, giữ nguyên dữ liệu các hồ sơ đã chọn mục cũ | Tránh mất dữ liệu bệnh nhân đã khai báo chỉ vì dọn dẹp danh mục |

## 4. Tham chiếu

- Nơi sử dụng: `02-NguoiDung-BenhNhan/SCR-PT-01-HoSoBenhLy.md`, `SCR-PT-07-NhacThuoc.md`
- Khớp từ khoá Triage: [SCR-AD-07-TuKhoaTriage.md](SCR-AD-07-TuKhoaTriage.md)
