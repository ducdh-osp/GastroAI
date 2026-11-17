# SCR-AD-13 — Giám sát cảnh báo khẩn cấp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Phía nhận của `UC0036` ("Gửi cảnh báo Triage tới admin") — không có mã UC riêng trong đề cương, nhưng là màn hình thật sự cần thiết để UC0036 có ý nghĩa |
| Actor | Admin — vai trò **vận hành** theo kịch bản cố định, không cần và không được tự ý đưa ra phán đoán y khoa (xem §1) |
| Mục tiêu nghiệp vụ | Lớp bảo vệ thứ hai cho bệnh nhân gặp dấu hiệu nguy hiểm — phòng trường hợp bệnh nhân đọc cảnh báo nhưng chủ quan bỏ qua |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao con người giám sát ở đây PHẢI theo kịch bản cố định, không được tự phán đoán

[SCR-PT-08-ChatAI.md](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md) đã hiện cảnh báo ngay cho bệnh nhân khi phát hiện dấu hiệu nguy hiểm ("gọi 115 ngay") — đây **đã là lớp an toàn tức thời đủ mạnh** (độ nhạy cao, xem `03-AIChatbot-RAG/00-TongQuan.md` §3.1). Màn hình này không tồn tại để "thẩm định lại xem AI nói đúng không" — Admin trực ở đây **không có chuyên môn y tế** và không nên giả vờ có.

Rủi ro thật sự không nằm ở việc "không có bác sĩ xác nhận lại" — mà nằm ở việc để Admin tự phán đoán phải làm gì khi không chắc chắn. Tình huống cụ thể: cảnh báo "nôn ra máu" lúc 2 giờ sáng, gọi điện bệnh nhân không bắt máy — nếu không có kịch bản rõ ràng, Admin dễ **đứng im vì không biết phải làm gì tiếp**, hoặc tệ hơn là **tự trấn an sai** ("chắc không sao đâu"). Cả hai đều là hành vi vượt quá chuyên môn và nguy hiểm hơn cả việc không làm gì — vì vậy màn hình này phải quy định sẵn **kịch bản xử lý cố định** (§3), không để Admin tự ứng biến theo phán đoán cá nhân.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Cảnh báo khẩn cấp                    [ Mới nhất ] [Đã xử lý] │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🔴 14:32 · Nguyễn Văn A                                    │ │
│ │    "...nôn ra máu từ sáng..."                               │ │
│ │    [  Nhận xử lý theo kịch bản  ]                           │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 🟡 13:10 · Trần Thị B · Đang xử lý bởi Admin Minh           │ │
│ │    "...đau bụng dữ dội từ hôm qua..."    Bước: Đã gọi,      │ │
│ │                                           chờ phản hồi       │ │
│ │                                         [Xem chi tiết →]    │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Kịch bản xử lý cố định khi "Nhận xử lý" —
┌───────────────────────────────────────────────────────────┐
│  Kịch bản xử lý — KHÔNG tư vấn y khoa, chỉ xác nhận an toàn    │
│  1. Gọi điện cho bệnh nhân theo số đã đăng ký.                  │
│     Nói đúng: "Hệ thống ghi nhận bạn vừa mô tả [triệu chứng],  │
│     bạn có đang ổn không, có cần hỗ trợ gọi cấp cứu không?"     │
│  2. Bắt máy, ổn → [ Đánh dấu: Đã xác nhận an toàn ]             │
│  3. Bắt máy, cần cấp cứu → [ Hướng dẫn gọi 115 / người thân ]   │
│  4. KHÔNG bắt máy trong 10 phút →                               │
│     [ Escalate: gọi liên hệ khẩn cấp trong hồ sơ bệnh nhân ]    │
│  ⚠ Tuyệt đối không tự chẩn đoán, không nói "chắc không sao",    │
│    không khuyên dùng thuốc gì qua điện thoại.                   │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hiện theo thời gian thực, không cần tải lại trang | Cảnh báo mới xuất hiện ngay trên màn đang mở | Với tình huống có thể khẩn cấp, vài phút chậm trễ do không biết có ca mới là không chấp nhận được |
| Cho "Nhận xử lý" để tránh 2 người cùng xử lý 1 ca chồng chéo | Bấm nhận → ca đó gán cho người đó, người khác thấy trạng thái "đang xử lý bởi..." | Tránh tình trạng 2 người cùng liên hệ 1 bệnh nhân gây rối, hoặc tệ hơn — ai cũng nghĩ người khác đã xử lý nên không ai làm |
| **Kịch bản xử lý hiện ngay khi nhận ca, không phải tài liệu tra cứu riêng** | Các bước cố định (gọi xác nhận → ổn/không ổn → escalate nếu không liên lạc được trong 10 phút) hiện trực tiếp trong màn xử lý ca đó | Đây là điểm quan trọng nhất của toàn màn hình: Admin không có chuyên môn y tế, nên **không được tự quyết định phải nói gì hay làm gì** — kịch bản phải sẵn có, cố định, không để tự ứng biến (xem §1) |
| Mốc thời gian "không liên lạc được" phải tự động nhắc, không dựa vào Admin tự nhớ | Đếm ngược/nhắc tự động sau khoảng thời gian cố định (vd 10 phút) nếu ca chưa chuyển trạng thái | Trong tình huống căng thẳng, con người dễ quên theo dõi thời gian — hệ thống phải tự nhắc để không bỏ lỡ mốc escalate |
| Sắp xếp ca mới/nghiêm trọng hơn lên đầu | Không chỉ sắp theo thời gian, còn theo mức độ (nếu có từ `UC0040`) | Nhiều cảnh báo dồn dập cùng lúc cần ưu tiên đúng thứ tự xử lý theo mức độ nguy hiểm, không phải thứ tự đến trước |
| Mọi ca đã xử lý vẫn lưu lại, xem được lịch sử (kể cả đã làm đúng kịch bản hay không) | Tab "Đã xử lý" riêng, ghi rõ bước nào đã thực hiện | Phục vụ rà soát sau này — hệ thống có đang phản ứng đủ nhanh và đúng kịch bản với các ca khẩn cấp không; cũng là cơ sở để cải thiện kịch bản nếu phát hiện lỗ hổng |

## 4. Tham chiếu

- Cơ chế phát hiện & đẩy cảnh báo: `03-AIChatbot-RAG/00-TongQuan.md` §3
- UI cảnh báo phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`
