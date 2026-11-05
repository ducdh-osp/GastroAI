# TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ THEO MÀN HÌNH (PHÂN HỆ TRÁCH NHIỆM)

Tài liệu đặc tả chi tiết luồng nghiệp vụ 15 màn hình phụ trách trong hệ thống GastroAI, đối chiếu chính xác với kiến trúc mã nguồn, cơ sở dữ liệu và giao diện thực tế của sản phẩm.

---

## DANH MỤC CÁC MÀN HÌNH ĐẶC TẢ

1. Màn hình Đăng ký Tài khoản Bệnh nhân (/register)
2. Màn hình Quên Mật khẩu (/forgot-password)
3. Màn hình Đặt lại Mật khẩu (/reset-password)
4. Màn hình Lịch sử Đăng nhập (/login-history và /cms/login-history)
5. Màn hình Phiên Chat của Tôi & Tư vấn Sức khỏe AI (/chat-history và /chat)
6. Màn hình Đánh giá Triệu chứng Tiêu hóa (/symptom-assessment)
7. Màn hình Bài viết Tiêu hóa - Thư viện Kiến thức Bệnh nhân (/articles, /articles/:slug)
8. Màn hình Hỏi đáp Thường xuyên - FAQ Bệnh nhân (/faq)
9. Màn hình Bài viết Kiến thức - Quản trị & Soạn thảo CMS (/cms/articles, /cms/articles/new, /cms/articles/:id/edit)
10. Màn hình Chủ đề Bài viết - Quản trị CMS (/cms/article-topics)
11. Màn hình Quản lý Câu hỏi Thường gặp - FAQ Quản trị CMS (/cms/faq)
12. Màn hình Danh mục Chuẩn hóa Y khoa - Quản trị CMS (/cms/catalogs)
13. Màn hình Đánh giá Chất lượng AI RAG và Triage - Quản trị CMS (/cms/evaluation)
14. Màn hình Phát Thông báo Hệ thống Broadcast - Quản trị CMS (/cms/broadcast)
15. Màn hình Bảng điều khiển Thống kê Hệ thống - Dashboard Quản trị CMS (/cms/dashboard)

---

### 1. Màn hình Đăng ký Tài khoản Bệnh nhân

- Tên màn: Đăng ký Tài khoản Bệnh nhân (Patient Registration).
- Luồng nghiệp vụ tổng quan: Cho phép người bệnh tạo mới tài khoản cá nhân trên hệ thống để bắt đầu ghi nhật ký sức khỏe tiêu hóa và trò chuyện cùng trợ lý AI. Hệ thống hướng dẫn và kiểm tra trực tiếp theo thời gian thực các quy tắc độ mạnh mật khẩu cũng như độ khớp mật khẩu xác nhận trên giao diện, thực thi giới hạn byte UTF-8 của BCrypt, lưu tài khoản ở trạng thái chờ kích hoạt (`email_verified = false`) và gửi email xác thực danh tính định dạng HTML chuẩn thương hiệu.
- Mô tả luồng nghiệp vụ:
  1. Người dùng truy cập đường dẫn /register từ trình duyệt.
  2. Giao diện hiển thị biểu mẫu đăng ký gồm 4 trường bắt buộc cùng cơ chế kiểm tra trực tiếp theo thời gian thực:
     - Họ và tên (fullName): Chuỗi ký tự bắt buộc nhập.
     - Email (email): Bắt buộc nhập, đúng định dạng email tiêu chuẩn, độ dài tối đa 255 ký tự.
     - Mật khẩu (password):
       + Hiển thị cố định dòng hướng dẫn quy tắc ngay bên dưới ô nhập: "Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 số và 1 ký tự đặc biệt."
       + Kiểm tra trực tiếp khi người dùng nhập liệu; nếu sai quy tắc sẽ báo lỗi rõ ràng yêu cầu tối thiểu 8 ký tự, tối đa 72 ký tự, dung lượng chuỗi mã hóa không vượt quá 72 byte UTF-8 (tương thích với giới hạn cắt 72 byte của thuật toán băm BCrypt khi nhập tiếng Việt có dấu), và bắt buộc chứa ít nhất 1 chữ in hoa (A-Z), 1 chữ số (0-9) cùng 1 ký tự đặc biệt.
     - Xác nhận mật khẩu (confirmPassword):
       + Liên kết phụ thuộc trực tiếp với trường Mật khẩu (`dependencies={['password']}`).
       + Ngay khi người dùng đang gõ ở trường Xác nhận mật khẩu hoặc chỉnh sửa lại trường Mật khẩu, bộ kiểm tra (validator) tự động đối chiếu tức thời và hiển thị ngay dòng cảnh báo lỗi "Mật khẩu xác nhận không khớp." bên dưới ô nhập nếu hai giá trị lệch nhau, không cần chờ bấm nút gửi biểu mẫu.
  3. Người dùng hoàn tất thông tin và nhấn nút "Đăng ký":
     - Kiểm tra tại Frontend: Xác nhận toàn bộ các trường đã vượt qua bộ quy tắc kiểm tra hợp lệ và hai trường mật khẩu trùng khớp hoàn toàn trước khi gọi API.
     - Xử lý tại Backend:
       + Kiểm tra trùng lặp email trong bảng patients (không phân biệt hoa thường). Nếu email đã tồn tại, trả về mã lỗi từ chối.
       + Kiểm tra lại chính sách mật khẩu qua bộ kiểm tra PatientPasswordPolicy ở tầng ứng dụng.
       + Băm mật khẩu bằng BCrypt và lưu bản ghi bệnh nhân mới vào bảng patients với trạng thái chưa xác thực email (`email_verified = false`), đồng thời lưu mật khẩu khởi tạo vào bảng lịch sử mật khẩu (`patient_password_history`).
       + Cơ chế xử lý tài khoản chưa xác thực qua Gmail trong Cơ sở dữ liệu: Khi `email_verified = false`, nếu người dùng cố tình đăng nhập, hệ thống sẽ chặn phiên truy cập với thông báo "Email chưa được xác thực" và tự động ghi nhận một bản ghi thất bại (`FAILURE`) kèm lý do `EMAIL_NOT_VERIFIED` vào bảng `patient_login_history`.
       + Tạo mã xác thực email dùng một lần (Verification Token có thời hạn hiệu lực 24 giờ), kết xuất thư kích hoạt theo mẫu `VERIFY_EMAIL` (giao diện HTML responsive chuẩn thương hiệu GastroAI có bố cục khung thư rõ ràng, nút bấm kích hoạt nổi bật, đường dẫn dự phòng và thông tin thời hạn hiệu lực) và đưa vào hàng đợi gửi thư (`email_outbox`).
  4. Phản hồi giao diện: Hiển thị thông báo xanh "Đăng ký thành công. Vui lòng xác thực email." và tự động chuyển hướng người dùng về màn hình Đăng nhập (/login) sau 1,8 giây.
- Điều hướng menu:
  - Đường dẫn truy cập trực tiếp: /register
  - Liên kết chuyển tiếp trên giao diện: Liên kết "Đã có tài khoản? Đăng nhập" dẫn tới /login.
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: patients, patient_password_history, patient_login_history, email_outbox, email_templates.
  - API phục vụ: POST /api/v1/auth/register (nhận RegisterRequest gồm fullName, email, password).
- Điều kiện tiên quyết:
  - Địa chỉ email đăng ký chưa tồn tại trong bảng patients.

---

### 2. Màn hình Quên Mật khẩu

- Tên màn: Quên Mật khẩu (Forgot Password).
- Luồng nghiệp vụ tổng quan: Tiếp nhận yêu cầu khôi phục quyền truy cập từ người bệnh quên mật khẩu đăng nhập. Hệ thống sinh liên kết đặt lại mật khẩu có hiệu lực trong 30 phút và gửi qua email đã đăng ký theo giao diện thư HTML chuẩn hóa, đồng thời bảo vệ thông tin định danh người dùng trước nguy cơ dò quét tài khoản.
- Mô tả luồng nghiệp vụ:
  1. Người dùng truy cập /forgot-password từ liên kết trên màn hình đăng nhập.
  2. Giao diện hiển thị trường nhập "Email" (tối đa 255 ký tự) kèm nút "Gửi liên kết đặt lại".
  3. Người dùng nhập địa chỉ email và nhấn gửi:
     - Kiểm tra tại Frontend: Bắt buộc nhập, đúng định dạng email và không quá 255 ký tự.
     - Xử lý tại Backend:
       + Tra cứu tài khoản bệnh nhân theo email trong bảng patients.
       + Cơ chế chống dò quét danh tính (User Enumeration Prevention): Dù email có tồn tại hay không, hệ thống vẫn trả về thông báo thành công đồng nhất để kẻ tấn công không thể suy đoán email nào đã đăng ký.
       + Nếu tài khoản tồn tại: Hệ thống tạo token khôi phục mật khẩu ngẫu nhiên an toàn với thời hạn hiệu lực là 30 phút, lưu thông tin token và kết xuất nội dung thư theo mẫu `RESET_PASSWORD` (định dạng HTML responsive chuẩn thương hiệu GastroAI, hiển thị tiêu đề khôi phục mật khẩu, nút thao tác "Đặt lại mật khẩu" nổi bật, liên kết dự phòng để sao chép thủ công, thông báo thời hạn 30 phút và lưu ý bảo mật) vào bảng `email_outbox` để gửi tới hòm thư của người bệnh.
  4. Giao diện hiển thị thông báo xác nhận đã gửi hướng dẫn qua email và cung cấp liên kết quay về màn hình đăng nhập.
