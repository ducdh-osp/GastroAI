# SCR-AUTH-03 — Đăng nhập

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0002` (đăng nhập), `UC0008` (bảo vệ khỏi đoán mật khẩu — biểu hiện trên chính màn này) |
| Actor | Người dùng đã có tài khoản |
| Mục tiêu nghiệp vụ | Đưa người dùng quen thuộc vào app nhanh nhất có thể — đây là màn hình được ghé thăm nhiều nhất trong toàn hệ thống |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Nguyên tắc thiết kế

Nếu đăng ký là "màn hình thuyết phục", đăng nhập là "màn hình tốc độ" — người dùng đã tin tưởng rồi, giờ chỉ muốn vào app nhanh nhất. Mọi yếu tố trang trí thừa đều là cản trở. Nhưng tốc độ không có nghĩa là bỏ qua việc xử lý tốt các tình huống lỗi — ngược lại, **chính ở màn hình tần suất cao này, một lỗi xử lý tệ sẽ làm phiền số đông người dùng nhất.**

## 2. Thiết kế màn hình

```text
┌─────────────────────────┬───────────────────────────────┐
│  (panel thương hiệu,      │   Đăng nhập tài khoản             │
│   giống SCR-AUTH-01,      │   Tiếp tục hành trình chăm sóc    │
│   để nhất quán cảm nhận)  │   tiêu hoá cùng GastroAI.          │
│                            │  ┌─────────────────────────────┐ │
│                            │  │ ⓘ [Banner tình huống nếu có] │ │
│                            │  │ Email                          │ │
│                            │  │ [___________________________] │ │
│                            │  │ Mật khẩu                 [👁] │ │
│                            │  │ [___________________________] │ │
│                            │  │                 Quên mật khẩu? │ │
│                            │  │      [    Đăng nhập    ]       │ │
│                            │  │                                 │ │
│                            │  │  Chưa có tài khoản? Đăng ký     │ │
└─────────────────────────┴───────────────────────────────┘
```

"Quên mật khẩu?" đặt ngay cạnh ô mật khẩu, không giấu ở chân trang — đây là lối thoát cần tìm thấy **trong vòng 1 giây** khi người dùng vừa nhận ra mình quên.

## 3. Các tình huống đăng nhập và cách phản hồi

| Tình huống | Thông điệp nên hiện | Vì sao phản hồi như vậy |
| :--- | :--- | :--- |
| Sai email hoặc sai mật khẩu | "Email hoặc mật khẩu không đúng" (dùng chung 1 câu cho cả 2 trường hợp) | Nếu nói riêng "email không tồn tại" sẽ lộ ra ai đang là người dùng GastroAI — với 1 app sức khoẻ, đó là thông tin cần giữ kín |
| Tài khoản chưa xác thực email | Banner ngay trên form: "Email chưa được xác thực" + nút "Gửi lại email xác thực" | Cho lối thoát **tại chỗ**, không bắt người dùng tự đi tìm lại email cũ hay quay về màn đăng ký |
| Nhập sai liên tiếp quá nhiều lần | "Tài khoản tạm khoá để bảo vệ bạn, thử lại sau N phút" — đếm ngược rõ ràng | Phải nói rõ đây là **bảo vệ**, không phải phạt, và luôn cho biết chính xác còn bao lâu — mập mờ chỉ khiến người dùng thật thêm hoảng |
| Tài khoản bị quản trị viên vô hiệu hoá | "Tài khoản đã bị tạm ngưng, vui lòng liên hệ hỗ trợ" | Khác hẳn trường hợp tự khoá do nhập sai — người dùng cần biết đây là quyết định của con người, không tự gỡ được bằng thao tác kỹ thuật |
| Đăng nhập thành công | Vào thẳng Trang chủ (`02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md`), không có màn hình trung gian "chào mừng" choán thời gian | Người dùng quay lại không cần được chào lại từ đầu mỗi lần |

## 4. Về việc khoá tạm thời (`UC0008`)

Số lần cho phép sai trước khi khoá, và thời gian khoá, là một đánh đổi nghiệp vụ: **sai số ít lần** → không đủ cản kẻ dò mật khẩu tự động; **khoá quá lâu** → biến một lần gõ nhầm của người dùng thật thành cả một trải nghiệm tồi tệ, đặc biệt nguy hiểm nếu họ đang cần tra cứu thông tin sức khoẻ gấp. Mức cân bằng hợp lý: **vài lần sai liên tiếp** mới khoá, và khoá trong **một khoảng ngắn vừa đủ** (vài phút, không phải vài giờ) — đủ để chặn tấn công tự động nhưng không trừng phạt người dùng thật quá mức.

## 5. Tham chiếu

- Lối ra khi quên mật khẩu: [SCR-AUTH-04-QuenMatKhau.md](SCR-AUTH-04-QuenMatKhau.md)
- Lối ra khi chưa xác thực: [SCR-AUTH-02-XacThucEmail.md](SCR-AUTH-02-XacThucEmail.md)
- Sau khi vào: `02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md`
