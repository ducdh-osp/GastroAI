# SCR-PT-11 — Câu hỏi thường gặp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0021` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Trả lời ngay những câu hỏi phổ biến nhất mà không cần chờ AI xử lý — nhanh hơn chat cho những thắc mắc đơn giản, lặp lại |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần FAQ khi đã có AI trả lời được mọi câu hỏi

Có AI không có nghĩa nên bắt mọi câu hỏi đều phải gõ ra và chờ xử lý. Những câu hỏi **lặp đi lặp lại giống nhau giữa rất nhiều người dùng** ("thang Bristol là gì", "app này có thay thế bác sĩ không", "dữ liệu của tôi có an toàn không") nên có câu trả lời **đã duyệt sẵn, hiện tức thì** — vừa nhanh hơn cho người dùng, vừa giảm tải những câu hỏi không cần tới khả năng suy luận của AI.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Câu hỏi thường gặp              [ 🔍 Tìm câu hỏi của bạn ]   │
│                                                                 │
│  Về ứng dụng                                                    │
│  ▾ AI tư vấn có thay thế được bác sĩ không?                    │
│    Không. AI đưa ra gợi ý sơ bộ dựa trên tài liệu y khoa...     │
│  ▸ Dữ liệu sức khoẻ của tôi có an toàn không?                  │
│  ▸ Tôi có thể dùng app này nếu không có bệnh nền không?        │
│                                                                 │
│  Về thang đo & theo dõi                                        │
│  ▸ Thang Bristol là gì?                                        │
│  ▸ Vì sao nên ghi nhật ký ăn uống đều đặn?                     │
│                                                                 │
│  Không tìm thấy câu trả lời?                                   │
│        [   Hỏi AI trực tiếp   ]                                │
└───────────────────────────────────────────────────────────┘
```

Nhóm theo chủ đề (không phải 1 danh sách dài không phân loại), và luôn mở sẵn ô tìm kiếm ngay đầu trang — với danh sách FAQ dài dần theo thời gian, tìm kiếm nhanh hơn cuộn tay.

## 3. Nguyên tắc

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Lối thoát khi không tìm thấy | Luôn có nút "Hỏi AI trực tiếp" ở cuối trang | FAQ chỉ phủ được câu hỏi phổ biến — không nên là ngõ cụt cho câu hỏi riêng của từng người |
| Nội dung câu trả lời | Ngắn gọn, không cố nhồi hết kiến thức vào 1 câu FAQ | Nếu câu hỏi cần giải thích sâu, nên dẫn sang [SCR-PT-12-ThuVienKienThuc.md](SCR-PT-12-ThuVienKienThuc.md) thay vì biến FAQ thành bài viết dài |
| Người biên soạn nội dung | Nội dung do đội ngũ có chuyên môn y tế của hệ thống duyệt trước khi hiển thị (xem `04-QuanTri-AdminCMS/`) | Khác với câu trả lời AI sinh ra theo ngữ cảnh, đây là nội dung tĩnh ai cũng thấy giống nhau — cần chuẩn xác tuyệt đối vì sai sót sẽ lặp lại với mọi người đọc |

## 4. Tham chiếu

- Đọc sâu hơn: [SCR-PT-12-ThuVienKienThuc.md](SCR-PT-12-ThuVienKienThuc.md)
- Hỏi riêng cho trường hợp của mình: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
- Quản lý nội dung: `04-QuanTri-AdminCMS/` (UC0062)
