# SCR-AD-13 — Giám sát cảnh báo khẩn cấp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Phía nhận của `UC0036` ("Gửi cảnh báo Triage tới admin") — không có mã UC riêng trong đề cương, nhưng là màn hình thật sự cần thiết để UC0036 có ý nghĩa |
| Actor | Hệ thống (tự động hoá phần lớn — xem §1), Admin + Giám sát vận hành (1 bước xác nhận cuối — xem §2 Bước 3; Giám sát vận hành thêm theo SCR-AD-03 v1.2) |
| Mục tiêu nghiệp vụ | Lớp bảo vệ thứ hai cho bệnh nhân gặp dấu hiệu nguy hiểm — hệ thống tự động phần hỏi thăm/nhắc nhở trong app, mọi liên hệ thật (người thân/bệnh nhân/115) luôn do Admin trực tiếp thực hiện bằng điện thoại cá nhân |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Tự động hoá tới đâu là thật, dừng ở đâu là bắt buộc

v1 của màn này từng bắt Admin tự cầm điện thoại gọi theo kịch bản — đúng về an toàn nhưng sai về tinh thần sản phẩm ("AI làm chủ hầu hết chức năng"). v2 định hướng gọi điện tự động đọc kịch bản (TTS qua Twilio hoặc dịch vụ tương đương) — nhưng **tính năng này cuối cùng không được xây dựng** (không có trong code, không có cả dạng stub/feature-flag). Dưới đây là tình trạng THẬT, không phải dự định:

| Hành động | Hệ thống hiện tại có tự làm được không? | Vì sao |
| :--- | :--- | :--- |
| Gửi push trong ứng dụng hỏi "Bạn có ổn không? [An toàn] [Cần hỗ trợ]" | ✅ Có, ngay lập tức | Chỉ là tin nhắn hiển thị trong app, không phải hành động không thể thu hồi — nhưng **chỉ tới được bệnh nhân nếu họ đang mở app/nhận push**, không có SMS dự phòng (đã cắt `UC0072`) |
| Lặp nhắc bệnh nhân + báo động lại Admin khi quá hạn (failsafe, Bước 4) | ✅ Có | Chỉ là gửi lại 2 thông báo đã có ở trên, không phải liên hệ ai mới |
| Gọi điện tự động đọc kịch bản cố định (TTS) | ❌ Không — **chưa từng được xây**, không phải "có nhưng tắt" | Cần tích hợp dịch vụ gọi điện bên thứ 3 (Twilio hoặc tương đương), đăng ký brandname số Việt Nam, tốn phí theo cuộc gọi — không kịp trong phạm vi đồ án, quyết định bỏ hẳn, không để lại cấu hình tắt/mở |
| Gọi điện cho người liên hệ khẩn cấp / gọi trực tiếp cho bệnh nhân / gọi 115 | ❌ Không — **luôn luôn là Admin tự thực hiện bằng điện thoại cá nhân** | Hành động không thể thu hồi, ảnh hưởng người thứ 3 (báo "người nhà bạn có thể đang nguy hiểm" mà là báo động giả thì gây hoảng loạn vô cớ) — không có kênh tự động để làm việc này dù muốn, nên **bắt buộc** là người thật, không phải lựa chọn thiết kế có thể đổi sau. Hệ thống chỉ hỗ trợ: hiển thị số liên hệ khẩn cấp đã khai (`MedicalProfile.emergencyContactPhone`) và ghi log kết quả Admin chọn (`POST /api/v1/cms/triage-alerts/{id}/contact`) |
| AI tự gọi 115 thay mặt bệnh nhân | ❌ Không bao giờ | Gọi 115 tiêu tốn nguồn lực cấp cứu thật — AI hiểu nhầm ngữ cảnh (vd "hồi xưa em từng nôn ra máu") thành cấp cứu hiện tại là rủi ro có hậu quả ngoài đời thật. AI chỉ **nhắc bệnh nhân tự gọi** (đã làm ở `SCR-PT-08-ChatAI.md`); quyết định gọi hộ (nếu có) luôn là của Admin ở Bước 3 |

