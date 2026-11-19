# SCR-AD-13 — Giám sát cảnh báo khẩn cấp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Phía nhận của `UC0036` ("Gửi cảnh báo Triage tới admin") — không có mã UC riêng trong đề cương, nhưng là màn hình thật sự cần thiết để UC0036 có ý nghĩa |
| Actor | Hệ thống (tự động hoá phần lớn — xem §1), Admin (chỉ 1 bước xác nhận cuối — xem §2 Bước 3) |
| Mục tiêu nghiệp vụ | Lớp bảo vệ thứ hai cho bệnh nhân gặp dấu hiệu nguy hiểm, **tự động hoá tối đa** — chỉ dừng lại ở 1 điểm cần người xác nhận trước khi làm phiền bên thứ 3 |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. AI-first: tự động hoá tới đâu là an toàn, dừng ở đâu là bắt buộc

v1 của màn này từng bắt Admin tự cầm điện thoại gọi theo kịch bản — đúng về an toàn nhưng sai về tinh thần sản phẩm ("AI làm chủ hầu hết chức năng"). Viết lại theo nguyên tắc: **mọi hành động chỉ là xác nhận/nhắc nhở thì AI tự làm 100%; hành động nào không thể thu hồi và ảnh hưởng tới người thứ 3 thì bắt buộc 1 người xác nhận trước.**

| Hành động | AI tự làm được không? | Vì sao |
| :--- | :--- | :--- |
| Gửi push/SMS hỏi "bạn có ổn không" | ✅ Có | Chỉ là tin nhắn, không hành động không hồi được |
| Gọi điện tự động đọc kịch bản cố định (TTS) | ✅ Có (nếu kịp tích hợp — xem ghi chú dưới) | Nội dung đã cố định sẵn, máy đọc hay người đọc không đổi thông tin truyền tải, không cần phán đoán y khoa |
| Lặp nhắc tăng dần nếu im lặng | ✅ Có | Chỉ là logic retry |
| **Báo cho người liên hệ khẩn cấp (bên thứ 3)** | ❌ Không — cần người xác nhận trước | Báo "người nhà bạn có thể đang nguy hiểm" lúc nửa đêm là hành động xã hội **không thu hồi được** — nếu là báo động giả (lỗi nhận diện, hoặc bệnh nhân chỉ tắt máy đi ngủ), gây hoảng loạn vô cớ cho người vô can. Cần 1 người chịu trách nhiệm quyết định, nhưng có failsafe timeout để không treo vô hạn nếu không ai xác nhận (xem Bước 4) |
| AI tự gọi 115 thay mặt bệnh nhân | ❌ Không bao giờ | Gọi 115 tiêu tốn nguồn lực cấp cứu thật — AI hiểu nhầm ngữ cảnh (vd "hồi xưa em từng nôn ra máu") thành cấp cứu hiện tại là rủi ro có hậu quả ngoài đời thật. AI chỉ **nhắc bệnh nhân tự gọi** (đã làm ở `SCR-PT-08-ChatAI.md`); quyết định gọi hộ (nếu có) là của Admin ở Bước 3, không phải của hệ thống |

> **Ghi chú tính khả thi (`ba-bigtech-10yr`)**: gọi điện tự động (TTS qua dịch vụ như Twilio) cần tích hợp bên thứ 3, đăng ký brandname gọi số Việt Nam, tốn chi phí theo cuộc gọi — **đánh dấu "Nên có, có thể cắt nếu không kịp trong phạm vi đồ án"**, dùng MoSCoW giống các UC khác trong `docs/masterplan-phan-he-usecase.xlsx`. Nếu cắt, Bước 2 dưới đây chỉ còn SMS lặp lại, không có cuộc gọi tự động — vẫn tốt hơn hẳn v1 (không có gì tự động cả).

## 2. Luồng xử lý — Admin chỉ xuất hiện ở đúng 1 bước

```text
┌───────────────────────────────────────────────────────────┐
│  Cảnh báo khẩn cấp              [Hệ thống đang tự xử lý] [Cần │
│                                   can thiệp] [Đã xử lý]        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🟢 14:32 · Nguyễn Văn A — Hệ thống đang tự xử lý            │ │
│ │    "...nôn ra máu từ sáng..."                               │ │
│ │    Đã gửi push (14:32) → SMS nhắc (14:34) → đang gọi tự     │ │
│ │    động lần 1 (14:37)                        [Xem timeline →]│ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 🔴 13:10 · Trần Thị B — CẦN CAN THIỆP                       │ │
│ │    Đã thử hết các kênh tự động, không phản hồi trong 10'    │ │
│ │    [  Xem toàn bộ timeline & Xác nhận escalate  ]           │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Bước 3: màn "Cần can thiệp" (bắt buộc người xác nhận) —
┌───────────────────────────────────────────────────────────┐
│  Trần Thị B — hệ thống đã thử hết các kênh tự động            │
│  Timeline: 13:10 push → 13:12 SMS → 13:15 gọi tự động lần 1 → │
│  13:18 gọi tự động lần 2 → không phản hồi bất kỳ kênh nào       │
│                                                                 │
│  Bấm xác nhận để hệ thống liên hệ người thân đã khai trong hồ   │
│  sơ (Nguyễn Văn C — Chồng — 09xx xxx xxx):                      │
│  [  Xác nhận escalate tới liên hệ khẩn cấp  ]                   │
│  ⚠ Nếu không có Admin nào xác nhận trong 5 phút, hệ thống sẽ    │
│    TỰ ĐỘNG escalate (failsafe — xem §3) để ca thật không bị bỏ sót.│
└───────────────────────────────────────────────────────────┘
```

