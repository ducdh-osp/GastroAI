# Rà soát chất lượng code và nghiệp vụ tiêu hóa

## Đã thực hiện

### Tái sử dụng luồng danh sách ở frontend

- Thêm `usePagedList` dùng cho Nhật ký Bristol và Nhật ký ăn uống. Hook quản lý tải dữ liệu, phân trang, lỗi và số thứ tự request để response đến muộn không thể ghi đè trang hiện tại.
- Thêm `useInFlightGuard` dùng cho xoá Bristol, xoá Food Diary, bật/tắt/xoá/xác nhận uống thuốc. Guard dùng cả `ref` và state, nên hai lần bấm trong cùng một render frame cũng chỉ tạo một request.
- Giữ modal riêng theo từng nghiệp vụ. Việc ép các form khác trường dữ liệu vào một modal generic sẽ làm code khó đọc và tăng nguy cơ thay đổi hành vi ngoài phạm vi.

### Gom kiểm tra xác thực và quyền sở hữu ở backend

- Thêm `AuthenticatedRequest` để các controller patient/chat dùng chung kiểm tra đăng nhập và lấy `patientId`.
- Các service tiếp tục dùng `OwnedResourceLoader` để kiểm tra bản ghi thuộc bệnh nhân, trả `ResourceNotFoundException`/404 thống nhất. Không tạo base service chung vì các repository và thông báo nghiệp vụ khác nhau; helper hiện tại đã gom đúng phần chung mà không che khuất nghiệp vụ.

## Đánh giá BA chuyên ngành tiêu hóa

### Bristol Stool Chart

Không thay đổi. Thang loại 1–7 hiện đúng chuẩn; loại 4 được mô tả là mức lý tưởng.

### Nhật ký ăn uống — backlog đề xuất

Chức năng hiện tại lưu món ăn và thời điểm, đủ cho ghi nhận cơ bản nhưng chưa đủ để tìm yếu tố khởi phát triệu chứng. Đề xuất backlog:

- `mealType`: sáng, trưa, tối, bữa phụ.
- Liên kết triệu chứng sau ăn theo thời điểm (ví dụ đầy bụng, đau bụng, tiêu chảy, mức độ và thời gian khởi phát), thay vì suy luận chỉ từ số lượng bữa.
- Timeline theo ngày hiển thị chung Food Diary và Bristol Log, với liên kết chỉ mang tính gợi ý lâm sàng, không khẳng định quan hệ nhân quả.

Những thay đổi này cần thiết kế schema, API, biểu đồ và migration riêng nên không triển khai lẫn vào đợt refactor/bug-fix hiện tại.

### Nhắc uống thuốc — backlog đề xuất

Model hiện dùng cho một lịch nhắc lặp lại hằng ngày, một giờ mỗi lịch. Để hỗ trợ đơn thuốc tiêu hóa thực tế (đặc biệt phác đồ H. pylori và PPI), đề xuất thiết kế riêng:

- Một đợt thuốc có nhiều giờ uống mỗi ngày.
- `startDate`/`endDate` để lịch tự hết hạn sau liệu trình 10–14 ngày hoặc theo đơn.
- `instructions` có cấu trúc hoặc text hướng dẫn: trước/sau ăn, cách bữa ăn bao lâu, ghi chú của bác sĩ.

Không mở rộng schema trong lần này để tránh thay đổi phạm vi lớn và tránh diễn giải lịch nhắc hiện có thành đơn thuốc y khoa.

### Ranh giới giữa hai khái niệm thuốc

- `MedicalProfile.currentMedications`: danh sách text tự do do bệnh nhân tự khai cho thuốc **không cần lịch nhắc trong ứng dụng** (ví dụ thuốc dùng một lần, không thường xuyên hoặc được quản lý ngoài ứng dụng). Đây không phải nguồn tạo lịch nhắc.
- `MedicationReminder`: nguồn chuẩn cho **liều cần nhắc trong ứng dụng**: tên thuốc, liều lượng, giờ uống, liệu trình, hướng dẫn và xác nhận đã uống.

Hai nguồn dữ liệu độc lập và không tự đồng bộ: tạo/sửa/tắt lịch nhắc không làm thay đổi hồ sơ, và ngược lại. Giao diện phải hướng bệnh nhân chỉ nhập một thuốc vào một nơi theo mục đích trên, để không phải nhập trùng.

Khi AI bắt đầu dùng dữ liệu thuốc, phải đọc từng nguồn với nhãn nguồn gốc rõ ràng; chỉ lấy lịch còn hiệu lực từ `MedicationReminder` và không tự coi hai chuỗi tên thuốc giống/gần giống nhau là cùng một thuốc. Nếu sản phẩm sau này cần một nguồn thuốc duy nhất, cần migration cùng luồng người dùng xác nhận dữ liệu trước khi hợp nhất.