- Điều hướng menu:
  - Đường dẫn: /forgot-password
  - Liên kết chuyển tiếp: "Quay lại đăng nhập" dẫn về /login.
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: patients, email_outbox, email_templates.
  - API phục vụ: POST /api/v1/auth/forgot-password (nhận ForgotPasswordRequest gồm email).
- Điều kiện tiên quyết:
  - Người dùng nhớ địa chỉ email đã đăng ký và có quyền truy cập vào hộp thư đó.

---

### 3. Màn hình Đặt lại Mật khẩu

- Tên màn: Đặt lại Mật khẩu (Reset Password).
- Luồng nghiệp vụ tổng quan: Cho phép người bệnh thiết lập mật khẩu mới thông qua đường dẫn chứa mã xác thực (token) nhận được từ email. Hệ thống kiểm tra tính hợp lệ của token, kiểm tra trực tiếp trên giao diện quy tắc độ mạnh mật khẩu và độ khớp mật khẩu xác nhận theo thời gian thực, đồng thời đối chiếu toàn bộ lịch sử mật khẩu tại tầng backend để ngăn chặn việc đặt trùng với bất kỳ mật khẩu nào đã từng sử dụng.
- Mô tả luồng nghiệp vụ:
  1. Người dùng mở liên kết trong email có định dạng /reset-password?token={token}.
  2. Kiểm tra ngay khi mở trang:
     - Giao diện đọc tham số token từ thanh địa chỉ URL. Nếu đường dẫn không có tham số token, màn hình hiển thị ngay cảnh báo lỗi "Liên kết đặt lại mật khẩu không có token".
  3. Biểu mẫu nhập liệu gồm 2 trường với cơ chế kiểm tra thời gian thực:
     - Mật khẩu mới (newPassword):
       + Hiển thị cố định dòng hướng dẫn quy tắc ngay bên dưới ô nhập: "Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 số và 1 ký tự đặc biệt."
       + Kiểm tra trực tiếp khi nhập: Bắt buộc tối thiểu 8 ký tự, tối đa 72 ký tự và không quá 72 byte UTF-8; phải có ít nhất 1 chữ in hoa, 1 chữ số và 1 ký tự đặc biệt. Nếu vi phạm sẽ hiển thị ngay thông báo lỗi quy tắc dưới ô nhập.
     - Xác nhận mật khẩu mới (confirmPassword):
       + Liên kết phụ thuộc trực tiếp với trường Mật khẩu mới (`dependencies={['newPassword']}`).
       + Tự động đối chiếu theo thời gian thực ngay khi người dùng gõ vào ô Xác nhận mật khẩu mới hoặc chỉnh sửa ô Mật khẩu mới; nếu hai giá trị không trùng khớp sẽ hiển thị ngay thông báo lỗi "Mật khẩu xác nhận không khớp." bên dưới trường nhập.
  4. Người dùng nhấn nút "Đổi mật khẩu":
     - Kiểm tra tại Frontend: Đảm bảo cả hai trường mật khẩu hợp lệ và khớp nhau hoàn toàn trước khi gửi yêu cầu xuống máy chủ.
     - Backend kiểm tra token: Xác minh token có tồn tại, chưa được sử dụng và còn trong thời hạn 30 phút hay không. Nếu hết hạn hoặc sai, trả về lỗi từ chối.
     - Kiểm tra chống tái sử dụng mật khẩu cũ (Password Reuse Prevention):
       + Backend truy vấn toàn bộ lịch sử mật khẩu trong bảng `patient_password_history` và mật khẩu hiện tại của bệnh nhân qua phương thức `rejectReusedPassword`.
       + Dùng BCrypt đối chiếu mật khẩu mới với mật khẩu hiện tại và tất cả các mật khẩu từng đặt trước đó. Nếu phát hiện trùng lặp với bất kỳ mật khẩu cũ nào, hệ thống lập tức chặn thao tác và trả về lỗi "Mật khẩu mới không được trùng với bất kỳ mật khẩu nào đã từng sử dụng".
     - Cập nhật dữ liệu bảo mật:
       + Băm mật khẩu mới bằng BCrypt và lưu vào bảng patients.
       + Lưu bản ghi mật khẩu mới vào bảng `patient_password_history` để phục vụ kiểm tra cho các lần đổi tiếp theo.
       + Thu hồi hiệu lực của token đặt lại mật khẩu, xóa trạng thái khóa tạm thời (nếu có) và đặt lại số lần đăng nhập sai về 0.
       + Tạo thông báo cảnh báo bảo mật (SECURITY_ALERT) gửi qua email và thông báo trong ứng dụng để người dùng nắm được biến động tài khoản.
  5. Giao diện hiển thị thông báo "Đổi mật khẩu thành công. Đang chuyển về trang đăng nhập..." và tự động điều hướng về /login sau 1,5 giây.
- Điều hướng menu:
  - Đường dẫn: /reset-password?token=...
  - Liên kết chuyển tiếp: "Quay lại đăng nhập" dẫn về /login.
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: patients, patient_password_history, in_app_notifications, email_outbox.
  - API phục vụ: POST /api/v1/auth/reset-password (nhận ResetPasswordRequest gồm token và newPassword).
- Điều kiện tiên quyết:
  - Người dùng truy cập từ đường dẫn có chứa token khôi phục hợp lệ và chưa hết hạn.

---

### 4. Màn hình Lịch sử Đăng nhập

- Tên màn: Lịch sử Đăng nhập (Login History - Cổng Bệnh nhân & Cổng Quản trị CMS).
- Luồng nghiệp vụ tổng quan: Cung cấp bảng theo dõi chi tiết các lượt đăng nhập và thao tác bảo mật gần đây của tài khoản ở cả hai cổng (Bệnh nhân và Quản trị CMS). Hệ thống tự động thống kê số lần thất bại trong 7 ngày gần nhất để phát cảnh báo bảo mật và gợi ý đổi mật khẩu kịp thời.
- Mô tả luồng nghiệp vụ:
  1. Người dùng truy cập màn hình từ thanh điều hướng bên trái:
     - Phía Bệnh nhân: Truy cập /login-history.
     - Phía Cán bộ CMS (Admin, Bác sĩ, Biên tập viên): Truy cập /cms/login-history.
  2. Cơ chế tự động co giãn phân trang theo màn hình (Viewport-Adaptive Pagination):
     - Ngay khi mở trang hoặc khi thay đổi kích thước cửa sổ trình duyệt, giao diện tự động tính toán số dòng hiển thị trên mỗi trang (từ 4 đến 20 dòng tùy theo chiều cao màn hình) để bảng dữ liệu luôn vừa khít khung nhìn mà không tạo khoảng trống thừa.
     - Sử dụng bộ đếm thứ tự truy vấn (requestId) để đảm bảo khi người dùng chuyển trang nhanh, kết quả của trang cũ trả về muộn không ghi đè lên trang hiện tại.
  3. Hệ thống hiển thị Bảng lịch sử đăng nhập gồm 5 cột thông tin:
     - Thời gian (attemptedAt): Hiển thị theo định dạng ngày/tháng/năm giờ:phút chuẩn tiếng Việt.
     - Thiết bị (deviceLabel): Hiển thị tên thiết bị và trình duyệt dễ đọc; khi di chuột vào sẽ hiển thị hộp chú thích (Tooltip) chứa nguyên vẹn chuỗi User-Agent gốc.
     - Địa chỉ IP (ipAddress): Hiển thị địa chỉ IP của thiết bị thực hiện truy cập.
     - Lý do thất bại (failureReason): Ghi rõ nguyên nhân nếu lần đăng nhập không thành công (ví dụ: Sai mật khẩu, Tài khoản đang bị khóa tạm thời); nếu thành công thì hiển thị dấu gạch ngang.
     - Trạng thái (outcome): Gắn nhãn màu trực quan theo 3 trạng thái:
       + Thành công (SUCCESS - nhãn xanh lá).
       + Thất bại (FAILURE - nhãn màu cam).
       + Đã chặn (BLOCKED - nhãn màu đỏ, xuất hiện khi tài khoản bị từ chối đăng nhập do đang trong thời gian khóa bảo vệ).
  4. Cảnh báo bảo mật chủ động (Security Warning Banner):
     - Backend tính toán và trả về chỉ số recentFailureCount (số lần đăng nhập hoặc đổi mật khẩu thất bại trong vòng 7 ngày qua).
     - Nếu recentFailureCount > 0, giao diện tự động hiển thị hộp cảnh báo màu vàng ở đầu trang: "Có hoạt động cần kiểm tra: X lần đăng nhập hoặc đổi mật khẩu không thành công trong 7 ngày qua. Nếu không phải bạn, hãy đổi mật khẩu ngay."
     - Hộp cảnh báo cung cấp 2 nút thao tác nhanh:
       + Nút "Đổi mật khẩu": Điều hướng trực tiếp tới trang Đổi mật khẩu (/change-password đối với bệnh nhân hoặc /cms/change-password đối với cán bộ CMS).
       + Nút "Đã hiểu": Ẩn hộp cảnh báo trong phiên làm việc hiện tại (lưu trạng thái vào sessionStorage).
