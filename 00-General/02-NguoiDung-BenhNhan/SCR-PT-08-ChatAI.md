# SCR-PT-08 — Trò chuyện với AI

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0017` (màn hình chat) — vận hành cùng `UC0035`, `UC0042`, `UC0043` ở `03-AIChatbot-RAG/` xuất hiện ngay trên màn này |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Đây là **lý do sản phẩm tồn tại** — nơi bệnh nhân nhận được tư vấn tiêu hoá cá nhân hoá, có trích dẫn nguồn, theo thời gian thực |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Đây không phải "1 màn chat thông thường" — đây là nơi bán giá trị cốt lõi

Mọi phân hệ khác (nhật ký, nhắc thuốc, hồ sơ bệnh lý) tồn tại để **nuôi** màn hình này: càng nhiều dữ liệu cá nhân đã ghi, AI càng trả lời sát với tình trạng thật của từng người thay vì chung chung. Thiết kế màn hình chat vì vậy không chỉ là "giao diện nhắn tin" — mà phải **chủ động phô bày** những gì AI làm được hơn hẳn một công cụ tìm kiếm thông thường:

1. **Trả lời có trích dẫn nguồn y khoa** — không phải "AI tự bịa", mà có thể lần ra câu trả lời dựa trên tài liệu nào.
2. **Hiểu ngữ cảnh cá nhân** — biết bệnh nhân này có tiền sử gì, đang dùng thuốc gì, Bristol gần đây ra sao (nếu đã khai báo) để không trả lời như một cuốn cẩm nang chung.
3. **Chủ động phát hiện nguy hiểm** — không chỉ trả lời câu hỏi được hỏi, mà cảnh báo ngay cả khi bệnh nhân vô tình mô tả một dấu hiệu nghiêm trọng mà không biết nó nghiêm trọng.
4. **Dẫn dắt cuộc trò chuyện đi xa hơn** — gợi ý câu hỏi tiếp theo, biến 1 lượt hỏi-đáp thành một buổi tư vấn có chiều sâu.

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
│ │  🤖 Đầy bụng sau khi ăn cay khá phổ biến, đặc biệt nếu bạn  │ │
│ │     có tiền sử viêm dạ dày (ghi nhận trong hồ sơ của bạn).  │ │
│ │     Theo tài liệu [1][2], nên hạn chế...                    │ │
│ │                                                              │ │
│ │     Đây là gợi ý sơ bộ dựa trên tài liệu y khoa, không      │ │
│ │     thay thế thăm khám trực tiếp.                            │ │
│ │                                                              │ │
│ │     👍 Hữu ích    👎 Không hữu ích       [1] [2] Xem nguồn   │ │
│ │                                                              │ │
│ │     Có thể bạn muốn hỏi thêm:                                │ │
│ │     ( Đồ cay có liên quan tới viêm dạ dày của tôi không? )   │ │
│ │     ( Tôi nên tránh những món gì khác? )                     │ │
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
| Trích dẫn nguồn | Luôn hiện kèm câu trả lời, bấm vào xem được nguyên văn đoạn tài liệu | Khác biệt lớn nhất với hỏi 1 chatbot thông thường — biến AI từ "nói gì cũng được" thành "có căn cứ kiểm chứng được", đây là yếu tố xây dựng niềm tin quan trọng nhất với tư vấn y tế |
| Câu hỏi gợi ý tiếp theo | Luôn đề xuất 2-3 câu hỏi liên quan sau mỗi câu trả lời, bấm là hỏi luôn không cần gõ lại | Biến 1 câu hỏi đơn lẻ thành cả một hành trình tìm hiểu — bệnh nhân thường không biết mình nên hỏi tiếp gì, AI chủ động dẫn dắt thay vì chờ bị động |
| Đánh giá hữu ích | Nút 👍/👎 ngay dưới mỗi câu trả lời, thao tác 1 chạm | Đây là kênh để AI "học" được câu trả lời nào thực sự giúp ích cho bệnh nhân Việt Nam với các câu hỏi tiêu hoá thường gặp — càng nhiều phản hồi, chất lượng gợi ý càng cải thiện theo thời gian |
| Cảnh báo khẩn cấp | Chèn banner **nổi bật, khác hẳn giao diện chat thông thường** (màu cảnh báo, không lẫn vào luồng tin nhắn), ngay khi phát hiện — không chờ bệnh nhân hỏi xong rồi mới cảnh báo ở cuối | Với dấu hiệu nguy hiểm (đau bụng dữ dội đột ngột, đi ngoài phân đen/máu, nôn ra máu...), vài giây chậm trễ trong việc gây chú ý có thể là khác biệt giữa "kịp đi khám" và "bỏ lỡ" — ưu tiên **recall cao**: thà cảnh báo nhầm một vài lần còn hơn bỏ sót 1 trường hợp thật |
| Ranh giới AI | Luôn có 1 dòng nhắc "gợi ý sơ bộ, không thay thế thăm khám" đi kèm mọi câu trả lời liên quan tới triệu chứng cụ thể | Đặc biệt quan trọng với các triệu chứng có thể là dấu hiệu của nhiều bệnh khác nhau với mức độ nguy hiểm rất khác nhau (vd đau vùng thượng vị có thể là viêm dạ dày thông thường hoặc dấu hiệu liên quan tim mạch) — AI không được tạo cảm giác "đã có kết luận cuối cùng" |
| Cá nhân hoá theo hồ sơ | Khi bệnh nhân đã khai hồ sơ bệnh lý ([SCR-PT-01](SCR-PT-01-HoSoBenhLy.md)), câu trả lời nên chủ động nhắc tới ngữ cảnh đó ("vì bạn có tiền sử...") | Đây là điểm khác biệt thật sự so với tra Google — cảm giác "AI này biết tôi" là lý do bệnh nhân quay lại thay vì chỉ tìm kiếm thông tin chung |

## 4. Trải nghiệm trả lời theo thời gian thực

Câu trả lời nên hiện dần từng chữ ngay khi AI đang "suy nghĩ" (không bắt chờ tới khi xong toàn bộ câu trả lời mới hiện) — với các câu trả lời dài (có trích dẫn, giải thích chi tiết), chờ một khối văn bản xuất hiện đột ngột sau vài giây im lặng tạo cảm giác chậm chạp hơn nhiều so với cảm nhận "AI đang trả lời ngay trước mắt tôi".

## 5. Tham chiếu

- Lưu lại & xuất phiên chat: [SCR-PT-09-LichSuChat.md](SCR-PT-09-LichSuChat.md)
- Cơ chế đứng sau màn hình này: `03-AIChatbot-RAG/` (truy xuất tài liệu, sàng lọc an toàn, sinh câu trả lời)
- Khi câu hỏi phức tạp cần hỏi-đáp có cấu trúc hơn chat tự do: `03-AIChatbot-RAG/` (bộ câu hỏi sàng lọc triệu chứng)
