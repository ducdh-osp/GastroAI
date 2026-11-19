# Tổng quan Phân hệ — Thông báo (Notification)

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | Thông báo (Notification) — bổ trợ |
| **UC nguồn** | `UC0067 - UC0076 (10 UC)` |
| **Actor** | Hệ thống (phần lớn), Bệnh nhân, Admin |
| **Tài liệu liên quan** | Mọi phân hệ khác đều "mượn" phân hệ này để gửi thông báo — đây là hạ tầng dùng chung |
| **Phiên bản** | v1.0 — viết theo trải nghiệm người dùng thực tế |

> Giống phân hệ AI Chatbot & RAG, phần lớn phân hệ này là **hạ tầng gửi tin**, không phải màn hình bệnh nhân chủ động mở ra dùng. Giá trị của nó không nằm ở giao diện mà ở việc **đúng người, đúng lúc, đúng kênh** — một cảnh báo khẩn cấp gửi chậm 10 phút hay một email xác thực không bao giờ tới đều là thất bại nghiêm trọng, dù giao diện các màn quản lý có đẹp tới đâu.

> **Nguyên tắc xuyên suốt: chỉ phần bắt buộc — cảnh báo Triage thời gian thực — là không thể thiếu. Mọi kênh còn lại (email, SMS) là lớp bổ trợ, có thể tinh giản nếu thời gian/nguồn lực không cho phép**, đúng tinh thần ưu tiên đã thống nhất cho các phân hệ bổ trợ.

---

## 1. Danh sách màn hình & UC

| Mã màn hình | File | Tên màn hình | UC |
| :--- | :--- | :--- | :--- |
| `SCR-NT-01` | [SCR-NT-01-TrungTamThongBao.md](SCR-NT-01-TrungTamThongBao.md) | Trung tâm thông báo | `UC0073`, `UC0076` |
| `SCR-NT-02` | [SCR-NT-02-MauThongBao.md](SCR-NT-02-MauThongBao.md) | Quản lý mẫu thông báo/email | `UC0074` |
| `SCR-NT-03` | [SCR-NT-03-GuiBroadcast.md](SCR-NT-03-GuiBroadcast.md) | Gửi thông báo broadcast | `UC0075` |

**UC không có màn hình riêng — là cơ chế gửi tin chạy nền:**

| UC | Tên | Vì sao không có màn |
| :--- | :--- | :--- |
| `UC0067` | Cảnh báo khẩn cấp qua WebSocket | Cơ chế đẩy tin — chỉ 1 phần của luồng escalation tự động hoá nhiều kênh ở `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md`, dùng khi cần đẩy case "Cần can thiệp" tới dashboard Admin |
| `UC0068` | Nhắc uống thuốc trong ứng dụng | Hiện dưới dạng thông báo nổi (popup), đã mô tả ở `02-NguoiDung-BenhNhan/SCR-PT-07-NhacThuoc.md` |
| `UC0069` | Email xác thực tài khoản | Cơ chế gửi — màn nhận kết quả ở `01-XacThuc-TaiKhoan/SCR-AUTH-02-XacThucEmail.md` |
| `UC0070` | Email đặt lại mật khẩu | Cơ chế gửi — màn nhận kết quả ở `01-XacThuc-TaiKhoan/SCR-AUTH-05-DatLaiMatKhau.md` |
| `UC0071` | Email nhắc uống thuốc khi offline | Mở rộng của `UC0068` — xem §2.2 |
| `UC0072` | SMS (OTP + nhắc escalation khẩn cấp) | Hạ tầng dùng chung — xem §2.3, đã đổi mức ưu tiên |

---

## 2. Nghiệp vụ dùng chung

### 2.1 Nguyên tắc chọn kênh gửi — không phải mọi thông báo đều "khẩn" như nhau

| Loại thông báo | Kênh | Vì sao |
| :--- | :--- | :--- |
| Cảnh báo Triage (bệnh nhân có dấu hiệu nguy hiểm) | Push → SMS → gọi tự động (tới bệnh nhân, tự động theo bậc) + WebSocket thời gian thực (tới dashboard Admin, chỉ khi cần can thiệp) | Duy nhất bắt buộc — vài phút chậm trễ có thể ảnh hưởng tới an toàn bệnh nhân. Chi tiết luồng: `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md` |
| Nhắc uống thuốc | Trong ứng dụng (nếu đang mở) → email (nếu không mở ứng dụng) | Ưu tiên kênh tức thời trước; chỉ "hạ" xuống email khi bệnh nhân không ở gần ứng dụng — không nên làm phiền bằng email nếu họ đang mở app và đã thấy thông báo rồi |
| Email xác thực/đặt lại mật khẩu | Email — kênh duy nhất hợp lý vì bản chất là xác minh quyền sở hữu hộp thư đó | |
| Broadcast hệ thống (bảo trì, tài liệu mới) | Trong ứng dụng | Không khẩn cấp, không cần làm phiền qua email/SMS |

### 2.2 Vì sao cần cả email VÀ trong ứng dụng cho nhắc thuốc (`UC0068` + `UC0071`)