- Điều hướng menu:
  - Phía Bệnh nhân: Sidebar chính -> Lịch sử đăng nhập (/login-history).
  - Phía CMS: Sidebar CMS -> Nhóm Tài khoản -> Lịch sử đăng nhập (/cms/login-history).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu:
    + Phía Bệnh nhân: PostgreSQL (bảng patient_login_history).
    + Phía CMS: MySQL (bảng admin_login_history).
  - API phục vụ:
    + GET /api/v1/auth/login-history?page={page}&size={size}
    + GET /api/v1/cms/auth/login-history?page={page}&size={size}
- Điều kiện tiên quyết:
  - Người dùng đã đăng nhập hợp lệ vào cổng Bệnh nhân hoặc cổng CMS.

---

### 5. Màn hình Phiên Chat của Tôi & Tư vấn Sức khỏe AI

- Tên màn: Phiên Chat của Tôi & Tư vấn Sức khỏe AI (My Chat Sessions & AI Health Consultation).
- Luồng nghiệp vụ tổng quan: Bao gồm màn hình quản lý lịch sử các cuộc hội thoại (/chat-history) và màn hình trò chuyện trực tiếp với trợ lý AI (/chat). Hệ thống cho phép bệnh nhân tra cứu lại toàn bộ nội dung tư vấn cũ, xuất biên bản hội thoại ra tệp PDF có cột trích dẫn nguồn y khoa, xóa phiên, hoặc nối tiếp cuộc trò chuyện cũ với quy trình xử lý 2 tầng: Sàng lọc cấp cứu Triage và Truy xuất tri thức RAG phản hồi thời gian thực qua SSE.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Luồng quản lý tại màn hình "Phiên chat của tôi" (/chat-history):
     - Khi mở trang, hệ thống tải danh sách tối đa 50 phiên trò chuyện gần nhất của bệnh nhân và bố trí theo giao diện 2 cột:
       + Cột trái (Danh sách phiên): Hiển thị tiêu đề phiên, thời gian cập nhật, nhãn cảnh báo nếu phiên có tin nhắn cấp cứu, nút Xuất PDF và nút Xóa phiên.
       + Cột phải (Nội dung phiên đang chọn): Hiển thị toàn bộ lịch sử tin nhắn người bệnh và phản hồi AI, bao gồm cả tệp đính kèm, các thẻ trích dẫn nguồn tài liệu y khoa và đánh giá Hữu ích / Không hữu ích.
     - Cơ chế chống đua dữ liệu (Request Sequence Guard): Khi người bệnh nhấp chuyển liên tiếp giữa các phiên ở cột trái, hệ thống đánh số thứ tự request để hủy bỏ kết quả trả về muộn của phiên trước, đảm bảo nội dung hiển thị ở cột phải luôn khớp chính xác với phiên đang chọn.
     - Thao tác Tiếp tục trò chuyện: Khi bấm nút "Tiếp tục trò chuyện", hệ thống điều hướng sang trang /chat kèm theo mã phiên (resumeSessionId) và tiêu đề phiên trong trạng thái điều hướng để người bệnh hỏi tiếp ngay trong mạch hội thoại cũ.
     - Thao tác Xuất PDF phiên chat: Người bệnh bấm biểu tượng tải PDF trên phiên chat, hệ thống gọi backend kết xuất tệp `phien-chat-{id}.pdf` bố cục 2 cột chuyên nghiệp (cột nội dung hội thoại và cột nguồn trích dẫn y khoa bên cạnh) về máy người dùng.
     - Thao tác Xóa phiên chat: Người bệnh bấm xóa phiên và xác nhận; hệ thống xóa phiên cùng toàn bộ tin nhắn, tệp đính kèm và đánh giá liên quan khỏi danh sách.
  2. Luồng hỏi đáp AI kết hợp Triage & RAG tại màn hình "Tư vấn sức khỏe" (/chat):
     - Người dùng nhập câu hỏi (hoặc chọn từ 4 gợi ý nhanh: Tư vấn triệu chứng sốt, Hỏi lịch uống thuốc, Đau bụng nên làm gì?, Chế độ ăn cho người đau dạ dày) và có thể đính kèm hình ảnh/tệp tài liệu.
     - TẦNG 1 - MÀNG LỌC AN TOÀN TRIAGE (Thực thi bắt buộc trước khi gọi AI):
       + Backend chuẩn hóa chuỗi văn bản (hỗ trợ cả tiếng Việt có dấu và không dấu) và đối chiếu với 10 nhóm dấu hiệu báo động đỏ:
         * XUAT_HUYET_TIEU_HOA (nôn ra máu, phân đen bã cà phê, đi cầu ra máu tươi).
         * DAU_BUNG_CAP_TINH (đau bụng dữ dội, đột ngột, bụng cứng như gỗ).
         * TAC_RUOT (không trung tiện được, chướng bụng căng kèm nôn).
         * SOC_MAT_MAU (ngất xỉu, hoa mắt chóng mặt dữ dội, vã mồ hôi lạnh).
         * NHIEM_TRUNG_NANG (đau bụng kèm sốt cao rét run, nghi viêm ruột thừa).
         * DAU_LAN_CO_QUAN_KHAC (đau bụng lan ra sau lưng, lên vai hoặc ngực).
         * KEM_HO_HAP_TIM_MACH (đau tức ngực, khó thở).
         * TIEU_DUONG_KEM_NON (bệnh nhân tiểu đường bị nôn liên tục).
         * VANG_DA_KEM_DAU_BUNG (vàng mắt, vàng da kèm đau bụng).
         * MAT_NUOC_NANG (tiêu chảy kéo dài gây lả người, tiểu rất ít, lơ mơ).
       + Nếu khớp dấu hiệu cấp cứu: Hệ thống gắn cờ `emergency = true`, lưu danh sách nhóm cảnh báo (`matchedGroups`), tự động khởi tạo cảnh báo khẩn cấp trong bảng `triage_alerts`, phát sự kiện WebSocket tới cổng CMS của bác sĩ và hiển thị ngay biểu ngữ cảnh báo đỏ yêu cầu người bệnh gọi 115 hoặc tới cơ sở y tế gần nhất.
     - TẦNG 2 - TRUY XUẤT TRI THỨC RAG & STREAMING SSE:
       + Hệ thống sinh vector embedding cho câu hỏi và truy vấn các đoạn văn bản y khoa tương đồng nhất từ bảng `document_chunks` (chỉ lấy từ các tài liệu có `published = true`).
       + Kết hợp ngữ cảnh hồ sơ bệnh lý cá nhân (`medical_profiles`) của người bệnh vào prompt của mô hình ngôn ngữ lớn.
       + Mở luồng truyền phát Server-Sent Events (SSE) qua endpoint `/chat/messages/stream` (hoặc `/chat/messages/with-attachments/stream` nếu có tệp đính kèm):
         * Ngay khi backend chấp nhận kết nối 2xx, giao diện chuyển trạng thái tin nhắn người dùng từ "Đang gửi" sang "Đã gửi".
         * Các cụm từ AI sinh ra được đẩy về liên tục qua sự kiện token và hiển thị trực tiếp lên màn hình.
         * Khi kết thúc (sự kiện `done`), hệ thống trả về đầy đủ danh sách nguồn tài liệu tham khảo (`sources` gồm tiêu đề tài liệu, đoạn trích `snippet`, đường dẫn `sourceUrl`) và các câu hỏi gợi ý liên quan (`relatedQuestions`).
     - Đánh giá câu trả lời AI: Dưới mỗi phản hồi của AI, bệnh nhân có thể bấm đánh giá Hữu ích (`HELPFUL`) hoặc Không hữu ích (`UNHELPFUL`); hệ thống cho phép đổi ý (cơ chế UPSERT vào bảng đánh giá).
- Điều hướng menu:
  - Sidebar chính: Mục "Lịch sử phiên chat" (/chat-history) và mục "Tư vấn sức khỏe" (/chat).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL (kèm tiện ích mở rộng pgvector)
  - Bảng dữ liệu: chat_sessions, chat_messages, chat_attachments, message_ratings, document_chunks, documents, triage_keywords, triage_alerts, medical_profiles.
  - API phục vụ:
    + GET /api/v1/chat/sessions (lấy danh sách phiên chat)
    + GET /api/v1/chat/sessions/{sessionId} (lấy chi tiết tin nhắn trong phiên)
    + GET /api/v1/chat/sessions/{sessionId}/pdf (xuất PDF phiên chat)
    + DELETE /api/v1/chat/sessions/{sessionId} (xóa phiên chat)
    + POST /api/v1/chat/messages/stream và POST /api/v1/chat/messages/with-attachments/stream (hỏi đáp SSE)
    + POST /api/v1/chat/messages/{messageId}/rating (đánh giá phản hồi AI)
- Điều kiện tiên quyết:
  - Người dùng đã đăng nhập với vai trò Bệnh nhân.

---

### 6. Màn hình Đánh giá Triệu chứng Tiêu hóa

