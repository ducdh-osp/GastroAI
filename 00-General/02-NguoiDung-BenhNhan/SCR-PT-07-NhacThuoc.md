# SCR-PT-07 — Nhắc uống thuốc

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0015` (thiết lập lịch nhắc), `UC0016` (xác nhận đã uống) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Giúp bệnh nhân tuân thủ đúng phác đồ thuốc — vấn đề thực tế lớn nhất trong điều trị bệnh tiêu hoá mạn tính |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Thực tế cần hiểu trước khi thiết kế màn này

Tuân thủ dùng thuốc (medication adherence) là điểm yếu phổ biến nhất trong điều trị các bệnh tiêu hoá cần dùng thuốc kéo dài. Vài thực tế quan trọng cần phản ánh vào thiết kế:

- **Thời điểm uống thuốc so với bữa ăn thường bị bỏ qua** — nhiều thuốc dạ dày cần uống *trước ăn* một khoảng thời gian nhất định để phát huy tác dụng tốt nhất; uống cùng lúc hay sau ăn làm giảm hiệu quả đáng kể dù bệnh nhân vẫn nghĩ mình "có uống thuốc đều". Vì vậy lịch nhắc nên cho khai báo rõ **"trước ăn" hay "sau ăn"**, không chỉ 1 giờ cố định trong ngày.
- **Tỷ lệ bỏ dở phác đồ giữa chừng rất cao trong thực tế** — đặc biệt với các phác đồ điều trị dài ngày, nhiều thuốc phối hợp: bệnh nhân thấy đỡ triệu chứng là tự ý dừng thuốc giữa đợt, dù phác đồ chưa đủ liệu trình. Hệ quả có thể nghiêm trọng (vi khuẩn kháng thuốc, bệnh tái phát). Màn hình không thể ép buộc bệnh nhân uống thuốc, nhưng **nên nhắc rõ nếu còn liều trong phác đồ chưa hoàn thành**, thay vì chỉ nhắc theo từng liều rời rạc không có bối cảnh.
- **Người bệnh ngại thừa nhận mình quên thuốc** — nếu màn hình tạo cảm giác bị "theo dõi và chấm điểm", bệnh nhân có xu hướng tắt thông báo hoặc xác nhận khống "đã uống" dù chưa uống thật, làm dữ liệu vô nghĩa và mất luôn tác dụng nhắc nhở.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Nhắc uống thuốc                      [ + Thêm lịch nhắc ]    │
│                                                                 │
│  Hôm nay                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 💊 Omeprazole 20mg                                          │ │
│  │    07:00 · Trước ăn sáng 30 phút                            │ │
│  │    [    ✓ Đã uống    ]                                     │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 💊 Men vi sinh                                              │ │
│  │    12:30 · Sau ăn trưa                      ✓ Đã xác nhận  │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                 │
│  Lịch sử xác nhận đã uống                       [Xem thêm ▾]   │
└───────────────────────────────────────────────────────────┘

— Thêm lịch nhắc mới —
┌─────────────────────────────────┐
│  Thêm lịch nhắc thuốc                │
│  Tên thuốc        [_________________] │
│  Liều dùng         [_________________] │
│  Thời điểm uống    ( Trước ăn / Sau ăn │
│                       / Giờ cố định )   │
│  Khung giờ         [ 07:00 ]            │
│  Lặp lại            ( Hằng ngày / Chọn  │
│                        ngày trong tuần ) │
│       [     Lưu     ]                   │
└─────────────────────────────────┘
```

## 3. Nguyên tắc cho hành vi xác nhận

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Tới giờ uống thuốc | Nhắc kèm **nút xác nhận ngay trong thông báo**, không bắt mở app rồi tìm đúng màn mới xác nhận được | Giảm tối đa số bước — càng nhiều bước, càng dễ bị trì hoãn rồi quên hẳn |
| Bỏ lỡ 1 liều | Vẫn hiện trong lịch sử là "chưa xác nhận", nhưng **không dùng ngôn ngữ trách móc** ("Bạn đã bỏ lỡ!"), chỉ nêu sự kiện trung lập | Cảm giác tội lỗi khiến bệnh nhân mạn tính né tránh mở app tiếp, phản tác dụng với đúng mục tiêu "giúp tuân thủ tốt hơn" |
| Muốn đổi giờ/liều đang dùng | Cho sửa lịch nhắc dễ dàng, nhưng khi đổi **liều lượng** nên có 1 dòng nhắc nhẹ "Hãy trao đổi với bác sĩ trước khi tự thay đổi liều dùng" | Phản ánh đúng thực tế bệnh nhân hay tự đổi liều không hỏi ý kiến — app không ngăn được hành vi đó, nhưng không nên vô tình khiến việc tự đổi liều trở nên quá dễ dàng/bình thường hoá mà thiếu một lời nhắc an toàn |
| Dừng hẳn 1 loại thuốc giữa phác đồ | Cho tắt lịch nhắc, nhưng hỏi lại nhẹ nhàng lý do (tuỳ chọn, không bắt buộc) — "Đã dùng hết đợt điều trị" / "Bác sĩ đổi thuốc" / "Khác" | Không cản bệnh nhân, nhưng việc hỏi lý do (dù không bắt buộc trả lời) tự nó đã là một điểm dừng để họ suy nghĩ lại trước khi dừng hẳn |

## 4. Tham chiếu

- Thông báo đẩy khi tới giờ: `05-ThongBao-Notification/`
- Tương tác thuốc cần AI lưu ý khi tư vấn: `03-AIChatbot-RAG/`
