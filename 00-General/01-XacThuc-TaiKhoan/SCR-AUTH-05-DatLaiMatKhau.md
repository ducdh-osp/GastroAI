# SCR-AUTH-05 — Đặt lại mật khẩu (bước 2)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0004` (bước 2) |
| Actor | Người dùng vừa bấm link đặt lại mật khẩu trong email |
| Mục tiêu nghiệp vụ | Cho người dùng một mật khẩu mới an toàn, rồi đưa họ quay lại dùng app càng nhanh càng tốt |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Thiết kế màn hình

```text
┌─────────────────────────────────────┐
│          Đặt lại mật khẩu             │
│                                        │
│   Mật khẩu mới                   [👁] │
│   [___________________________]       │
│   ⓘ tối thiểu 8 ký tự, có hoa, số và  │
│     ký tự đặc biệt                    │
│                                        │
│        [   Đặt lại mật khẩu   ]       │
└─────────────────────────────────────┘
```

Chỉ 1 ô nhập (không cần ô "nhập lại mật khẩu" thứ hai — nút hiện mật khẩu `[👁]` đã đủ để người dùng tự kiểm tra lại, bớt đi 1 trường là bớt 1 điểm có thể gây lỗi gõ không khớp gây bực bội).

## 2. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Mật khẩu mới trùng với mật khẩu đã dùng trước đây (kể cả mật khẩu rất cũ, không chỉ mật khẩu gần nhất) | Từ chối, giải thích ngắn gọn "Vui lòng chọn mật khẩu khác với các mật khẩu bạn từng dùng" | Người dùng đi "quên mật khẩu" thường vì nghi ngờ mật khẩu cũ không còn an toàn — cho phép quay lại dùng mật khẩu cũ (bất kỳ mật khẩu cũ nào) sẽ vô hiệu hoá chính mục đích của việc đặt lại |
| Liên kết đã hết hạn | Báo rõ, kèm đường dẫn quay lại [SCR-AUTH-04-QuenMatKhau.md](SCR-AUTH-04-QuenMatKhau.md) để yêu cầu lại ngay | Không để người dùng vào ngõ cụt |
| Thành công | Đưa thẳng tới màn đăng nhập, có sẵn thông báo "Đặt lại mật khẩu thành công, hãy đăng nhập bằng mật khẩu mới" | Người dùng vừa trải qua một hành trình hơi căng thẳng (lo lắng → chờ email → đặt mật khẩu) — cần một xác nhận rõ ràng, dứt khoát để yên tâm là đã xong |

## 3. Vì sao liên kết này phải "sống" ngắn hơn hẳn liên kết xác thực email

Nếu liên kết đặt lại mật khẩu bị lộ (hộp thư bị xem trộm, chuyển tiếp nhầm...), hậu quả là **mất quyền kiểm soát một tài khoản đã có sẵn dữ liệu sức khoẻ** — nghiêm trọng hơn nhiều so với việc lộ liên kết kích hoạt một tài khoản còn trống. Vì vậy cửa sổ hiệu lực của liên kết này cần ngắn (tính bằng phút, không phải bằng giờ) — đủ cho một người đang ngồi trước máy chờ email tới, nhưng không đủ để trở thành rủi ro treo lơ lửng cả ngày.

## 4. Tham chiếu

- Bước trước: [SCR-AUTH-04-QuenMatKhau.md](SCR-AUTH-04-QuenMatKhau.md)
- Sau khi xong: [SCR-AUTH-03-DangNhap.md](SCR-AUTH-03-DangNhap.md)