- Tên màn: Đánh giá Triệu chứng Tiêu hóa Có Cấu trúc (Structured Symptom Assessment).
- Luồng nghiệp vụ tổng quan: Cung cấp công cụ sàng lọc triệu chứng tiêu hóa theo quy trình 3 bước chuẩn hóa. Hệ thống thu thập triệu chứng chính, thời gian, nhóm đối tượng, mức độ tự đánh giá, diễn tiến, mức ảnh hưởng sinh hoạt và 15 dấu hiệu cảnh báo; từ đó phân loại mức độ nghiêm trọng, giải thích rõ lý do phân loại và đưa ra khuyến nghị chăm sóc phù hợp.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Người bệnh mở trang /symptom-assessment từ nút "Đánh giá triệu chứng" trên màn hình Tư vấn sức khỏe.
  2. Giao diện hiển thị thanh tiến trình 3 bước (Steps):
     - BƯỚC 1 - TRIỆU CHỨNG:
       + Mục 1 - Triệu chứng chính (primarySymptom): Chọn 1 trong 8 nhóm: Đau bụng (`ABDOMINAL_PAIN`), Tiêu chảy (`DIARRHEA`), Táo bón (`CONSTIPATION`), Buồn nôn (`NAUSEA`), Nôn (`VOMITING`), Ợ nóng / trào ngược (`HEARTBURN`), Đầy bụng (`BLOATING`), hoặc Triệu chứng khác (`OTHER`). Nếu chọn Triệu chứng khác, hệ thống hiển thị thêm ô nhập mô tả chi tiết bắt buộc (tối đa 300 ký tự).
       + Mục 2 - Thời gian kéo dài (duration): Chọn 1 trong 5 mốc: Dưới 24 giờ (`LESS_THAN_24_HOURS`), 1–3 ngày (`ONE_TO_THREE_DAYS`), Trên 3 ngày (`MORE_THAN_THREE_DAYS`), Tái diễn nhiều lần (`RECURRING`), Không chắc (`UNSURE`).
       + Mục 3 - Đối tượng được đánh giá (patientGroup): Người trưởng thành (`ADULT`), Người dưới 18 tuổi (`UNDER_18`), Đang mang thai hoặc mới sinh (`PREGNANT_OR_RECENTLY_POSTPARTUM`), Không chắc (`UNSURE`).
     - BƯỚC 2 - MỨC ĐỘ:
       + Mục 1 - Mức độ tự đánh giá (reportedSeverity): Nhẹ (`MILD`), Trung bình (`MODERATE`), Nặng (`SEVERE`).
       + Mục 2 - Diễn tiến triệu chứng gần đây (progression): Đang giảm (`IMPROVING`), Không thay đổi (`STABLE`), Đang nặng hơn (`WORSENING`).
       + Mục 3 - Mức ảnh hưởng sinh hoạt (activityImpact): Không ảnh hưởng (`NONE`), Ảnh hưởng một phần (`SOME_LIMITATION`), Không thể sinh hoạt bình thường (`PREVENTS_NORMAL_ACTIVITY`).
     - BƯỚC 3 - DẤU HIỆU CẢNH BÁO (warningSigns):
       + Nhóm 1 - 13 Dấu hiệu cấp cứu (Emergency Warning Signs):
         1. Nôn ra máu hoặc chất nôn như bã cà phê (`BLOOD_IN_VOMIT`)
         2. Đi ngoài ra máu hoặc phân đen như hắc ín (`BLACK_OR_BLOODY_STOOL`)
         3. Đau bụng xuất hiện đột ngột hoặc dữ dội (`SUDDEN_SEVERE_ABDOMINAL_PAIN`)
         4. Bụng cứng hoặc đau nhiều khi chạm vào (`RIGID_OR_TENDER_ABDOMEN`)
         5. Không thể đi ngoài hoặc trung tiện (`UNABLE_TO_PASS_STOOL_OR_GAS`)
         6. Không thể đi tiểu (`UNABLE_TO_URINATE`)
         7. Khó thở hoặc đau/tức ngực (`BREATHING_DIFFICULTY_OR_CHEST_PAIN`)
         8. Ngất, lú lẫn hoặc khó đánh thức (`FAINTING_OR_CONFUSION`)
         9. Sốt cao kèm đau bụng (`HIGH_FEVER_WITH_ABDOMINAL_PAIN`)
         10. Đau bụng lan ra lưng hoặc vai (`PAIN_RADIATING_TO_BACK_OR_SHOULDER`)
         11. Đang mắc tiểu đường và bị nôn (`DIABETES_WITH_VOMITING`)
         12. Vàng da/vàng mắt kèm đau bụng (`JAUNDICE_WITH_ABDOMINAL_PAIN`)
         13. Tiểu rất ít kèm lả, lơ mơ hoặc khó đánh thức (`SEVERE_DEHYDRATION`)
         Nếu người bệnh tích chọn bất kỳ dấu hiệu nào trong nhóm này, giao diện lập tức hiển thị hộp cảnh báo đỏ nhắc người bệnh gọi 115 ngay mà không cần chờ hoàn tất biểu mẫu.
       + Nhóm 2 - 2 Dấu hiệu cần bác sĩ xem xét (Clinician Review Warning Signs):
         14. Sụt cân không rõ nguyên nhân (`UNEXPLAINED_WEIGHT_LOSS`)
         15. Khó nuốt hoặc đau khi nuốt (`DIFFICULT_OR_PAINFUL_SWALLOWING`)
  3. Xử lý phân loại lâm sàng và trả kết quả tại Backend:
     - Khi người bệnh bấm "Xem kết quả đánh giá", backend thực thi `SymptomAssessmentHistoryService.assessAndSave()`:
       + Xác định mức độ nghiêm trọng (`severityLevel`): `MILD` (Nhẹ), `MODERATE` (Trung bình), `SEVERE` (Nặng) hoặc `UNDETERMINED` (Chưa thể đánh giá - áp dụng khi đối tượng là trẻ dưới 18 tuổi, phụ nữ mang thai hoặc thông tin thời gian không rõ).
       + Xác định khuyến nghị chăm sóc (`careRecommendation`):
         * `EMERGENCY_CARE` (Cần cấp cứu ngay): Khi có ít nhất 1 dấu hiệu cấp cứu. Hệ thống đồng thời tự động tạo bản ghi trong bảng `triage_alerts` và phát thông báo khẩn tới bác sĩ trên cổng CMS.
         * `SEE_DOCTOR` (Nên gặp bác sĩ): Khi có dấu hiệu sụt cân/khó nuốt, triệu chứng mức độ nặng, cản trở sinh hoạt, đang nặng lên, kéo dài trên 3 ngày/tái diễn hoặc thuộc nhóm đối tượng đặc thù.
         * `SELF_CARE_AT_HOME` (Tự chăm sóc tại nhà): Khi triệu chứng nhẹ, ổn định hoặc đang giảm và không có bất kỳ dấu hiệu cảnh báo nào.
       + Sinh danh sách mã giải thích lý do (`reasonCodes`): Giải thích minh bạch trên giao diện vì sao hệ thống đưa ra kết quả đó (ví dụ: `WARNING_SIGNS_PRESENT`, `IMPACT_OR_WORSENING_ESCALATION`, `UNSUPPORTED_PATIENT_GROUP`).
  4. Từ màn hình kết quả, người bệnh có thể bấm "Đánh giá mới" để làm lại hoặc bấm "Tư vấn thêm với Trợ lý AI" để chuyển sang màn hình `/chat`.
- Điều hướng menu:
  - Truy cập từ nút "Đánh giá triệu chứng" trên thanh công cụ của màn hình Tư vấn sức khỏe (/chat) hoặc đường dẫn trực tiếp /symptom-assessment.
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: symptom_assessments, triage_alerts, patients.
  - API phục vụ: POST /api/v1/patient/triage/assessments (gửi đánh giá và lưu kết quả), GET /api/v1/patient/triage/assessments (lấy lịch sử đánh giá).
- Điều kiện tiên quyết:
  - Người dùng đã đăng nhập với vai trò Bệnh nhân.

---

### 7. Màn hình Bài viết Tiêu hóa (Thư viện Kiến thức Bệnh nhân)

