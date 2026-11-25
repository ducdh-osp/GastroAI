# Tổng quan Phân hệ — AI Chatbot & RAG (+ Triage)

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | AI Chatbot & RAG — lõi, phần quan trọng nhất của toàn hệ thống |
| **UC nguồn** | `UC0028 - UC0044 (17 UC)` |
| **Actor** | Hệ thống (phần lớn), Bệnh nhân (một số màn hình cụ thể) |
| **Tài liệu liên quan** | `02-NguoiDung-BenhNhan/` (nơi màn Chat hiển thị kết quả của phân hệ này) · `04-QuanTri-AdminCMS/` (quản lý kho tri thức, xem báo cáo đánh giá chất lượng) |
| **Phiên bản** | v1.0 — viết theo trải nghiệm người dùng thực tế |

> **Khác với mọi phân hệ khác trong tài liệu này, phân hệ này gần như không có "màn hình" theo nghĩa thông thường.** Phần lớn 17 UC ở đây là **cơ chế đứng sau** màn Chat (`02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md`) — bệnh nhân không bao giờ "mở" ra pipeline RAG hay bộ lọc Triage, họ chỉ cảm nhận được kết quả: câu trả lời đúng, có trích dẫn, tới nhanh, và được cảnh báo kịp thời khi có nguy hiểm. Tài liệu này mô tả phân hệ như một **bộ máy vận hành phía sau**, với yêu cầu chất lượng rõ ràng cho từng khâu — không phải như một tập hợp màn hình.

> **Đây là nơi lời hứa "chatbot AI chuyên biệt tiêu hoá, đáng tin cậy hơn hỏi Google" phải được chứng minh bằng chất lượng thật, không phải bằng giao diện đẹp.** Một màn chat đẹp mà trả lời sai hoặc bỏ sót dấu hiệu nguy hiểm thì tệ hơn nhiều so với không có AI — tài liệu này vì vậy đặt nặng các tiêu chí chất lượng/an toàn hơn là mô tả thao tác.

---

## 1. Hai lớp cơ chế & 2 màn hình thật sự tồn tại

| Lớp | Gồm những UC | Có màn hình không? |
| :--- | :--- | :--- |
| **Lớp trả lời (RAG)** | `UC0028, 029, 030, 031, 032, 033, 037` | Không — kết quả hiện trên `SCR-PT-08-ChatAI.md` |
| **Lớp an toàn (Triage)** | `UC0034, 036, 038, 040` | Không — kết quả hiện trên `SCR-PT-08-ChatAI.md` (cảnh báo) và trên các màn Admin (`04-QuanTri-AdminCMS/`) |
| **Màn hình thật** | `UC0039`, `UC0041` | [SCR-AI-01-SangLocTrieuChung.md](SCR-AI-01-SangLocTrieuChung.md) |
| **Đã cắt khỏi phạm vi** | `UC0044` | [SCR-AI-02-NhanDienMonAn.md](SCR-AI-02-NhanDienMonAn.md) — không triển khai, xem ghi chú trong file |
| **Đã mô tả UI ở phân hệ khác** | `UC0035, 042, 043` | `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md` (chỉ mô tả *cơ chế* ở đây, không lặp lại UI) |
| **UI ở phân hệ Admin** | `UC0037, 038` | `04-QuanTri-AdminCMS/` (trang đánh giá chất lượng AI) |

---

## 2. Lớp trả lời (RAG) — nghiệp vụ dùng chung

### 2.1 Hành trình 1 câu hỏi của bệnh nhân, từ góc nhìn nghiệp vụ

Ngữ cảnh hiện tại gồm câu hỏi, ảnh/tài liệu gửi kèm và các đoạn RAG truy xuất. Cả luồng thường và streaming chưa tự nạp hồ sơ bệnh lý hoặc nhật ký của bệnh nhân; mục tiêu cá nhân hoá và giới hạn hiển thị được mô tả tại [SCR-PT-08](../02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md).

