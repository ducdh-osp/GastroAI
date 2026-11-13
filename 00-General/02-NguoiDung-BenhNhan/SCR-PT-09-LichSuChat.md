# SCR-PT-09 — Lịch sử trò chuyện

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0018` (xem lại phiên chat), `UC0027` (xuất 1 phiên ra PDF) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Cho bệnh nhân quay lại một lời tư vấn cũ bất cứ lúc nào — tư vấn sức khoẻ không phải thứ đọc 1 lần rồi quên |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao "xem lại" quan trọng với tư vấn sức khoẻ hơn là với chat thông thường

Một câu trả lời về triệu chứng, thuốc, hay chế độ ăn thường cần đọc lại nhiều lần, có khi vài tuần sau khi tái phát triệu chứng tương tự. Khác với chat hỏi-đáp giải trí (hỏi xong quên), tư vấn sức khoẻ cần được **lưu trữ dễ tìm lại** — đây là lý do màn hình này tồn tại riêng, không chỉ cuộn lên trong 1 cuộc chat dài vô tận.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Lịch sử trò chuyện                     [ 🔍 Tìm kiếm ]        │
│                                                                 │
│  Hôm nay                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 💬 "Tôi hay bị đầy bụng sau khi ăn đồ cay..."                │ │
│  │    12 tin nhắn · 14:32                                      │ │
│  └─────────────────────────────────────────────────────────┘ │
│  Tuần trước                                                     │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 💬 "Thuốc Omeprazole uống lúc nào là đúng?"                 │ │
│  │    6 tin nhắn · 08/11                     [Xuất PDF ⬇]     │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

Tiêu đề mỗi phiên lấy từ câu hỏi đầu tiên (không bắt bệnh nhân tự đặt tên) — họ sẽ không bao giờ dành thời gian đặt tên cho 1 cuộc trò chuyện, nhận diện qua nội dung là cách duy nhất thực tế.

## 3. Nguyên tắc

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Tìm kiếm | Tìm theo nội dung câu hỏi/trả lời, không chỉ theo ngày | Sau vài tháng dùng app, bệnh nhân khó nhớ chính xác "mình hỏi về việc này hôm nào" — chỉ nhớ nội dung |
| Mở lại 1 phiên cũ | Hiện lại đầy đủ như lúc trò chuyện (kèm trích dẫn nguồn, nút đánh giá vẫn giữ nguyên trạng thái đã chọn trước đó) | Xem lại không nên là 1 bản tóm tắt cụt lủn — giá trị nằm ở chi tiết gốc |
| Xuất PDF 1 phiên | Có ở từng phiên — khác với xuất toàn bộ hồ sơ sức khoẻ ([SCR-PT-01](SCR-PT-01-HoSoBenhLy.md) §5), đây là xuất **đúng 1 cuộc tư vấn cụ thể** để lưu riêng hoặc đưa bác sĩ xem về đúng vấn đề đó | Nhu cầu khác nhau: đôi khi chỉ cần mang đúng 1 câu trả lời về 1 loại thuốc, không cần cả hồ sơ sức khoẻ đầy đủ |

## 4. Tham chiếu

- Tiếp tục trò chuyện: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
- Xuất toàn bộ hồ sơ sức khoẻ: [SCR-PT-01-HoSoBenhLy.md](SCR-PT-01-HoSoBenhLy.md) §5