- Tên màn: Bài viết Tiêu hóa - Thư viện Kiến thức Y khoa (Patient Knowledge Library).
- Luồng nghiệp vụ tổng quan: Cung cấp thư viện các bài viết y khoa chuyên ngành tiêu hóa đã được kiểm duyệt kèm bộ hình ảnh giải phẫu minh họa trực quan. Hỗ trợ tìm kiếm chính xác theo từ khóa trong tiêu đề bài viết, lọc theo chủ đề và xem chi tiết bài viết cùng nguồn tham chiếu chính thống.
- Mô tả luồng nghiệp vụ:
  1. Người bệnh truy cập mục "Bài viết tiêu hoá" (/articles).
  2. Hệ thống tải danh sách các chủ đề hiện có và danh sách bài viết đã xuất bản (phân trang 12 bài viết mỗi trang).
  3. Tìm kiếm và lọc bài viết:
     - Ô tìm kiếm từ khóa: Người dùng nhập từ khóa và nhấn nút "Tìm" (hoặc nhấn Enter). Backend thực hiện tìm kiếm chỉ so khớp trên trường tiêu đề bài viết (`title`) không phân biệt hoa thường, giúp kết quả trả về sát nghĩa tuyệt đối với từ khóa in đậm trên tiêu đề (ví dụ tìm "viêm" chỉ trả về các bài viêm dạ dày, không trả về bài loét dạ dày - tá tràng).
     - Nút xóa nhanh (biểu tượng dấu nhân trong ô tìm kiếm) và nút "Bỏ chọn": Khi có bất kỳ bộ lọc từ khóa hoặc chủ đề nào đang kích hoạt, nút "Bỏ chọn" sẽ xuất hiện để người dùng đặt lại toàn bộ bộ lọc và tải lại danh sách gốc chỉ với một cú nhấp chuột.
     - Bộ lọc chủ đề (Select): Cho phép chọn lọc bài viết theo từng chuyên đề tiêu hóa hoặc xem "Tất cả chủ đề".
  4. Hiển thị danh sách thẻ bài viết:
     - Mỗi thẻ bài viết hiển thị: Ảnh bìa minh họa y khoa vector (SVG) tương ứng với bệnh lý, nhãn chủ đề màu xanh, tiêu đề bài viết (đã được làm sạch hoàn toàn tiền tố [DEMO]) và đoạn tóm tắt nội dung.
  5. Xem chi tiết bài viết (/articles/:slug):
     - Khi nhấp vào một thẻ bài viết, hệ thống điều hướng tới `/articles/:slug`.
     - Hiển thị toàn văn bài viết định dạng Markdown với các hình ảnh minh họa y khoa được căn giữa trong khung `<figure>` kèm dòng chú thích `<figcaption>` rõ ràng bên dưới.
     - Khối "Nguồn tham khảo" ở cuối bài: Hiển thị tên nguồn kèm liên kết gốc (`sourceUrl`) và ngày kiểm tra đường dẫn nguồn gần nhất (`sourceCheckedAt`), đi kèm dòng khuyến cáo miễn trừ trách nhiệm y khoa.
- Điều hướng menu:
  - Sidebar chính: Mục "Bài viết tiêu hoá" (/articles) -> Nhấp vào thẻ bài viết để mở chi tiết (/articles/:slug).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: knowledge_articles, article_topics.
  - API phục vụ:
    + GET /api/v1/articles (tham số q, category, page, size)
    + GET /api/v1/articles/categories (lấy danh sách tên chủ đề)
    + GET /api/v1/articles/{slug} (lấy chi tiết bài viết theo slug)
- Điều kiện tiên quyết:
  - Người dùng đã đăng nhập vào cổng Bệnh nhân; bài viết phải có trạng thái `published = true`.

---

### 8. Màn hình Hỏi đáp Thường xuyên (FAQ Bệnh nhân)

- Tên màn: Câu hỏi Thường gặp về Tiêu hóa (Patient FAQ).
- Luồng nghiệp vụ tổng quan: Giúp bệnh nhân tra cứu nhanh các câu hỏi và giải đáp ngắn gọn, thiết thực về bệnh lý tiêu hóa, chế độ sinh hoạt và cách theo dõi triệu chứng đã được bác sĩ kiểm duyệt.
- Mô tả luồng nghiệp vụ:
  1. Người bệnh mở mục "Hỏi đáp thường gặp" (/faq).
  2. Hệ thống tải đồng thời danh sách các chủ đề FAQ và toàn bộ danh sách câu hỏi đã được xuất bản.
  3. Tìm kiếm và lọc câu hỏi:
     - Ô tìm kiếm: Cho phép nhập từ khóa để tìm kiếm câu hỏi liên quan; hỗ trợ nút xóa nhanh (dấu nhân) ngay trong ô nhập để xóa từ khóa và tải lại toàn bộ danh sách tức thì.
     - Bộ lọc chủ đề: Lọc câu hỏi theo nhóm bệnh lý và nút "Bỏ chọn" để xóa nhanh toàn bộ điều kiện lọc đang áp dụng.
  4. Hiển thị và tương tác danh sách câu hỏi:
     - Các câu hỏi được nhóm theo từng thẻ Chủ đề (Category Card).
     - Bên trong mỗi chủ đề, các câu hỏi được trình bày dưới dạng danh sách đóng/mở (Collapse / Accordion).
     - Khi người bệnh nhấp vào tiêu đề câu hỏi, khung nội dung trả lời mở rộng hiển thị văn bản Markdown chi tiết (các gạch đầu dòng, chữ in đậm hướng dẫn xử trí tại nhà và dấu hiệu cần đi khám).
- Điều hướng menu:
  - Sidebar chính: Mục "Hỏi đáp thường gặp" (/faq).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: faqs (chỉ lấy các bản ghi có `published = true`, sắp xếp theo `sort_order`), article_topics.
  - API phục vụ:
    + GET /api/v1/faqs (tham số q, category)
    + GET /api/v1/faqs/categories
- Điều kiện tiên quyết:
  - Người dùng đã đăng nhập vào cổng Bệnh nhân.

---

### 9. Màn hình Bài viết Kiến thức (Quản trị & Soạn thảo CMS)

- Tên màn: Quản lý & Soạn thảo Bài viết Kiến thức Y khoa (CMS Knowledge Articles & Article Editor).
- Luồng nghiệp vụ tổng quan: Cung cấp công cụ cho Quản trị viên, Bác sĩ và Biên tập viên nội dung quản lý toàn bộ bài viết trong kho tri thức dành cho bệnh nhân. Hỗ trợ quy trình biên tập Markdown, kiểm tra nguồn tham khảo, bắt buộc ghi nhận người duyệt y khoa trước khi xuất bản và kiểm soát xung đột chỉnh sửa đồng thời bằng Khóa Lạc quan (Optimistic Locking).
- Mô tả luồng nghiệp vụ chi tiết:
  1. Màn hình Danh sách bài viết (/cms/articles):
     - Bộ lọc và tìm kiếm:
       + Ô tìm kiếm theo tiêu đề bài viết (so khớp chính xác từ khóa trong tiêu đề, hỗ trợ nút xóa nhanh).
       + Bộ lọc theo Chủ đề bài viết (lấy từ danh mục chủ đề chuẩn).
       + Bộ lọc theo Trạng thái xuất bản: Tất cả trạng thái, Đã xuất bản, Bản nháp.
       + Nút "Bỏ chọn" xuất hiện khi có bộ lọc đang bật để đặt lại toàn bộ bộ lọc về mặc định.
     - Bảng danh sách hiển thị: Tiêu đề bài viết (kèm đường dẫn `/articles/{slug}`), Chủ đề, Trạng thái (Đã xuất bản / Bản nháp), Người duyệt y khoa (`medicalReviewer` và thời điểm duyệt `medicalReviewedAt`), Nguồn tham khảo (`sourceName`), Ngày cập nhật và cột Thao tác.
     - Quy trình Xuất bản / Ẩn bài viết nhanh trên bảng:
       + Khi bấm nút "Xuất bản" trên một bài viết đang ở trạng thái Bản nháp: Hệ thống mở hộp thoại yêu cầu nhập tên **Người duyệt y khoa** (mặc định điền sẵn tên bác sĩ/admin đang đăng nhập). Chỉ khi xác nhận người duyệt, bài viết mới được chuyển sang trạng thái Xuất bản.
       + Khi bấm nút "Ẩn" trên bài viết đang xuất bản: Bài viết được chuyển ngay về trạng thái Bản nháp và ẩn khỏi thư viện bệnh nhân.
  2. Màn hình Soạn thảo bài viết (/cms/articles/new và /cms/articles/:id/edit):
     - Biểu mẫu biên tập chi tiết gồm:
       + Tiêu đề bài viết (title) và Đường dẫn tĩnh (slug - tự động sinh từ tiêu đề nếu để trống).
       + Chủ đề (category): Chọn từ danh sách chủ đề chuẩn hóa.
       + Tóm tắt ngắn (summary): Đoạn mô tả hiển thị trên thẻ thư viện.
       + Nội dung chi tiết (content): Hỗ trợ cú pháp Markdown đầy đủ và chèn ảnh minh họa y khoa `/images/articles/*.svg`.
       + Thông tin nguồn kiểm chứng: Tên nguồn (`sourceName`), Đường dẫn nguồn (`sourceUrl` bắt buộc định dạng URL hợp lệ) và Ngày kiểm tra nguồn (`sourceCheckedAt`).
       + Thứ tự ưu tiên hiển thị (`sortOrder`).
       + Trạng thái xuất bản và tên Người duyệt y khoa.
  3. Kiểm soát xung đột bằng Khóa Lạc quan (Optimistic Locking):
     - Mỗi bản ghi bài viết đi kèm số phiên bản (`version`). Khi gửi yêu cầu cập nhật hoặc đổi trạng thái xuất bản, nếu `version` gửi lên thấp hơn `version` hiện có trong cơ sở dữ liệu (do người khác vừa sửa trước đó), backend trả về lỗi xung đột để ngăn ghi đè mất dữ liệu.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Kho tri thức -> Bài viết kiến thức (/cms/articles) -> Nút "Tạo bài viết mới" (/cms/articles/new) hoặc "Sửa" (/cms/articles/:id/edit).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL & MySQL (ghi nhật ký kiểm toán Audit Log)
  - Bảng dữ liệu: knowledge_articles, article_topics, audit_logs.
  - API phục vụ:
    + GET /api/v1/cms/articles (tham số q, category, published, page, size)
    + GET /api/v1/cms/articles/{id}
    + POST /api/v1/cms/articles
    + PUT /api/v1/cms/articles/{id}
    + PATCH /api/v1/cms/articles/{id}/publish
    + DELETE /api/v1/cms/articles/{id}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN, DOCTOR hoặc CONTENT_EDITOR.