**4 bước cụ thể:**

| Bước | Thời điểm | Ai làm | Hành động |
| :--- | :--- | :--- | :--- |
| 1 | Ngay khi cảnh báo kích hoạt | Hệ thống (AI, tự động) | Gửi push "Bạn có ổn không? [An toàn] [Cần hỗ trợ]" tới bệnh nhân |
| 2 | +2 phút nếu chưa phản hồi | Hệ thống (AI, tự động) | Gửi SMS nhắc (tái dùng hạ tầng `UC0072`, xem `05-ThongBao-Notification/00-TongQuan.md` §2.3 đã cập nhật), rồi gọi tự động đọc kịch bản cố định nếu đã tích hợp (xem ghi chú tính khả thi) |
| 3 | +10 phút nếu vẫn hoàn toàn im lặng | **1 Admin bất kỳ trong số đang trực** | Case chuyển "Cần can thiệp", đẩy real-time + thông báo tới **toàn bộ Admin trực cùng lúc** (không chỉ 1 người, tránh single point of failure). Admin xem lại toàn bộ timeline, bấm xác nhận escalate tới liên hệ khẩn cấp — hoặc tự gọi lại/gọi 115 nếu thấy cần theo đúng tình huống |
| 4 (failsafe) | +5 phút sau Bước 3 nếu **không Admin nào** xác nhận | Hệ thống (AI, tự động) | Tự động escalate tới liên hệ khẩn cấp dù không ai xác nhận, ghi log rõ "tự động do không có Admin xác nhận trong 5 phút" | 

Bước 4 đảm bảo 1 ca cấp cứu thật không rơi vào khoảng trống nếu toàn bộ Admin cũng vắng mặt — nhưng vẫn ưu tiên có người xác nhận trước (Bước 3) vì đây là lựa chọn an toàn hơn khi có thể.

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| 3 trạng thái rõ ràng, không mập mờ | "Hệ thống đang tự xử lý" / "Cần can thiệp" / "Đã xử lý" | Admin chỉ cần quan tâm tab "Cần can thiệp" — đây là **toàn bộ việc Admin phải làm** trong luồng này, mọi thứ khác hệ thống tự lo |
| Thông báo "Cần can thiệp" phải tới **tất cả** Admin trực cùng lúc | Không gán ngẫu nhiên cho 1 người | Tránh single point of failure — nếu người duy nhất được gán đang bận/ngủ quên, ca cấp cứu thật không có ai xử lý |
| Timeline tự động phải hiện đầy đủ, không phải "hộp đen" | Mọi bước hệ thống đã làm (push lúc nào, SMS lúc nào, gọi tự động lúc nào, có phản hồi hay không) hiện rõ trong màn "Cần can thiệp" | Admin cần đủ bối cảnh để quyết định đúng ở bước duy nhất cần quyết định — không thể quyết định tốt nếu không biết hệ thống đã thử gì |
| Failsafe tự động nếu không Admin nào xác nhận (Bước 4) | Đếm ngược độc lập với việc Admin có đang xem màn hay không | Nguyên tắc tối cao của toàn hệ thống Triage (`03-AIChatbot-RAG/00-TongQuan.md` §3.1): thà hành động nhầm còn hơn bỏ sót — nếu Admin cũng không phản hồi, hệ thống vẫn phải tự lo, không được để ca "treo" vô thời hạn |
| Sắp xếp ca "Cần can thiệp" lên đầu, trước cả "Hệ thống đang tự xử lý" | Không chỉ sắp theo thời gian, còn theo mức độ (nếu có từ `UC0040`) | Đây là tab duy nhất đòi hỏi hành động người thật — phải là thứ đầu tiên Admin nhìn thấy khi mở màn |
| Mọi ca đã xử lý vẫn lưu lại, xem được lịch sử đầy đủ (cả phần tự động lẫn phần Admin) | Tab "Đã xử lý" riêng | Phục vụ rà soát sau này — hệ thống có đang phản ứng đủ nhanh không, failsafe có kích hoạt đúng không; cũng là cơ sở cải thiện ngưỡng thời gian (2'/10'/5') nếu thực tế cho thấy chưa hợp lý |

## 4. Tham chiếu

- Cơ chế phát hiện & đẩy cảnh báo: `03-AIChatbot-RAG/00-TongQuan.md` §3
- UI cảnh báo phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`
- Kênh SMS tái dùng cho escalation: `05-ThongBao-Notification/00-TongQuan.md` §2.3
- Trường liên hệ khẩn cấp: `02-NguoiDung-BenhNhan/SCR-PT-01-HoSoBenhLy.md`
- Mọi bước (tự động lẫn Admin) ghi vào: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md) — không tạo bảng log riêng
