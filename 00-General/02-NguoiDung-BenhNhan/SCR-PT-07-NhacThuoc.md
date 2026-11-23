# SCR-PT-07 — Nhắc uống thuốc

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0015` (thiết lập lịch nhắc), `UC0016` (xác nhận đã uống) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Giúp bệnh nhân tuân thủ đúng phác đồ thuốc — vấn đề thực tế lớn nhất trong điều trị bệnh tiêu hoá mạn tính |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Thực tế cần hiểu trước khi thiết kế màn này

Tuân thủ dùng thuốc (medication adherence) là điểm yếu phổ biến nhất trong điều trị các bệnh tiêu hoá cần dùng thuốc kéo dài. Vài thực tế quan trọng cần phản ánh vào thiết kế:

- **Hướng dẫn dùng thuốc cần đi cùng lịch nhắc.** Hiện bệnh nhân nhập nội dung như "trước ăn sáng 30 phút" vào trường Hướng dẫn dùng thuốc; hệ thống nhắc theo các giờ cố định do bệnh nhân chọn. Chưa có trường phân loại Trước ăn/Sau ăn hoặc cơ chế tính giờ từ bữa ăn.
- **Lịch nhắc có thể gắn với khoảng điều trị.** Form có ngày bắt đầu/kết thúc tuỳ chọn; bỏ trống thì dùng liên tục. Các giờ đã chọn lặp hằng ngày trong khoảng này. Hệ thống chưa lưu đầy đủ từng liều dự kiến để thống kê liều bỏ lỡ qua các ngày.
- **Người bệnh ngại thừa nhận mình quên thuốc** — nếu màn hình tạo cảm giác bị "theo dõi và chấm điểm", bệnh nhân có xu hướng tắt thông báo hoặc xác nhận khống "đã uống" dù chưa uống thật, làm dữ liệu vô nghĩa và mất luôn tác dụng nhắc nhở.

## 2. Thiết kế màn hình

Trang `/medications` gồm khu vực các liều đã đến giờ chưa xác nhận hôm nay, bảng quản lý lịch nhắc và bảng lịch sử xác nhận có phân trang. Lịch nhắc độc lập với danh sách thuốc tự khai trong Hồ sơ bệnh lý.

```text
┌───────────────────────────────────────────────────────────┐
│  Nhắc uống thuốc                      [ + Thêm lịch nhắc ]    │
│                                                                 │
│  Liều đến giờ chưa xác nhận hôm nay                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 💊 Omeprazole 20mg                                          │ │
│  │    07:00 · Hướng dẫn: theo đơn bác sĩ                       │ │
│  │    [Uống Omeprazole (07:00)]                               │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Bảng lịch: thuốc, liều, giờ, liệu trình/hướng dẫn           │ │
│  │ [Bật nhắc] [Xác nhận từng giờ] [Sửa] [Tắt nhắc]             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                 │
│  Lịch sử xác nhận: thời điểm xác nhận, thuốc, giờ liều          │
│                                              [Phân trang]     │
└───────────────────────────────────────────────────────────┘

— Thêm lịch nhắc mới —
┌─────────────────────────────────┐
│  Thêm lịch nhắc thuốc                │
│  Tên thuốc        [_________________] │
│  Liều dùng         [_________________] │
│  Các giờ uống      [07:00] [12:30]    │
│  Thời gian điều trị [Từ ngày–Đến ngày]│
│  Hướng dẫn dùng thuốc [____________] │
│  Đang bật nhắc     [Bật/Tắt]          │
│       [     Lưu     ]                   │
└─────────────────────────────────┘
```

Tên thuốc bắt buộc, chọn từ danh mục hoặc tự nhập (tối đa 200 ký tự). Liều lượng tuỳ chọn (200 ký tự). Cần chọn ít nhất một giờ, không trùng nhau; giao diện cho chọn theo bước 15 phút. Hướng dẫn dùng thuốc tuỳ chọn, tối đa 1.000 ký tự. Form không có lựa chọn ngày trong tuần; lịch luôn lặp hằng ngày.

## 3. Nguyên tắc cho hành vi xác nhận

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Nhận popup WebSocket khi đang dùng app | Popup có nút **Đã uống thuốc** khi thông báo có mã lịch và giờ liều; bấm gọi API xác nhận, đánh dấu thông báo đã đọc và đồng bộ trang thuốc | Đã có xác nhận nhanh tại `AppHeader`; không cần chuyển trang trong luồng này |
| Mở thông báo từ chuông hoặc trung tâm thông báo | Bấm thông báo điều hướng tới `/medications`, sau đó chọn liều để xác nhận | Danh sách thông báo chưa có nút xác nhận liều trực tiếp như popup |
| Có liều đến giờ chưa xác nhận trong hôm nay | Trang thuốc tính từ lịch đang bật và các giờ đã xác nhận hôm nay, rồi hiển thị nút xác nhận | Đây là danh sách tính tại thời điểm xem; không phải lịch sử liều bỏ lỡ được lưu riêng |
| Xem lịch sử | Chỉ có các lần đã xác nhận thành công trong `MedicationConfirmation`; chưa có bản ghi trạng thái "chưa xác nhận/bỏ lỡ" theo ngày | Không suy ra bệnh nhân đã bỏ thuốc chỉ vì không bấm xác nhận |
| Muốn đổi giờ/liều đang dùng | Cho sửa lịch nhắc; khi sửa liều lượng có dòng "Hãy trao đổi với bác sĩ trước khi tự thay đổi liều dùng" | Nhắc bệnh nhân kiểm tra chỉ dẫn điều trị |
| Dừng lịch nhắc | Công tắc Bật nhắc cập nhật trực tiếp; nút Tắt nhắc có xác nhận Có/Huỷ. Cả hai không hỏi lý do; lịch sử xác nhận trước đây vẫn được giữ | Hệ thống tắt lịch, không xoá các lần đã xác nhận |

API xác nhận một liều trong ngày hiện tại theo múi giờ Việt Nam, kiểm tra quyền sở hữu, lịch đang hoạt động và giờ thuộc lịch; từ chối xác nhận trùng. Không có thao tác xác nhận bù một ngày cũ trong giao diện hiện tại.

## 4. Đề xuất chưa triển khai

- Phân loại Trước ăn/Sau ăn và tính giờ nhắc theo bữa ăn.
- Chọn các ngày cụ thể trong tuần.
- Hỏi và lưu lý do tắt lịch.
- Lưu lịch sử liều dự kiến/chưa xác nhận qua các ngày. Cần thiết kế cách giữ lịch sử khi thay đổi giờ hoặc liệu trình; không thể tái tạo chính xác lịch cũ chỉ từ cấu hình lịch hiện tại.

Các mục này cần thiết kế và kiểm thử riêng; không mô tả như khả năng đã có trong bản hiện tại.

## 5. Tham chiếu

- Thông báo đẩy khi tới giờ: `05-ThongBao-Notification/`
- Phạm vi ngữ cảnh AI hiện sử dụng: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md).
- Đối chiếu code: `MedicationReminderModal.tsx`, `MedicationRemindersPage.tsx`, `AppHeader.tsx`, `NotificationBell.tsx`, `MedicationReminder.java`, `MedicationConfirmation.java`, `MedicationReminderService.java`.