---

### 10. Màn hình Chủ đề Bài viết (Quản trị CMS)

- Tên màn: Quản lý Chủ đề Bài viết (CMS Article Topics).
- Luồng nghiệp vụ tổng quan: Quản lý danh mục các chủ đề phân loại dùng chung cho Bài viết kiến thức và Câu hỏi thường gặp (FAQ). Đảm bảo tính nhất quán của cây danh mục và bảo vệ toàn vẹn dữ liệu khi đổi tên hoặc xóa chủ đề.
- Mô tả luồng nghiệp vụ:
  1. Người dùng truy cập /cms/article-topics từ thanh điều hướng CMS.
  2. Bảng danh sách hiển thị các chủ đề hiện có, sắp xếp theo thứ tự ưu tiên:
     - Tên chủ đề (`name`): Ví dụ Dạ dày, Thực quản, Đại tràng, Triệu chứng thường gặp, Chế độ ăn & lối sống.
     - Thứ tự hiển thị (`sortOrder`): Số nguyên quy định vị trí xuất hiện của chủ đề trên bộ lọc bệnh nhân.
     - Số bài viết đang sử dụng (`articleCount`): Đếm chính xác số bài viết đang gắn với chủ đề này.
     - Các nút thao tác: Sửa chủ đề và Xóa chủ đề.
  3. Thêm mới và Chỉnh sửa chủ đề:
     - Nhấn nút "Thêm chủ đề", nhập Tên chủ đề và Thứ tự hiển thị vào hộp thoại.
     - Khi đổi tên một chủ đề đã có bài viết sử dụng: Hệ thống tự động cập nhật đồng bộ tên chủ đề mới cho các bài viết liên quan và kiểm tra khóa lạc quan (`version`).
  4. Ràng buộc an toàn khi Xóa chủ đề:
     - Nếu chủ đề đang có bài viết sử dụng (`articleCount > 0`), hệ thống khóa thao tác xóa (hoặc trả về lỗi 409 Conflict từ backend) kèm thông báo yêu cầu chuyển các bài viết sang chủ đề khác trước khi xóa.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Kho tri thức -> Chủ đề bài viết (/cms/article-topics).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL & MySQL (Audit Log)
  - Bảng dữ liệu: article_topics, knowledge_articles, audit_logs.
  - API phục vụ:
    + GET /api/v1/cms/article-topics
    + POST /api/v1/cms/article-topics
    + PUT /api/v1/cms/article-topics/{id}
    + DELETE /api/v1/cms/article-topics/{id}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN, DOCTOR hoặc CONTENT_EDITOR.

---

### 11. Màn hình Quản lý Câu hỏi Thường gặp (FAQ Quản trị CMS)

- Tên màn: Quản lý Câu hỏi Thường gặp (CMS FAQ Management).
- Luồng nghiệp vụ tổng quan: Cho phép đội ngũ y tế và quản trị viên biên soạn, phân loại, duyệt y khoa và điều khiển trạng thái xuất bản của ngân hàng câu hỏi thường gặp hiển thị cho bệnh nhân.
- Mô tả luồng nghiệp vụ:
  1. Người dùng mở trang /cms/faq.
  2. Công cụ tìm kiếm và lọc:
     - Tìm kiếm theo nội dung câu hỏi hoặc câu trả lời.
     - Lọc theo Chủ đề / Danh mục và lọc theo Trạng thái (Đang xuất bản / Bản nháp).
  3. Bảng dữ liệu hiển thị:
     - STT / Thứ tự ưu tiên (`sortOrder`).
     - Câu hỏi & Nhãn chủ đề (`question`, `category`).
     - Câu trả lời tóm lược (`answer` - hỗ trợ hiển thị định dạng Markdown trực tiếp trên bảng).
     - Xuất bản & Kiểm duyệt: Hiển thị nhãn trạng thái, tên người duyệt (`medicalReviewer`) và nút chuyển nhanh Xuất bản / Ẩn.
     - Thời gian cập nhật gần nhất và các nút Xem chi tiết, Chỉnh sửa, Xóa.
  4. Thêm mới, Chỉnh sửa và Duyệt xuất bản:
     - Biểu mẫu hộp thoại cho phép nhập Câu hỏi, chọn Chủ đề, nhập Nội dung trả lời Markdown, Thứ tự sắp xếp và bật/tắt Xuất bản.
     - Quy tắc kiểm duyệt y khoa bắt buộc: Khi bật trạng thái Xuất bản (dù trong form tạo mới, chỉnh sửa hay bấm nút Xuất bản nhanh trên bảng), hệ thống bắt buộc phải có thông tin Người duyệt y khoa (`medicalReviewer`).
     - Hệ thống áp dụng kiểm tra phiên bản (`version`) để chống ghi đè khi nhiều người cùng biên tập một câu hỏi.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Kho tri thức -> Câu hỏi thường gặp (/cms/faq).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL & MySQL (Audit Log)
  - Bảng dữ liệu: faqs, article_topics, audit_logs.
  - API phục vụ:
    + GET /api/v1/cms/faqs (tham số q, category, published, page, size)
    + GET /api/v1/cms/faqs/categories
    + GET /api/v1/cms/faqs/{id}
    + POST /api/v1/cms/faqs
    + PUT /api/v1/cms/faqs/{id}
    + PATCH /api/v1/cms/faqs/{id}/publish
    + DELETE /api/v1/cms/faqs/{id}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN, DOCTOR hoặc CONTENT_EDITOR.

---

### 12. Màn hình Danh mục Chuẩn hóa Y khoa (Quản trị CMS)

- Tên màn: Quản lý Danh mục Chuẩn hóa Y khoa (CMS Standardized Catalogs).
- Luồng nghiệp vụ tổng quan: Quản lý bộ từ điển danh mục chuẩn hóa gồm 3 nhóm cốt lõi: Triệu chứng (`SYMPTOM`), Bệnh lý nền / Tiền sử (`CONDITION`) và Thuốc điều trị (`MEDICATION`). Màn hình giải thích rõ nguồn gốc, ý nghĩa của từng mã định danh chuẩn hóa và cho phép bác sĩ chủ động tạo thêm mã định danh mới ngay trên giao diện người dùng (UI) mà không cần can thiệp vào cơ sở dữ liệu hay sửa mã nguồn.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Người dùng truy cập /cms/catalogs.
  2. Giao diện cung cấp:
     - Hộp hướng dẫn quy tắc mã chuẩn hóa: Giải thích rõ cách thức các mã định danh liên kết với Hồ sơ bệnh lý, Nhật ký ăn uống và bộ máy sàng lọc Triage.
     - Các tab chuyển đổi nhanh giữa 3 phân loại: Tất cả, Triệu chứng (`SYMPTOM`), Bệnh lý nền (`CONDITION`), Thuốc (`MEDICATION`).
     - Thanh tìm kiếm theo tên hoặc mã code và bộ lọc trạng thái (Đang hoạt động / Đã ẩn).
  3. Bảng danh sách hiển thị:
     - Mã chuẩn hóa (`code`): Mã định danh viết hoa nối gạch dưới (ví dụ: `EPIGASTRIC_PAIN`, `GERD`, `OMEPRAZOLE`), đi kèm nhãn giải nghĩa và màn hình nghiệp vụ đang sử dụng mã đó.
     - Tên hiển thị tiếng Việt (`name`): Tên chuẩn y khoa hiển thị cho bệnh nhân chọn trên ứng dụng.
     - Phân loại (`type`): Nhãn màu phân biệt Triệu chứng, Bệnh lý hoặc Thuốc.
     - Mã liên kết Triage (`triageCode`): Dành riêng cho nhóm Triệu chứng có tính chất báo động đỏ, liên kết trực tiếp tới 10 nhóm cảnh báo cấp cứu của hệ thống (ví dụ: `XUAT_HUYET_TIEU_HOA`, `DAU_BUNG_CAP_TINH`).
     - Thứ tự sắp xếp (`sortOrder`), Trạng thái hoạt động (công tắc bật/tắt trực tiếp) và nút Chỉnh sửa / Xóa.
  4. Luồng Thêm / Sửa mục danh mục và Tạo mã chuẩn hóa mới trên UI:
     - Khi bấm "Thêm danh mục mới" (hoặc "Sửa"), hộp thoại mở ra gồm:
       + Phân loại danh mục (`SYMPTOM`, `CONDITION`, `MEDICATION`).
       + Trường chọn **Mã chuẩn hóa (Code định danh)**: Hiển thị danh sách thả xuống (Dropdown) liệt kê các mã chuẩn hóa kèm thẻ Tag phân loại, ý nghĩa lâm sàng ngắn gọn và màn hình áp dụng.
       + Nút **"Thêm mã mới"** nằm ngay bên cạnh trường chọn mã:
         * Khi bác sĩ muốn bổ sung một mã hoàn toàn mới chưa có trong danh sách gợi ý, bác sĩ nhấn nút "Thêm mã mới".
         * Một hộp thoại con mở ra cho phép bác sĩ nhập: Nhóm danh mục (Tag), Mã chuẩn hóa định danh (tự động chuẩn hóa chữ in hoa `UPPER_SNAKE_CASE`), Ý nghĩa ngắn gọn và Màn hình sử dụng (ví dụ: Hồ sơ bệnh lý & Tư vấn AI).
         * Sau khi bấm lưu, mã mới được bổ sung ngay vào từ điển chọn mã, tự động điền vào biểu mẫu đang mở và lưu trữ bền vững để sử dụng cho các lần sau.
       + Trường chọn **Mã cảnh báo Triage (`triageCode`)**: Chỉ hiển thị khi phân loại là Triệu chứng (`SYMPTOM`), cho phép chọn từ danh sách 10 mã nhóm cấp cứu chuẩn của TriageService.
     - Khi bấm Lưu: Backend kiểm tra tính duy nhất của cặp `(type, code)`, lưu vào bảng `standardized_catalogs` và cập nhật ngay danh sách hiển thị trên các biểu mẫu phía bệnh nhân.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Kho tri thức -> Danh mục chuẩn hóa (/cms/catalogs).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL & MySQL (Audit Log)
  - Bảng dữ liệu: standardized_catalogs, audit_logs.
  - API phục vụ:
    + GET /api/v1/cms/catalogs (tham số type, q, active)
    + GET /api/v1/cms/catalogs/{id}
    + POST /api/v1/cms/catalogs
    + PUT /api/v1/cms/catalogs/{id}
    + PATCH /api/v1/cms/catalogs/{id}/toggle-active
    + DELETE /api/v1/cms/catalogs/{id}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN, DOCTOR hoặc CONTENT_EDITOR.

