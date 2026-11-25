# SCR-AUTH-07 — Lịch sử đăng nhập

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0007` |
| Actor | Nhân sự CMS đang đăng nhập |
| Mục tiêu nghiệp vụ | Cho nhân sự CMS tự kiểm tra các lần truy cập vào tài khoản nội bộ của chính mình |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

> **Phạm vi:** màn này chỉ còn áp dụng cho tài khoản CMS. Bệnh nhân không có màn xem lịch sử đăng nhập; hệ thống vẫn ghi nhận các lần đăng nhập ở lớp bảo mật và gửi thông báo khi phát hiện lần nhập sai mật khẩu.

## 1. Vì sao tính năng này đáng làm, dù không ai yêu cầu hằng ngày

Người dùng không mở màn này mỗi ngày — nhưng **biết rằng nó tồn tại** đã là một phần quan trọng trong việc xây dựng lòng tin với một ứng dụng nắm giữ thông tin sức khoẻ cá nhân. Khi có cảm giác bất an ("hình như máy tôi vừa bị ai đó động vào", "sao có email lạ báo đăng nhập") — đây phải là nơi họ tra cứu được ngay, tự mình, không cần chờ liên hệ ai.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────────────┐
│  Lịch sử đăng nhập                                                    │
│  ⚠ Có 2 lần đăng nhập thất bại trong 7 ngày qua — nếu không phải bạn, │  ← chỉ hiện khi có
│    hãy đổi mật khẩu ngay                                              │     thất bại thật
│ ┌────────────────┬───────────┬───────────────┬────────────┬───────┐ │
│ │ Thời gian        │ Thiết bị  │ Địa chỉ IP    │ Kết quả    │       │ │
│ ├────────────────┼───────────┼───────────────┼────────────┼───────┤ │
│ │ Hôm nay, 09:02   │ Chrome ·  │ 113.190.23.45 │ ✓ Thành     │       │ │
│ │                  │ Windows   │               │   công      │       │ │
│ │ Hôm nay, 08:55   │ Chrome ·  │ 113.190.23.45 │ ✕ Sai mật   │       │ │
│ │                  │ Windows   │               │   khẩu      │       │ │
│ └────────────────┴───────────┴───────────────┴────────────┴───────┘ │
│                                                Phân trang: ‹ 1 2 3 ›   │
└───────────────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc hiển thị

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Cảnh báo thất bại gần đây | Phải tính trên **toàn bộ** lịch sử gần đây, không chỉ những gì đang hiện trên trang hiện tại (nếu có phân trang) | Một người dùng chỉ xem trang đầu không nên bỏ lỡ 1 lần thất bại đáng ngờ nằm ở trang sau |
| Hiển thị thiết bị & địa chỉ IP | Phân tích chuỗi `User-Agent` thành tên thiết bị dễ đọc ("Chrome · Windows", kèm tooltip chuỗi gốc) và hiển thị nguyên vẹn địa chỉ IP thực tế ghi nhận được (không tra cứu GeoIP) | Nhân sự CMS đọc được ngay trình duyệt/hệ điều hành quen thuộc và đối chiếu chính xác địa chỉ IP mạng mà không bị sai lệch vị trí do NAT nhà mạng |
| Khi phát hiện bất thường | Luôn kèm gợi ý hành động ngay tại chỗ ("đổi mật khẩu ngay"), đừng chỉ hiển thị dữ liệu thô | Tra được thông tin mà không biết phải làm gì tiếp theo thì tính năng chưa trọn vẹn |
| Thời gian lưu trữ | Đủ dài để phát hiện mẫu hình bất thường kéo dài (vài tháng), không cần lưu vô hạn | Cân bằng giữa hữu ích và tối giản dữ liệu nhạy cảm lưu trữ lâu dài |

## 4. Phạm vi dữ liệu

Màn hình này **chỉ bao giờ hiện lịch sử của chính nhân sự CMS đang xem** — không có cách nào, kể cả qua việc can thiệp vào đường dẫn, để xem được lịch sử đăng nhập của người khác. Lịch sử đăng nhập của bệnh nhân không được cung cấp qua giao diện người dùng.

## 5. Tham chiếu

- [00-TongQuan.md](00-TongQuan.md) §5 — Ma trận phân quyền
