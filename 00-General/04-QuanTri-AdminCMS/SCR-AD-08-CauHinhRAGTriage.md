# SCR-AD-08 — Cấu hình tham số RAG/Triage

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0059` |
| Actor | Admin (chỉ Admin — đây là cấu hình kỹ thuật ảnh hưởng toàn hệ thống, không phải nội dung chuyên môn thường ngày) |
| Mục tiêu nghiệp vụ | Cho phép tinh chỉnh cách AI hoạt động mà không cần sửa code mỗi lần — nhưng vẫn đủ giới hạn để tránh chỉnh sai làm hỏng cả hệ thống |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao màn hình này cần thận trọng hơn hẳn các màn cấu hình khác

Khác với cấu hình nội dung (từ khoá, FAQ, bài viết — sai thì sửa lại dễ dàng, ảnh hưởng giới hạn), các tham số ở đây **ảnh hưởng trực tiếp tới hành vi lõi của AI cho mọi bệnh nhân cùng lúc**: đặt sai 1 ngưỡng có thể khiến AI tìm sai tài liệu hàng loạt, hoặc khiến lớp an toàn Triage kém nhạy đi mà không ai nhận ra ngay. Vì vậy màn hình này nên **hạn chế ở Admin**, có giải thích rõ ràng từng tham số ảnh hưởng gì, không chỉ là 1 form chỉnh số khô khan.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Cấu hình RAG & Triage                                         │
│                                                                 │
│  Truy xuất tài liệu (RAG)                                      │
│  Số đoạn tài liệu tham khảo mỗi câu trả lời     [ 5 ▾ ]        │
│  ⓘ Nhiều hơn → câu trả lời đầy đủ hơn nhưng chậm hơn            │
│                                                                 │
│  Ngưỡng mức độ liên quan tối thiểu              [ 0.7 ▾ ]      │
│  ⓘ Thấp hơn → tìm được nhiều hơn nhưng dễ lẫn nội dung không   │
│    liên quan                                                    │
│                                                                 │
│  Kênh thông báo khi có cảnh báo Triage                         │
│  ☑ Trong ứng dụng    ☑ Email cho Admin trực                    │
│                                                                 │
│                              [     Lưu thay đổi     ]          │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Mỗi tham số luôn kèm giải thích ảnh hưởng (không chỉ là 1 con số trần trụi) | Ghi chú ngay dưới mỗi ô, bằng ngôn ngữ nghiệp vụ, không phải thuật ngữ kỹ thuật | Người chỉnh tham số (Admin) không nhất thiết hiểu sâu kỹ thuật RAG — cần hiểu được đánh đổi thực tế để quyết định đúng, không chỉnh theo cảm tính |
| Giá trị mặc định hợp lý sẵn, không bắt buộc phải chỉnh | Nếu không đụng vào, hệ thống vẫn chạy tốt với giá trị mặc định | Tính năng này "có thể cắt nếu thiếu thời gian" theo đúng tinh thần ưu tiên của dự án — không nên là 1 bước bắt buộc phải cấu hình mới chạy được |
| Không cho chỉnh tham số liên quan tới **ngưỡng phát hiện nguy hiểm** ở màn này | Ngưỡng độ nhạy Triage không nằm trong form số liệu tự do — xử lý riêng, thận trọng hơn (gần với §3.1 của `03-AIChatbot-RAG/00-TongQuan.md`: luôn thiên về cảnh báo nhiều hơn bỏ sót) | Tránh tình trạng 1 thao tác chỉnh cấu hình "vô tình" làm giảm độ nhạy an toàn mà không ai nhận ra ngay hậu quả |

## 4. Tham chiếu

- Nguyên tắc vận hành RAG/Triage mà các tham số này điều chỉnh: `03-AIChatbot-RAG/00-TongQuan.md`