---

### 13. Màn hình Đánh giá Chất lượng AI RAG và Triage (Quản trị CMS)

- Tên màn: Đánh giá Định lượng Chất lượng AI RAG & Độ nhạy Triage (CMS AI & Triage Evaluation).
- Luồng nghiệp vụ tổng quan: Công cụ thực nghiệm và kiểm định chất lượng định lượng cho hai thành phần cốt lõi của hệ thống là Bộ sàng lọc an toàn y khoa (Triage) và Bộ truy xuất tri thức y khoa (RAG). Màn hình cho phép chạy tự động các bộ kịch bản kiểm thử chuẩn (Golden Benchmark Datasets), tính toán các chỉ số thống kê khoa học và hiển thị chi tiết từng ca kiểm thử để bác sĩ cùng kỹ sư AI đánh giá độ an toàn của hệ thống.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Người dùng (Admin hoặc Bác sĩ) mở trang /cms/evaluation.
  2. Khối Thẻ Tóm tắt Tổng quan ở đầu trang tải dữ liệu từ `/cms/evaluation/summary`, hiển thị nhanh các chỉ số quan trọng nhất của lần đánh giá gần nhất và thời điểm thực hiện.
  3. Màn hình cung cấp 2 tab chuyên sâu:

  A. TAB 1 - ĐÁNH GIÁ ĐỘ NHẠY TRIAGE (TRIAGE SENSITIVITY EVALUATION):
     - Mục tiêu lâm sàng: Đảm bảo lớp Triage nhận diện đầy đủ tối thiểu 5 nhóm dấu hiệu cấp cứu tiêu hóa phổ biến với độ nhạy (Recall) rất cao nhằm hạn chế tối đa rủi ro bỏ sót ca nguy hiểm (False Negative).
     - Bộ dữ liệu kiểm thử chuẩn (`BENCHMARK_CASES` gồm 32 kịch bản):
       + Nhóm ca cấp cứu thực sự (`expectedEmergency = true`): Bao phủ các tình huống xuất huyết tiêu hóa (nôn ra máu tươi, phân đen như bã cà phê, cả câu có dấu và câu gõ không dấu tiếng Việt), đau bụng cấp tính dữ dội, bụng cứng như gỗ, tắc ruột không trung tiện được, bí tiểu cấp, sốc mất máu/ngất xỉu, sốt cao kèm đau bụng, đau ngực khó thở, vàng da kèm đau bụng.
       + Nhóm ca thông thường (`expectedEmergency = false`): Hỏi chế độ ăn, đầy hơi nhẹ sau ăn, hỏi cách dùng thuốc, táo bón nhẹ.
       + Nhóm ca biên dễ gây báo động giả (Boundary / Distractor cases): Chứa các từ nhạy cảm như "máu", "sốt", "đau" nhưng trong ngữ cảnh an toàn (ví dụ hỏi xét nghiệm máu, sốt nhẹ đã hạ).
     - Quy trình thực thi và Công thức đo lường:
       + Khi nhấn nút "Chạy đánh giá lại Triage", backend đưa toàn bộ các câu kiểm thử qua `TriageService` (đồng bộ `synchronized` chống xung đột luồng) và phân loại từng ca vào 4 nhóm của Ma trận nhầm lẫn (Confusion Matrix):
         * Dương tính thật (`TP` - True Positive): Ca cấp cứu được nhận diện đúng là cấp cứu.
         * Âm tính thật (`TN` - True Negative): Ca thông thường được nhận diện đúng là không cấp cứu.
         * Dương tính giả (`FP` - False Positive): Ca thông thường nhưng bị cảnh báo nhầm là cấp cứu.
         * Âm tính giả (`FN` - False Negative): Ca cấp cứu thực sự nhưng bị bỏ sót (chỉ số nguy hiểm nhất cần đưa về 0).
       + Hệ thống tính toán bộ 7 chỉ số đánh giá:
         * Độ nhạy (Sensitivity / Recall):
           $$\text{Recall} = \frac{TP}{TP + FN}$$
         * Tỷ lệ báo động giả (False Alarm Rate / FPR):
           $$\text{FAR} = \frac{FP}{FP + TN}$$
         * Độ đặc hiệu (Specificity):
           $$\text{Specificity} = \frac{TN}{TN + FP}$$
         * Độ chuẩn xác (Precision):
           $$\text{Precision} = \frac{TP}{TP + FP}$$
         * Độ chính xác tổng thể (Accuracy), Điểm cân bằng F1-Score và đặc biệt là **Điểm F2-Score** (công thức đặt trọng số cho độ nhạy Recall cao gấp đôi Precision theo đúng nguyên tắc an toàn y tế):
           $$F_2 = \frac{5 \cdot \text{Precision} \cdot \text{Recall}}{4 \cdot \text{Precision} + \text{Recall}}$$
     - Hiển thị kết quả trực quan:
       + Bảng thống kê độ nhạy theo từng nhóm bệnh lý cấp cứu (`categoryMetrics`).
       + Bảng chi tiết từng câu kiểm thử kèm bộ lọc nhanh theo kết quả phân loại (`ALL`, `TP`, `TN`, `FP`, `FN`) và ô tìm kiếm câu hỏi để bác sĩ rà soát ngay các câu bị nhầm lẫn.

  B. TAB 2 - ĐÁNH GIÁ ĐỘ CHÍNH XÁC TRUY XUẤT RAG (RAG RETRIEVAL EVALUATION):
     - Mục tiêu kỹ thuật: Đo lường khả năng tìm đúng đoạn tài liệu y khoa liên quan trong kho tri thức đối với tập 20 câu hỏi chuyên ngành tiêu hóa chuẩn (`BENCHMARK_QUERIES`).
     - Cơ chế thực thi thông minh & Fast-Fallback chống Timeout:
       + Người dùng có thể chọn tham số cấu hình số lượng đoạn trích xuất `Top-K` (Top 3, Top 5, Top 10) và nhấn nút "Chạy đánh giá RAG". Thời gian chờ tối đa phía client được nâng lên 120 giây cho tác vụ đánh giá.
       + Hệ thống gọi `GeminiEmbeddingClient` để nhúng câu hỏi sang vector và tìm kiếm tương đồng Cosine trong `EmbeddingStore` (pgvector).
       + Cơ chế tự động chuyển đổi dự phòng (Fast-Fallback): Nếu dịch vụ Gemini API chưa cấu hình khóa hoặc trả về lỗi xác thực/mạng ngay ở câu hỏi đầu tiên, hệ thống lập tức ngắt việc gọi API tuần tự (tránh làm treo luồng máy chủ suốt 20 câu hỏi) và tự động chuyển sang chế độ truy xuất từ khóa toàn văn (`LEXICAL_FALLBACK`) trên các đoạn tài liệu trong PostgreSQL, hoàn tất đánh giá chỉ trong vài chục mili-giây kèm thông báo rõ chế độ truy xuất (`retrievalMode`) và cảnh báo (`warning`) trên giao diện.
     - Các chỉ số Truy xuất Thông tin (Information Retrieval Metrics) được tính toán:
       + Hit Rate @ 1, Hit Rate @ 3, Hit Rate @ K: Tỷ lệ phần trăm câu hỏi có chứa ít nhất một đoạn tài liệu đúng kỳ vọng nằm trong Top 1, Top 3 và Top K kết quả đầu tiên.
       + Mean Reciprocal Rank (MRR): Trung bình nghịch đảo thứ hạng của kết quả đúng đầu tiên:
         $$MRR = \frac{1}{|Q|} \sum_{i=1}^{|Q|} \frac{1}{\text{rank}_i}$$
       + Precision @ K và Recall @ K trung bình.
       + Độ tương đồng Cosine trung bình (Average Similarity) và Độ trễ truy xuất trung bình (Average Latency tính bằng mili-giây).
     - Bảng kết quả từng truy vấn: Cho phép lọc theo trạng thái Tìm thấy (`HIT`) hoặc Không tìm thấy (`MISS`), bấm mở rộng từng dòng để xem chi tiết danh sách các đoạn văn bản (`retrievedChunks`), tên tài liệu gốc, thứ hạng (`rank`) và điểm tương đồng của từng đoạn.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Giám sát -> Đánh giá AI & Triage (/cms/evaluation).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: documents, document_chunks, knowledge_articles, triage_keywords.
  - API phục vụ:
    + GET /api/v1/cms/evaluation/summary
    + GET /api/v1/cms/evaluation/triage
    + POST /api/v1/cms/evaluation/triage/run
    + GET /api/v1/cms/evaluation/rag
    + POST /api/v1/cms/evaluation/rag/run?topK={topK}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN hoặc DOCTOR.

