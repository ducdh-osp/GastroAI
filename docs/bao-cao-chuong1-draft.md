# Báo cáo đồ án GastroAI — Bản nháp theo mẫu E4_QuyenDoAn

> File nháp lưu lại nội dung đã chốt qua trao đổi, đối chiếu với code thật (`TriageService.java`,
> `RagQueryService.java`, `ChunkingService.java`, `RagEvaluationService.java`,
> `TriageEvaluationService.java`) và git log thật — không có số liệu bịa. Copy từng mục vào đúng
> vị trí trong Google Docs theo cấu trúc mẫu `docs/E4_QuyenDoAn_W3_V2.docx`.
>
> Số liệu đo thật (Hit Rate@1=95.5%, MRR=0.955, Triage Sensitivity=100%/24 ca dương tính,
> False Alarm=0%/22 ca âm tính — tổng golden dataset 46 ca, xem `TriageEvaluationService.java`)
> lấy từ trang Đánh giá CMS ngày 19/11/2026 — nếu chạy lại benchmark, số có thể đổi, cần cập
> nhật lại trước khi nộp chính thức.

---

## Mở đầu

### Hình 1. Sơ đồ tổng quan bài toán

Đặt ngay sau phần (a) Bài toán thực tế và ý nghĩa, trước phần (b). Nội dung sơ đồ (luồng trái → phải):

```
[VẤN ĐỀ]                              [GIẢI PHÁP — GastroAI]
Người bệnh có triệu chứng   →   Hỏi qua Chat AI
tiêu hóa, tự tra cứu mạng        │
                                  ├──▶ Lớp RAG: truy xuất tài liệu y khoa
   2 rủi ro:                     │    đã kiểm định → trả lời kèm trích dẫn
   • Thông tin sai lệch,         │
     không kiểm chứng            ├──▶ Lớp Triage tầng 1: quét dấu hiệu
     (hallucination)             │    cấp cứu → cảnh báo khẩn nếu phát hiện
   • Bỏ lỡ dấu hiệu                │
     cấp cứu                     └──▶ Theo dõi sức khỏe cá nhân: nhật ký
                                       ăn uống + Bristol → gợi ý tương quan
                                       thực phẩm–triệu chứng theo thời gian

                              ↓
                  Câu trả lời tham khảo có căn cứ +
                  cảnh báo an toàn kịp thời +
                  khuyến nghị gặp bác sĩ khi cần
                  (không thay thế chẩn đoán)
```

*(Chưa vẽ thành ảnh thật — cần quyết định tự vẽ PowerPoint/draw.io hay nhờ dựng ảnh.)*

### Bảng 1. Phân chia công việc theo thành viên

(Đã đưa vào Google Docs — xem trang thật, số liệu lấy từ sheet "Phân công & đóng góp" trong
`docs/masterplan-phan-he-usecase.xlsx`.)

| Thành viên | Số UC phụ trách | Tổng điểm độ khó | % đóng góp |
|---|---|---|---|
| Đỗ Hữu Đức (Trưởng nhóm) | 10 | 30.0 | 17.4% |
| Nguyễn Phương Nam | 29 | 57.5 | 30.7% |
| Ngô Văn Hải | 26 | 49.5 | 24.6% |
| Nguyễn Hồng Thăng | 20 | 51.5 | 27.7% |

---

## CHƯƠNG I. GIỚI THIỆU ĐỀ TÀI

### 1.1 Bối cảnh và lý do chọn đề tài

#### 1.1.1 Nhu cầu và khó khăn hiện tại

Các bệnh lý tiêu hóa phổ biến (viêm loét dạ dày – tá tràng, trào ngược dạ dày – thực quản, hội
chứng ruột kích thích, rối loạn tiêu hóa...) chịu ảnh hưởng lớn từ thói quen ăn uống và lối sống
hiện đại; tại Việt Nam, ung thư dạ dày và ung thư đại trực tràng nằm trong nhóm ung thư thường
gặp nhất, xếp lần lượt thứ 5 và thứ 4 theo thống kê GLOBOCAN 2020 [1]. Trong khi đó, người bệnh
có xu hướng tự tra cứu thông tin trên internet trước khi đến gặp bác sĩ: một nghiên cứu phân
tích dữ liệu tìm kiếm quy mô lớn ghi nhận trung bình 15,49% người dùng tra cứu triệu chứng liên
quan đến bệnh của mình trước khi được chẩn đoán chính thức [2], còn một khảo sát tại Mỹ cho thấy
hơn 75% người được hỏi tra cứu triệu chứng trên mạng trước khi đi khám [3]. Từ thực tế đó, nhóm
xác định ba khó khăn chính:
- Nguồn thông tin sức khỏe trôi nổi trên mạng thường thiếu kiểm chứng, không cá nhân hóa theo
  tình trạng từng người.
- Các chatbot AI tổng quát trả lời trôi chảy nhưng không bắt buộc đối chiếu tài liệu y khoa cụ
  thể, dẫn đến rủi ro trả lời sai nhưng vẫn tự tin (hallucination).
- Người bệnh tự tra cứu có thể chủ quan, bỏ lỡ thời điểm cần can thiệp y tế khẩn cấp.

*(Đã loại bỏ con số "10% dân số Việt Nam mắc bệnh tiêu hóa" — nguồn không rõ gốc, chính chuyên
gia được trích dẫn cũng phủ nhận con số này.)*

#### 1.1.2 Người dùng và đối tượng hưởng lợi

- **Bệnh nhân:** tra cứu thông tin sức khỏe tiêu hóa qua chatbot, ghi nhật ký ăn uống/Bristol,
  nhận cảnh báo khi có dấu hiệu nguy hiểm.
- **Quản trị viên/Biên tập nội dung (CMS):** quản lý kho tài liệu y khoa cho RAG, cấu hình từ
  khóa Triage, theo dõi nhật ký hoạt động hệ thống.

#### 1.1.3 Cơ sở tham chiếu

Đề tài dựa trên: thang phân loại phân Bristol (Bristol Stool Form Scale) [4] — công cụ lâm sàng
chuẩn để người bệnh tự ghi nhận tính chất phân; kiến trúc RAG (Retrieval-Augmented Generation)
kết hợp mô hình ngôn ngữ lớn với truy xuất tài liệu nhằm giảm hallucination; và nguyên tắc chế độ
ăn FODMAP thấp (Đại học Monash) làm cơ sở cho việc theo dõi tương quan thực phẩm – triệu chứng.

#### 1.1.4 Khảo sát các nhóm giải pháp hiện có

| Nhóm giải pháp | Ưu điểm | Hạn chế |
|---|---|---|
| Ứng dụng tra cứu/sàng lọc triệu chứng chuyên biệt | Có cơ sở tri thức y khoa dạng cấu trúc, phân loại được mức độ triệu chứng | Tương tác theo chuỗi câu hỏi/trắc nghiệm cố định, không phải hội thoại tự nhiên; bao quát đa chuyên khoa nên thiếu chiều sâu riêng cho tiêu hóa |
| Ứng dụng nhật ký ăn uống/triệu chứng | Hỗ trợ tốt việc ghi chép, lưu trữ và thống kê dữ liệu cá nhân theo thời gian | Không có chatbot AI tư vấn hội thoại; không có cơ chế cảnh báo an toàn y khoa tích hợp |
| Hệ thống của đề tài (GastroAI) | Hội thoại tự nhiên có trích dẫn nguồn (RAG); cảnh báo cấp cứu chủ động chuyên biệt tiêu hóa (Triage); gắn liền theo dõi sức khỏe cá nhân để gợi ý tương quan thực phẩm–triệu chứng | Là lớp hỗ trợ sàng lọc/tham khảo, không thay thế chẩn đoán bác sĩ; độ chính xác phụ thuộc chất lượng kho tài liệu đã nạp và chi phí/giới hạn tần suất gọi API LLM |

**Bảng 2.** So sánh sơ bộ các nhóm giải pháp người học đang dùng với hướng tiếp cận của đề tài.

*Không nêu tên thương hiệu cụ thể (Ada Health, FODMAP Snap...) — không ai trong nhóm dùng thử,
tránh bị hỏi vặn chi tiết sản phẩm không trả lời được.*

#### 1.1.5 Lý do chọn đề tài

- **Nhu cầu thực tế:** người bệnh cần câu trả lời có căn cứ, cá nhân hóa và cảnh báo an toàn kịp
  thời, thay vì tự tra cứu rời rạc trên mạng.
- **Tính khả thi:** kiến trúc RAG + LLM API cho phép xây dựng nguyên mẫu mà không cần huấn luyện
  mô hình riêng; tài liệu y khoa công khai đủ để xây kho tri thức ban đầu.
- **Giá trị học thuật:** kết hợp RAG, lớp sàng lọc an toàn (Triage) và theo dõi sức khỏe cá nhân
  trong cùng hệ thống, có đánh giá định lượng (Hit Rate, MRR, Recall, F1...) thay vì chỉ mô tả
  định tính.

**Trích dẫn cần thêm vào "Tài liệu tham khảo":**
- [1] GLOBOCAN 2020 — thống kê ung thư dạ dày/đại trực tràng tại Việt Nam.
  https://vneconomy.vn/ung-thu-chuyen-dich-sang-nhom-lien-quan-den-loi-song-va-tuoi-tho.htm
- [2] JMIR 2020 — phân tích dữ liệu tìm kiếm triệu chứng trước chẩn đoán.
  https://www.jmir.org/2020/3/e15065
- [3] YouGov 2018 — khảo sát tra cứu triệu chứng trực tuyến trước khi khám.
  https://today.yougov.com/news/2018/04/09/over-three-quarters-americans-look-their-symptoms-
- [4] Bristol Stool Form Scale (Lewis & Heaton, 1997) — kiến thức y khoa phổ biến, không cần link.

---

### 1.2 Mục tiêu và câu hỏi nghiên cứu/kỹ thuật

#### 1.2.1 Mục tiêu tổng quát

Xây dựng hệ thống chatbot AI hỗ trợ tư vấn thông tin sức khỏe tiêu hóa dựa trên kiến trúc RAG, có
cơ chế cảnh báo an toàn y khoa và tính năng theo dõi sức khỏe cá nhân, phục vụ đồng thời hai nhóm
người dùng: bệnh nhân và quản trị viên/biên tập nội dung. Chất lượng của lớp truy xuất RAG và lớp
sàng lọc an toàn Triage được đánh giá định lượng thay vì chỉ mô tả định tính.

#### 1.2.2 Mục tiêu cụ thể

- Xây dựng pipeline RAG hoàn chỉnh (số hóa tài liệu → chunking theo cấu trúc mục/đề mục → sinh
  embedding → lưu trữ vector trong pgvector → truy xuất top-k) cho phép AI trả lời có trích dẫn
  nguồn.
- Xây dựng lớp Triage tầng 1 nhận diện tối thiểu 5 nhóm dấu hiệu cấp cứu tiêu hóa, ưu tiên độ
  nhạy (recall) cao để hạn chế bỏ sót ca nguy hiểm.
- Xây dựng chức năng chat với phản hồi dạng streaming thời gian thực (SSE).
- Xây dựng phân hệ theo dõi sức khỏe cá nhân (hồ sơ bệnh lý, nhật ký ăn uống, nhật ký Bristol,
  nhắc uống thuốc) và gợi ý tương quan thực phẩm–triệu chứng dựa trên thống kê tần suất.
