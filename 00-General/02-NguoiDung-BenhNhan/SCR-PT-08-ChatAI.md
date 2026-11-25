# SCR-PT-08 — Trò chuyện với AI

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0017` (màn hình chat) — vận hành cùng `UC0035`, `UC0042`, `UC0043` ở `03-AIChatbot-RAG/` xuất hiện ngay trên màn này |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Hỏi đáp thông tin tiêu hoá theo thời gian thực, có nguồn tham khảo khi truy xuất được tài liệu; cá nhân hoá từ hồ sơ là mục tiêu chưa triển khai |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khả năng hiện tại và mục tiêu cá nhân hoá

Chat hiện dùng câu hỏi đang gửi, ảnh/tài liệu đính kèm và các đoạn tài liệu truy xuất từ kho RAG. Cả `RagQueryService` và `StreamingRagQueryService` **chưa tự đọc hồ sơ bệnh lý, nhật ký ăn uống/Bristol hoặc lịch thuốc**. Có hồ sơ đã lưu không có nghĩa AI biết tiền sử, dị ứng hay thuốc của bệnh nhân; người dùng cần cung cấp thông tin liên quan trong câu hỏi hoặc tài liệu đính kèm.

Việc đưa dữ liệu hồ sơ vào ngữ cảnh AI vẫn là mục tiêu thiết kế, thống nhất với phần hiện trạng trong [SCR-PT-01](SCR-PT-01-HoSoBenhLy.md). Giao diện và ví dụ hội thoại cần phản ánh đúng các khả năng đang có:

1. **Hiển thị nguồn tham khảo khi có kết quả RAG** — các đoạn hiển thị đã được xử lý như mô tả ở §3; có nguồn không tự bảo đảm mọi nội dung sinh ra là chính xác.
2. **Dùng thông tin người dùng gửi cùng câu hỏi** — không khẳng định đã đọc dữ liệu trong Hồ sơ bệnh lý hoặc nhật ký.
3. **Chủ động phát hiện nguy hiểm** — không chỉ trả lời câu hỏi được hỏi, mà cảnh báo ngay cả khi bệnh nhân vô tình mô tả một dấu hiệu nghiêm trọng mà không biết nó nghiêm trọng.
4. **Gợi ý câu hỏi tiếp theo** — tối đa 3 câu; có thể không có nếu bước sinh gợi ý lỗi.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Trò chuyện với AI tư vấn tiêu hoá              [+ Cuộc mới]  │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                                           Tôi hay bị đầy   │ │
│ │                                           bụng sau khi ăn  │ │
│ │                                           đồ cay, có sao   │ │
│ │                                           không?       🧑  │ │
│ │                                                              │ │
│ │  🤖 Bạn vừa mô tả đầy bụng sau khi ăn đồ cay.               │ │
│ │     Tôi sẽ dựa vào thông tin bạn gửi và tài liệu liên quan │ │
│ │     để giải thích thêm...                                  │ │
│ │                                                              │ │
│ │     Đây là gợi ý sơ bộ dựa trên tài liệu y khoa, không      │ │
│ │     thay thế thăm khám trực tiếp.                            │ │
│ │                                                              │ │
│ │     👍 Hữu ích    👎 Không hữu ích       [1] [2] Xem nguồn   │ │
│ │                                                              │ │
│ │     Có thể bạn muốn hỏi thêm:                                │ │
│ │     ( Tôi nên ghi lại thông tin nào về triệu chứng? )        │ │
│ │     ( Tôi nên mô tả thời điểm xuất hiện như thế nào? )        │ │
│ └─────────────────────────────────────────────────────────┘ │
│  [ Nhập câu hỏi của bạn...                          ] [Gửi]  │
└───────────────────────────────────────────────────────────┘

— Khi phát hiện dấu hiệu nguy hiểm, chèn ngay 1 banner nổi bật —
┌─────────────────────────────────────────────────────────┐
│ 🚨 Dấu hiệu bạn vừa mô tả cần được thăm khám ngay. Nếu đang │
│    gặp nguy hiểm, hãy gọi 115 hoặc tới cơ sở y tế gần nhất. │
└─────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc để AI thật sự "tận dụng được" trong trải nghiệm, không chỉ là 1 ô chat

| Yếu tố | Nguyên tắc thiết kế | Vì sao |
| :--- | :--- | :--- |
| Nguồn tham khảo | Hiện tối đa 2 nguồn khi có kết quả RAG. Đoạn hiển thị được che liều lượng và Gemini viết lại cho dễ đọc; nếu viết lại lỗi/sai định dạng thì dùng đoạn gốc đã che liều. Không gọi đây là "trích nguyên văn" | Phân biệt nội dung tham khảo đã xử lý với văn bản nguồn nguyên bản |
| Câu hỏi gợi ý tiếp theo | Hiện tối đa 3 câu khi sinh được; bấm để hỏi tiếp. Lỗi sinh gợi ý không làm hỏng câu trả lời chính | Hỗ trợ người dùng tiếp tục tìm hiểu, không cam kết luôn có gợi ý |
| Đánh giá hữu ích | Nút 👍/👎 dưới câu trả lời để ghi nhận phản hồi | Cung cấp dữ liệu đánh giá chất lượng; không mô tả là AI tự học ngay sau mỗi lượt bấm |
| Cảnh báo khẩn cấp | Chèn banner **nổi bật, khác hẳn giao diện chat thông thường** (màu cảnh báo, không lẫn vào luồng tin nhắn), ngay khi phát hiện — không chờ bệnh nhân hỏi xong rồi mới cảnh báo ở cuối | Với dấu hiệu nguy hiểm (đau bụng dữ dội đột ngột, đi ngoài phân đen/máu, nôn ra máu...), vài giây chậm trễ trong việc gây chú ý có thể là khác biệt giữa "kịp đi khám" và "bỏ lỡ" — ưu tiên **recall cao**: thà cảnh báo nhầm một vài lần còn hơn bỏ sót 1 trường hợp thật |
| Ranh giới AI | Luôn có 1 dòng nhắc "gợi ý sơ bộ, không thay thế thăm khám" đi kèm mọi câu trả lời liên quan tới triệu chứng cụ thể | Đặc biệt quan trọng với các triệu chứng có thể là dấu hiệu của nhiều bệnh khác nhau với mức độ nguy hiểm rất khác nhau (vd đau vùng thượng vị có thể là viêm dạ dày thông thường hoặc dấu hiệu liên quan tim mạch) — AI không được tạo cảm giác "đã có kết luận cuối cùng" |
| Cá nhân hoá theo hồ sơ | Chưa triển khai. Không dùng ví dụ "theo hồ sơ của bạn" khi người dùng chưa cung cấp thông tin đó trong câu hỏi/tệp gửi kèm | Tránh làm người dùng tưởng hệ thống đã đối chiếu dị ứng, tiền sử hay thuốc đang dùng |

Trong `SourceReferences`, người dùng thấy nhãn "Tài liệu tham khảo N" và đoạn nội dung, có thể mở rộng bằng "Xem thêm"; không phải trình xem nguyên bản tài liệu. Khi không có tài liệu RAG phù hợp, danh sách nguồn rỗng và không có khối nguồn để hiển thị.

## 4. Trải nghiệm trả lời theo thời gian thực

Câu trả lời nên hiện dần từng chữ ngay khi AI đang "suy nghĩ" (không bắt chờ tới khi xong toàn bộ câu trả lời mới hiện) — với các câu trả lời dài (có trích dẫn, giải thích chi tiết), chờ một khối văn bản xuất hiện đột ngột sau vài giây im lặng tạo cảm giác chậm chạp hơn nhiều so với cảm nhận "AI đang trả lời ngay trước mắt tôi".

## 5. Tham chiếu

- Lưu lại & xuất phiên chat: [SCR-PT-09-LichSuChat.md](SCR-PT-09-LichSuChat.md)
- Cơ chế đứng sau màn hình này: `03-AIChatbot-RAG/` (truy xuất tài liệu, sàng lọc an toàn, sinh câu trả lời)
- Khi câu hỏi phức tạp cần hỏi-đáp có cấu trúc hơn chat tự do: `03-AIChatbot-RAG/` (bộ câu hỏi sàng lọc triệu chứng)
- Đối chiếu code: `ChatService.java`, `RagQueryService.java` (`answerWithSources`, `buildSources`), `StreamingRagQueryService.java` (`streamAnswer`), `SourceReferences.tsx`.