```text
   [ Bệnh nhân gõ câu hỏi trên SCR-PT-08-ChatAI ]
                      │
                      ▼
   ┌──────────────────────────────────────┐
   │ 1. Có phải dấu hiệu nguy hiểm không?    │ ← Lớp an toàn (Triage, §3)
   │    kiểm tra TRƯỚC KHI trả lời bất cứ gì │    luôn chạy trước, không bao giờ bỏ qua
   └──────────────────┬───────────────────┘
          ┌────────────┴────────────┐
          ▼ (Có)                     ▼ (Không/chưa rõ)
   Cảnh báo khẩn cấp ngay      ┌──────────────────────────────┐
   (UC0035, xem SCR-PT-08)     │ 2. Tìm trong kho tài liệu y khoa │
   Vẫn tiếp tục trả lời bình   │    đoạn nào liên quan nhất tới   │
   thường sau cảnh báo —       │    câu hỏi này (UC0028)           │
   không chặn hẳn cuộc trò     └──────────────┬────────────────┘
   chuyện, vì bệnh nhân vẫn                     ▼
   cần được tư vấn thêm trong   ┌──────────────────────────────┐
   lúc chờ/di chuyển tới cơ sở  │ 3. Dựa trên đoạn tìm được, soạn  │
   y tế                         │    câu trả lời kèm trích dẫn      │
                                 │    nguồn cụ thể (UC0029)          │
                                 └──────────────┬────────────────┘
                                                 ▼
                                 ┌──────────────────────────────┐
                                 │ 4. Hiện dần câu trả lời ngay khi  │
                                 │    đang soạn, không bắt chờ xong  │
                                 │    toàn bộ mới hiện (UC0030)      │
                                 └──────────────────────────────┘
```

### 2.2 Vì sao "trả lời có trích dẫn" (`UC0028`, `UC0029`) là yêu cầu không thể thoả hiệp

Một AI trả lời trôi chảy nhưng không kiểm chứng được nguồn gốc là rủi ro lớn với tư vấn y tế — nó có thể "nói rất tự tin" những điều sai (hiện tượng phổ biến với AI tạo sinh). Yêu cầu nghiệp vụ bắt buộc: **mọi câu trả lời liên quan tới thông tin y khoa cụ thể phải truy ngược được về đúng đoạn tài liệu đã dùng để tạo ra nó.** Đây không phải tính năng "nice to have" mà là điều kiện để sản phẩm xứng đáng được tin dùng cho tư vấn sức khoẻ — khác hẳn một chatbot thông thường.

Chất lượng của bước "tìm đoạn tài liệu liên quan" (`UC0028`) quyết định toàn bộ chất lượng câu trả lời phía sau — nếu tìm sai/thiếu, dù bước sinh câu trả lời (`UC0029`) có giỏi thế nào cũng chỉ đang "trả lời đúng dựa trên tài liệu sai". Vì vậy cần đo lường độ chính xác của bước này một cách định lượng, không chỉ đánh giá cảm tính (xem `UC0037`, §2.5).

**Hiện trạng hiển thị:** `RagQueryService.buildSources()` chọn tối đa 2 nguồn, che liều lượng rồi viết lại đoạn tham khảo cho dễ đọc. Nếu bước viết lại lỗi/sai định dạng, dùng đoạn gốc đã che liều; đoạn người dùng xem không được coi là trích nguyên văn. `SourceReferences` hiển thị đoạn nội dung dưới nhãn "Tài liệu tham khảo N", không mở tài liệu gốc. Khi không truy xuất được tài liệu phù hợp, danh sách nguồn rỗng. Đây là giới hạn hiện tại so với mục tiêu truy vết nguồn đầy đủ nêu trên.

### 2.3 Vì sao cần hiện trả lời dần theo thời gian thực (`UC0030`)

Với câu trả lời dài (giải thích triệu chứng, liệt kê lưu ý), chờ vài giây im lặng rồi thấy cả khối văn bản xuất hiện đột ngột tạo cảm giác "máy đang treo" nhiều hơn hẳn so với thấy chữ xuất hiện dần như đang "suy nghĩ trước mắt mình" — đặc biệt quan trọng với người dùng đang lo lắng vì triệu chứng, sự chờ đợi mơ hồ càng làm tăng cảm giác bất an.

### 2.4 Nạp kho tri thức — số hoá, chia đoạn, tạo vector (`UC0031`, `UC0032`, `UC0033`)

