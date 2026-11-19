# SCR-AUTH-06 — Đổi mật khẩu

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0005` |
| Actor | Người dùng đang đăng nhập |
| Mục tiêu nghiệp vụ | Cho người dùng chủ động làm mới mật khẩu bất cứ lúc nào, không cần đợi "quên" mới đổi được |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với "Đặt lại mật khẩu" (`SCR-AUTH-05`)

Ở đây người dùng **đang đăng nhập** — họ không chứng minh danh tính qua email nữa, mà qua việc đang có một phiên làm việc hợp lệ. Nhưng đổi mật khẩu vẫn là thao tác đủ nhạy cảm để cần **xác thực lại ngay tại chỗ**: bắt nhập đúng mật khẩu hiện tại trước khi cho đổi. Tình huống thực tế cần tính tới: nhiều người dùng sức khoẻ lớn tuổi hay dùng chung thiết bị gia đình, hoặc quên đăng xuất ở máy dùng chung — yêu cầu nhập lại mật khẩu hiện tại là lớp bảo vệ cuối trước khi ai đó khác (đang ngồi vào máy đã đăng nhập sẵn của người khác) có thể tự ý đổi mật khẩu và chiếm tài khoản.

## 2. Thiết kế màn hình

```text
┌─────────────────────────────────────┐
│          Đổi mật khẩu                 │
│                                        │
│   Mật khẩu hiện tại               [👁]│
│   [___________________________]       │
│                                        │
│   Mật khẩu mới                    [👁]│
│   [___________________________]       │
│   ⓘ tối thiểu 8 ký tự, có hoa, số và  │
│     ký tự đặc biệt                    │
│                                        │
│        [   Đổi mật khẩu   ]           │
└─────────────────────────────────────┘
```

Nằm trong dropdown tài khoản trên header (xem `02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md` §2) — không phải 1 ô trên Trang chủ, vì người dùng chỉ ghé qua đây khi chủ động muốn, không phải việc làm thường xuyên như các chức năng chính.

## 3. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Nhập sai mật khẩu hiện tại | Báo lỗi ngay tại ô đó, không xoá ô mật khẩu mới đã gõ | Không bắt người dùng gõ lại từ đầu vì 1 lỗi ở ô khác |
| Mật khẩu mới trùng mật khẩu cũ đã từng dùng | Từ chối, lý do giống hệt `SCR-AUTH-05` | Cùng 1 nguyên tắc nghiệp vụ, áp dụng nhất quán ở cả 2 nơi đổi mật khẩu |
| Thành công | Xác nhận ngắn gọn ngay trên trang, **không đăng xuất người dùng** | Đổi mật khẩu xong mà bị đá ra ngoài phải đăng nhập lại là một trải nghiệm khó chịu không cần thiết — phiên hiện tại vẫn hợp lệ |

## 4. Tham chiếu

- Quy tắc mật khẩu dùng chung: [00-TongQuan.md](00-TongQuan.md) §4.2
- Màn liên quan: [SCR-AUTH-05-DatLaiMatKhau.md](SCR-AUTH-05-DatLaiMatKhau.md)