- Đảm bảo tái lập: cấu hình qua `application.yml`/biến môi trường, dữ liệu kiểm thử nạp qua
  Flyway migration cố định, ghi nhận đầy đủ qua Audit Log cho các thao tác nhạy cảm.

#### 1.2.3 Câu hỏi nghiên cứu/kỹ thuật

- Độ chính xác truy xuất RAG (Hit Rate@1/3/K, MRR) trên bộ câu hỏi kiểm thử theo từng nhóm bệnh
  tiêu hóa đạt mức nào, và truy xuất bằng vector embedding có vượt trội so với tìm kiếm từ khóa
  thuần túy không?
- Lớp Triage tầng 1 dựa trên đối sánh từ khóa có đạt độ nhạy (recall) đủ cao để hạn chế bỏ sót ca
  cấp cứu thật, với tỷ lệ báo động giả ở mức chấp nhận được không?
- Gợi ý tương quan thực phẩm–triệu chứng dựa trên thống kê tần suất đơn giản (không dùng ML) có
  đủ ổn định khi số lượng bữa ăn ghi nhận còn ít không?
- Kiến trúc đa cơ sở dữ liệu (PostgreSQL/MySQL) có đảm bảo tách biệt dữ liệu nhạy cảm mà không
  ảnh hưởng đáng kể tới độ trễ phản hồi chat thời gian thực không?

#### 1.2.4 Baseline sử dụng

- **Truy xuất RAG:** tìm kiếm từ khóa thuần túy (lexical/SQL LIKE trên nội dung tài liệu) làm mốc
  so sánh với truy xuất bằng vector embedding (pgvector similarity search).
- **Triage:** đối chiếu với tập ca kiểm thử đã gán nhãn thủ công (golden dataset: ca cấp cứu thật
  và ca không cấp cứu).
- **Tương quan thực phẩm–triệu chứng:** tỷ lệ bữa ăn kèm triệu chứng tính chung trên toàn bộ dữ
  liệu (baseline symptom rate) làm mốc xác định món ăn có tương quan nổi bật hay không.
- **Chat streaming:** thời gian phản hồi token đầu tiên (TTFT) so với gọi API không streaming.

#### 1.2.5 Chỉ số xác nhận thành công

| Chỉ số | Ngưỡng | Minh chứng |
|---|---|---|
| Hit Rate@3 (truy xuất RAG) | ≥ 70% | Báo cáo đánh giá trên bộ 22 câu hỏi kiểm thử theo nhóm bệnh tiêu hóa — thực đạt 95,5% (đo 19/11/2026) |
| Độ nhạy (Recall/Sensitivity) của Triage | ≥ 95% | Báo cáo trên tập 46 ca đã gán nhãn (24 dương tính + 22 âm tính, TP/FN/FP/TN, F1/F2) — thực đạt Sensitivity 100% (24/24), False Alarm Rate 0% (0/22) trên golden dataset tự xây dựng |
| MRR (truy xuất RAG) | ≥ 0,7 | nt — thực đạt 0,955 |
| Thời gian phản hồi token đầu tiên (TTFT) khi chat | ≤ 2 giây | Log độ trễ phía backend (chưa đo thật) |

**Bảng 3.** Chỉ số - Ngưỡng - Minh chứng.

---

### 1.3 Đối tượng, phạm vi và giới hạn

#### 1.3.1 Đối tượng

Đối tượng sử dụng gồm bệnh nhân (người trưởng thành) và quản trị viên/biên tập nội dung. Đối
tượng nghiên cứu của đề tài:
- Kiến trúc RAG trong hỏi–đáp y tế chuyên ngành.
- Kiến trúc backend đa cơ sở dữ liệu (PostgreSQL cho dữ liệu người dùng/vector, MySQL cho dữ liệu
  quản trị nội bộ).
- Cơ chế sàng lọc an toàn (Triage) và thông báo thời gian thực (SSE/WebSocket).

#### 1.3.2 Phạm vi

Chỉ tập trung vào nhóm bệnh lý tiêu hóa phổ biến (dạ dày – tá tràng, trào ngược, đại tràng/IBS,
rối loạn tiêu hóa thông thường). Đối tượng người dùng là người trưởng thành, không xử lý trường
hợp nhi khoa/sản khoa. Hệ thống đóng vai trò **hỗ trợ sàng lọc và cung cấp thông tin tham khảo**,
không thay thế chẩn đoán và điều trị của bác sĩ.

**Ngoài phạm vi:** không tích hợp gọi video/thoại trực tiếp với bác sĩ, không tích hợp thanh toán
hoặc đặt lịch khám thực tế, không xử lý đa ngôn ngữ (chỉ hỗ trợ tiếng Việt).

#### 1.3.3 MVP và stretch goals

**MVP gồm:**
- Pipeline RAG hoàn chỉnh cho câu trả lời có trích dẫn nguồn.
- Lớp Triage tầng 1 đối sánh từ khóa (15 ánh xạ dấu hiệu cảnh báo → 13 nhóm mã riêng biệt, 11/13
  nhóm set cờ khẩn cấp thật — xem Chương III §3.3).
- Chat với phản hồi streaming thời gian thực (SSE).
- Phân hệ theo dõi sức khỏe cá nhân: hồ sơ bệnh lý, nhật ký ăn uống, nhật ký Bristol, nhắc thuốc.
- Phân hệ quản trị (CMS) với RBAC 4 vai trò nội bộ (Admin, Biên tập chuyên môn y khoa, Cộng tác
  viên nội dung, Giám sát vận hành), quản lý kho tri thức và Audit Log.

**Stretch goals** (chỉ khi MVP đã đạt chỉ số cốt lõi):
- Mở rộng gợi ý tương quan thực phẩm–triệu chứng.
- Mở rộng đánh giá Triage/RAG sang hội thoại tự do thực tế, không chỉ bộ test gán nhãn sẵn.
- Nâng cấp lớp sàng lọc từ đối sánh từ khóa sang có thêm tầng phân tích ngữ nghĩa.

#### 1.3.4 Dữ liệu được phép sử dụng

Tài liệu y khoa công khai (phác đồ điều trị, cẩm nang chuẩn đã kiểm định) cho kho tri thức RAG;
dữ liệu người dùng giai đoạn phát triển là dữ liệu demo tự tạo (seed qua Flyway migration), không
chứa thông tin cá nhân thật. Không dùng tài liệu nội bộ bệnh viện chưa được phép công bố hoặc tài
liệu có bản quyền.

#### 1.3.5 Ràng buộc

- **Chi phí/giới hạn API:** trong giai đoạn phát triển, dự án dùng tầng miễn phí của Gemini API
  (giới hạn khoảng 20 request/ngày cho mô hình chat) — ràng buộc thực tế ảnh hưởng tần suất kiểm
  thử, không phải giới hạn kiến trúc cố định; có thể gỡ bỏ bằng cách bật billing khi cần mở rộng
  quy mô sử dụng (vd buổi demo/bảo vệ).
- **Pháp lý/đạo đức:** luôn hiển thị khuyến cáo "không thay thế chẩn đoán bác sĩ"; hệ thống có
  bước lọc tự động không đọc lại nguyên văn liều lượng thuốc cụ thể từ tài liệu chuyên môn dành
  cho bác sĩ khi trả lời bệnh nhân.

#### 1.3.6 Giới hạn

- Lớp Triage mới ở tầng 1 (đối sánh từ khóa), chưa kiểm chứng trên dữ liệu hội thoại tự do đa
  dạng của người dùng thật ngoài bộ test tự xây dựng.
- Độ chính xác truy xuất RAG phụ thuộc trực tiếp vào số lượng và chất lượng tài liệu đã nạp.
- Gợi ý tương quan thực phẩm–triệu chứng chỉ là thống kê tần suất đơn giản, không phải suy luận
  nhân quả, không dùng ML.
- Hệ thống là nguyên mẫu hỗ trợ sàng lọc, không thay thế chẩn đoán của bác sĩ.

---

### 1.4 Kết quả dự kiến và đóng góp

#### 1.4.1 Sản phẩm bàn giao

Tính đến commit `ca1625e` (19/11/2026, 337 commit):

| Mã | Artifact | Nơi lưu | Trạng thái |
|---|---|---|---|
| SW-01 | Mã nguồn: frontend React/Vite, backend Spring Boot | `fe/`, `be/` | Chạy được, cả 2 phân hệ bệnh nhân + CMS |
| SW-02 | Kiểm thử tự động backend (JUnit) | `be/src/test/java` | 110 file test |
| SW-03 | Kiểm thử tự động frontend | `fe/` | **Chưa có** — cần bổ sung trước khi nộp |
| SW-04 | Đặc tả API | Swagger UI (springdoc) | Đã có, sinh tự động |
| DA-01 | Dữ liệu seed/kiểm thử (PostgreSQL) | `be/.../db/migration/postgres/` | 60 migration |
| DA-02 | Dữ liệu seed/kiểm thử (MySQL, CMS) | `be/.../db/migration/mysql/` | 16 migration |
| AI-01 | Kho tri thức RAG đã số hóa | PostgreSQL + pgvector | Đã nạp tài liệu mẫu, đang mở rộng |
| RS-01 | Tài liệu nghiệp vụ, đề cương, báo cáo | `docs/` | Đang hoàn thiện theo mẫu E4 |

**Bảng 4.** Danh sách artifact, phiên bản và trạng thái.

#### 1.4.2 Đóng góp của đề tài

- **Pipeline RAG có ràng buộc ngữ cảnh** — trả lời bắt buộc đối chiếu tài liệu truy xuất, kèm
  trích dẫn nguồn; có lọc không đọc lại liều lượng thuốc cụ thể từ tài liệu dành cho bác sĩ.
- **Lớp Triage tái lập và audit được** — đối sánh từ khóa (không ML), mọi cảnh báo truy ngược
  được tới đúng nhóm từ khóa đã khớp.
- **Đánh giá định lượng tái lập được** — bộ đánh giá RAG/Triage chạy trực tiếp từ CMS, số liệu
  sinh từ script và dữ liệu thật.
- **Theo dõi sức khỏe cá nhân gắn liền phân tích** — nhật ký ăn uống/Bristol phục vụ gợi ý tương
  quan thực phẩm–triệu chứng tự động theo baseline cá nhân.
- **Nguyên mẫu chạy end-to-end thật** — cả hai phân hệ (bệnh nhân, CMS) đã triển khai chạy được,
  kiến trúc đa cơ sở dữ liệu tách biệt dữ liệu nhạy cảm.

#### 1.4.3 Đóng góp theo thành viên

| Thành viên | Đóng góp phần mềm/dữ liệu chính (theo UC đã commit) | Số UC phụ trách |
|---|---|---|
| Đỗ Hữu Đức (Trưởng nhóm) | Lõi pipeline RAG (UC0028/029/033), Triage (UC0034), phân tích tương quan thực phẩm (UC0022/023), đăng nhập (UC0002), cấu hình RAG/Triage CMS (UC0054/059) | 10 |
| Nguyễn Phương Nam | Phần lớn UC trang bệnh nhân (hồ sơ, nhật ký, nhắc thuốc, chat), RBAC/quản lý người dùng CMS, hệ thống email/thông báo | 29 |
| Ngô Văn Hải | FAQ, thư viện kiến thức, dashboard CMS, toàn bộ tầng đánh giá chất lượng AI (UC0037-040, 042) | 26 |
| Nguyễn Hồng Thăng | Audit log, backup/restore CMS, số hóa/chunking tài liệu (UC0031/032), upload tài liệu y khoa (UC0048) | 20 |

