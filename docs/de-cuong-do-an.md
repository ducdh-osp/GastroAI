# ĐỀ CƯƠNG ĐỒ ÁN TỐT NGHIỆP

**Tên đề tài:** Hệ thống Gastro AI — Chatbot AI hỗ trợ tư vấn và theo dõi sức khỏe hệ tiêu hóa ứng dụng công nghệ RAG

**Sinh viên thực hiện:** Đỗ  Hữu Đức
**Mã số sinh viên:** B22DCDT091
**Giảng viên hướng dẫn:** Nguyễn Duy Phương

---

## 1. Lý do chọn đề tài

Các bệnh lý về hệ tiêu hóa (viêm loét dạ dày – tá tràng, trào ngược dạ dày – thực quản, hội chứng ruột kích thích, rối loạn tiêu hóa...) là nhóm bệnh phổ biến tại Việt Nam, chịu ảnh hưởng lớn từ thói quen ăn uống và căng thẳng trong đời sống hiện đại. Phần lớn người bệnh có xu hướng tự tra cứu thông tin trên internet trước khi đến gặp bác sĩ, trong khi nguồn thông tin trôi nổi trên mạng thường thiếu kiểm chứng và không được cá nhân hóa theo tình trạng của từng người.

Các chatbot AI tổng quát hiện nay (ChatGPT, Gemini...) có khả năng trả lời trôi chảy nhưng tồn tại hai hạn chế lớn khi áp dụng vào lĩnh vực y tế:

1. **Không có cơ chế kiểm chứng nguồn thông tin y khoa**, dẫn đến rủi ro "ảo giác" (hallucination) — AI có thể đưa ra thông tin sai lệch nhưng trình bày với độ tự tin cao.
2. **Không có khả năng nhận diện và cảnh báo các dấu hiệu cấp cứu**, khiến người dùng có thể chủ quan bỏ lỡ thời điểm cần can thiệp y tế khẩn cấp (ví dụ: nôn ra máu, đi ngoài phân đen — dấu hiệu xuất huyết tiêu hóa).

Xuất phát từ thực tế trên, đề tài "Hệ thống Gastro AI" được đề xuất nhằm xây dựng một chatbot AI **chuyên biệt cho lĩnh vực tiêu hóa**, ứng dụng kiến trúc **RAG (Retrieval-Augmented Generation)** để đảm bảo câu trả lời được đối chiếu với tài liệu y khoa đã được kiểm định, đồng thời tích hợp một **lớp sàng lọc an toàn (Triage)** để phát hiện sớm các dấu hiệu nguy hiểm và cảnh báo người dùng kịp thời.

## 2. Mục tiêu đề tài

### 2.1. Mục tiêu tổng quát

Xây dựng hệ thống chatbot AI hỗ trợ tư vấn thông tin sức khỏe tiêu hóa dựa trên kiến trúc RAG, có cơ chế cảnh báo an toàn y khoa và tính năng theo dõi sức khỏe cá nhân, phục vụ đồng thời hai nhóm người dùng: bệnh nhân và quản trị viên/bác sĩ nội dung.

### 2.2. Mục tiêu cụ thể

- Xây dựng pipeline RAG hoàn chỉnh (số hóa tài liệu → chunking → embedding → lưu trữ vector → truy xuất) cho phép AI trả lời có trích dẫn nguồn tài liệu.
- Xây dựng lớp Triage nhận diện tối thiểu 5 nhóm dấu hiệu cấp cứu tiêu hóa phổ biến, ưu tiên độ nhạy (recall) cao nhằm hạn chế bỏ sót ca nguy hiểm.
- Xây dựng chức năng chat với phản hồi dạng streaming theo thời gian thực (SSE).
- Xây dựng phân hệ theo dõi sức khỏe cá nhân: hồ sơ bệnh lý, nhật ký ăn uống, nhật ký tình trạng tiêu hóa theo thang phân loại Bristol, nhắc nhở uống thuốc.
- Xây dựng phân hệ quản trị (CMS) với phân quyền RBAC tối thiểu 3 vai trò, quản lý tài liệu tri thức và nhật ký hoạt động hệ thống.
- Thiết kế kiến trúc đa cơ sở dữ liệu (PostgreSQL cho dữ liệu người dùng/vector, MySQL cho dữ liệu quản trị nội bộ) nhằm tách biệt luồng dữ liệu nhạy cảm.

## 3. Đối tượng và phạm vi thực hiện

**Đối tượng nghiên cứu:** công nghệ RAG trong lĩnh vực hỏi–đáp y tế chuyên ngành; kiến trúc hệ thống backend đa nguồn dữ liệu; cơ chế thông báo thời gian thực.

