# SCR-AUTH-02 — Xác thực email

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0001` |
| Actor | Người vừa đăng ký, đang mở email |
| Mục tiêu nghiệp vụ | Xác nhận email có thật và thuộc về đúng người, bằng thao tác nhẹ nhàng nhất có thể |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Nguyên tắc thiết kế

Người dùng tới màn này bằng cách **bấm 1 link trong email**, không gõ gì cả — vì vậy màn hình không nên có form. Việc xác thực phải tự chạy ngay khi trang mở ra, và kết quả phải rõ ràng trong vài giây: thành công hay không, và **phải làm gì tiếp theo**. Đây không phải nơi để giải thích dài dòng — người dùng chỉ muốn biết "xong chưa, vào app được chưa".

## 2. Thiết kế màn hình

```text
          — Thành công —                      — Hết hạn / lỗi —
┌─────────────────────────────┐    ┌─────────────────────────────┐
│       Xác thực email           │    │       Xác thực email           │
│                                 │    │                                 │
│  ✓ Email đã được xác thực!     │    │  ⚠ Liên kết đã hết hạn          │
│    Bạn có thể đăng nhập ngay.  │    │                                 │
│                                 │    │  Nhập lại email để chúng tôi   │
│      [  Đăng nhập ngay  ]      │    │  gửi liên kết mới:              │
│                                 │    │  [___________________________] │
│                                 │    │      [   Gửi lại email   ]     │
└─────────────────────────────┘    └─────────────────────────────┘
```

Không dùng thêm bước trung gian "đang xử lý" kéo dài — chỉ cần một trạng thái tải ngắn rồi chuyển thẳng sang 1 trong 2 kết quả trên.

## 3. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Mở link lần đầu, còn hạn | Xác thực ngay, hiện thành công | Chuẩn |
| Bấm lại chính link đó lần 2 (vô tình bấm 2 lần, hoặc mở lại tab cũ) | Vẫn hiện thành công, **không báo lỗi** | Với người dùng, bấm lại 1 link đã xác thực rồi không phải là "sai" — báo lỗi ở đây chỉ gây hoang mang không cần thiết |
| Link đã quá hạn | Hiện rõ lý do + ô nhập email để gửi lại ngay tại chỗ | Không bắt quay lại màn đăng ký gõ lại từ đầu — họ chỉ cần 1 email mới |
| Gửi lại thành công | Xác nhận ngắn gọn, nhắc kiểm tra cả mục thư rác | Email đôi khi rơi vào spam — nhắc trước giúp giảm thắc mắc "sao không thấy gửi" |

## 4. Lưu ý thời hạn

Liên kết xác thực nên có thời hạn **đủ dài để không làm khó người hay trì hoãn mở email** (ví dụ vài giờ tới 1 ngày) — hậu quả nếu liên kết bị lộ chỉ là kích hoạt hộ một tài khoản chưa chứa dữ liệu gì, nên không cần siết chặt như liên kết đặt lại mật khẩu (xem [00-TongQuan.md](00-TongQuan.md) §4.4).

## 5. Tham chiếu

- Trước đó: [SCR-AUTH-01-DangKy.md](SCR-AUTH-01-DangKy.md)
- Sau khi thành công: [SCR-AUTH-03-DangNhap.md](SCR-AUTH-03-DangNhap.md)