**Bảng 5.** Đóng góp dự kiến và minh chứng theo thành viên.

*Minh chứng: lịch sử commit theo tác giả trên repository GitHub của nhóm (337 commit tính đến
19/11/2026). Không bịa số PR/issue cụ thể vì repo không gắn mã issue theo từng commit một cách
hệ thống.*

---

## CHƯƠNG II. CƠ SỞ LÝ THUYẾT VÀ CÔNG TRÌNH LIÊN QUAN

> Mục 2.1/2.2 trong mẫu E4 **không có ví dụ đã viết** (đội IELTS cũng để trống placeholder), chỉ có
> checklist yêu cầu. Nội dung dưới đây tự soạn, chỉ chọn lý thuyết/công trình thật liên quan trực
> tiếp đến code đã có, có trích dẫn kiểm chứng qua WebSearch (không bịa DOI/số liệu).

### 2.1 Kiến thức nền tảng

**1. Thang phân loại phân Bristol (Bristol Stool Form Scale)** [Lewis & Heaton, 1997]

Thang đo lâm sàng 7 loại, phân loại hình dạng/độ rắn của phân từ loại 1 (táo bón nặng, dạng cục
cứng rời) đến loại 7 (tiêu chảy, dạng lỏng hoàn toàn); loại 3–5 được coi là bình thường.
*Dùng ở: Chương III (thiết kế dữ liệu nhật ký Bristol), Chương IV (giao diện ghi nhận), và làm cơ
sở xác định "Bristol bất thường" (loại 1,2,6,7) trong thuật toán tương quan thực phẩm–triệu chứng
ở Chương III/V.*

**2. Retrieval-Augmented Generation (RAG)** [Lewis et al., 2020]

Kiến trúc kết hợp mô hình ngôn ngữ lớn (LLM) với một bước **truy xuất** (retrieval) tài liệu liên
quan từ kho tri thức ngoài trước khi sinh câu trả lời, nhằm giảm "ảo giác" (hallucination) — mô
hình tự bịa thông tin không có căn cứ. Khác với LLM thuần túy chỉ dựa vào tri thức đã học lúc huấn
luyện, RAG cho phép câu trả lời bám theo tài liệu cụ thể, có thể trích dẫn nguồn và cập nhật tri
thức mà không cần huấn luyện lại mô hình.
*Dùng ở: Chương III (thiết kế pipeline RAG), Chương IV (sơ đồ kiến trúc, prompt ràng buộc ngữ cảnh).*

**3. Vector embedding và độ tương đồng cosine**

Văn bản được chuyển thành vector số thực nhiều chiều (embedding) sao cho các đoạn có ngữ nghĩa gần
nhau thì vector gần nhau trong không gian vector. Độ gần đo bằng cosine similarity:

```
cos(A, B) = (A · B) / (‖A‖ × ‖B‖)
```

Giá trị càng gần 1 thì càng tương đồng ngữ nghĩa. Truy xuất top-k dựa trên tìm k vector tài liệu
gần nhất với vector câu hỏi (pgvector tính qua khoảng cách, `distance ≈ 1 − cosine similarity`).
*Dùng ở: Chương III (thiết kế truy xuất), Chương V (Hit Rate/MRR/độ tương đồng cosine đo thật trên
22 câu hỏi kiểm thử).*

**4. Chunking văn bản**

Tài liệu dài cần chia thành đoạn nhỏ (chunk) trước khi sinh embedding vì mô hình embedding có giới
hạn độ dài đầu vào, và chunk nhỏ giúp truy xuất chính xác hơn. Chiến lược chia theo ranh giới
mục/đề mục (không cắt cứng theo số ký tự) giữ nguyên ngữ cảnh ngữ nghĩa, kèm đoạn chồng lấn
(overlap) giữa các chunk liền kề để tránh mất thông tin ở ranh giới cắt.
*Dùng ở: Chương III (thiết kế pipeline số hóa tài liệu).*

**5. Sàng lọc triệu chứng (Triage) trong y tế**

Trong lâm sàng, triage là phân loại mức độ ưu tiên/khẩn cấp của bệnh nhân dựa trên dấu hiệu lâm
sàng. Áp dụng vào hệ thống hỗ trợ, một lớp sàng lọc rule-based ưu tiên **độ nhạy (sensitivity) cao**
hơn độ chính xác (precision) — chấp nhận báo động giả nhiều hơn để giảm bỏ sót ca nguy hiểm thật
(false negative), vì chi phí bỏ sót trong y tế cao hơn nhiều so với chi phí báo động nhầm.
*Dùng ở: Chương III (thiết kế lớp Triage tầng 1), Chương V (đánh giá độ nhạy/tỷ lệ báo động giả).*

**6. Các chỉ số đánh giá truy xuất thông tin (Information Retrieval metrics)**

```
Hit Rate@K = (số câu hỏi có tài liệu đúng trong top-K) / (tổng số câu hỏi)
MRR = (1/N) × Σ (1 / rank_i)   — rank_i: vị trí xuất hiện đầu tiên của tài liệu đúng (0 nếu không có)
Precision@K = (số tài liệu liên quan trong top-K) / K
Recall@K   = (số tài liệu liên quan tìm được) / (tổng số tài liệu liên quan có thật)
```

*Dùng ở: Chương V (Hit Rate@1=95,5%, MRR=0,955 đo trên 22 câu hỏi kiểm thử — xem Bảng 3 Chương I).*

**7. Các chỉ số đánh giá phân loại nhị phân (Binary classification metrics)**

```
Sensitivity/Recall = TP/(TP+FN)      Specificity = TN/(TN+FP)
Precision = TP/(TP+FP)               False Alarm Rate (FPR) = FP/(FP+TN)
F-beta = (1+β²)(P×R) / (β²P + R)     — F2 (β=2) ưu tiên Recall hơn Precision, phù hợp Triage vì
                                        bỏ sót ca cấp cứu (FN) nguy hiểm hơn báo động giả (FP)
```

*Dùng ở: Chương V (Triage Sensitivity=100% trên 24 ca dương tính, False Alarm Rate=0% trên 22 ca
âm tính — golden dataset 46 ca).*

**8. Server-Sent Events (SSE)** [WHATWG/W3C]

Giao thức truyền dữ liệu một chiều từ server xuống client qua HTTP, giữ kết nối mở để gửi liên tục
nhiều sự kiện — phù hợp hiển thị câu trả lời AI theo từng token ngay khi sinh ra (streaming).
*Dùng ở: Chương III/IV (thiết kế luồng chat thời gian thực).*

**9. Kiểm soát truy cập theo vai trò (RBAC)** [Sandhu et al., 1996]

Mô hình phân quyền gán quyền cho vai trò (role) thay vì trực tiếp cho người dùng; người dùng được
gán vào một/nhiều vai trò để kế thừa quyền tương ứng.
*Dùng ở: Chương III/IV (phân quyền CMS: Admin/Biên tập chuyên môn y khoa — đã bỏ vai trò "Bác sĩ"
như tài khoản vận hành riêng theo quyết định 17/11/2026, xem `00-General/04-QuanTri-AdminCMS/00-TongQuan.md` §0).*

**Tài liệu tham khảo (bổ sung vào danh mục cuối báo cáo):**
- Lewis, P. et al. (2020). *Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks.* NeurIPS.
- Lewis, S.J. & Heaton, K.W. (1997). *Stool form scale as a useful guide to intestinal transit time.*
- Sandhu, R. et al. (1996). *Role-Based Access Control Models.* IEEE Computer.
- WHATWG. *Server-Sent Events specification.* html.spec.whatwg.org/multipage/server-sent-events.html

---

### 2.2 Công trình/hệ thống liên quan

> Checklist mẫu: tối thiểu 5-10 công trình/hệ thống gần bài toán, chỉ ra điểm mạnh/yếu và khoảng
> trống, kèm bảng so sánh method/data/metric/limitation. Toàn bộ 7 công trình dưới đây đã kiểm
> chứng thật qua WebSearch ngày 20/11/2026 (tên tác giả, năm, venue, số liệu) — không bịa.

**Bảng 6.** So sánh các công trình/hệ thống liên quan với hướng tiếp cận của GastroAI.

| Công trình/Hệ thống | Phương pháp | Dữ liệu | Thước đo chính | Hạn chế so với GastroAI |
|---|---|---|---|---|
| Semigran et al., 2015 (BMJ) | Symptom checker, cây quyết định (không LLM) | 45 ca bệnh chuẩn hoá | Top-1 đúng: 34%; triage phù hợp 80%/55%/33% | Không LLM; thiên an toàn quá mức; không trích dẫn nguồn |
| Singhal et al., 2023 (Nature) — Med-PaLM | LLM instruction-tuned (Flan-PaLM) | MultiMedQA (6 bộ QA + HealthSearchQA) | 67,6% MedQA; 92,6% phù hợp đồng thuận khoa học | Không truy xuất thời gian thực; không tiếng Việt |
| Zakka et al., 2024 (NEJM AI) — Almanac | RAG trên tài liệu lâm sàng chọn lọc | 314 câu hỏi, 9 chuyên khoa, 8 bác sĩ đánh giá | Trích dẫn đúng 91%; vượt ChatGPT-4/Bing/Bard | Hướng tới bác sĩ; không Triage; không mã nguồn mở |
| Xiong et al., 2024 (ACL) — MIRAGE/MedRAG | Benchmark RAG (4 retriever x 6 LLM) | 7.663 câu hỏi, 5 bộ QA | Tăng 1–18% so với chain-of-thought | Chỉ là benchmark; không Triage; không tiếng Việt |
| Li et al., 2023 (Cureus) — ChatDoctor | LLaMA fine-tuned + truy xuất bổ sung | HealthCareMagic-100k | Vượt ChatGPT về precision/recall/F1 (tự báo cáo) | Tác giả tự nhận chưa đảm bảo đúng chẩn đoán; không Triage |
| Ji et al., 2023 (ACM CSUR) | Khảo sát lý thuyết hallucination | Tổng hợp nhiều công trình | Không có số đo thực nghiệm riêng | Chỉ lý thuyết tổng quan, không giải pháp cụ thể |
| RMIT (CMPB) | Fine-tune LLM mở cho tiếng Việt | ~337.000 cặp hỏi-đáp tiếng Việt | BertScore, Rouge-L, LLM-as-judge | Fine-tuning khó cập nhật nguồn; thiếu chuyên gia chấm |
| GastroAI (đề tài này) | RAG + Triage rule-based tách biệt + theo dõi cá nhân hoá | Tài liệu tiếng Việt về tiêu hoá + dữ liệu cá nhân | Hit Rate@1, MRR (RAG); Sensitivity, False Alarm (Triage) | Quy mô nhỏ (22 câu hỏi, 46 ca Triage); chưa thẩm định độc lập |

