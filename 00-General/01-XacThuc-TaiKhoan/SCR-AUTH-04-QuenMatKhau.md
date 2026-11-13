# SCR-AUTH-04 — Quên mật khẩu (bước 1: nhập email)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0004` (bước 1) |
| Actor | Người dùng đã quên mật khẩu |
| Mục tiêu nghiệp vụ | Mở lối thoát cho người dùng thật, mà không trở thành công cụ dò xem ai là người dùng của hệ thống |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Nguyên tắc thiết kế

Người dùng tới đây đang **lo lắng** (sợ mất quyền truy cập vào dữ liệu sức khoẻ của mình) — màn hình cần trấn an bằng sự đơn giản: chỉ 1 ô nhập, 1 nút bấm, và một lời hứa rõ ràng về điều sắp xảy ra.

## 2. Thiết kế màn hình

```text
┌─────────────────────────────────────┐
│          Quên mật khẩu?               │
│   Nhập email đã đăng ký, chúng tôi    │
│   sẽ gửi hướng dẫn đặt lại mật khẩu.  │
│                                        │
│   Email                                │
│   [___________________________]       │
│                                        │
│        [   Gửi hướng dẫn   ]          │
│                                        │
│   Nhớ ra mật khẩu rồi? Đăng nhập       │
└─────────────────────────────────────┘
```

## 3. Nguyên tắc phản hồi — điểm khác biệt quan trọng nhất của màn này

Dù email đó **có tồn tại trong hệ thống hay không**, màn hình phải trả lời **đúng một câu giống hệt nhau**: *"Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu."* — không được nói khác đi dù chỉ một chút (không báo "email không tồn tại", không phản hồi nhanh/chậm khác nhau).

Lý do: đây là **một app sức khoẻ**. Việc để lộ "email X có phải là người dùng GastroAI không" tương đương với để lộ việc một người có đang theo dõi/điều trị vấn đề tiêu hoá hay không — một thông tin tế nhị mà bản thân người đó chưa chắc muốn ai khác biết. Nguyên tắc này **quan trọng hơn** mong muốn UX "nói rõ cho người dùng biết họ gõ sai email" — ở đúng màn hình này, sự mơ hồ là có chủ đích và là đúng đắn.

## 4. Điều xảy ra tiếp theo

Nếu email tồn tại, một liên kết đặt lại mật khẩu có **thời hạn ngắn** được gửi đi (xem lý do ở [00-TongQuan.md](00-TongQuan.md) §4.4) — đủ ngắn để giảm rủi ro nếu hộp thư của người dùng bị người khác xem trộm, nhưng đủ dài để họ kịp mở email trong một lần ngồi xuống kiểm tra bình thường.

## 5. Tham chiếu

- Bước tiếp theo: [SCR-AUTH-05-DatLaiMatKhau.md](SCR-AUTH-05-DatLaiMatKhau.md)
- Quay lại: [SCR-AUTH-03-DangNhap.md](SCR-AUTH-03-DangNhap.md)
