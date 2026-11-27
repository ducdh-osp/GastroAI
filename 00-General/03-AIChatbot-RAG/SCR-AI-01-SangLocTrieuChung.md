# SCR-AI-01 — Bộ câu hỏi sàng lọc triệu chứng có cấu trúc

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0039` (hỏi-đáp theo bước), `UC0041` (gợi ý mức độ chăm sóc ở cuối) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Thu thập thông tin triệu chứng đầy đủ, có hệ thống — dành cho những lúc bệnh nhân không biết nên bắt đầu mô tả từ đâu |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần thêm hình thức này, bên cạnh chat tự do

Chat tự do ([SCR-PT-08-ChatAI.md](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md)) phù hợp khi bệnh nhân đã biết rõ muốn hỏi gì. Nhưng nhiều người gặp triệu chứng mới lại **không biết nên mô tả thế nào cho đủ** — họ có thể quên kể những chi tiết quan trọng (kéo dài bao lâu, có kèm sốt không, ăn gì trước đó...) nếu chỉ gõ tự do 1-2 câu. Hình thức hỏi-đáp theo từng bước, giống các mô hình kiểm tra triệu chứng phổ biến trong ngành y tế số, giải quyết đúng vấn đề này: hệ thống **chủ động hỏi đúng những câu cần hỏi**, theo một trình tự hợp lý, đảm bảo không bỏ sót thông tin quan trọng cho việc đánh giá.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Kiểm tra triệu chứng                      Bước 2/5            │
│  ████████░░░░░░░░░░░░░░░░                                     │
│                                                                 │
│  Cơn đau bắt đầu từ khi nào?                                   │
│                                                                 │
│  ( ) Vài giờ trước, đột ngột                                   │
│  ( ) 1-2 ngày nay                                              │
│  ( ) Hơn 1 tuần, âm ỉ                                          │
│  ( ) Tái đi tái lại nhiều lần trong thời gian dài               │
│                                                                 │
│                                    [ ← Quay lại ] [ Tiếp theo → ]│
└───────────────────────────────────────────────────────────┘

— Màn kết quả cuối cùng —
┌───────────────────────────────────────────────────────────┐
│  Kết quả sàng lọc                                               │
│                                                                 │
│  🟡 Mức độ: Nên sắp xếp gặp bác sĩ trong vài ngày tới          │
│                                                                 │
│  Dựa trên: cơn đau kéo dài hơn 1 tuần kèm đầy bụng sau ăn,      │
│  đây không phải dấu hiệu cấp cứu, nhưng không nên trì hoãn      │
│  quá lâu vì có thể liên quan tới viêm dạ dày cần điều trị.      │
│                                                                 │
│  ⓘ Đây là gợi ý sơ bộ, không thay thế chẩn đoán của bác sĩ.    │
│                                                                 │
│      [  Trò chuyện thêm với AI về kết quả này  ]               │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc thiết kế luồng hỏi-đáp

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hiện rõ tiến độ | Thanh tiến trình + "Bước X/Y" | Người đang lo lắng về sức khoẻ cần biết "còn bao lâu nữa xong" — không biết điểm kết thúc dễ gây mất kiên nhẫn giữa chừng |
| Cho quay lại sửa câu trả lời trước | Nút "Quay lại" luôn có | Bệnh nhân có thể nhớ ra thêm chi tiết hoặc muốn sửa câu trả lời trước đó khi đọc câu hỏi sau — không nên là luồng 1 chiều cứng nhắc |
| Câu hỏi đầu tiên luôn kiểm tra dấu hiệu nguy hiểm rõ ràng nhất | Đặt câu hỏi sàng lọc red-flag (chảy máu, đau dữ dội đột ngột...) ngay từ đầu, không để cuối | Nếu có dấu hiệu nguy hiểm rõ ràng, nên cảnh báo **ngay lập tức** thay vì bắt hoàn thành hết 5 bước mới biết — vài phút chờ đợi không cần thiết có thể là vấn đề với tình huống thật sự khẩn cấp |
| Kết quả luôn giải thích "dựa trên gì" | Không chỉ đưa ra mức độ, phải nêu rõ yếu tố nào dẫn tới kết luận đó | Giúp bệnh nhân hiểu được logic, tăng độ tin cậy, và tự nhận ra nếu mình trả lời thiếu chính xác ở bước nào |
| Áp dụng chung bộ câu hỏi nhưng phân ngưỡng theo nhóm đối tượng | Dùng chung luồng 5 bước, song nhóm `Dưới 18 tuổi` và `Đang mang thai / mới sinh` (hoặc `Không chắc`) áp ngưỡng nâng cảnh báo sớm và khuyến nghị gặp nhân viên y tế thận trọng hơn người trưởng thành | Tránh bỏ sót nguy cơ ở trẻ em/thai kỳ mà không bỏ phí câu trả lời chi tiết của người dùng (theo [00-TongQuan.md](00-TongQuan.md) §3.1, §3.3) |
| Luôn có lối dẫn sang chat tự do ở cuối | "Trò chuyện thêm với AI về kết quả này" | Bộ câu hỏi cố định không thể bao quát hết mọi tình huống — sau khi có kết quả sơ bộ, bệnh nhân nên có thể hỏi thêm chi tiết theo cách tự nhiên |

## 4. Tham chiếu

- Nguyên tắc phân loại mức độ: [00-TongQuan.md](00-TongQuan.md) §3.3
- Hỏi tiếp theo cách tự do: `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`