**Khoảng trống mà đề tài nhắm tới:** các hệ thống symptom-checker rule-based (Semigran) không tận
dụng được LLM để diễn giải tự nhiên; các LLM y khoa tổng quát (Med-PaLM, ChatDoctor) không có bước
truy xuất tài liệu có trích dẫn theo thời gian thực và không có lớp an toàn Triage tách biệt; các
hệ thống RAG y khoa đã có (Almanac, MedRAG) hướng tới giới chuyên môn hoặc chỉ là benchmark nghiên
cứu, không gắn với theo dõi cá nhân hoá; nghiên cứu tiếng Việt hiện có (RMIT) dùng fine-tuning nên
khó trích dẫn nguồn như RAG. GastroAI không tuyên bố mỗi thành phần riêng lẻ là mới (RAG, Triage
rule-based, theo dõi Bristol đều đã có tiền lệ) — điểm khác biệt là **tổ hợp cả ba trong một hệ
thống tiếng Việt chuyên biệt cho bệnh tiêu hoá**, với lớp Triage tách khỏi RAG (không để LLM tự
quyết định mức độ nguy hiểm) — tổ hợp này chưa thấy ở các công trình đã khảo sát.

**Tài liệu tham khảo (bổ sung):**
- Semigran, H.L. et al. (2015). *Evaluation of symptom checkers for self diagnosis and triage: audit study.* BMJ 2015;351:h3480.
- Singhal, K. et al. (2023). *Large language models encode clinical knowledge.* Nature 620, 172–180.
- Zakka, C. et al. (2024). *Almanac: Retrieval-Augmented Language Models for Clinical Medicine.* NEJM AI 1(2).
- Xiong, G., Jin, Q., Lu, Z., Zhang, A. (2024). *Benchmarking Retrieval-Augmented Generation for Medicine.* Findings of ACL 2024, 6233–6251.
- Li, Y. et al. (2023). *ChatDoctor: A Medical Chat Model Fine-Tuned on LLaMA Using Medical Domain Knowledge.* Cureus 15(6):e40895.
- Ji, Z. et al. (2023). *Survey of Hallucination in Natural Language Generation.* ACM Computing Surveys 55(12), 1–38.
- RMIT University research team. *Fine-tuning large language models for improved health communication in low-resource languages.* Computer Methods and Programs in Biomedicine.

---

### 2.3 Công nghệ và lý do lựa chọn

> Checklist mẫu: tại sao chọn stack/model này thay vì 1-2 lựa chọn khác, rủi ro lock-in/licensing,
> kèm bảng trade-off có tiêu chí. Dựa trên stack thật đang chạy (xem `docs/de-cuong-do-an.md` mục 7
> và code backend/frontend thật).

**Bảng 7.** Trade-off các lựa chọn công nghệ chính.

| Thành phần | Lựa chọn | Lựa chọn khác đã cân nhắc | Tiêu chí quyết định | Rủi ro lock-in/licensing |
|---|---|---|---|---|
| Backend | Spring Boot (Java) | Node.js/Express, Django/FastAPI (Python) | Cả nhóm có nền Java; JPA + Flyway cho quản lý migration có kiểm soát phiên bản rõ ràng; hệ sinh thái ổn định cho đồ án nhiều UC | Thấp — mã nguồn mở (Apache 2.0), không ràng buộc hạ tầng cụ thể |
| Frontend | React + TypeScript + Ant Design | Vue 3, Angular | React có hệ sinh thái/tài liệu lớn nhất, dễ tra cứu; TypeScript bắt lỗi lúc biên dịch; AntD có sẵn component bảng/form phù hợp cho CMS quản trị, giảm thời gian tự xây UI | Thấp — mã nguồn mở (MIT) |
| CSDL bệnh nhân + RAG | PostgreSQL + pgvector | Vector DB chuyên dụng (Pinecone/Weaviate/Milvus) tách riêng | Lưu được embedding và dữ liệu quan hệ trong cùng một CSDL, tránh đồng bộ hai hệ thống; tự host được, không gửi dữ liệu bệnh nhân ra dịch vụ vector DB bên thứ ba; đủ cho quy mô dữ liệu đồ án | Thấp (tự host, mã nguồn mở) — đánh đổi: hiệu năng tìm kiếm gần đúng (ANN) ở quy mô rất lớn (hàng triệu vector) kém hơn vector DB chuyên dụng, chấp nhận được ở quy mô hiện tại |
| CSDL CMS | MySQL (tách riêng khỏi Postgres) | Dùng chung một Postgres cho cả 2 phân hệ | Tách biệt vật lý dữ liệu nhạy cảm (bệnh nhân) khỏi dữ liệu quản trị theo nguyên tắc cô lập rủi ro | Thấp — mã nguồn mở, có thể hợp nhất lại nếu cần |
| Nhà cung cấp LLM/embedding | Google Gemini API | OpenAI API; mô hình mã nguồn mở tự host (Llama, Qwen…) | Có hạn mức miễn phí cho giai đoạn phát triển (xem Chương I, mục 1.3); hỗ trợ tiếng Việt khá tốt; không cần hạ tầng GPU tự host (ngoài khả năng nhóm sinh viên) | **Cao nhất trong toàn bộ stack** — phụ thuộc trực tiếp giá/hạn mức/điều khoản của Google (đã gặp thật: hạn mức 20 request/ngày ở free tier); câu hỏi người dùng được gửi ra API bên thứ ba, cần cân nhắc thêm nếu dùng dữ liệu y tế thật; lớp gọi API được tách qua một client dùng chung trong code (`embeddingClient`) nên khi đổi nhà cung cấp chỉ cần viết lại tầng này, không phải toàn bộ nghiệp vụ |
| Kênh real-time | SSE cho chat streaming, WebSocket cho cảnh báo Triage | WebSocket cho toàn bộ (kể cả chat) | SSE đơn giản hơn cho luồng một chiều server→client (dựa trên HTTP, tự reconnect), phù hợp hiển thị câu trả lời AI theo từng token; WebSocket giữ cho kênh cần hai chiều thật sự (đẩy cảnh báo khẩn kể cả khi người dùng không đang mở chat) | Thấp — cả hai là giao thức chuẩn mở (WHATWG, RFC 6455), không ràng buộc nhà cung cấp |
| Xác thực | JWT (API bệnh nhân) + session (CMS) | JWT cho cả 2 phân hệ | CMS cần thu hồi quyền truy cập ngay lập tức khi tài khoản quản trị bị lộ (session invalidate được tức thời ở server), API bệnh nhân ưu tiên khả năng mở rộng không trạng thái (stateless) | Thấp — JWT (RFC 7519) và session đều là cơ chế chuẩn mở, tự triển khai |

**Nhận xét chung:** phần lớn stack (backend, frontend, CSDL, real-time, xác thực) đều là công nghệ
mã nguồn mở, tự host, rủi ro lock-in thấp và có thể thay thế từng phần. Rủi ro lock-in thật sự tập
trung duy nhất ở nhà cung cấp LLM/embedding (Gemini) — đây cũng là lý do mục 1.3.5 Chương I ghi rõ
ràng buộc hạn mức miễn phí chỉ áp dụng cho giai đoạn phát triển, không phải giới hạn kiến trúc
vĩnh viễn.

---

## CHƯƠNG III. PHÂN TÍCH YÊU CẦU, DỮ LIỆU VÀ PHƯƠNG PHÁP

### 3.1 Yêu cầu chức năng và phi chức năng

> Checklist mẫu: actors/use case/user story, NFR hiệu năng/bảo mật/độ tin cậy/usability, kèm sơ đồ
> use case + bảng FR/NFR có ID và acceptance criteria.
>
> **Cảnh báo drift đã phát hiện (20/11/2026), cập nhật 23/11/2026:** `docs/masterplan-phan-he-usecase.xlsx`
> (sheet "Use case chi tiết") vẫn ghi actor "Admin / Bác sĩ" cho nhiều UC quản trị, nhưng tài liệu
> nghiệp vụ mới hơn (`00-General/04-QuanTri-AdminCMS/00-TongQuan.md` §0, quyết định **17/11/2026**)
> đã **bỏ vai trò "Bác sĩ" như một tài khoản vận hành CMS riêng** — AI đảm nhận tư vấn trực tiếp,
> nhóm không có bác sĩ thật để "đóng vai" tài khoản này.
>
> **Lần đổi mô hình vai trò thứ 2 (23/11/2026):** từ 2 vai trò (Admin, Biên tập chuyên môn y khoa)
> nâng lên **4 vai trò nội bộ** — thêm **Cộng tác viên nội dung** và **Giám sát vận hành** (xem lập
> luận đầy đủ, không phải "thêm cho có", ở `00-General/04-QuanTri-AdminCMS/SCR-AD-03-PhanQuyenRBAC.md`
> §2). Nơi nào xlsx/code (`CmsRole.java`, `CmsRbacPage.tsx` vẫn còn nhãn `DOCTOR`/"Bác sĩ") chưa
> khớp quyết định bỏ "Bác sĩ", và nơi nào `SecurityConfig.java`/`cmsNav.ts` chưa cấp quyền màn hình
> cho 2 vai trò mới, đều là drift thật, chưa refactor xong — bảng dưới đây viết theo quyết định mới
> nhất (đúng), có ghi chú UC nào bị ảnh hưởng.

**Actors:**
- **Bệnh nhân** — người dùng cuối phân hệ bệnh nhân (chat AI, theo dõi sức khỏe, nhắc thuốc).
- **Admin** — quản trị CMS, không bị giới hạn màn hình nào (phân quyền, tạo tài khoản CMS, cấu
  hình, audit log).
- **Biên tập chuyên môn y khoa** — soạn **và duyệt/xuất bản** nội dung y tế (tài liệu RAG, từ khóa
  Triage, FAQ/bài viết) — tên gọi phản ánh đúng thực tế: nội dung do nhóm biên soạn/đối chiếu từ
  tài liệu y khoa đã công bố, không phải bác sĩ hành nghề thẩm định (nguyên tắc trung thực học
  thuật, xem `00-TongQuan.md` §0).
- **Cộng tác viên nội dung** *(mới)* — chỉ soạn **nháp** FAQ/bài viết + quản lý chủ đề bài viết,
  không duyệt/xuất bản được, không đụng Kho tài liệu RAG hay Từ khóa Triage.
- **Giám sát vận hành** *(mới)* — chỉ xem Dashboard thống kê và xác nhận escalate ở màn Giám sát
  cảnh báo khẩn cấp — không xem Audit Log, không đụng nội dung/tài khoản.
- **Hệ thống** — actor kỹ thuật cho các use case tự động (RAG, Triage, phân tích tương quan) không
  do người dùng thao tác trực tiếp, theo quy ước UML cho tiến trình nền.

**Hình 2. Sơ đồ Use case tổng quan** (ảnh cần vẽ lại theo đúng khối dưới đây — bản cũ `usecase_diagram.png`
đã lỗi thời theo RBAC 2 vai trò, chưa vẽ lại; mô tả khối dưới đây để đối chiếu nội dung khi vẽ):

