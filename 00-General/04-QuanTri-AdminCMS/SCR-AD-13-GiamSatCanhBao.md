# SCR-AD-13 — Giám sát cảnh báo khẩn cấp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Phía nhận của `UC0036` ("Gửi cảnh báo Triage tới admin") — không có mã UC riêng trong đề cương, nhưng là màn hình thật sự cần thiết để UC0036 có ý nghĩa |
| Actor | Admin, Bác sĩ |
| Mục tiêu nghiệp vụ | Lớp bảo vệ thứ hai cho bệnh nhân gặp dấu hiệu nguy hiểm — phòng trường hợp bệnh nhân đọc cảnh báo nhưng chủ quan bỏ qua |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần con người giám sát, dù AI đã cảnh báo trực tiếp cho bệnh nhân

[SCR-PT-08-ChatAI.md](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md) đã hiện cảnh báo ngay cho bệnh nhân khi phát hiện dấu hiệu nguy hiểm — nhưng không phải ai cũng hành động đúng ngay lập tức dù được cảnh báo (chủ quan, hoảng loạn không biết làm gì, hoặc đơn giản là bỏ qua). Màn hình này cho đội ngũ chuyên môn **chủ động theo dõi theo thời gian thực** và có thể liên hệ trực tiếp nếu thấy cần, thay vì phó mặc hoàn toàn cho bệnh nhân tự xử lý.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Cảnh báo khẩn cấp                    [ Mới nhất ] [Đã xử lý] │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🔴 14:32 · Nguyễn Văn A                                    │ │
│ │    "...nôn ra máu từ sáng..."                               │ │
│ │    [  Nhận xử lý  ]                                         │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 🟡 13:10 · Trần Thị B · Đang xử lý bởi BS. Lê Văn C         │ │
│ │    "...đau bụng dữ dội từ hôm qua..."                       │ │
│ │                                         [Xem trao đổi →]    │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Hiện theo thời gian thực, không cần tải lại trang | Cảnh báo mới xuất hiện ngay trên màn đang mở | Với tình huống có thể khẩn cấp, vài phút chậm trễ do không biết có ca mới là không chấp nhận được |
| Cho "Nhận xử lý" để tránh 2 người cùng xử lý 1 ca chồng chéo | Bấm nhận → ca đó gán cho người đó, người khác thấy trạng thái "đang xử lý bởi..." | Tránh tình trạng 2 người cùng liên hệ 1 bệnh nhân gây rối, hoặc tệ hơn — ai cũng nghĩ người khác đã xử lý nên không ai làm |
| Sắp xếp ca mới/nghiêm trọng hơn lên đầu | Không chỉ sắp theo thời gian, còn theo mức độ (nếu có từ `UC0040`) | Nhiều cảnh báo dồn dập cùng lúc cần ưu tiên đúng thứ tự xử lý theo mức độ nguy hiểm, không phải thứ tự đến trước |
| Mọi ca đã xử lý vẫn lưu lại, xem được lịch sử | Tab "Đã xử lý" riêng | Phục vụ rà soát sau này — hệ thống có đang phản ứng đủ nhanh và đủ với các ca khẩn cấp không |

## 4. Tham chiếu

- Cơ chế phát hiện & đẩy cảnh báo: `03-AIChatbot-RAG/00-TongQuan.md` §3
- UI cảnh báo phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`