Đây là quy trình "hậu cần" biến 1 tài liệu y khoa gốc (PDF, docx) thành dạng AI tra cứu được:

1. **Số hoá (`UC0031`)**: trích xuất đúng nội dung chữ từ tài liệu gốc — sai sót ở bước này (thiếu chữ, lẫn bảng biểu, đọc nhầm ký tự) sẽ kéo theo sai lệch ở mọi bước sau, không thể sửa bằng cách làm tốt hơn ở các bước tiếp theo.
2. **Chia đoạn (`UC0032`)**: cắt tài liệu dài thành các đoạn vừa đủ ngắn để tra cứu chính xác, nhưng đủ dài để giữ nguyên ngữ cảnh — cắt quá vụn làm mất ý nghĩa đoạn văn, cắt quá dài làm loãng kết quả tìm kiếm.
3. **Tạo vector (`UC0033`)**: biến mỗi đoạn thành 1 dạng dữ liệu máy so sánh được mức độ liên quan, kèm theo thông tin tài liệu gốc/trang/thời điểm — đây chính là thứ cho phép "trích dẫn nguồn" ở `UC0029` trỏ về đúng chỗ.

Toàn bộ quy trình này không có màn hình bệnh nhân nhìn thấy — nhưng *tốc độ và chất lượng* của nó quyết định trực tiếp kho tri thức có "theo kịp" tài liệu y khoa cập nhật hay không (xem `UC0048/049` ở `04-QuanTri-AdminCMS/` — nơi Admin nạp tài liệu và Biên tập chuyên môn y khoa xác nhận đối chiếu nguồn trước khi kích hoạt quy trình này cho AI dùng).

### 2.5 Đo lường chất lượng truy xuất (`UC0037`)

Không thể chỉ "cảm thấy AI trả lời có vẻ ổn" — cần một bộ câu hỏi-đáp mẫu để đo định lượng: trong số các đoạn tài liệu được tìm ra cho mỗi câu hỏi, bao nhiêu phần trăm thực sự liên quan. Đây là cơ sở khách quan để biết khi nào cần cải thiện cách tìm kiếm/chia đoạn, thay vì chỉ dựa cảm tính. UI xem báo cáo này nằm ở `04-QuanTri-AdminCMS/` (trang đánh giá AI).

---

## 3. Lớp an toàn (Triage) — nghiệp vụ dùng chung

### 3.1 Nguyên tắc tối cao: thà cảnh báo nhầm, không được bỏ sót

Đây là nguyên tắc quan trọng nhất của toàn bộ phân hệ, áp dụng cho `UC0034`, `UC0038`, `UC0040`: khi cân nhắc giữa "báo động giả" (phiền một chút) và "bỏ sót dấu hiệu nguy hiểm thật" (có thể nguy hiểm tính mạng), hệ thống **luôn phải nghiêng về phía cảnh báo nhiều hơn là bỏ sót** — ưu tiên recall (bắt được hết các trường hợp nguy hiểm thật) hơn precision (giảm số lần báo động giả).

### 3.2 Những dấu hiệu bắt buộc phải nhận diện được (`UC0034`)

Đây là danh sách tối thiểu — hệ thống sàng lọc tầng 1 phải luôn bắt được các dấu hiệu sau xuất hiện trong lời kể của bệnh nhân, dù diễn đạt bằng bất kỳ cách nào (văn nói thông thường, không nhất thiết dùng đúng thuật ngữ y khoa):

| Dấu hiệu | Vì sao bắt buộc |
| :--- | :--- |
| Nôn ra máu | Dấu hiệu xuất huyết tiêu hoá trên, có thể đe doạ tính mạng nếu không xử trí kịp |
| Đi ngoài phân đen hoặc có máu | Dấu hiệu xuất huyết tiêu hoá — cần phân biệt khẩn cấp với rối loạn tiêu hoá thông thường |
| Đau bụng dữ dội, khởi phát đột ngột | Có thể là cấp cứu ngoại khoa (vd thủng tạng rỗng, viêm ruột thừa...) — không phải cơn đau âm ỉ thông thường |
| Sụt cân không rõ nguyên nhân | Dấu hiệu cảnh báo cần loại trừ bệnh lý nghiêm trọng, dù không "khẩn cấp tức thời" như các dấu hiệu trên nhưng không được bỏ qua |
| Khó nuốt tiến triển dần | Dấu hiệu cảnh báo tổn thương thực quản cần thăm khám sớm |