```
[Bệnh nhân] ──┬── Xác thực & tài khoản (đăng nhập, đổi mật khẩu)
              ├── Chat AI (hỏi đáp RAG + sàng lọc Triage tầng 1)
              ├── Sàng lọc triệu chứng có cấu trúc (form từng bước)
              ├── Theo dõi sức khỏe (nhật ký ăn uống, Bristol, tương quan thực phẩm)
              ├── Nhắc thuốc & thông báo
              ├── Hồ sơ y tế cá nhân, xuất PDF
              └── FAQ / thư viện kiến thức

[Admin] ──────┬── Quản lý người dùng & phân quyền RBAC
              ├── Cấu hình tham số RAG/Triage
              ├── Xem đánh giá chất lượng AI (RAG/Triage)
              ├── Audit log (nhật ký thao tác nhạy cảm)
              └── Tạo tài khoản nhân sự CMS  ← actor duy nhất, mới (UC0077)

[Biên tập ────┬── Quản lý tài liệu RAG (tải lên, số hóa, chunking)
 chuyên môn    ├── Quản lý từ khóa Triage
 y khoa]       ├── Soạn FAQ / bài viết kiến thức (nháp)
               ├── Duyệt & xuất bản FAQ / bài viết kiến thức
               └── Quản lý chủ đề bài viết

[Cộng tác ────┬── Soạn FAQ / bài viết kiến thức (nháp)   ← dùng chung oval với Biên tập
 viên nội      └── Quản lý chủ đề bài viết                  ← dùng chung oval với Biên tập
 dung] (mới)     (KHÔNG nối: Quản lý tài liệu RAG, Quản lý từ khóa Triage,
                  Duyệt & xuất bản — xem SCR-AD-03 §3)

[Giám sát ────┬── Dashboard thống kê                     ← dùng chung oval với Admin
 vận hành]     └── Giám sát cảnh báo khẩn cấp (xác nhận escalate)  ← mới, dùng chung với Admin
 (mới)           (KHÔNG nối: Audit log, Quản lý từ khóa Triage — dễ nhầm vì tên
                  đều có "giám sát"/"Triage", xem SCR-AD-03 §3)

[Hệ thống] ───┬── Truy xuất + sinh câu trả lời RAG
              ├── Sàng lọc Triage (đối sánh từ khóa) + đánh giá mức độ nghiêm trọng
              └── Phân tích tương quan thực phẩm–triệu chứng
```

Nguyên tắc vẽ (giữ nhất quán, không nối Admin vào mọi oval): chỉ nối actor **chịu trách nhiệm
chính** của use case, đúng cách bản cũ đã làm (Admin không nối vào các oval của Biên tập dù vẫn có
quyền xem). Ngoại lệ: "Tạo tài khoản nhân sự CMS" là use case hoàn toàn mới, chỉ Admin — nối thẳng.
"Dashboard" và "Giám sát cảnh báo khẩn cấp" dùng chung 2 actor (Admin + Giám sát vận hành) vì cả
hai cùng là actor chính thật sự theo SCR-AD-03 §4, không phải Admin "mượn quyền".

**User stories (mẫu đại diện):**
- Là một bệnh nhân, tôi muốn hỏi AI về triệu chứng tiêu hóa, để nhận câu trả lời có trích dẫn
  nguồn thay vì tự tra cứu thông tin không kiểm chứng. (UC0028/029)
- Là một bệnh nhân, tôi muốn được cảnh báo ngay nếu mô tả có dấu hiệu cấp cứu, để không bỏ lỡ thời
  điểm cần đi khám gấp. (UC0034)
- Là một bệnh nhân, tôi muốn ghi lại bữa ăn và tình trạng Bristol hằng ngày, để hệ thống gợi ý món
  ăn nào có thể liên quan đến triệu chứng theo thời gian. (UC0013/022)
- Là một admin, tôi muốn chỉnh danh sách từ khóa cảnh báo Triage qua giao diện, để không phải sửa
  code và deploy lại mỗi khi phát hiện từ khóa còn thiếu. (UC0054)
- Là một biên tập chuyên môn y khoa (vai trò CMS), tôi muốn tải tài liệu y khoa mới lên kho tri
  thức, để câu trả lời AI cập nhật kiến thức mới mà không cần huấn luyện lại mô hình. (UC0048)

**Bảng 8.** FR/NFR đại diện, có mã UC và tiêu chí chấp nhận.

| Mã | Loại | Actor | Yêu cầu | Tiêu chí chấp nhận |
|---|---|---|---|---|
| UC0002 | FR | Bệnh nhân | Đăng nhập | Đúng thông tin → nhận JWT; bệnh nhân/admin dùng cơ chế xác thực tách riêng (JWT/session) |
| UC0006 | FR | Hệ thống | Phân quyền RBAC | Middleware chặn route nếu vai trò không đủ quyền; 4 vai trò nội bộ (Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành) |
| UC0013 | FR | Bệnh nhân | Ghi nhận Bristol | Chọn đúng 1/7 loại theo Bristol Stool Chart, lưu kèm thời điểm |
| UC0022 | FR | Hệ thống | Tương quan thực phẩm–triệu chứng | ≥15 bữa ăn/90 ngày → tối đa 10 món có ≥3 lần xuất hiện, sắp theo % giảm dần |
| UC0023 | FR | Bệnh nhân | Hồ sơ nhạy cảm thực phẩm | Hiển thị baseline % + phân loại màu theo mức chênh so với baseline |
| UC0028 | FR | Hệ thống | Truy xuất ngữ cảnh RAG | Top-k đoạn có cosine similarity vượt ngưỡng cấu hình; dưới ngưỡng → prompt không ngữ cảnh |
| UC0029 | FR | Hệ thống | Sinh câu trả lời có trích dẫn | Chỉ dựa ngữ cảnh truy xuất, tối đa 2 nguồn hiển thị, liều thuốc cụ thể bị lọc |
| UC0031-033 | FR | Hệ thống | Số hóa, chunking, embedding tài liệu | Chunk tối đa 2000 ký tự/overlap 200, tôn trọng ranh giới mục; mỗi chunk lưu kèm metadata nguồn |
| UC0034 | FR | Hệ thống | Sàng lọc Triage tầng 1 | Khớp từ khóa dấu hiệu cấp cứu (đã chuẩn hoá bỏ dấu) → gắn cờ cảnh báo, ưu tiên recall |
| UC0039 | FR | Bệnh nhân | Sàng lọc triệu chứng có cấu trúc | Form hỏi-đáp theo bước, độc lập với chat tự do |
| UC0040 | FR | Hệ thống | Đánh giá mức độ nghiêm trọng | Phân loại MILD/MODERATE/SEVERE theo dấu hiệu, ảnh hưởng hoạt động, diễn tiến, thời gian |
| UC0042 | FR | Bệnh nhân | Đánh giá phản hồi AI | Gắn hữu ích/không hữu ích sau mỗi câu trả lời, lưu làm dữ liệu đánh giá |
| UC0047 | FR | Admin | Gán vai trò RBAC | Gán 1 trong 4 vai trò nội bộ (Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành) cho 1 tài khoản CMS |
| UC0077 | FR | Admin | Tạo tài khoản nhân sự CMS | Tạo tài khoản mới + gán đúng 1 trong 4 vai trò ngay từ đầu, không qua Flyway; mật khẩu tạm hiện đúng 1 lần; ghi Audit Log (`CMS_ACCOUNT_CREATE`) |
| UC0048 | FR | Biên tập chuyên môn y khoa | Tải lên tài liệu y khoa | Tài liệu mới được số hóa, chunk, sinh embedding, truy xuất được ngay sau khi tải lên |
| UC0052 | FR | Admin | Audit log | Thao tác nhạy cảm được ghi lại, không sửa/xoá được qua giao diện thường |
| UC0054 | FR | Biên tập chuyên môn y khoa | Quản lý từ khóa Triage | Thêm/sửa/xoá từ khóa qua CMS, có hiệu lực ngay không cần deploy lại |
| UC0059 | FR | Admin | Cấu hình tham số RAG/Triage | Đổi top-k/ngưỡng similarity qua UI, áp dụng ngay cho truy vấn tiếp theo |
| UC0068 | FR | Bệnh nhân | Nhắc uống thuốc in-app | Gửi nhắc qua in-app/WebSocket đúng khung giờ khi đang online |
| NFR-01 | NFR | — | Hiệu năng | Trả lời qua streaming SSE giảm cảm giác chờ; TTFT/latency tổng chưa đo đầy đủ (xem TODO) |
| NFR-02 | NFR | — | Bảo mật | RBAC (4 vai trò: Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành) + audit log thao tác nhạy cảm; tách CSDL bệnh nhân/CMS vật lý; route CMS không session → 403 (đã xác nhận qua curl) |
| NFR-03 | NFR | — | Độ tin cậy | Flyway migration kiểm soát phiên bản (60 Postgres + 16 MySQL); soft-delete cho phép khôi phục nhật ký ghi nhầm (UC0020) |
| NFR-04 | NFR | — | Usability | Chuẩn hoá UI CMS bằng AntD; chưa có kiểm thử usability với người dùng ngoài nhóm — hạn chế cần nêu khi bảo vệ |
| NFR-05 | NFR | — | Khả năng kiểm thử/tái lập | Bộ đánh giá RAG/Triage chạy trực tiếp qua API CMS, số liệu sinh từ dữ liệu thật, không hardcode |

*Bảng 8 chỉ liệt kê đại diện 18 FR phủ đủ mọi phân hệ chính. Danh sách đầy đủ 76 UC (mã, actor,
mô tả, độ khó, người phụ trách) xem `docs/masterplan-phan-he-usecase.xlsx` — có thể đính kèm làm
Phụ lục A nếu hội đồng yêu cầu bản đầy đủ.*

---

### 3.2 Dữ liệu/benchmark và quy trình tiền xử lý

> Checklist mẫu (ghi chú nội bộ, không copy): nguồn/license/kích thước/phân bố/split, tránh
> leakage, data quality issues; kèm dataset card, biểu đồ EDA, train/val/test protocol.

**Lưu ý khung quy chiếu** (nội dung thật, đưa vào báo cáo — trả lời thẳng câu hỏi "train/val/test
protocol" của mẫu bằng cách giải thích vì sao không áp dụng kiểu chia đó): GastroAI không huấn
luyện mô hình ML nào — dùng LLM có sẵn qua API (Gemini) cho RAG, và luật tay (keyword matching)
cho Triage. Vì vậy không có "tập huấn luyện" theo nghĩa cổ điển; "dữ liệu" ở đây tách thành 3 nhóm
độc lập, không trộn lẫn vai trò.

**a) Kho tri thức RAG (dữ liệu runtime — hệ thống dùng để trả lời)**

- **Nguồn:** 25 tài liệu, mỗi tài liệu 1 chủ đề bệnh tiêu hóa (GERD, viêm loét dạ dày-tá tràng do
  HP, IBS, khó tiêu chức năng, táo bón, tiêu chảy, không dung nạp lactose, Celiac, sỏi mật, viêm
  tụy, xuất huyết tiêu hóa, viêm dạ dày ruột virus, mất nước, an toàn thực phẩm, dấu hiệu báo động,
  viêm gan A/B, xơ gan, gan nhiễm mỡ, viêm ruột thừa, trĩ, Crohn, viêm loét đại tràng, ung thư dạ
  dày, ung thư đại trực tràng).