Bệnh nhân không phải lúc nào cũng mở ứng dụng đúng giờ uống thuốc — thông báo trong app vô dụng nếu họ không mở app lên nhìn thấy. Email là lớp dự phòng cho đúng tình huống này: **chỉ gửi khi xác định bệnh nhân không đang online** (gửi cả 2 kênh cùng lúc là thừa, gây cảm giác bị làm phiền). Nội dung email nhắc nhở nên **chung chung** ("bạn có lịch nhắc thuốc cần xác nhận"), không nêu tên thuốc hay liều lượng cụ thể trong nội dung email — vì email là kênh kém an toàn hơn (có thể bị người khác xem trên thiết bị dùng chung, hộp thư chuyển tiếp nhầm...), không nên để lộ chi tiết tình trạng sức khoẻ cụ thể qua kênh này.

### 2.3 SMS (`UC0072`) — đã đổi mức ưu tiên khi trở thành kênh escalation an toàn

Ban đầu SMS chỉ phục vụ OTP, ưu tiên thấp nhất (phát sinh chi phí theo tin nhắn, trong khi email đủ đáp ứng xác thực với chi phí thấp hơn). **Nhưng từ khi SMS được tái dùng làm kênh nhắc tự động trong luồng xử lý cảnh báo khẩn cấp** (`04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md` Bước 2 — nhắc bệnh nhân xác nhận an toàn khi không mở app), nó không còn là "có thể cắt" nữa: thiếu kênh này, bệnh nhân không mở app đúng lúc gặp nguy hiểm sẽ không nhận được bất kỳ nhắc nhở nào ngoài push (vô dụng nếu không mở app).

| Mục đích dùng SMS | Mức ưu tiên |
| :--- | :--- |
| OTP xác thực (mục đích ban đầu) | Có thể cắt nếu thiếu thời gian — email đã đủ dùng |
| Nhắc xác nhận an toàn trong luồng cảnh báo khẩn cấp (`SCR-AD-13`) | **Nên có** — là 1 trong số ít kênh tiếp cận được bệnh nhân khi không mở app, hạ tầng gửi SMS dùng chung cho cả 2 mục đích nên làm 1 lần dùng được cho cả 2 |

Gọi điện tự động (TTS) ở cùng luồng là bước tiếp theo, tốn kém và phức tạp tích hợp hơn hẳn SMS — xem ghi chú tính khả thi ở `SCR-AD-13-GiamSatCanhBao.md` §1.

### 2.4 Độ trễ chấp nhận được cho email (bất đồng bộ)

Khác với cảnh báo Triage (phải tức thời), các email khác có thể gửi bất đồng bộ — nghĩa là hành động chính (đăng ký, đặt lại mật khẩu) **không chờ email gửi xong mới coi là thành công**. Lợi ích: nếu dịch vụ gửi email tạm thời trục trặc, bệnh nhân vẫn tạo được tài khoản/đặt được mật khẩu mới, hệ thống tự thử gửi lại sau — không để 1 sự cố ở bên thứ ba (dịch vụ email) làm hỏng cả luồng nghiệp vụ chính.

---

## 3. Những yêu cầu không được bỏ sót (`ba-bigtech-10yr`)

- **Không bao giờ để kênh bổ trợ (email) chặn luồng nghiệp vụ chính** — xem §2.4.
- **Nội dung thông báo không được để lộ dữ liệu sức khoẻ cụ thể qua kênh kém an toàn hơn** — xem §2.2 (email nhắc thuốc không nêu tên thuốc).
- **Tôn trọng lựa chọn tắt thông báo của bệnh nhân** ([SCR-NT-01](SCR-NT-01-TrungTamThongBao.md)) — trừ cảnh báo an toàn bắt buộc (Triage), mọi loại thông báo khác phải tắt được theo ý người dùng, không ép buộc.

## 4. Traceability (UC ↔ Nơi biểu hiện)

| UC | Nơi biểu hiện |
| :--- | :--- |
| `UC0067` | `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md` |
| `UC0068` | `02-NguoiDung-BenhNhan/SCR-PT-07-NhacThuoc.md` |
| `UC0069` | `01-XacThuc-TaiKhoan/SCR-AUTH-02-XacThucEmail.md` |
| `UC0070` | `01-XacThuc-TaiKhoan/SCR-AUTH-05-DatLaiMatKhau.md` |
| `UC0071` | Mở rộng `UC0068` — không màn riêng |
| `UC0072` | Không có màn riêng — hạ tầng SMS dùng chung cho OTP và `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md` |
| `UC0073`, `UC0076` | `SCR-NT-01` |
| `UC0074` | `SCR-NT-02` |
| `UC0075` | `SCR-NT-03` |

## 5. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo, viết theo trải nghiệm người dùng thực tế |
| v1.1 | 2026-11-19 | Đổi mức ưu tiên SMS (`UC0072`) từ "có thể cắt" thành "nên có" — SMS giờ là hạ tầng dùng chung cho cả OTP và luồng escalation cảnh báo khẩn cấp tự động hoá ở `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md` |
