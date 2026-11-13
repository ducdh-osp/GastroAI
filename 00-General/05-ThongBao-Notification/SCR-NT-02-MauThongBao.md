# SCR-NT-02 — Quản lý mẫu thông báo/email

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0074` |
| Actor | Admin |
| Mục tiêu nghiệp vụ | Cho phép sửa nội dung email hệ thống gửi đi mà không cần đội kỹ thuật sửa code mỗi lần đổi câu chữ |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần màn hình này

Nội dung email (xác thực tài khoản, đặt lại mật khẩu, nhắc thuốc...) đôi khi cần chỉnh sửa — đổi giọng văn, sửa lỗi chính tả, cập nhật thông tin liên hệ hỗ trợ. Nếu nội dung này nằm cứng trong code, mỗi lần sửa câu chữ đều phải qua quy trình kỹ thuật đầy đủ (sửa code, kiểm thử, triển khai) cho một thay đổi có thể chỉ là 1 câu — không tương xứng với mức độ thay đổi.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Mẫu thông báo & email                                         │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Email xác thực tài khoản          Cập nhật lần cuối: 05/11   │ │
│ │ Email đặt lại mật khẩu             Cập nhật lần cuối: 05/11   │ │
│ │ Email nhắc uống thuốc (khi offline)Cập nhật lần cuối: 01/11   │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Chỉnh sửa 1 mẫu —
┌───────────────────────────────────────────────────────────┐
│  Email xác thực tài khoản                                      │
│  Tiêu đề    [Xác thực tài khoản GastroAI của bạn____________]  │
│  Nội dung                                                       │
│  [Chào {{ho_ten}}, vui lòng bấm vào liên kết sau để xác thực...]│
│  Biến có thể dùng: {{ho_ten}}, {{lien_ket_xac_thuc}}            │
│                                                                 │
│  [      Xem thử      ]              [     Lưu thay đổi     ]   │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Luôn có nút "Xem thử" trước khi lưu | Hiện email sẽ trông như thế nào với dữ liệu mẫu | Sửa nội dung email mà không xem trước dễ để lọt lỗi định dạng/thiếu biến — email gửi sai tới hàng loạt người dùng khó thu hồi |
| Liệt kê rõ các biến có thể dùng | Hiện ngay trong màn soạn, không bắt nhớ | Người chỉnh sửa (Admin) không nhất thiết nhớ chính xác tên biến kỹ thuật |
| Thay đổi áp dụng cho email **tiếp theo**, không ảnh hưởng email đã gửi | Rõ ràng, tránh hiểu nhầm "sửa xong gửi lại hết email cũ" | Khớp đúng với cách vận hành hợp lý — không re-gửi hàng loạt chỉ vì sửa 1 câu chữ |
| Email nhắc thuốc không được thêm biến chứa tên thuốc/liều lượng cụ thể | Giới hạn ngay trong danh sách biến cho phép | Nhất quán nguyên tắc ở `00-TongQuan.md` §2.2 — không để lộ chi tiết sức khoẻ qua kênh email |

## 4. Tham chiếu

- Email này được dùng ở: `01-XacThuc-TaiKhoan/SCR-AUTH-02-XacThucEmail.md`, `SCR-AUTH-05-DatLaiMatKhau.md`, `02-NguoiDung-BenhNhan/SCR-PT-07-NhacThuoc.md`
