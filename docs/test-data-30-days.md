# Bộ dữ liệu kiểm thử 30 ngày

Flyway tự nạp bộ dữ liệu này khi backend khởi động trên một database chưa chạy các migration:

- PostgreSQL: `V46__seed_comprehensive_30_day_test_data.sql`
- MySQL: `V9__seed_comprehensive_30_day_test_data.sql`

Mật khẩu chung của tất cả tài khoản dưới đây là `Passw0rd!`.

## Tài khoản bệnh nhân

Đăng nhập tại `/login`.

| Email | Họ tên |
| --- | --- |
| `gastroai.patient01@gmail.com` | Nguyễn Minh An |
| `gastroai.patient02@gmail.com` | Trần Thu Bình |
| `gastroai.patient03@gmail.com` | Lê Hoàng Chi |
| `gastroai.patient04@gmail.com` | Phạm Gia Dũng |
| `gastroai.patient05@gmail.com` | Võ Ngọc Hà |

Mỗi bệnh nhân có dữ liệu trong 30 ngày gần nhất:

- 30 lịch sử đăng nhập;
- 90 bữa ăn, tương ứng 3 bữa/ngày;
- 30 bản ghi Bristol;
- 2 lịch nhắc thuốc với 3 giờ uống/ngày và các liều đã uống/bỏ lỡ;
- 6 phiên chat, 12 tin nhắn và đánh giá phản hồi AI;
- 5 lần đánh giá triệu chứng;
- 9 thông báo với trạng thái đã đọc, chưa đọc và đã xóa;
- 1 cảnh báo triage ở các trạng thái khác nhau.

## Tài khoản CMS

Đăng nhập tại `/cms/login`. Tài khoản biên tập chọn loại đăng nhập **Admin** vì được lưu trong bảng `admins`.

| Loại đăng nhập | Email | Quyền |
| --- | --- | --- |
| Admin | `gastroai.admin01@gmail.com` | `ADMIN` |
| Admin | `gastroai.editor01@gmail.com` | `CONTENT_EDITOR` |
| Bác sĩ | `gastroai.doctor01@gmail.com` | `DOCTOR` |
| Bác sĩ | `gastroai.doctor02@gmail.com` | `DOCTOR` |
| Bác sĩ | `gastroai.doctor03@gmail.com` | `DOCTOR` |

Mỗi tài khoản CMS có ít nhất một lịch sử đăng nhập cho từng ngày trong 30 ngày gần nhất,
bao gồm cả lần thành công và thất bại để kiểm thử cảnh báo bảo mật và bộ lọc.

## Dữ liệu dùng chung

Bộ seed còn bổ sung 12 FAQ và 8 bài viết đã duyệt để kiểm thử tìm kiếm, phân trang và quản lý nội dung.
Các tài liệu RAG, embedding, danh mục và từ khóa triage tiếp tục dùng dữ liệu từ các migration có sẵn;
migration này không tạo embedding giả hoặc tệp đính kèm không tồn tại trên ổ đĩa.