---

### 14. Màn hình Phát Thông báo Hệ thống Broadcast (Quản trị CMS)

- Tên màn: Quản lý Thông báo Hệ thống Broadcast (CMS System Broadcast).
- Luồng nghiệp vụ tổng quan: Cho phép Quản trị viên phát thông báo chung tới toàn bộ bệnh nhân hoặc từng nhóm bệnh nhân mục tiêu (thông báo lịch bảo trì, cập nhật tài liệu y khoa mới, khuyến cáo sức khỏe theo mùa), hỗ trợ xem trước trực tiếp nội dung và tự động lọc theo cài đặt quyền riêng tư của người bệnh.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Quản trị viên truy cập trang /cms/broadcast.
  2. Màn hình chính hiển thị:
     - Thẻ hướng dẫn cơ chế hoạt động của thông báo Broadcast.
     - Bảng lịch sử các đợt thông báo đã phát: Hiển thị Mã ID, Tiêu đề & Nội dung rút gọn, Nhóm đối tượng nhận, Số lượng người nhận thực tế (`recipientCount`), Thời gian gửi và nút Xem chi tiết.
  3. Tạo và Phát thông báo Broadcast mới:
     - Quản trị viên nhấn nút "Tạo thông báo Broadcast", hộp thoại mở ra gồm:
       + Tiêu đề thông báo (`title`): Bắt buộc nhập, độ dài từ 5 đến 200 ký tự.
       + Nội dung thông báo (`message`): Bắt buộc nhập, độ dài từ 10 đến 2000 ký tự.
       + Nhóm đối tượng nhận (`targetGroup`): Chọn Toàn bộ bệnh nhân (`ALL`), Bệnh nhân đã xác thực email (`VERIFIED`), hoặc Nhóm bệnh nhân hoạt động (`ACTIVE`).
       + Đường dẫn điều hướng khi nhấn vào thông báo (`actionUrl` - tùy chọn): Ví dụ `/articles` hoặc `/medications` để đưa bệnh nhân tới thẳng tính năng liên quan khi họ nhấp vào thông báo.
       + Công tắc "Gửi thông báo đẩy tức thời (WebSocket)" (`sendWebSocket`): Mặc định bật.
       + Khung Xem trước hiển thị (Live Preview): Cập nhật theo thời gian thực ngay khi Admin gõ phím, mô phỏng chính xác giao diện thẻ thông báo mà bệnh nhân sẽ nhìn thấy.
  4. Xử lý phân phối thông báo tại Backend:
     - Khi nhấn "Phát thông báo ngay", `BroadcastNotificationService.sendBroadcast()` thực hiện:
       + Lấy danh sách các bệnh nhân đã xác thực email trong bảng `patients`.
       + Đối chiếu hàng loạt (Batch Filter) với bảng cài đặt thông báo `patient_notification_settings`: Chỉ giữ lại những bệnh nhân đang bật tùy chọn cho phép nhận thông báo hệ thống (`system_broadcast_enabled = true`).
       + Lưu bản ghi gốc vào bảng `broadcast_announcements` kèm tổng số người nhận thực tế (`recipient_count`).
       + Khởi tạo danh sách thông báo thuộc loại `SYSTEM` trong bảng `in_app_notifications` cho toàn bộ bệnh nhân đủ điều kiện.
       + Phát sự kiện `NotificationCreatedEvent` để đẩy thông báo tức thời qua WebSocket tới các bệnh nhân đang trực tuyến.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Giám sát -> Thông báo broadcast (/cms/broadcast).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL & MySQL (Audit Log)
  - Bảng dữ liệu: broadcast_announcements, in_app_notifications, patient_notification_settings, patients, audit_logs.
  - API phục vụ:
    + GET /api/v1/cms/notifications/broadcast (tham số page, size)
    + POST /api/v1/cms/notifications/broadcast
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN.

---

### 15. Màn hình Bảng điều khiển Thống kê Hệ thống (Dashboard Quản trị CMS)

- Tên màn: Bảng điều khiển Thống kê Hệ thống (CMS System Dashboard).
- Luồng nghiệp vụ tổng quan: Tổng hợp và trực quan hóa toàn bộ số liệu vận hành của hệ thống GastroAI theo khoảng thời gian tùy chọn (7 ngày, 14 ngày, 30 ngày, 90 ngày). Cung cấp số liệu về lưu lượng hội thoại AI, tần suất triệu chứng tiêu hóa được hỏi nhiều nhất, tình hình xử lý cảnh báo cấp cứu Triage, mức độ hài lòng của người bệnh và chức năng kết xuất báo cáo ra tệp Excel hoặc PDF.
- Mô tả luồng nghiệp vụ chi tiết:
  1. Quản trị viên mở trang /cms/dashboard.
  2. Thanh công cụ góc trên bên phải cho phép:
     - Chọn khoảng thời gian thống kê (`days`): 7 ngày qua, 14 ngày qua, 30 ngày qua (mặc định), hoặc 90 ngày qua.
     - Nút "Làm mới" để tải lại số liệu tức thời.
     - Nút thả xuống "Xuất báo cáo": Cho phép chọn xuất báo cáo thống kê ra tệp **Excel (`.xlsx`)** hoặc **PDF (`.pdf`)**.
  3. Hệ thống trình bày dữ liệu qua 4 hàng thẻ KPI và các biểu đồ chuyên sâu:
     - Khối 4 Thẻ Chỉ số Tổng quan (KPI Cards):
       + Tổng người dùng bệnh nhân (`totalPatients`) và tổng số phiên hội thoại (`totalSessions`).
       + Tổng số tin nhắn (`totalMessages`) kèm số tin nhắn phát sinh riêng trong ngày hôm nay (`todayMessages`).
       + Tổng số cảnh báo khẩn cấp Triage (`totalAlerts`), phân tách rõ số ca Mới (`newAlerts`), Đang xử lý (`inProgressAlerts`) và Đã xử lý (`resolvedAlerts`).
       + Tỷ lệ hài lòng câu trả lời AI (`satisfactionRate` tính theo %), kèm số lượt đánh giá Hữu ích (`helpfulRatings`) và Chưa hữu ích (`unhelpfulRatings`), cùng tổng số lượt đánh giá triệu chứng (`totalAssessments`).
     - Biểu đồ Xu hướng Tin nhắn theo Ngày (Area Chart - `dailyChats`):
       + Biểu diễn song song tổng số tin nhắn của bệnh nhân, tin nhắn của AI và các tin nhắn kích hoạt cảnh báo cấp cứu qua từng ngày trong chu kỳ đã chọn.
     - Biểu đồ & Bảng Tần suất Triệu chứng Tiêu hóa (`topSymptoms`):
       + Thống kê các triệu chứng tiêu hóa được người bệnh đề cập hoặc đánh giá nhiều nhất (mã triệu chứng, tên triệu chứng, tổng số lượt, số lượt cấp cứu và tỷ lệ phần trăm chiếm dụng).
     - Biểu đồ Tròn & Bảng Phân bổ Cảnh báo Triage (`triageGroups` và `triageStatuses`):
       + Phân tích tỷ lệ phần trăm các nhóm dấu hiệu báo động đỏ được kích hoạt nhiều nhất (Xuất huyết tiêu hóa, Đau bụng cấp, Tắc ruột...) và tỷ lệ hoàn thành xử lý cảnh báo của đội ngũ bác sĩ.
  4. Luồng Xuất báo cáo PDF / Excel:
     - Khi Quản trị viên chọn định dạng xuất, backend gọi `DashboardExportService` tạo tệp báo cáo nhị phân (`GastroAI-Dashboard-Report-{days}d-{yyyyMMdd}.xlsx` hoặc `.pdf`) chứa đầy đủ bảng số liệu tổng hợp và trả về trình duyệt để tải xuống máy tự động.
- Điều hướng menu:
  - Sidebar CMS: Nhóm Giám sát -> Thống kê hệ thống (/cms/dashboard).
- Nguồn dữ liệu liên quan:
  - Cơ sở dữ liệu: PostgreSQL
  - Bảng dữ liệu: patients, chat_sessions, chat_messages, message_ratings, triage_alerts, symptom_assessments.
  - API phục vụ:
    + GET /api/v1/cms/dashboard/stats?days={days}
    + GET /api/v1/cms/dashboard/export?format={EXCEL|PDF}&days={days}
- Điều kiện tiên quyết:
  - Đăng nhập cổng CMS với vai trò ADMIN.
