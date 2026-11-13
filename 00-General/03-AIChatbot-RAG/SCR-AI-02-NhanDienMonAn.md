# SCR-AI-02 — Nhận diện món ăn qua ảnh

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0044` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Giảm ma sát khi ghi nhật ký ăn uống — chụp ảnh thay vì gõ chữ, đặc biệt hữu ích khi đang ở ngoài, không tiện gõ |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao tính năng này, và vì sao nó là "có thì tốt" chứ không bắt buộc

Ghi nhật ký ăn uống đều đặn là nền tảng cho [SCR-PT-10-NhayCamThucPham.md](../02-NguoiDung-BenhNhan/SCR-PT-10-NhayCamThucPham.md) — nhưng gõ mô tả món ăn mỗi bữa là một ma sát nhỏ, lặp lại nhiều lần mỗi ngày, dễ khiến người dùng nản sau vài tuần. Chụp ảnh nhanh hơn gõ chữ trong hầu hết tình huống thực tế (đang ở quán ăn, đang di chuyển). Tuy vậy đây là tính năng bổ sung, **không nên là cách duy nhất** để ghi nhật ký — gõ tay tự do vẫn phải luôn sẵn có, vì nhận diện ảnh không phải lúc nào cũng chính xác với ẩm thực Việt Nam đa dạng.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────┐
│  Chụp ảnh món ăn                          │
│                                            │
│         [   📷 Chụp ảnh   ]              │
│         [   🖼 Chọn từ thư viện  ]         │
└───────────────────────────────────────┘

— Sau khi nhận diện —
┌───────────────────────────────────────┐
│  [ảnh vừa chụp]                            │
│                                            │
│  AI nhận diện được:                        │
│  ✓ Phở bò                    (độ tin cậy cao)│
│  ? Có thể có: rau sống, giá đỗ               │
│                                            │
│  Mức FODMAP ước tính: Trung bình           │
│  ⓘ Chỉ là ước tính, có thể chỉnh sửa nếu    │
│    không đúng với món thực tế của bạn.      │
│                                            │
│  [ Sửa lại nếu chưa đúng ]                 │
│  [      Lưu vào nhật ký ăn uống      ]     │
└───────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Luôn cho sửa kết quả nhận diện | Nút "Sửa lại" rõ ràng, không ẩn | Nhận diện ảnh món ăn Việt Nam (vốn đa dạng vùng miền, biến tấu nhiều) sẽ có lúc sai hoặc thiếu chi tiết — phải luôn cho người dùng tự sửa, không ép dùng kết quả AI như duy nhất đúng |
| Không khẳng định chắc chắn với thông tin chưa chắc | Dùng "có thể có", kèm "độ tin cậy" | Tránh tạo cảm giác AI "biết tuyệt đối" — với dữ liệu sẽ dùng để phân tích sức khoẻ sau này ([SCR-PT-10](../02-NguoiDung-BenhNhan/SCR-PT-10-NhayCamThucPham.md)), độ chính xác đầu vào ảnh hưởng trực tiếp tới chất lượng phân tích |
| Mức FODMAP chỉ là ước tính tham khảo | Luôn kèm dòng giải thích giới hạn | Phân loại FODMAP chính xác cần biết rõ thành phần/cách chế biến thật — ước tính từ ảnh chỉ mang tính gợi ý ban đầu, không nên dùng để tự ý loại bỏ món ăn khỏi khẩu phần mà không kiểm chứng |
| Sau khi lưu, hành vi giống hệt nhập tay | Dữ liệu lưu vào cùng 1 nơi với [SCR-PT-02-NhatKyAnUong.md](../02-NguoiDung-BenhNhan/SCR-PT-02-NhatKyAnUong.md) | Đây chỉ là 1 cách nhập liệu khác, không phải 1 loại dữ liệu khác — tránh tạo 2 "nguồn sự thật" tách biệt cho cùng 1 khái niệm (nhật ký ăn uống) |

## 4. Tham chiếu

- Nơi dữ liệu được lưu & dùng tiếp: `02-NguoiDung-BenhNhan/SCR-PT-02-NhatKyAnUong.md`, `SCR-PT-10-NhayCamThucPham.md`