> ⚠️ **Đã cắt SMS (`UC0072`, quyết định 2026-11-19) và đã quyết định không xây gọi tự động TTS** — luồng này chỉ còn **1 kênh tự động duy nhất tiếp cận bệnh nhân**: push trong ứng dụng (vô dụng nếu app đóng/không nhận push). Mọi liên hệ thật với người thân, bệnh nhân hoặc 115 đều do Admin tự cầm điện thoại cá nhân thực hiện. Vì độ tin cậy kênh tự động thấp, mốc chuyển "Cần can thiệp" rút ngắn xuống **3 phút** (không phải 5 hay 10 phút như các bản trước — khớp đúng mặc định thật trong code, `TriageEscalationScheduler`).

> **Lưu ý vận hành (`ba-gastro-10yr`)**: với từ khoá nặng nhất (nôn ra máu, đi cầu phân đen/máu), lớp phản hồi tức thời thật là tin AI khuyên bệnh nhân tự gọi 115 ngay trong cùng lượt chat (`SCR-PT-08`) — luồng Triage ở màn này là lớp **an toàn thứ hai**, cho trường hợp bệnh nhân không tự hành động được. 3 phút hợp lý cho vai trò này, nhưng chỉ đúng nếu Admin thực sự theo dõi real-time — nếu đồ án không cam kết trực 24/7, nên hiểu rõ giới hạn này, tránh tạo cảm giác an toàn giả.

## 2. Luồng xử lý — Admin chỉ xuất hiện ở đúng 1 bước (và đó là bước "cầm điện thoại thật")

```text
┌───────────────────────────────────────────────────────────┐
│  Cảnh báo khẩn cấp              [Hệ thống đang tự xử lý] [Cần │
│                                   can thiệp] [Đã xử lý]        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🟢 14:32 · Nguyễn Văn A — Hệ thống đang tự xử lý            │ │
│ │    "...nôn ra máu từ sáng..."                               │ │
│ │    Đã gửi push "Bạn có ổn không?" (14:32) — tự chuyển "Cần  │ │
│ │    can thiệp" sau 3 phút nếu bệnh nhân vẫn im lặng           │ │
│ │                                               [Xem timeline →]│ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 🔴 13:10 · Trần Thị B — CẦN CAN THIỆP · Admin Lan đang liên hệ│ │
│ │    Không phản hồi push sau 3 phút                           │ │
│ │    [  Xem timeline & ghi kết quả liên hệ  ]                  │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Bước 3: màn "Cần can thiệp" (bắt buộc Admin cầm điện thoại thật) —
┌───────────────────────────────────────────────────────────┐
│  Trần Thị B — không phản hồi push "Bạn có ổn không?" sau 3'   │
│  Timeline: 13:10 gửi push → 13:13 chuyển "Cần can thiệp"      │
│  (bệnh nhân im lặng)                                           │
│                                                                 │
│  Liên hệ khẩn cấp đã khai: Nguyễn Văn C — Chồng — 09xx xxx xxx │
│  ⚠ Hệ thống KHÔNG tự gọi — Admin tự dùng điện thoại cá nhân   │
│                                                                 │
│  [ Tôi đang liên hệ ]  ← tự nguyện, tránh 2 Admin gọi trùng,   │
│    Admin khác vẫn xem/xử lý được, chỉ không claim trùng        │
│                                                                 │
│  Sau khi gọi xong (có thể gọi nhiều lần/nhiều người), ghi kết  │
│  quả mỗi lần:                                                  │
│  [Đã gọi người thân] [Người thân không nghe máy]               │
│  [Người thân xác nhận BN an toàn] [Đã gọi được cho BN]         │
│  [Đã gọi 115]                                                  │
│                                                                 │
│  [   Đóng ca   ]  (cần ≥1 kết quả liên hệ đã ghi)               │
│  [ Báo nhầm / Spam ]  (cần nhập lý do, huỷ toàn bộ ca)          │
│                                                                 │
│  ⚠ Không Admin nào "Tôi đang liên hệ" hoặc ghi kết quả trong   │
│    5 phút kể từ khi chuyển "Cần can thiệp" → hệ thống tự báo   │
│    động lại TOÀN BỘ Admin + nhắc lại bệnh nhân (failsafe,      │
│    Bước 4). Hệ thống vẫn KHÔNG tự gọi cho ai.                  │
└───────────────────────────────────────────────────────────┘
```