- **"License":** nội dung do nhóm (vai trò Biên tập chuyên môn y khoa) biên soạn lại — không copy
  nguyên văn — dựa trên đối chiếu nhiều nguồn y khoa công khai uy tín: Mayo Clinic, NHS UK,
  Cleveland Clinic, CDC, American Cancer Society, MSD Manuals, Hello Bacsi; mỗi tài liệu có trích
  dẫn URL nguồn ở cuối từng mục. Đây chính là lý do RBAC gọi vai trò này là "Biên tập chuyên môn y
  khoa" chứ không phải "Bác sĩ duyệt" (xem Chương I §1.1.5, Chương III §3.1).
- **Kích thước:** 25 documents → 238 chunks → 238 embedding vectors, sinh qua Gemini embedding API
  thật (không phải vector giả lập — xem comment trong migration gốc).
- **Phân bố:** khá đều — 19/25 tài liệu có đúng 9 chunk, dao động 9–13 chunk/tài liệu (tài liệu
  nhiều mục nhất: "09-soi-mat.pdf", 13 chunk). Đồng đều vì `ChunkingService` chia theo ranh giới
  mục/đề mục, và các tài liệu được viết theo cùng 1 khung cấu trúc.

**b) Bộ benchmark đánh giá RAG — RQ-01 đến RQ-22 (dữ liệu đo lường, không dùng để trả lời)**

- 22 câu hỏi tự nhiên do nhóm tự viết (không copy nguyên văn câu trong tài liệu), mỗi câu gắn
  `expectedDocKeyword` (khớp tên tài liệu) + danh sách `topicalKeywords` để chấm độ liên quan.
- Phủ 23/25 tài liệu (1 câu — RQ-21 — so sánh đồng thời Crohn và viêm loét đại tràng, phủ 2 tài
  liệu cùng lúc); **còn thiếu benchmark cho 2/25 chủ đề (viêm gan A, ung thư dạ dày)** — khoảng
  trống thật, chưa bổ sung.
- **Tránh leakage:** câu hỏi diễn đạt lại bằng ngôn ngữ tự nhiên của người hỏi, không trích nguyên
  câu từ tài liệu — giảm rủi ro "truy xuất đúng chỉ vì trùng từ y hệt". Tuy nhiên đây là benchmark
  tự xây dựng nội bộ (nhóm viết cả câu hỏi lẫn tài liệu), không phải benchmark độc lập bên thứ ba —
  không loại trừ hoàn toàn thiên vị, cần nêu rõ khi bảo vệ.

**c) Golden dataset đánh giá Triage — 46 ca (dữ liệu đo lường, không dùng để trả lời)**

- **24 ca dương tính thật** (expectedEmergency = true): Xuất huyết tiêu hóa (6), Đau bụng cấp tính
  (5), Tắc ruột (3), Nhiễm trùng nặng (3), Hô hấp/tim mạch — gồm cả sốc mất máu (3), nhóm cấp cứu
  khác — tiết niệu/đường mật/mất nước/nội tiết (4).
- **22 ca âm tính thật:** 12 ca "thông thường" (câu hỏi sinh hoạt/dinh dưỡng bình thường) + **10 ca
  "distractor"** cố tình chứa từ dễ gây nhầm với từ khóa cấp cứu nhưng ngữ cảnh không nguy hiểm —
  ví dụ "xét nghiệm máu", "hiến máu nhân đạo", "uống viên sắt làm sẫm phân", "nước ép củ dền làm đỏ
  phân", "tiêm phòng viêm gan B ở đâu". Thiết kế để kiểm tra Triage không báo động giả chỉ vì khớp
  từ đơn lẻ.
- Mỗi ca gán nhãn thủ công bởi nhóm, kèm ghi chú lý do — **không có bước gán nhãn độc lập bởi
  người thứ hai** (inter-rater agreement) — hạn chế thật, cần nêu khi bảo vệ.
- **"Split":** không chia train/val/test vì không huấn luyện gì — toàn bộ 46 ca là test set cố
  định, chạy lại được qua CMS (`POST /api/v1/cms/evaluation/triage/run`).

**Hình 3. Phân bố 46 ca trong golden dataset đánh giá Triage** — *(chèn ảnh `triage_distribution.png`)*

**d) Dữ liệu vận hành (nhật ký ăn uống, Bristol) — dữ liệu demo, KHÔNG phải dataset nghiên cứu**

Dữ liệu cho 1 bệnh nhân mẫu (patient_id=6) dùng minh họa tính năng tương quan thực phẩm–triệu
chứng trong lúc phát triển: baseline tỷ lệ triệu chứng 27,82% trên 90 ngày, phân loại 3 màu
(đỏ/vàng/xám) theo mức chênh so với baseline. Đây là dữ liệu demo tự tạo để kiểm tra tính năng
chạy đúng — không phải dữ liệu lâm sàng thật, không dùng để huấn luyện hay làm căn cứ đánh giá
định lượng chính thức.

**Data quality issues đã phát hiện thật trong quá trình phát triển** (bằng chứng có quy trình kiểm
tra dữ liệu, không chỉ seed rồi bỏ đó):
- **Đã phát hiện & sửa:** dữ liệu demo nhật ký ăn uống cũ còn sót text rác "- mẫu ngày N" từ
  template, khiến thuật toán tương quan tính nhầm từ "ngày"/"mẫu" là từ khóa món ăn — đã sửa bằng
  UPDATE có kiểm soát, xác minh qua API thật trước/sau khi sửa.
- **Đã phát hiện, chưa sửa:** lỗi trong `VietnameseText.tokenize()` — từ "cua" (cua biển, sau khi
  bỏ dấu) trùng với "của" (hư từ bị lọc bỏ), khiến "cua" bị loại nhầm khỏi phân tích tương quan dù
  là từ khóa món ăn hợp lệ. Ghi nhận là hạn chế kỹ thuật còn tồn tại.

**Bảng 9.** Dataset card tóm tắt.

| Tập dữ liệu | Nguồn | Quy mô | Vai trò |
|---|---|---|---|
| Kho tri thức RAG | Nhóm biên soạn, đối chiếu Mayo Clinic/NHS/CDC/Cleveland Clinic/Hello Bacsi... | 25 tài liệu, 238 chunk, 238 embedding | Runtime: ngữ cảnh trả lời RAG |
| Benchmark RAG (RQ-01→22) | Nhóm tự viết | 22 câu hỏi, phủ 23/25 tài liệu | Đo Hit Rate/MRR/Precision/Recall |
| Golden dataset Triage | Nhóm tự viết + gán nhãn thủ công | 46 ca (24 dương tính + 22 âm tính, gồm 10 distractor) | Đo Sensitivity/Specificity/F1/F2 |
| Dữ liệu demo bệnh nhân | Seed thử nghiệm, 1 bệnh nhân mẫu | 90 ngày nhật ký (patient_id=6) | Minh họa tính năng — KHÔNG dùng đánh giá định lượng |

---

### 3.3 Phương pháp/thuật toán đề xuất

> Checklist mẫu (ghi chú nội bộ, không copy): pipeline từng bước, baseline vs proposed khác nhau ở
> đâu, hyperparameter/threshold nào cần tune; kèm pseudocode/flowchart + bảng cấu hình thí nghiệm.
> 3 thuật toán dưới đây lấy đúng từ code thật đã đọc (`RagQueryService.java`, `TriageService.java`,
> `FoodSymptomCorrelationService.java`, `ChunkingService.java`, `AppRuntimeConfigService.java`,
> migration `V36__app_runtime_config.sql`) — số mặc định xác minh lại ngày 20/11/2026, không bịa.

**A. Pipeline RAG (Retrieval-Augmented Generation)**

*Baseline vs proposed* (đã nêu ở Chương I §1.2.4): baseline = tìm kiếm từ khóa thuần túy
(lexical/SQL LIKE trên nội dung tài liệu); proposed = truy xuất bằng vector embedding (pgvector
cosine similarity).

*Pipeline từng bước:*
1. Nhận câu hỏi người dùng.
2. Sinh embedding câu hỏi qua Gemini embedding API.
3. Tìm top-k chunk gần nhất bằng pgvector (`EmbeddingStore.findTopK`, k = `ragTopK`).
4. Lọc theo ngưỡng `ragSimilarityThreshold` (nếu `null` thì không lọc, giữ nguyên top-k).
5. Nếu còn chunk sau lọc: redact liều lượng thuốc cụ thể (`DosageRedactor`) → ghép thành
   `contextText` → chèn vào `SYSTEM_PROMPT_PREFIX` (ép LLM chỉ trả lời dựa trên ngữ cảnh, nói rõ
   nếu thiếu thông tin, không echo lại liều thuốc verbatim).
6. Nếu không còn chunk nào: dùng `SYSTEM_PROMPT_NO_CONTEXT`.
7. Gọi LLM sinh câu trả lời từ prompt đã build.
8. Build danh sách nguồn hiển thị (tối đa 2), Gemini viết lại snippet dễ đọc — lỗi/dị dạng thì
   fallback về text gốc đã redact.
9. (Phụ, không ảnh hưởng câu trả lời chính) sinh câu hỏi liên quan qua 1 lời gọi LLM riêng, lỗi thì
   bỏ qua âm thầm, không chặn luồng chính.

```
function answerQuestion(question):
    qVector = embed(question)
    candidates = pgvector.findTopK(qVector, ragTopK)
    relevant = filter(candidates, c -> ragSimilarityThreshold == null
                                        or c.similarity >= ragSimilarityThreshold)
    if relevant is empty:
        prompt = SYSTEM_PROMPT_NO_CONTEXT
    else:
        context = redactDosage(join(relevant))
        prompt = SYSTEM_PROMPT_PREFIX + context
    answer = LLM.generate(prompt, question)
    sources = buildSources(relevant, max = 2)
    return answer, sources
```

**B. Pipeline Triage tầng 1 (sàng lọc an toàn rule-based)**

*Baseline vs proposed:* baseline = để LLM tự nhận định mức độ khẩn cấp qua prompt, không có lớp
riêng (rủi ro: LLM có thể diễn đạt mập mờ hoặc bỏ sót); proposed = lớp tách biệt, chạy độc lập,
đối sánh từ khóa, thiết kế ưu tiên recall cao hơn precision (chấp nhận báo động giả để không bỏ
sót ca nguy hiểm — xem Chương II §2.1 mục 5).

*Pipeline từng bước:*
1. Nhận văn bản đầu vào (câu hỏi chat tự do — `check()` — hoặc câu trả lời form sàng lọc có cấu
   trúc — `assess()`, UC0039/UC0040).
2. Chuẩn hóa: lowercase, thay đ→d, NFD-normalize, bỏ dấu kết hợp (`normalize()`).
3. Với chat tự do (`check()`): với mỗi nhóm từ khóa lấy **động từ CMS**
   (`keywordSource.keywordsByGroup()` — UC0054, không hardcode), kiểm tra khớp substring bất kỳ từ
   khóa nào trong nhóm.