**Phạm vi thực hiện:**
- Chỉ tập trung vào nhóm bệnh lý tiêu hóa phổ biến (dạ dày – tá tràng, trào ngược, đại tràng/IBS, rối loạn tiêu hóa thông thường), không mở rộng sang toàn bộ lĩnh vực y khoa.
- Đối tượng người dùng là người trưởng thành; không xử lý các trường hợp nhi khoa hoặc sản khoa.
- Hệ thống đóng vai trò **hỗ trợ sàng lọc và cung cấp thông tin tham khảo**, không thay thế chẩn đoán và điều trị của bác sĩ.

**Phạm vi không thực hiện (out of scope):**
- Không tích hợp gọi video/thoại trực tiếp với bác sĩ.
- Không tích hợp thanh toán hoặc đặt lịch khám tại cơ sở y tế thực tế.
- Không xử lý đa ngôn ngữ (chỉ hỗ trợ tiếng Việt trong phạm vi đồ án).

## 4. Phương pháp thực hiện

- **Phương pháp nghiên cứu tài liệu:** tổng hợp tài liệu, phác đồ điều trị và cẩm nang y khoa chuẩn về bệnh lý tiêu hóa làm nguồn tri thức cho hệ thống RAG.
- **Phương pháp phát triển phần mềm:** phát triển theo mô hình lặp (Iterative), chia nhỏ theo từng phân hệ chức năng, ưu tiên hoàn thiện phần lõi (RAG + Triage + Chat) trước khi mở rộng các phân hệ phụ trợ.
- **Phương pháp thực nghiệm:** xây dựng bộ dữ liệu kiểm thử (câu hỏi–đáp án mẫu, kịch bản triệu chứng cấp cứu) để đánh giá định lượng độ chính xác truy xuất và độ nhạy của lớp Triage.

## 5. Nội dung thực hiện chi tiết

### 5.1. Phân hệ Người dùng (Bệnh nhân)

- Giao tiếp với AI qua giao diện chat, hỗ trợ streaming phản hồi theo thời gian thực.
- Khai báo và quản lý hồ sơ bệnh lý cá nhân.
- Ghi nhận nhật ký ăn uống hằng ngày.
- Theo dõi tình trạng tiêu hóa theo thang phân loại phân Bristol (Bristol Stool Chart).
- Thiết lập và nhận nhắc nhở uống thuốc định kỳ.

### 5.2. Phân hệ AI Chatbot & RAG

- Tích hợp mô hình ngôn ngữ lớn (LLM) thông qua API, kết hợp kiến trúc RAG.
- Pipeline số hóa tài liệu y khoa: trích xuất văn bản → chia đoạn (chunking) → sinh vector embedding → lưu trữ trong PostgreSQL/pgvector kèm metadata (nguồn, trang, thời điểm cập nhật).
- Cơ chế truy xuất ngữ cảnh liên quan (similarity search) để đưa vào prompt trước khi sinh câu trả lời; câu trả lời có trích dẫn nguồn tài liệu tham chiếu.
- Lớp sàng lọc an toàn y khoa (Triage): tầng 1 dựa trên đối sánh từ khóa nguy hiểm (ví dụ: nôn ra máu, đi ngoài phân đen, đau bụng dữ dội kèm sốt cao, sụt cân nhanh không rõ nguyên nhân, khó thở kèm đau ngực); khi phát hiện, hệ thống hiển thị cảnh báo tức thời khuyến nghị người dùng đến cơ sở y tế.

### 5.3. Phân hệ Quản trị (Admin/Bác sĩ)

- Trang CMS quản lý người dùng và phân quyền theo mô hình RBAC.
- Tải lên và quản lý phiên bản tài liệu y khoa cho kho tri thức RAG (bao gồm chức năng số hóa lại khi tài liệu cập nhật).
- Bảng điều khiển thống kê (Dashboard): số lượt chat, tần suất triệu chứng được hỏi nhiều nhất, số lần lớp Triage được kích hoạt.
- Nhật ký hoạt động hệ thống (Audit Log) ghi nhận các thao tác nhạy cảm (chỉnh sửa tài liệu, thay đổi phân quyền, truy cập dữ liệu người dùng).

## 6. Kiến trúc hệ thống

Hệ thống áp dụng kiến trúc **đa cơ sở dữ liệu (Multi-Datasource)**, tách biệt luồng dữ liệu khách hàng và luồng quản trị nội bộ:

- **PostgreSQL (kèm pgvector):** lưu hồ sơ người dùng, lịch sử phiên chat, nhật ký sức khỏe, và dữ liệu vector embedding phục vụ RAG.
- **MySQL:** lưu dữ liệu nghiệp vụ quản trị nội bộ (CMS), phân quyền RBAC, và nhật ký hệ thống (Audit Log).

