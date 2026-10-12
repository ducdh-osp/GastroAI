# Báo cáo sửa lỗi mức trung bình: dữ liệu sai lệch và validation

Ngày cập nhật: 2026-09-30

## Đã sửa

1. **Cửa sổ trend Bristol không còn là rolling 24 giờ**
   - Tách helper `VietnamDateRange` dùng múi giờ `Asia/Ho_Chi_Minh`.
   - `BristolLogService.trend()` và `FoodDiaryService.trend()` cùng lấy đúng N ngày lịch, bao gồm hôm nay, với mốc bắt đầu/kết thúc theo ngày Việt Nam.

2. **Chặn dữ liệu thời gian ở tương lai**
   - Thêm `@PastOrPresent` cho `loggedAt` và `eatenAt`.
   - DatePicker Bristol và nhật ký ăn uống không cho chọn ngày tương lai.

3. **Đồng bộ validation frontend/backend**
   - Hồ sơ: chiều cao `30..300cm`, cân nặng `1..500kg`; ngày sinh phải trước hôm nay, không chọn hôm nay.
   - Thêm giới hạn hiển thị/nhập cho ghi chú, món ăn, tên thuốc và liều lượng.

4. **Giới hạn kích thước request**
   - `BristolLogRequest`: notes tối đa 5000 ký tự.
   - `FoodDiaryEntryRequest`: description tối đa 500, notes tối đa 5000.
   - `MedicationReminderRequest`: medicineName/dosage tối đa 200, khớp VARCHAR(200).
   - `MedicalProfileRequest`: medicalHistory tối đa 5000, từng mục danh sách tối đa 200.
   - `ChatMessageRequest`: content tối đa 1000, khớp giới hạn nhập FE.

5. **404 và thông báo tiếng Việt cho resource ownership**
   - Thêm `ResourceNotFoundException`, map HTTP 404.
   - Dùng `OwnedResourceLoader` chung cho Bristol, food diary, medication reminder và chat history.
   - Không phân biệt “không tồn tại” với “không thuộc bệnh nhân” trong response.

6. **Loại bỏ response cũ khi đổi trang nhanh**
   - Bristol, food diary, login history và lịch sử xác nhận thuốc dùng request id; chỉ request mới nhất được phép cập nhật state/loading/error.

## Kiểm tra

- Đã bổ sung thay đổi vào backend/frontend hiện có, không đổi use case ngoài các luồng liên quan.
- Backend test: 65 test, 0 lỗi.
- Frontend production build: thành công.
- Frontend lint: thành công; chỉ còn các cảnh báo React/lint có sẵn về setState trong effect và Fast Refresh ở các file khác, không có lỗi mới do compile.