Danh sách này không "đóng cứng" — cần quản lý được (xem `UC0054` ở `04-QuanTri-AdminCMS/`) để đội ngũ chuyên môn bổ sung khi phát hiện cách diễn đạt mới của bệnh nhân mà hệ thống chưa bắt được.

### 3.3 Từ "có/không cảnh báo" tới "mức độ nghiêm trọng" (`UC0040`, `UC0041`)

Chỉ báo "có nguy hiểm hay không" là chưa đủ tinh tế — nhiều tình huống nằm giữa "bình thường" và "cấp cứu ngay". Nên phân theo 3 mức hành động cụ thể, dễ hành động hơn là chỉ 1 ngưỡng nhị phân:

| Mức độ | Gợi ý hành động cho bệnh nhân |
| :--- | :--- |
| Nhẹ | Có thể tự theo dõi/chăm sóc tại nhà, kèm hướng dẫn cụ thể |
| Trung bình | Nên sắp xếp gặp bác sĩ trong thời gian gần (không cấp cứu ngay nhưng không nên trì hoãn lâu) |
| Nặng | Cần tới cơ sở y tế ngay lập tức |

Gợi ý mức độ chăm sóc (`UC0041`) chỉ có ý nghĩa nếu đi kèm **lý do cụ thể** ("vì bạn mô tả đau kèm sốt kéo dài hơn 2 ngày") — một khuyến nghị không giải thích dễ bị bỏ qua hoặc gây hoang mang không cần thiết.

### 3.4 Khi hệ thống cảnh báo, điều gì xảy ra phía sau (`UC0036`)

Cảnh báo không chỉ hiện cho bệnh nhân thấy (`UC0035`) — hệ thống đồng thời **tự động** xác nhận lại tình trạng bệnh nhân (push, rồi gọi tự động nếu đã tích hợp), không chỉ dựa vào việc bệnh nhân tự ý thức được mức độ nghiêm trọng và tự đi khám. Admin trực chỉ được kéo vào **khi mọi kênh tự động đều không có phản hồi** — xem luồng đầy đủ ở `04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md`. Đây là lớp bảo vệ thứ hai, dành cho trường hợp bệnh nhân đọc cảnh báo nhưng chủ quan bỏ qua (hoặc không mở app để thấy cảnh báo) — vai trò của Admin ở đây là **xác nhận cuối cùng trước khi escalate tới bên thứ 3**, không phải đưa ra chẩn đoán.

### 3.5 Đo lường độ nhạy của bộ lọc (`UC0038`)

Cũng như `UC0037` ở lớp RAG, cần một tập câu hỏi/tình huống kiểm thử (gồm cả các câu *thật sự* nguy hiểm lẫn các câu *không* nguy hiểm nhưng dễ gây nhầm) để đo định lượng: hệ thống có bắt được đủ các trường hợp nguy hiểm thật không (recall), và có báo động giả quá nhiều tới mức gây phiền không (tỷ lệ báo động giả). UI xem báo cáo này cũng ở `04-QuanTri-AdminCMS/`.

---

## 4. Bộ câu hỏi sàng lọc có cấu trúc — khi chat tự do chưa đủ

`UC0039` + `UC0041` là trường hợp đặc biệt: có màn hình thật ([SCR-AI-01-SangLocTrieuChung.md](SCR-AI-01-SangLocTrieuChung.md)) vì đây là lúc hệ thống **chủ động hỏi theo từng bước** thay vì chờ bệnh nhân tự mô tả tự do — hữu ích khi bệnh nhân không biết nên mô tả triệu chứng thế nào cho đủ, hoặc khi cần thu thập thông tin có hệ thống hơn là 1 câu hỏi ngẫu hứng.

## 5. Nhận diện món ăn qua ảnh — đã cắt khỏi phạm vi

