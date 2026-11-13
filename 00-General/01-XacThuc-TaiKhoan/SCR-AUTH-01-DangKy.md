# SCR-AUTH-01 — Đăng ký tài khoản

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0001` |
| Actor | Người dùng mới, chưa có tài khoản |
| Mục tiêu nghiệp vụ | Biến một khách lạ thành một tài khoản thật, với ít ma sát nhất có thể |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao màn hình này quan trọng hơn vẻ ngoài của nó

Đây là màn hình có **tỷ lệ bỏ cuộc cao nhất** trong toàn bộ hành trình — người dùng chưa nhận được gì từ sản phẩm nên chưa có động lực kiên nhẫn. Mọi trường thêm vào form là một lý do để họ thoát ra. Triết lý thiết kế: **chỉ hỏi đúng 3 thứ tối thiểu để tạo được 1 tài khoản** — họ tên, email, mật khẩu. Mọi thông tin khác (hồ sơ bệnh lý, thói quen ăn uống, thuốc đang dùng...) thuộc về hành trình *sau khi đăng nhập lần đầu*, lúc đó người dùng đã bước qua "cửa", đã nhìn thấy AI tư vấn hoạt động, và sẵn sàng đầu tư thêm thời gian khai báo vì đã tin sản phẩm mang lại giá trị.

## 2. Thiết kế màn hình

Bố cục 2 vùng trên màn hình rộng (gập thành 1 cột trên điện thoại — nơi phần lớn người dùng Việt Nam thực sự truy cập):

- **Vùng bên trái (chỉ hiện trên màn lớn)** — "mặt tiền" thuyết phục: không nên để trống hay chỉ có logo. Đây là cơ hội duy nhất để nói với người dùng còn đang do dự *vì sao họ nên tin tưởng đưa thông tin sức khoẻ cho một con AI* — một dòng định vị ngắn ("Trợ lý AI tiêu hoá, trích dẫn nguồn y khoa thật"), kèm 2-3 điểm tin cậy cụ thể (vd: "Câu trả lời có trích dẫn tài liệu y khoa", "Cảnh báo ngay khi phát hiện dấu hiệu nguy hiểm", "Dữ liệu sức khoẻ được mã hoá riêng tư"). Thiếu phần này, màn đăng ký chỉ còn là 1 cái form vô cảm.
- **Vùng bên phải (luôn hiện, full-width trên điện thoại)** — form, đặt trên 1 khối nổi bật rõ ràng khỏi nền, không viền rối mắt.

```text
┌─────────────────────────┬───────────────────────────────┐
│  (nền màu thương hiệu,    │   Tạo tài khoản GastroAI          │
│   chỉ hiện trên desktop)  │   Bắt đầu hành trình chăm sóc     │
│                            │   hệ tiêu hoá cùng AI.            │
│  "Trợ lý AI tiêu hoá,      │  ┌─────────────────────────────┐ │
│   trích dẫn nguồn y khoa   │  │ Họ và tên                     │ │
│   thật"                    │  │ [___________________________] │ │
│                            │  │ Email                          │ │
│  ✓ Câu trả lời có trích    │  │ [___________________________] │ │
│    dẫn tài liệu y khoa     │  │ Mật khẩu                 [👁] │ │
│  ✓ Cảnh báo ngay khi có    │  │ [___________________________] │ │
│    dấu hiệu nguy hiểm      │  │ ⓘ tối thiểu 8 ký tự, có hoa,   │ │
│  ✓ Dữ liệu sức khoẻ được   │  │   số và ký tự đặc biệt         │ │
│    mã hoá riêng tư         │  │                                 │ │
│                            │  │      [     Đăng ký     ]       │ │
│                            │  │                                 │ │
│                            │  │  Đã có tài khoản? Đăng nhập     │ │
└─────────────────────────┴───────────────────────────────┘
```

**Nguyên tắc UX cho từng chi tiết nhỏ**:
- Gợi ý độ mạnh mật khẩu (`ⓘ`) hiển thị **ngay bên dưới ô nhập**, đổi màu từng tiêu chí (đạt → xanh) khi gõ — không bắt người dùng bấm "Đăng ký" rồi mới biết mình sai gì.
- Nút "hiện mật khẩu" (con mắt) bắt buộc có — mật khẩu dài, phức tạp, gõ sai trên điện thoại là chuyện thường, không có nút này là tự tạo thêm thất bại giả.
- Nút "Đăng ký" chỉ có **đúng 1 hành động chính**, không đặt cạnh nút phụ nào gây phân vân.

## 3. Thông tin thu thập

| Trường | Vì sao cần | Góp ý khi nhập sai |
| :--- | :--- | :--- |
| Họ và tên | Xưng hô cá nhân hoá khi AI/hệ thống giao tiếp sau này | "Vui lòng nhập họ tên của bạn" |
| Email | Định danh tài khoản + kênh duy nhất để khôi phục quyền truy cập và nhận cảnh báo quan trọng | "Email chưa đúng định dạng" |
| Mật khẩu | Bảo vệ dữ liệu sức khoẻ cá nhân — đủ mạnh để khó đoán nhưng người dùng vẫn nhớ được | hiển thị checklist trực quan, không phải câu lỗi dài dòng |

## 4. Điều gì xảy ra sau khi bấm "Đăng ký"

Tài khoản được tạo ngay ở trạng thái "chưa kích hoạt" — **không** để người dùng vào thẳng app mà chưa xác thực email, vì đây là kênh liên lạc duy nhất nếu sau này cần cảnh báo điều gì khẩn cấp liên quan tới sức khoẻ. Màn hình chuyển sang trạng thái "Kiểm tra hộp thư của bạn", nói rõ: đã gửi gì, cần làm gì tiếp, và **có ngay nút "Gửi lại email" nếu không thấy** — không bắt người dùng đoán xem phải làm gì nếu email không tới (thư rác, gõ sai tên miền...).

## 5. Trường hợp cần xử lý khéo

| Tình huống | Cách xử lý nên có | Vì sao |
| :--- | :--- | :--- |
| Email đã có người đăng ký trước | Báo rõ "Email này đã được sử dụng" kèm gợi ý sang màn đăng nhập hoặc quên mật khẩu | Với màn đăng ký, người dùng cần biết để không loay hoay đăng ký lại — khác với màn "quên mật khẩu" vốn cần giữ kín việc email có tồn tại hay không |
| Người dùng đăng ký nhưng không bao giờ xác thực email | Sau một thời gian hợp lý, nên coi như họ đổi ý và dọn tài khoản đó đi, để email họ dùng (nếu muốn) đăng ký lại từ đầu không bị báo "đã tồn tại" một cách vô lý mãi mãi | Tránh tình trạng 1 lượt thử ngẫu hứng chặn vĩnh viễn người dùng thật quay lại sau |
| Gõ mật khẩu không đạt yêu cầu | Chặn ngay khi gõ (không chờ submit), chỉ rõ còn thiếu tiêu chí nào | Giảm số lần thử sai → giảm bực bội |

## 6. Tham chiếu

- Bước tiếp theo: [SCR-AUTH-02-XacThucEmail.md](SCR-AUTH-02-XacThucEmail.md)
- Quy tắc mật khẩu dùng chung: [00-TongQuan.md](00-TongQuan.md) §4.2