**Các bước cụ thể** (giữ đúng số 1/3/4 như trong code thật — `TriageEscalationScheduler`, `TriageAlertService`, `TriageAlertAdminController` đều gọi các mốc này là "bước 1"/"bước 3"/"bước 4"; **không có "Bước 2"** — đây là khoảng dành cho gọi tự động TTS đã quyết định không xây, xem §1):

| Bước | Thời điểm | Ai làm | Hành động |
| :--- | :--- | :--- | :--- |
| 1 | Ngay khi cảnh báo kích hoạt | Hệ thống (tự động) | Gửi push "Bạn có ổn không? [An toàn] [Cần hỗ trợ]" tới bệnh nhân |
| 3 | +3 phút nếu bệnh nhân vẫn hoàn toàn im lặng | Hệ thống (tự động) chuyển trạng thái → sau đó 1 Admin bất kỳ trong số đang trực | Case chuyển "Cần can thiệp", báo tới **toàn bộ Admin đang trực cùng lúc**. Admin xem timeline, có thể bấm "Tôi đang liên hệ" (`claim`, tự nguyện) rồi **tự dùng điện thoại cá nhân** gọi người thân/bệnh nhân/115 theo tình huống, ghi lại kết quả (`recordContact`, 1 trong 5 outcome). Có thể `release` (trả lại) nếu không xử lý tiếp — ca mở lại cho Admin khác, đồng hồ failsafe tính lại từ lúc trả |
| 4 (failsafe) | +5 phút sau khi chuyển "Cần can thiệp" ở Bước 3, nếu **chưa Admin nào `claim` hoặc ghi kết quả liên hệ** | Hệ thống (tự động) | Báo động lại **toàn bộ Admin** (thông báo mới) và nhắc lại bệnh nhân qua push, ghi log "tự động do không có Admin xác nhận trong 5 phút". **Hệ thống không có khả năng tự liên hệ người thân/bệnh nhân/115** — failsafe chỉ lặp lại 2 cảnh báo đã có, không thay được việc 1 người thật cầm điện thoại gọi. Ca vẫn ở "Cần can thiệp" tới khi có Admin xử lý |

> ⚠️ **Hạn chế đã phát hiện khi rà soát code** (`TriageAlertRepository.claimIfOpen`): bấm "Tôi đang liên hệ" set `acknowledgedAt` ngay, nghĩa là **failsafe dừng vĩnh viễn cho ca đó ngay khi có Admin claim — kể cả khi Admin đó sau đó không quay lại ghi bất kỳ kết quả liên hệ nào**. Chưa có timeout phụ bắt lại trường hợp "đã claim nhưng bỏ quên" (chỉ tự hồi phục nếu chính Admin đó chủ động bấm "Trả lại"/`release`). Khuyến nghị: failsafe nên tiếp tục đếm nếu ca đã claim quá X phút mà chưa có kết quả liên hệ nào được ghi, không chỉ dựa vào đã claim hay chưa. *(Việc sửa thuộc về code, không phải tài liệu — nêu ở đây để tài liệu không hứa một mức an toàn cao hơn thực tế.)*

> **Khuyến nghị bổ sung outcome (chưa triển khai)**: 5 kết quả hiện có (`TriageContactResult`) bất đối xứng — có "Người thân không nghe máy" nhưng không có outcome tương ứng cho bệnh nhân, và không có outcome "đã thử hết người thân + bệnh nhân, không ai bắt máy, chưa gọi 115". Đề xuất bổ sung `PATIENT_NO_ANSWER` và `UNREACHABLE_ALL` — trường hợp thứ hai là tín hiệu nên đẩy Admin tới quyết định gọi 115 chủ động, cần tách biệt khỏi 1 lần gọi nhỡ đơn lẻ.

