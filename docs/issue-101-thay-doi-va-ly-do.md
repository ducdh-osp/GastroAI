# Báo cáo thay đổi issue 101

Ngày hoàn thiện: 24/11/2026. Nhánh làm việc: `fix/issue-101-business-spec-gaps`, tách từ `dev` tại commit `d4e4156` sau khi pull.

Bản sửa giải quyết sự lệch nhau giữa tài liệu và code trong [issue #101](https://github.com/ducdh-osp/GastroAI/issues/101). Thay đổi chức năng chính là **bắt buộc nhập lý do khóa tài khoản bệnh nhân và truyền lý do vào Audit Log**. Các phần còn lại cập nhật mô tả theo chức năng đang có, đồng thời ghi rõ những đề xuất chưa triển khai.

Có 10 tài liệu nghiệp vụ được cập nhật: 8 tài liệu trong issue và 2 tài liệu tổng quan liên quan. Có 5 file code/test được thêm hoặc sửa. File này giải thích các thay đổi để bạn đối chiếu và review.

## 1. Chức năng được thêm hoặc sửa

Trước đây, Admin chỉ xác nhận Có/Hủy để khóa bệnh nhân. API không nhận lý do và gửi `null` vào trường chi tiết của Audit Log. Sau bản sửa, Admin nhập lý do trong modal, frontend gửi lý do tới API và backend kiểm tra trước khi khóa.

| File | Thay đổi | Lý do |
| :--- | :--- | :--- |
| [CmsUsersPage.tsx](../fe/src/pages/cms/CmsUsersPage.tsx) | Thêm modal hiển thị bệnh nhân cần khóa và ô lý do bắt buộc, tối đa 1.000 ký tự. Giữ nội dung khi API lỗi; hủy rồi mở lại thì xóa nội dung cũ. | Giúp Admin xác nhận đúng người và có căn cứ cho thao tác ảnh hưởng quyền truy cập. |
| [CmsUsersPage.tsx](../fe/src/pages/cms/CmsUsersPage.tsx) | Dùng `useInFlightGuard` có sẵn để chặn gửi lặp; khóa thao tác đóng/hủy khi request khóa đang chạy. Mở khóa giữ hộp xác nhận, không yêu cầu lý do. | Tránh hai lần bấm tạo request trùng hoặc đóng modal khi chưa biết kết quả. |
| [cmsUsers.ts](../fe/src/api/cmsUsers.ts) | Gửi `{ reason }` khi khóa. Chữ ký TypeScript yêu cầu lý do với `locked: true`; mở khóa vẫn không có body. | Giữ giao diện và hợp đồng API thống nhất, phát hiện lời gọi thiếu lý do khi kiểm tra kiểu. |
| [LockPatientRequest.java](../be/src/main/java/vn/gastroai/be/api/cms/user/LockPatientRequest.java) — file mới | Loại khoảng trắng đầu/cuối; dùng `@NotBlank` và `@Size(max = 1000)` cho lý do. | API vẫn bảo vệ quy tắc khi client bỏ qua validation trên giao diện. Giới hạn khớp sức chứa trường audit. |
| [CmsUserAdminController.java](../be/src/main/java/vn/gastroai/be/api/cms/user/CmsUserAdminController.java) | Nhận body có `@Valid`, gọi khóa trước rồi truyền lý do vào `AuditLogService.record`. | Lý do đi hết luồng FE → API → Audit Log; lỗi khóa không được ghi thành sự kiện thành công. |
| [CmsUserAdminControllerTest.java](../be/src/test/java/vn/gastroai/be/api/cms/user/CmsUserAdminControllerTest.java) | Mở rộng từ 5 lên 18 ca test, tính cả các lần chạy tham số. | Kiểm tra dữ liệu biên, quyền truy cập, audit và lỗi thao tác chính. |

API khóa mới:

```http
POST /api/v1/cms/users/{patientId}/lock
Content-Type: application/json

{"reason":"Khóa theo yêu cầu hỗ trợ của bệnh nhân"}
```

Thiếu body, thiếu lý do, lý do chỉ chứa khoảng trắng hoặc dài hơn 1.000 ký tự sau chuẩn hóa trả HTTP 400 trước khi gọi thao tác khóa. Lý do hợp lệ được ghi vào `detail` của sự kiện `PATIENT_LOCK` theo cơ chế ghi audit hiện tại.

API mở khóa giữ nguyên: `POST /api/v1/cms/users/{patientId}/unlock`, không cần body. Cơ chế tăng phiên bản token khi khóa vẫn được giữ: mở khóa cho phép đăng nhập mới, không phục hồi JWT đã bị thu hồi.

**Khi triển khai, cần cập nhật frontend và backend cùng phiên bản.** Client cũ gọi API khóa mà không gửi lý do sẽ nhận HTTP 400. Không cần migration database vì `audit_logs.detail` đã tồn tại.

## 2. Đối chiếu đủ 18 mục của issue

Các số dòng dưới đây là vị trí đối chiếu tại bản sửa này. "Sửa tài liệu" có thể bao gồm ghi rõ giới hạn và chuyển ý tưởng cũ sang phần đề xuất, không có nghĩa tính năng đó đã được xây dựng.

| Mục | Xử lý và lý do | Bằng chứng code |
| :--- | :--- | :--- |
| 01 — PT-02, loại bữa | Bổ sung Khác, đủ 5 loại bữa và mặc định Khác. Code đã hỗ trợ, tài liệu thiếu. | [FoodDiaryEntryModal.tsx:83](../fe/src/components/foodDiary/FoodDiaryEntryModal.tsx#L83) |
| 02 — PT-02, khởi phát triệu chứng | Bổ sung `symptomOnsetMinutes`, đơn vị phút, không bắt buộc, phân biệt 0 với bỏ trống. Ghi đúng giới hạn UI 0–10.080 và API kiểm tra số nguyên không âm. | [FoodDiaryEntryModal.tsx:89](../fe/src/components/foodDiary/FoodDiaryEntryModal.tsx#L89), [FoodDiaryEntryRequest.java:19](../be/src/main/java/vn/gastroai/be/api/patient/FoodDiaryEntryRequest.java#L19) |
| 03 — PT-03, cấu trúc trang | Ghi rõ Xu hướng là tab trong `/food-diary`; route lịch sử cũ chỉ redirect. | [router.tsx:104](../fe/src/routes/router.tsx#L104) |
| 04 — PT-03, toggle con | Bỏ toggle Biểu đồ/Danh sách khỏi mockup. Nhật ký và Xu hướng là hai tab ngang hàng; bấm cột lọc nhật ký theo ngày. | [FoodDiaryPage.tsx:139](../fe/src/pages/patient/FoodDiaryPage.tsx#L139), [FoodDiaryPage.tsx:89](../fe/src/pages/patient/FoodDiaryPage.tsx#L89) |
| 05 — PT-04, cấu trúc trang | Đồng bộ hai tab Nhật ký/Xu hướng và route `/bristol-history` chuyển sang `/bristol?tab=trend`. | [router.tsx:136](../fe/src/routes/router.tsx#L136) |
| 06 — PT-04, triệu chứng kèm theo | Đổi tên ô thành Ghi chú (`notes`); triệu chứng là ví dụ nội dung của ghi chú. Không tạo field mới vì dữ liệu hiện chỉ có một trường ghi chú. | [BristolLogModal.tsx:76](../fe/src/components/bristol/BristolLogModal.tsx#L76), [BristolLogRequest.java:12](../be/src/main/java/vn/gastroai/be/api/patient/BristolLogRequest.java#L12) |
| 07 — PT-05, vùng tô màu | Chuyển vùng tô mức 3–5 sang đề xuất chưa triển khai. Mô tả biểu đồ và thống kê hiện tại. | [BristolTrendChart.tsx:46](../fe/src/components/bristol/BristolTrendChart.tsx#L46) |
| 08 — PT-05, banner AI | Chuyển banner sang đề xuất. Ví dụ 3 lần mức 6–7 trong 7 ngày không được mô tả như quy tắc đã chạy. | [BristolTrendTab.tsx:64](../fe/src/components/bristol/BristolTrendTab.tsx#L64) |
| 09 — PT-07, trước/sau ăn | Mô tả giờ cố định và ô Hướng dẫn dùng thuốc tự do. Phân loại và tính giờ theo bữa ăn là đề xuất riêng. | [MedicationReminderModal.tsx:105](../fe/src/components/medication/MedicationReminderModal.tsx#L105), [MedicationReminderModal.tsx:116](../fe/src/components/medication/MedicationReminderModal.tsx#L116) |
| 10 — PT-07, lặp lịch | Ghi rõ lặp hằng ngày trong khoảng điều trị, chưa chọn ngày trong tuần. | [MedicationReminder.java:17](../be/src/main/java/vn/gastroai/be/domain/notification/MedicationReminder.java#L17) |
| 11 — PT-07, xác nhận từ thông báo | Điều chỉnh kết luận audit: popup WebSocket đã có nút Đã uống thuốc khi có mã lịch/giờ liều; chuông và trang danh sách thông báo chỉ điều hướng. Không viết lại chức năng đã có. | [AppHeader.tsx:80](../fe/src/components/layout/AppHeader.tsx#L80), [NotificationBell.tsx:60](../fe/src/components/layout/NotificationBell.tsx#L60) |
| 12 — PT-07, lý do tắt lịch | Mô tả công tắc cập nhật trực tiếp và nút Tắt nhắc có hộp xác nhận; cả hai chưa hỏi lý do. Lưu lý do là đề xuất. | [MedicationRemindersPage.tsx:215](../fe/src/pages/patient/MedicationRemindersPage.tsx#L215), [MedicationRemindersPage.tsx:249](../fe/src/pages/patient/MedicationRemindersPage.tsx#L249) |
| 13 — PT-07, lịch sử bỏ lỡ | Phân biệt danh sách liều đến giờ hôm nay với lịch sử đã xác nhận. Ghi rõ chưa lưu lịch sử liều chưa xác nhận/bỏ lỡ. | [MedicationRemindersPage.tsx:277](../fe/src/pages/patient/MedicationRemindersPage.tsx#L277), [MedicationReminderService.java:151](../be/src/main/java/vn/gastroai/be/application/notification/MedicationReminderService.java#L151) |
| 14 — PT-08, dùng hồ sơ cá nhân | Sửa giới thiệu, ví dụ hội thoại và nguyên tắc: chat chưa tự đọc hồ sơ/nhật ký/lịch thuốc. Đồng bộ với PT-01; ngữ cảnh hiện có là câu hỏi, tệp gửi kèm và RAG. | [RagQueryService.java:71](../be/src/main/java/vn/gastroai/be/application/rag/RagQueryService.java#L71), [StreamingRagQueryService.java:53](../be/src/main/java/vn/gastroai/be/application/rag/StreamingRagQueryService.java#L53) |
| 15 — PT-08, nguyên văn nguồn | Mô tả đúng đoạn nguồn được che liều và viết lại; fallback cũng là đoạn đã che liều. Không gọi đó là nguyên văn. | [RagQueryService.java:132](../be/src/main/java/vn/gastroai/be/application/rag/RagQueryService.java#L132) |
| 16 — AD-02, lý do khóa | Sửa code xuyên suốt và cập nhật tài liệu theo §1 của báo cáo này. Giữ yêu cầu truy vết lý do vì có giá trị nghiệp vụ rõ ràng. | [LockPatientRequest.java:7](../be/src/main/java/vn/gastroai/be/api/cms/user/LockPatientRequest.java#L7), [CmsUserAdminController.java:40](../be/src/main/java/vn/gastroai/be/api/cms/user/CmsUserAdminController.java#L40) |
| 17 — NT-02, biến email | Sửa ví dụ thành `userName`/`verificationUrl`; liệt kê biến của cả ba mẫu. Không mở rộng regex hoặc tạo alias cho tên biến sai trong doc. | [EmailTemplateCode.java:9](../be/src/main/java/vn/gastroai/be/domain/notification/EmailTemplateCode.java#L9), [EmailTemplateRenderer.java:22](../be/src/main/java/vn/gastroai/be/application/notification/EmailTemplateRenderer.java#L22) |
| 18 — NT-02, nội dung mẫu | Thêm hai ô bắt buộc HTML và text dự phòng, giới hạn độ dài, biến bắt buộc, cách xem trước và gửi thử bản đã lưu. | [CmsEmailTemplatesPage.tsx:183](../fe/src/pages/cms/CmsEmailTemplatesPage.tsx#L183) |

## 3. Những chỉnh sửa nhất quán đi kèm

- [Tổng quan Bệnh nhân](../00-General/02-NguoiDung-BenhNhan/00-TongQuan.md): ghi rõ cá nhân hóa AI mới là mục tiêu; cập nhật cách nhóm tab và mô tả thống kê tương quan thực phẩm. Tránh việc tài liệu tổng quan tiếp tục khẳng định khả năng mà các màn chi tiết vừa xác nhận chưa có.
- [Tổng quan AI/RAG](../00-General/03-AIChatbot-RAG/00-TongQuan.md): đồng bộ nguồn ngữ cảnh thực tế và giới hạn đoạn nguồn hiển thị với PT-08.
- PT-02/PT-04: mô tả đúng lựa chọn Nhật ký/Thùng rác; PT-04 dùng radio với nhãn mô tả thay cho hình minh họa chưa có. Chỉ cập nhật tài liệu của UC0020, không sửa chức năng khôi phục thuộc phần Thăng.
- PT-08: làm rõ nguồn có thể rỗng, tối đa hai nguồn hiển thị, gợi ý tối đa ba câu có thể vắng khi lỗi; phản hồi hữu ích là dữ liệu đánh giá, không khẳng định AI tự học ngay sau mỗi lượt bấm.
- AD-02: cập nhật các cột đang hiển thị, thu hồi token, hành vi hủy/thử lại và giới hạn ghi audit qua hai database.

## 4. Kiểm thử đã thực hiện

### Backend

**163 test chạy thành công, 0 failure, 0 error, 0 skipped**, thuộc 16 lớp test liên quan:

| Nhóm | Số test |
| :--- | ---: |
| API khóa bệnh nhân, service quản lý bệnh nhân, Audit Log | 30 |
| Xác thực bệnh nhân | 30 |
| Nhật ký ăn uống | 28 |
| Nhật ký Bristol | 24 |
| Lịch thuốc và nhắc liều đến giờ | 22 |
| Mẫu email | 19 |
| RAG thường và streaming | 10 |
| Tổng | 163 |

Các ca bổ sung kiểm tra lý do hợp lệ có tiếng Việt và khoảng trắng đầu/cuối; thiếu body, thiếu/null/rỗng/toàn khoảng trắng; 1.000 và 1.001 ký tự; khóa thất bại không ghi audit thành công; các vai trò ngoài Admin không thể khóa/mở khóa kể cả gửi lý do hợp lệ. Test service hiện có xác nhận việc thu hồi token khi khóa và không phục hồi token cũ khi mở khóa.

Lệnh chạy tại thư mục `be`:

```powershell
.\mvnw.cmd -q "-Dtest=CmsUserAdminControllerTest,AdminPatientManagementServiceTest,AuditLogServiceTest,FoodDiaryControllerTest,FoodDiaryServiceTest,BristolLogControllerTest,BristolLogServiceTest,MedicationReminderControllerTest,MedicationReminderServiceTest,MedicationDueNotificationServiceTest,EmailTemplateRendererTest,EmailTemplateServiceTest,EmailTemplateAdminControllerTest,RagQueryServiceTest,StreamingRagQueryServiceTest,AuthServiceTest" test
```

### Frontend và trình duyệt

- `npm run lint`: thành công, còn các cảnh báo có sẵn; ở trang quản lý bệnh nhân, cảnh báo dependency `reload` trong `useEffect` thuộc phần code cũ được giữ nguyên.
- `npm run build`: TypeScript và Vite build thành công; có cảnh báo bundle lớn hơn 500 kB.
- Playwright với Chrome, API giả lập: 6 nhóm tình huống thành công, không có lỗi runtime chưa xử lý. Đã kiểm tra lý do rỗng/trắng; hủy và mở lại; lỗi API giữ dữ liệu; thử lại và chặn gửi lặp; mở khóa; modal ở chiều rộng 390 px. Đã xem ảnh giao diện desktop/mobile sau khi modal mở xong.
- Rà soát `git diff --check`, đường dẫn liên kết Markdown và đối chiếu đủ 18 mục.

Kiểm thử trình duyệt dùng bệnh nhân giả lập, không tác động tài khoản hoặc dữ liệu bệnh nhân thật. Chưa chạy tích hợp toàn hệ thống với PostgreSQL/MySQL thật, SMTP hoặc Gemini; chưa thực hiện lại toàn bộ giao diện của các phân hệ chỉ sửa tài liệu. Kết luận cho các phần đó dựa trên source và các test tương ứng đã chạy, không được hiểu là mọi luồng thực tế đều đã được kiểm thử đầu-cuối.

## 5. Giới hạn cần biết

`AuditLogService` hiện bắt lỗi ghi log để thao tác chính đã thành công không bị ảnh hưởng. Bệnh nhân ở PostgreSQL, audit ở MySQL; bản sửa truyền đúng lý do vào luồng audit nhưng không bổ sung giao dịch nguyên tử hoặc hàng đợi ghi bền vững giữa hai database. Nếu MySQL lỗi, việc khóa vẫn có thể thành công mà thiếu audit. Đây là giới hạn hạ tầng sẵn có và đã được ghi vào AD-02.

Các khả năng sau được ghi rõ là chưa triển khai: chọn ngày uống trong tuần, tự tính giờ trước/sau ăn, lưu lý do tắt lịch thuốc, lưu lịch sử liều bỏ lỡ, vùng tô và banner AI trên biểu đồ Bristol, tự đưa hồ sơ/nhật ký vào prompt. Việc triển khai chúng cần thiết kế nghiệp vụ và dữ liệu riêng; bản sửa hiện tại không ngầm tuyên bố đã có các khả năng này.

## 6. Nội dung đề xuất cho PR

**Tiêu đề:** `fix(cms), docs: xử lý các điểm lệch nghiệp vụ trong issue 101`

Admin hiện phải nhập lý do khi khóa tài khoản bệnh nhân; API kiểm tra lý do và đưa vào Audit Log sau khi thao tác khóa thành công. Đồng thời đồng bộ 8 tài liệu nghiệp vụ cùng 2 tài liệu tổng quan với code, sửa kết luận về xác nhận thuốc từ popup WebSocket và ghi rõ tính năng chưa triển khai.

Validation: 163 test backend; lint và build frontend; 6 nhóm kiểm thử UI với API giả lập; kiểm tra diff và liên kết tài liệu. API khóa yêu cầu body mới, cần triển khai FE/BE cùng phiên bản. Giới hạn ghi audit qua hai database được giữ và mô tả rõ. Tham chiếu issue #101.
