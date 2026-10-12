# Báo cáo sửa lỗi UI sau kiểm thử

Ngày cập nhật: 2026-09-30

## Đã sửa

1. **Hồ sơ bệnh lý**
   - Đổi khung nội dung từ `max-w-2xl` sang `max-w-4xl`, đồng nhất với các trang dashboard theo dõi sức khỏe.
   - Các trường tags nhận dấu phẩy làm dấu phân tách và hiển thị hướng dẫn nhập khi danh sách gợi ý rỗng; người dùng vẫn có thể gõ giá trị rồi nhấn Enter.

2. **Ghi nhận Bristol**
   - Ép `Radio.Group` dùng flex column bằng inline style trực tiếp để CSS của Ant Design không làm loại 6 trôi lên cùng hàng loại 5.

3. **Nhắc uống thuốc**
   - Backend trả thêm `confirmedToday` cho từng reminder dựa trên lịch sử xác nhận trong ngày theo múi giờ Việt Nam.
   - Sau khi ghi nhận thành công, UI cập nhật ngay dòng tương ứng thành “Đã xác nhận hôm nay” và khóa nút.
   - Khi tải lại trang, reminder đã xác nhận trong ngày vẫn bị khóa; backend vẫn giữ ràng buộc chống ghi nhận lặp để bảo vệ khi gọi API trực tiếp.

## Kiểm tra

- Backend: 65 test, 0 lỗi.
- Frontend production build: thành công.
- Frontend lint: thành công; chỉ còn các cảnh báo React có sẵn, không có lỗi compile.