4. Nếu khớp nhóm nào thuộc nhóm mã cấp cứu (`EMERGENCY_GROUP_CODES` — 11/13 nhóm mã riêng biệt,
   suy ra tự động từ `EMERGENCY_SIGNS` tĩnh dùng cho `assess()`; 2 nhóm còn lại — sụt cân không rõ
   nguyên nhân, khó/đau khi nuốt — là mức ALARM nhẹ hơn, không set cờ khẩn cấp) → gắn cờ cảnh báo
   khẩn cấp ngay.
5. Với form có cấu trúc (`assess()`, UC0040): kết hợp thêm mức ảnh hưởng hoạt động, diễn tiến,
   thời gian kéo dài → phân loại MILD/MODERATE/SEVERE theo logic leo thang mức độ.

```
function checkTriage(text):
    normalized = stripDiacritics(lowercase(text).replace('đ', 'd'))
    matchedGroups = []
    for group, keywords in keywordSource.keywordsByGroup():   // động, chỉnh qua CMS (UC0054)
        if any(kw in normalized for kw in keywords):
            matchedGroups.add(group)
    isEmergency = any(g in EMERGENCY_GROUP_CODES for g in matchedGroups)
    return isEmergency, matchedGroups
```

**C. Thuật toán phân tích tương quan thực phẩm–triệu chứng**

*Baseline vs proposed* (đã nêu ở Chương I §1.2.4): baseline = tỷ lệ triệu chứng chung trên toàn bộ
dữ liệu bệnh nhân; proposed = tỷ lệ triệu chứng riêng theo từng món ăn, so với baseline để xác
định "nổi bật" hay không (quy tắc màu đỏ/vàng/xám ở tầng hiển thị — `foodSensitivityColor()`).

*Pipeline từng bước:*
1. Lấy toàn bộ bữa ăn trong khoảng thời gian (vd 90 ngày). Nếu số bữa < `MIN_MEALS_FOR_ANALYSIS`
   → không đủ dữ liệu, dừng.
2. Lấy thêm Bristol log kéo dài quá mốc kết thúc khoảng `SYMPTOM_WINDOW` để không bỏ sót triệu
   chứng xảy ra sau bữa ăn cuối trong khoảng xét.
3. Với mỗi bữa ăn: đánh dấu "có triệu chứng" nếu tự báo cáo HOẶC có Bristol bất thường
   (`ABNORMAL_BRISTOL_TYPES`) trong vòng `SYMPTOM_WINDOW` SAU bữa ăn.
4. Tokenize mô tả món ăn (bỏ dấu, bỏ từ ngắn/stopword/số).
5. Gộp theo từng token: đếm số lần xuất hiện, số lần kèm triệu chứng → tỷ lệ %.
6. Lọc token có số lần xuất hiện ≥ `MIN_OCCURRENCES`.
7. Sắp xếp theo tỷ lệ % giảm dần (rồi số lần xuất hiện giảm dần), lấy `TOP_N`.

```
function analyzeCorrelation(patientId, range):
    meals = getMeals(patientId, range)
    if meals.size < MIN_MEALS_FOR_ANALYSIS: return insufficientData
    abnormalBristol = getBristol(patientId, range.end + SYMPTOM_WINDOW)
        .filter(b -> b.type in ABNORMAL_BRISTOL_TYPES)
    for meal in meals:
        meal.symptomatic = meal.selfReported
            or existsAbnormalWithin(meal.time, meal.time + SYMPTOM_WINDOW, abnormalBristol)
    tokenStats = {}
    for meal in meals:
        for token in tokenize(meal.description):
            tokenStats[token].count += 1
            if meal.symptomatic: tokenStats[token].symptomCount += 1
    results = [t for t in tokenStats if t.count >= MIN_OCCURRENCES]
    results.sortByDesc(t -> t.symptomRate, thenBy = t -> t.count)
    return results[:TOP_N]
```

**Bảng 10.** Cấu hình thí nghiệm / hyperparameter — giá trị thật xác minh trong code.

| Thành phần | Tham số | Giá trị | Chỉnh qua CMS? | Ghi chú |
|---|---|---|---|---|
| RAG | ragTopK | 5 (mặc định), hợp lệ 1–20 | Có (UC0059) | Dùng trực tiếp làm `LIMIT` trong SQL |
| RAG | ragSimilarityThreshold | NULL (mặc định = không lọc), hợp lệ 0.0–2.0 | Có (UC0059) | NULL = tắt lọc theo ngưỡng |
| Chunking | MAX_CHUNK_SIZE | 2000 ký tự | Không | Hardcode `ChunkingService` |
| Chunking | OVERLAP_SIZE | 200 ký tự | Không | Hardcode |
| Chunking | MIN_STANDALONE_CHUNK_SIZE | 60 ký tự | Không | Gộp chunk tiêu đề quá ngắn vào chunk lân cận |
| Triage | Danh sách từ khóa (13 nhóm mã, 11 nhóm emergency) | — | Có (UC0054) | Không có ngưỡng số, là danh sách khớp từ |
| Tương quan thực phẩm | SYMPTOM_WINDOW | 48 giờ | Không | Hardcode `FoodSymptomCorrelationService` |
| Tương quan thực phẩm | ABNORMAL_BRISTOL_TYPES | {1, 2, 6, 7} | Không | Hardcode |
| Tương quan thực phẩm | MIN_MEALS_FOR_ANALYSIS | 15 bữa | Không | Hardcode |
| Tương quan thực phẩm | MIN_OCCURRENCES | 3 lần | Không | Hardcode |
| Tương quan thực phẩm | TOP_N | 10 kết quả | Không | Hardcode |

*Hạn chế thật đáng nêu:* chỉ 2 nhóm tham số (RAG, từ khóa Triage) chỉnh được qua CMS mà không cần
sửa code/deploy lại; các ngưỡng của Chunking và phân tích tương quan thực phẩm vẫn hardcode — nếu
cần tinh chỉnh (vd đổi `SYMPTOM_WINDOW` từ 48h sang giá trị khác) phải sửa code.

---

### 3.4 Thiết kế thí nghiệm và tiêu chí đánh giá

> Checklist mẫu (ghi chú nội bộ, không copy): research questions/ablation, định nghĩa metric, thời
> điểm đóng băng test set; kèm evaluation protocol chạy lại được. Ngày đóng băng xác minh qua
> `git log -S "<chuỗi cuối trong danh sách>" -- <file>` (tìm commit cuối cùng SỬA NỘI DUNG danh
> sách, không phải lần cuối file được chạm vào vì lý do khác) — không đoán.

**Câu hỏi nghiên cứu/kỹ thuật** (đã nêu ở Chương I §1.2.3) — đối chiếu với kết quả đo được:

| # | Câu hỏi | Metric trả lời | Trạng thái |
|---|---|---|---|
| 1 | Truy xuất vector có vượt trội tìm kiếm từ khóa thuần túy không? | Hit Rate@1/3/K, MRR | **Một phần** — đã đo chế độ vector (95,5%/0,955), **chưa chạy baseline lexical (LEXICAL_FALLBACK) cùng bộ câu hỏi để so sánh trực tiếp bằng số** — hiện mới là so sánh khái niệm |
| 2 | Triage rule-based có đạt độ nhạy đủ cao, báo động giả chấp nhận được không? | Sensitivity, False Alarm Rate | **Đã trả lời** — 100% / 0% trên golden dataset 46 ca |
| 3 | Tương quan thực phẩm–triệu chứng có ổn định khi ít dữ liệu không? | Ngưỡng `MIN_MEALS_FOR_ANALYSIS` | **Chưa trả lời** — mới có 1 bệnh nhân demo, chưa đủ dữ liệu nhiều người dùng thật để đánh giá độ ổn định thống kê |
| 4 | Đa CSDL có ảnh hưởng đáng kể độ trễ chat không? | TTFT | **Chưa trả lời** — chưa đo TTFT thật (xem TODO) |

**Ablation:** `RagEvaluationService.evaluate(topK)` hỗ trợ kỹ thuật để chạy nhiều giá trị `topK`
khác nhau và so sánh (API: `POST /api/v1/cms/evaluation/rag/run?topK=...`) — nhưng **thực tế mới
chạy và ghi nhận 1 lần với topK mặc định = 5** (19/11/2026); chưa có sweep nhiều topK để biết Hit
Rate/MRR đổi thế nào theo topK. Đây là ablation khả thi về mặt hạ tầng nhưng chưa thực hiện — nên
làm trước khi bảo vệ nếu còn thời gian.

**Định nghĩa metric:** dùng đúng công thức đã nêu ở Chương II §2.1 mục 6-7 (Hit Rate@K, MRR,
Precision@K, Recall@K cho RAG; Sensitivity, Specificity, FPR, F1/F2 cho Triage) — không định nghĩa
lại ở đây để tránh trùng lặp.

**Test set đóng băng khi nào:** cả bộ benchmark RAG (RQ-01…RQ-22) và golden dataset Triage (46 ca)
được tạo trong cùng 1 commit — `68c29c3` (30/10/2026, "feat: implement UC0037... UC0038...") — và
**chưa bị sửa nội dung lần nào kể từ đó** (xác minh bằng `git log -S` trên chuỗi cuối mỗi danh
sách — `"RQ-22"` và `"TC-DIST-10"` — chỉ trả về đúng 1 commit). Các commit sau đó (31/10, 01/11,
05/11/2026) có sửa file nhưng chỉ sửa **logic chạy đánh giá** (xử lý timeout, đánh dấu dữ liệu giả
lập, tránh lỗi constructor) — không đụng vào danh sách câu hỏi/ca kiểm thử. Vậy test set đã đóng
băng ổn định 21 ngày tính đến lúc viết báo cáo (20/11/2026).

**Evaluation protocol — chạy lại được:**
1. Đăng nhập CMS với tài khoản vai trò Admin hoặc Biên tập chuyên môn y khoa.
2. Vào trang Đánh giá (Evaluation) trong CMS.
3. Bấm chạy — gọi `POST /api/v1/cms/evaluation/rag/run?topK=5` hoặc
   `POST /api/v1/cms/evaluation/triage/run`, thực thi **trực tiếp trên dữ liệu/API thật** tại thời
   điểm bấm, không phải số cố định hardcode sẵn.
4. Kết quả trả về kèm `evaluatedAt` (timestamp), cache tạm (`cachedReport`) tới lần chạy kế tiếp.
5. Vì chạy qua Gemini API thật, kết quả **có thể đổi nhẹ giữa các lần chạy** (LLM không hoàn toàn
   deterministic) hoặc đổi nhiều hơn nếu kho tài liệu RAG/cấu hình `topK`/`threshold` thay đổi —
   mọi số liệu trong báo cáo đều ghi kèm ngày đo (19/11/2026) để minh bạch, và cần đo lại nếu nộp
   muộn hơn nhiều so với ngày này.

---

## CHƯƠNG IV. PHÂN TÍCH THIẾT KẾ HỆ THỐNG

### 4.1 Kiến trúc tổng thể

> Checklist mẫu (ghi chú nội bộ, không copy): component/service là gì, luồng dữ liệu và trust
> boundary; kèm architecture diagram + deployment diagram. Grounded từ cấu trúc thư mục thật
> (`be/src/main/java/.../{domain,application,infrastructure,api,config}`,
> `fe/src/{components,pages,routes,api}`), `SecurityConfig.java` (CORS/JWT/session),
> `WebSocketConfig.java`, `ChatController.java` (SSE), `PostgresConfig.java`/`MysqlConfig.java`
> (2 datasource riêng biệt).