`UC0044` ([SCR-AI-02-NhanDienMonAn.md](SCR-AI-02-NhanDienMonAn.md)) — **cắt khỏi phạm vi đồ án (2026-11-19)**, không triển khai. Bệnh nhân ghi nhật ký ăn uống bằng nhập tay tự do ở `02-NguoiDung-BenhNhan/SCR-PT-02-NhatKyAnUong.md` như bình thường, không có lối tắt chụp ảnh.

---

## 6. Những yêu cầu không được bỏ sót (`ba-bigtech-10yr`)

- **Thời gian phản hồi**: với sản phẩm tư vấn sức khoẻ, cảm giác "AI đang chờ lâu" trong lúc lo lắng về triệu chứng gây trải nghiệm tệ hơn nhiều so với chờ đợi thông thường — mục tiêu chất lượng là thời gian bắt đầu hiện chữ đầu tiên phải nhanh, dù câu trả lời đầy đủ có thể cần thêm thời gian để hoàn tất.
- **Luôn log lại quyết định của lớp an toàn**: mỗi lần hệ thống quyết định "cảnh báo" hay "không cảnh báo" cho 1 cuộc trò chuyện cần lưu vết — đây là dữ liệu bắt buộc phải có để sau này đánh giá lại (`UC0038`) và để điều tra nếu có khiếu nại/sự cố.
- **Không để lộ dữ liệu sức khoẻ qua kho tri thức dùng chung**: kho tài liệu y khoa (`UC0031-033`) là tài liệu *chung*, không chứa thông tin cá nhân bệnh nhân nào — tuyệt đối không được lẫn dữ liệu cá nhân của người này vào ngữ cảnh trả lời cho người khác.

## 7. Traceability (UC ↔ Nơi biểu hiện)

| UC | Biểu hiện ở đâu |
| :--- | :--- |
| `UC0028`, `UC0029`, `UC0030` | Cơ chế đứng sau `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md` |
| `UC0031`, `UC0032`, `UC0033` | Cơ chế chạy khi Admin nạp tài liệu — `04-QuanTri-AdminCMS/` (UC0048) |
| `UC0034`, `UC0036`, `UC0040` | Cơ chế đứng sau cảnh báo trên `SCR-PT-08-ChatAI.md` + màn giám sát Admin |
| `UC0035` | UI ở `SCR-PT-08-ChatAI.md` |
| `UC0037`, `UC0038` | Báo cáo ở `04-QuanTri-AdminCMS/` (trang đánh giá AI) |
| `UC0039`, `UC0041` | [SCR-AI-01-SangLocTrieuChung.md](SCR-AI-01-SangLocTrieuChung.md) |
| `UC0042`, `UC0043` | UI ở `SCR-PT-08-ChatAI.md` |
| `UC0044` | **Cắt khỏi phạm vi** — [SCR-AI-02-NhanDienMonAn.md](SCR-AI-02-NhanDienMonAn.md) giữ lại chỉ để tham khảo |

## 8. Tham chiếu

- `02-NguoiDung-BenhNhan/SCR-PT-08-ChatAI.md` — nơi hầu hết kết quả của phân hệ này hiển thị.
- `04-QuanTri-AdminCMS/` — quản lý kho tri thức, từ khoá Triage, báo cáo đánh giá chất lượng.

## 9. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo, viết theo trải nghiệm người dùng thực tế |
| v1.1 | 2026-11-17 | Bỏ "Bác sĩ" khỏi §2.4/§3.4 — đổi thành Admin (vận hành) + Biên tập chuyên môn y khoa (duyệt nội dung), theo quyết định đổi mô hình vai trò ở `04-QuanTri-AdminCMS/00-TongQuan.md` |
| v1.2 | 2026-11-19 | `UC0044` (nhận diện món ăn qua ảnh) cắt khỏi phạm vi đồ án — cập nhật §1, §5, §7 |
| v1.3 | 2026-11-19 | Sửa §3.4 (`UC0036`) bị lệch so với `SCR-AD-13` đã viết lại AI-first — mô tả cũ vẫn ghi "Admin tự gọi xác nhận theo kịch bản" dù màn đó đã đổi thành hệ thống tự động, Admin chỉ xác nhận bước cuối |