> **Hạn chế khác đã phát hiện**: màn chi tiết ca chỉ trả số liên hệ khẩn cấp cho viewer có vai trò Admin (`TriageAlertService.getDetail`) — nếu sau này có vai trò khác (vd Bác sĩ trực) cũng được phép `claim`/xử lý ca, vai trò đó sẽ không thấy số để gọi. Cần đồng bộ điều kiện này với bất kỳ thay đổi vai trò nào ở `SCR-AD-03-PhanQuyenRBAC.md`.

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| 3 trạng thái rõ ràng, không mập mờ | "Hệ thống đang tự xử lý" / "Cần can thiệp" / "Đã xử lý" | Admin chỉ cần quan tâm tab "Cần can thiệp" — đây là **toàn bộ việc Admin phải làm** trong luồng này, mọi thứ khác hệ thống tự lo |
| Thông báo "Cần can thiệp" phải tới **tất cả** Admin trực cùng lúc | Không gán ngẫu nhiên cho 1 người | Tránh single point of failure — nếu người duy nhất được gán đang bận/ngủ quên, ca cấp cứu thật không có ai xử lý |
| Timeline tự động phải hiện đầy đủ, không phải "hộp đen" | Mọi bước hệ thống đã làm (push lúc nào, gọi tự động lúc nào, có phản hồi hay không) hiện rõ trong màn "Cần can thiệp" | Admin cần đủ bối cảnh để quyết định đúng ở bước duy nhất cần quyết định — không thể quyết định tốt nếu không biết hệ thống đã thử gì |
| Failsafe tự động nếu không Admin nào xác nhận (Bước 4) | Đếm ngược độc lập với việc Admin có đang xem màn hay không | Nguyên tắc tối cao của toàn hệ thống Triage (`03-AIChatbot-RAG/00-TongQuan.md` §3.1): thà hành động nhầm còn hơn bỏ sót — nếu Admin cũng không phản hồi, hệ thống vẫn phải tự lo, không được để ca "treo" vô thời hạn |
| Sắp xếp ca "Cần can thiệp" lên đầu, trước cả "Hệ thống đang tự xử lý" | Không chỉ sắp theo thời gian, còn theo mức độ (nếu có từ `UC0040`) | Đây là tab duy nhất đòi hỏi hành động người thật — phải là thứ đầu tiên Admin nhìn thấy khi mở màn |
| Admin "nhận" ca (claim) để tránh 2 người cùng gọi 1 gia đình đang hoảng loạn | Nút "Tôi đang liên hệ" / "Trả lại" — tự nguyện, Admin khác vẫn xem/xử lý được | Nhiều Admin cùng trực có thể cùng mở 1 ca; nếu không đánh dấu, 2 người gọi trùng 1 gia đình đúng lúc họ cần bình tĩnh nhất là phiền phức không cần thiết |
| Mọi ca đã xử lý vẫn lưu lại, xem được lịch sử đầy đủ (cả phần tự động lẫn phần Admin) | Tab "Đã xử lý" riêng | Phục vụ rà soát sau này — hệ thống có đang phản ứng đủ nhanh không, failsafe có kích hoạt đúng không; cũng là cơ sở cải thiện ngưỡng thời gian (3 phút chuyển "Cần can thiệp" / 5 phút failsafe) nếu thực tế cho thấy chưa hợp lý |

## 4. Tham chiếu

- Cơ chế phát hiện & đẩy cảnh báo: `03-AIChatbot-RAG/00-TongQuan.md` §3
- UI cảnh báo phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`
- SMS (`UC0072`) đã cắt khỏi phạm vi — hệ quả tới luồng này: `05-ThongBao-Notification/00-TongQuan.md` §2.3
- Trường liên hệ khẩn cấp: `02-NguoiDung-BenhNhan/SCR-PT-01-HoSoBenhLy.md`
- Mọi bước (tự động lẫn Admin) ghi vào: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md) — không tạo bảng log riêng