**Các component/service:**

1. **Frontend — React SPA** (TypeScript, Vite, Ant Design): 1 ứng dụng duy nhất, 2 cây route tách
   biệt — `/*` cho bệnh nhân (`AppShell.tsx`), `/cms/*` cho nội bộ CMS (`CmsAppShell.tsx`) — không
   phải 2 ứng dụng triển khai riêng.
2. **Backend — Spring Boot**: 1 process, kiến trúc phân lớp `domain/application/infrastructure/
   api/config`. Lộ ra 3 kiểu giao tiếp: REST API (đa số nghiệp vụ), SSE (stream câu trả lời AI
   theo token qua `ChatController`), WebSocket (đẩy cảnh báo Triage realtime + thông báo in-app
   qua `WebSocketConfig`).
3. **PostgreSQL + pgvector**: dữ liệu bệnh nhân (hồ sơ, nhật ký ăn uống, Bristol) + kho tri thức
   RAG (document/chunk/embedding) — nhóm dữ liệu nhạy cảm nhất hệ thống.
4. **MySQL**: dữ liệu CMS/admin (tài khoản nội bộ, RBAC, audit log, FAQ/bài viết, cấu hình runtime)
   — tách vật lý khỏi PostgreSQL để cô lập rủi ro (đã nêu từ Chương I §1.1.4).
5. **Gemini API (Google)**: dịch vụ ngoài — sinh embedding + sinh câu trả lời — **thành phần duy
   nhất nằm ngoài trust boundary nhóm kiểm soát**.

**Hình 4. Kiến trúc tổng thể** — *(chèn ảnh `architecture_diagram_vp.png`)*

**Trust boundary (4 ranh giới đáng chú ý):**

| # | Ranh giới | Auth | Mức rủi ro | Ghi chú |
|---|---|---|---|---|
| 1 | Trình duyệt bệnh nhân ↔ Backend | JWT | Trung bình — public internet | Route `/*` |
| 2 | Trình duyệt CMS ↔ Backend | Session | Trung bình — nội bộ, nhưng tài khoản có quyền cao | Route `/cms/*`, tách UI khỏi phân hệ bệnh nhân để giảm thao tác nhầm |
| 3 | Backend ↔ Gemini API | API key | **Cao nhất** — dữ liệu RA KHỎI hệ thống | Câu hỏi/văn bản bệnh nhân rời khỏi hạ tầng nhóm kiểm soát; lý do `DosageRedactor` lọc liều thuốc trước khi đưa ngữ cảnh qua ranh giới này (Chương III §3.3) và lý do cân nhắc lock-in ở Chương II §2.3 Bảng 7 |
| 4 | Backend ↔ PostgreSQL vs Backend ↔ MySQL | JDBC nội bộ | Thấp — không qua internet | Tách vật lý 2 CSDL để cô lập dữ liệu nhạy cảm bệnh nhân khỏi dữ liệu quản trị |

**Luồng dữ liệu chính — ví dụ 1 câu hỏi chat RAG:**
1. Bệnh nhân gõ câu hỏi → trình duyệt gửi qua kết nối SSE đã mở tới Backend (ranh giới 1).
2. Backend gọi Gemini API sinh embedding câu hỏi (ranh giới 3, lượt đi).
3. Backend truy vấn PostgreSQL/pgvector tìm chunk liên quan (nội bộ, ranh giới 4).
4. Backend build prompt (đã redact liều thuốc), gọi Gemini API sinh câu trả lời (ranh giới 3,
   lượt đi thứ 2).
5. Backend stream câu trả lời về trình duyệt bệnh nhân qua SSE (ranh giới 1, lượt về).
6. Song song: nếu Triage tầng 1 phát hiện cảnh báo khẩn, Backend đẩy alert qua WebSocket tới
   trình duyệt CMS đang mở (ranh giới 2, lượt về) — không phụ thuộc bệnh nhân có đang chat hay
   không nếu CMS đã mở kênh WebSocket từ trước.

**Deployment (không containerize — Docker đã loại khỏi phạm vi đồ án, xem `docs/de-cuong-do-an.md`):**

| Thành phần | Cách chạy | Ghi chú |
|---|---|---|
| Backend | 1 JVM process (`./mvnw spring-boot:run` hoặc chạy jar) | Đọc cấu hình từ `application-local.yml` |
| Frontend | 1 Vite dev server (`npm run dev`) hoặc build tĩnh | Không phải 2 app riêng (xem component 1) |
| PostgreSQL | Instance local trên máy dev (cổng tùy máy, vd 5433 nếu 5432 đã bị chiếm) | 60 migration Flyway |
| MySQL | Instance local trên máy dev, cổng mặc định | 16 migration Flyway |

**Hình 5. Sơ đồ triển khai (Deployment)** — *(chèn ảnh `deployment_diagram_vp.png`)*

*Hạn chế thật:* mỗi thành viên chạy trên máy riêng, chưa có môi trường triển khai chung
(staging/cloud) hay container hóa — nên "deployment diagram" ở đây đơn giản hơn nhiều so với một
hệ thống production thật; nêu rõ khi bảo vệ thay vì ngụy trang thành kiến trúc production.

---

## Việc còn thiếu / cần làm trước khi nộp

- [ ] Vẽ Hình 1 thành ảnh thật (hiện mới có bản mô tả khối).
- [x] Hình 2 (sơ đồ Use case) đã vẽ xong dạng ảnh (SVG tự dựng theo phong cách Visual Paradigm,
      không phải vẽ bằng tay trong Visual Paradigm thật — chấp nhận được vì mẫu không bắt buộc tool).
- [ ] Bổ sung test tự động cho frontend (hiện 0 file) — SW-03 đang trống.
- [ ] Đo thật TTFT (Thời gian phản hồi token đầu tiên) — hiện chưa có số đo.
- [ ] Nếu chạy lại benchmark RAG/Triage, cập nhật lại số liệu trong Bảng 3 (có thể đổi so với
      19/11/2026).
- [ ] Soát lại toàn bộ trước khi nộp: không thêm tên thương hiệu đối thủ cụ thể chưa ai dùng thử,
      không đặt ngưỡng bằng đúng số đã đo được (dễ bị coi là nguỵ biện).
- [ ] Chương II (2.1/2.2): 7 trích dẫn đã kiểm tra tên tác giả/năm/venue/số liệu chính qua
      WebSearch ngày 20/11/2026 — nhưng DOI/số trang chính xác của Lewis&Heaton 1997 (Bristol) và
      Sandhu 1996 (RBAC) chưa được WebSearch xác minh lại (dùng từ kiến thức sẵn có), nên kiểm tra
      lại trước khi đưa vào danh mục tài liệu tham khảo chính thức.
- [x] 2.3 "Công nghệ và lý do lựa chọn" đã viết xong (Bảng 7).
- [ ] **Drift "Bác sĩ" phát hiện 20/11/2026, vẫn CHƯA xử lý**: tài liệu nghiệp vụ
      `00-TongQuan.md` §0 (quyết định 17/11/2026) đã bỏ vai trò "Bác sĩ" như tài khoản CMS riêng —
      nhưng `CmsRole.java` (enum `DOCTOR`), `CmsRbacPage.tsx`/`cmsNav.ts` (nhãn "Bác sĩ") và
      `docs/masterplan-phan-he-usecase.xlsx` (dòng UC0045 vẫn "Admin / Bác sĩ") **vẫn chưa refactor**.
      Rủi ro demo: hội đồng mở CMS thật thấy dropdown còn "Bác sĩ". Cần: (a) đồng bộ code/xlsx trước
      khi nộp, hoặc (b) nói rõ với hội đồng đây là gap đã biết — câu tự thừa nhận này PHẢI có mặt ở
      bản nộp thật (docx/Google Docs), không chỉ nằm trong file nháp nội bộ này.
- [ ] **Drift RBAC lần 2 (2→4 vai trò), phát sinh 23/11/2026, do 2 agent review (SA 10yr + GVHD 10yr)
      phát hiện, CHƯA xử lý hết**:
      1. Hình 2 (sơ đồ Use case) cần vẽ lại theo khối ASCII mới ở §3.1 — thêm actor Cộng tác viên nội
         dung + Giám sát vận hành, thêm oval Dashboard/Tạo tài khoản/Giám sát cảnh báo khẩn cấp, tách
         oval FAQ/bài viết thành "soạn" và "duyệt & xuất bản".
      2. `SCR-AD-03-PhanQuyenRBAC.md` §4 có lỗi đánh máy đã sửa (cột Admin ở dòng AD-10/AD-11 từng ghi
         nhầm "(soạn)" thay vì không giới hạn) — xác nhận lại đã đúng chưa nếu đổi ý sau này.
      3. `SCR-AD-05-DashboardBaoCao.md`, `SCR-AD-13-GiamSatCanhBao.md` đã thêm actor Giám sát vận hành
         vào bảng Thuộc tính — nhưng **code chưa theo**: `SecurityConfig.java`/`cmsNav.ts` chưa cấp
         quyền màn hình cho `CONTENT_AUTHOR`/`OPERATIONS` ở bất kỳ API nào ngoài RBAC; và
         `TriageAlertService.getDetail()` chỉ trả `EmergencyContact` cho viewer `"ADMIN"` — Giám sát
         vận hành vào màn SCR-AD-13 thật sẽ KHÔNG thấy số điện thoại cần gọi. Đây là việc còn phải
         làm trước khi tính năng 4-vai-trò dùng được thật, không chỉ là vấn đề tài liệu.
      4. `docs/masterplan-phan-he-usecase.xlsx`: dòng UC0045 còn "Admin / Bác sĩ" (trùng gap ở trên);
         UC0077 (Tạo tài khoản CMS, đã thêm vào bảng FR/NFR trên) chưa có trong xlsx chính thức.
      5. Màn Tạo tài khoản CMS (UC0077) **hiện có code thật đang chạy trên máy Đức nhưng CHƯA commit**
         — nếu commit sát ngày bảo vệ, nhớ đồng bộ lại cả xlsx + sơ đồ Use case cùng lúc, đừng để lệch
         tiếp lần nữa.
- [ ] Chương III §3.4 phát hiện 3 gap thật, nên làm trước khi bảo vệ nếu còn thời gian: (1) chưa
      chạy baseline lexical (LEXICAL_FALLBACK) để so sánh bằng số với vector retrieval cho RQ1;
      (2) chưa sweep nhiều giá trị `topK` dù hạ tầng đã hỗ trợ (ablation); (3) tương quan thực
      phẩm–triệu chứng mới test trên 1 bệnh nhân demo, chưa đủ dữ liệu nhiều người dùng thật để trả
      lời RQ3 (ổn định khi ít dữ liệu).
- [ ] Repo chưa có file `README.md` ở thư mục gốc — mục 5.1 (Chương V) yêu cầu bằng chứng README
      hướng dẫn người khác chạy hệ thống; cần viết trước khi nộp.
- [x] Hình 4 (kiến trúc tổng thể) + Hình 5 (deployment) đã vẽ xong dạng ảnh
      (`architecture_diagram.png`, `deployment_diagram.png`) — kiểm tra lại khớp code nếu kiến
      trúc đổi trước lúc nộp.