Việc tách biệt này giúp cô lập dữ liệu nhạy cảm của người dùng khỏi hệ thống quản trị nội bộ, đồng thời cho phép tối ưu hiệu năng riêng cho từng loại tải (đọc/ghi tần suất cao ở phân hệ chat so với truy vấn thống kê ở phân hệ quản trị).

## 7. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java Spring Boot, Spring Data JPA, Hibernate, Flyway |
| Frontend | React, TypeScript, Tailwind CSS, Ant Design |
| Cơ sở dữ liệu | PostgreSQL + pgvector, MySQL |
| AI/RAG | API LLM, cơ chế embedding văn bản, similarity search trên pgvector |
| Giao tiếp thời gian thực | Server-Sent Events (SSE), WebSocket |
| Xác thực | JWT (phân hệ người dùng), session riêng (phân hệ quản trị) |

## 8. Phương pháp đánh giá

- **Độ chính xác truy xuất (RAG):** đánh giá trên bộ câu hỏi–đáp án mẫu tự xây dựng từ tài liệu đã nạp vào hệ thống (đo tỷ lệ tài liệu liên quan xuất hiện trong top-k kết quả truy xuất).
- **Độ nhạy của lớp Triage:** kiểm thử trên tập câu mô phỏng có/không chứa dấu hiệu cấp cứu, đo tỷ lệ phát hiện đúng (recall) và tỷ lệ báo động giả (false positive).
- **Hiệu năng hệ thống:** đo thời gian phản hồi token đầu tiên (TTFT) của chat streaming và khả năng xử lý đồng thời ở mức tải cơ bản.

## 9. Kế hoạch thực hiện dự kiến

| Giai đoạn | Thời gian | Nội dung |
|---|---|---|
| 1 | Tuần 1–2 | Thiết kế cơ sở dữ liệu, khởi tạo dự án Spring Boot đa datasource, thiết lập frontend cơ bản |
| 2 | Tuần 3–5 | Xây dựng pipeline RAG (số hóa tài liệu, embedding, truy xuất) và chức năng chat streaming |
| 3 | Tuần 6–7 | Xây dựng lớp Triage và cơ chế cảnh báo thời gian thực |
| 4 | Tuần 8–9 | Xây dựng phân hệ người dùng: hồ sơ bệnh lý, nhật ký ăn uống/Bristol, nhắc thuốc |
| 5 | Tuần 10 | Xây dựng phân hệ quản trị (CMS): quản lý người dùng, tài liệu, RBAC |
| 6 | Tuần 11 | Xây dựng dashboard thống kê và audit log |
| 7 | Tuần 12 | Hoàn thiện kênh thông báo ngoại tuyến (email/SMS) |
| 8 | Tuần 13–14 | Kiểm thử, đánh giá định lượng, hoàn thiện tài liệu và báo cáo, chuẩn bị bảo vệ |

## 10. Rủi ro và biện pháp giảm thiểu

| Rủi ro | Biện pháp giảm thiểu |
|---|---|
| Chi phí/giới hạn tần suất gọi API LLM | Sử dụng cache cho câu hỏi thường gặp; dùng mô hình nhỏ hơn cho tác vụ phân loại Triage |
| Rủi ro về độ chính xác y khoa và trách nhiệm pháp lý | Luôn hiển thị khuyến cáo "không thay thế chẩn đoán bác sĩ"; câu trả lời luôn kèm trích dẫn nguồn; trường hợp không chắc chắn sẽ khuyến nghị gặp bác sĩ thay vì suy đoán |
| Khối lượng công việc lớn so với nguồn lực một sinh viên thực hiện | Ưu tiên hoàn thiện phần lõi (RAG, Triage, Chat) trước; các phân hệ phụ trợ (thống kê nâng cao, thông báo đa kênh) có thể tinh giản nếu thời gian không cho phép |

## 11. Kết quả dự kiến

- Hệ thống Gastro AI hoạt động hoàn chỉnh với đầy đủ ba phân hệ: người dùng, AI/RAG, quản trị.
- Báo cáo đánh giá định lượng về độ chính xác truy xuất RAG và độ nhạy của lớp Triage.
- Mã nguồn hệ thống và tài liệu hướng dẫn cài đặt, vận hành.
- Báo cáo đồ án tốt nghiệp hoàn chỉnh (thực hiện ở giai đoạn sau).

## 12. Tài liệu tham khảo (dự kiến bổ sung)

- Tài liệu, phác đồ điều trị và cẩm nang y khoa chuẩn về bệnh lý dạ dày, đường ruột (nguồn cụ thể sẽ được liệt kê khi hoàn thiện kho tri thức RAG).
- Tài liệu kỹ thuật chính thức của Spring Boot, React, PostgreSQL/pgvector.
- Các bài báo/tài liệu tham khảo về kiến trúc RAG (Retrieval-Augmented Generation) trong ứng dụng hỏi–đáp chuyên ngành.
