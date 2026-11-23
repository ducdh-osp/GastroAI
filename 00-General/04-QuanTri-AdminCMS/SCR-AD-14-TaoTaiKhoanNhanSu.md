# SCR-AD-14 — Tạo tài khoản nhân sự CMS

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0077` *(mới — đã đối chiếu `docs/masterplan-phan-he-usecase.xlsx` sheet "Use case chi tiet": mã cao nhất đang dùng là `UC0076`, nên `UC0077` là số đúng tiếp theo; vẫn cần người giữ file đó tự thêm 1 dòng chính thức vào sheet, tài liệu không tự chỉnh xlsx)* |
| Actor | Admin (duy nhất) |
| Mục tiêu nghiệp vụ | Cho Admin tạo tài khoản CMS mới và gán 1 trong 4 vai trò ngay từ đầu, không còn phụ thuộc vào việc chèn dữ liệu test qua Flyway |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần màn này

Hiện tại **không có bất kỳ UI hay API nào** để tạo tài khoản CMS — 2 bảng tài khoản nội bộ (`admins`, `doctors` trên MySQL) chỉ có dữ liệu nhờ seed Flyway (`V2__seed_cms_test_accounts.sql`, `V6__seed_more_cms_test_accounts.sql`), tức là chỉ tạo được bằng cách sửa code + chạy migration — không khả thi một khi hệ thống đã "vận hành" dù chỉ ở quy mô đồ án (thêm 1 thành viên dùng CMS là không làm được nếu không nhờ dev chèn SQL tay). Yêu cầu nâng từ 2 lên 4 vai trò ([SCR-AD-03](SCR-AD-03-PhanQuyenRBAC.md)) làm lộ rõ thêm gap này: có vai trò mới (`Cộng tác viên nội dung`, `Giám sát vận hành`) mà không có cách tạo tài khoản thật mang vai trò đó, ma trận quyền ở SCR-AD-03 §4 sẽ mãi chỉ là lý thuyết.

## 2. Khác với Đăng ký tài khoản bệnh nhân ([SCR-AUTH-01](../01-XacThuc-TaiKhoan/SCR-AUTH-01-DangKy.md)) ra sao — và vì sao khác là đúng

Dễ nhầm 2 màn này vì cả hai đều "tạo tài khoản mới", nhưng mô hình tin cậy ngược hẳn nhau:

| | SCR-AUTH-01 (bệnh nhân tự đăng ký) | SCR-AD-14 (màn này) |
| :--- | :--- | :--- |
| Ai tạo | Người dùng tự tạo cho chính mình | Admin tạo cho **người khác** |
| Hệ thống biết danh tính người nhận trước không | Không — vừa là khách lạ | Có — đồng nghiệp cùng team, Admin xác nhận ngoài đời |
| Vì sao cần xác thực email | Chống đăng ký giả/spam, xác nhận email còn dùng được | Không cần — không có rủi ro "giả mạo tự đăng ký" vì Admin chủ động tạo |
| Ai chọn mật khẩu | Người dùng tự chọn ngay lúc đăng ký | Xem §5 — điểm khác nhau có chủ đích, không tái dùng y nguyên luồng SCR-AUTH-01 được |
| Trạng thái ban đầu | "Chưa kích hoạt" tới khi xác thực email | Kích hoạt ngay — không có bước xác thực email vì không cần chống giả mạo như trên |

Vì không phải self-registration, màn này **không** cần form 2 cột "mặt tiền thuyết phục" như SCR-AUTH-01 (§1 tài liệu đó) — người được tạo tài khoản không cần "được thuyết phục tin tưởng", họ đã là đồng nghiệp. Form ở đây đơn giản, 1 cột, đúng triết lý "công cụ cho nhân sự thành thạo" đã nêu ở `00-TongQuan.md`.

## 3. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  ‹ Quay lại Phân quyền nhân sự                                │
│                                                                │
│  Tạo tài khoản nhân sự CMS                                    │
│                                                                │
│  Họ và tên                                                     │
│  [___________________________________________]               │
│  Email                                                         │
│  [___________________________________________]               │
│  Vai trò                                                       │
│  [ Chọn vai trò...                          ▾ ]               │
│    Admin · Biên tập chuyên môn y khoa ·                       │
│    Cộng tác viên nội dung · Giám sát vận hành                 │
│  Mật khẩu ban đầu                                    [👁]     │
│  [___________________________________________] [🎲 Tạo ngẫu nhiên]│
│  ⓘ tối thiểu 8 ký tự, có hoa, số và ký tự đặc biệt            │
│                                                                │
│            [Huỷ]        [   Tạo tài khoản   ]                 │
└───────────────────────────────────────────────────────────┘

— Sau khi tạo thành công —
┌───────────────────────────────────────────────────────────┐
│  ✓ Đã tạo tài khoản cho "Lê Văn E"                             │
│                                                                │
│  Mật khẩu ban đầu (chỉ hiện DUY NHẤT 1 lần, hãy gửi cho         │
│  nhân sự này ngay — không thể xem lại sau khi đóng màn này):   │
│  ┌─────────────────────────────┐                             │
│  │  Tr0ng!8xK2                   │  [📋 Sao chép]              │
│  └─────────────────────────────┘                             │
│  Khuyến nghị: nhắc nhân sự đổi mật khẩu ngay sau lần           │
│  đăng nhập đầu tiên (Đổi mật khẩu — xem SCR-AUTH-06).          │
│                                                                │
│                    [  Về danh sách Phân quyền  ]              │
└───────────────────────────────────────────────────────────┘
```

## 4. Thông tin thu thập & validate

| Trường | Vì sao cần | Validate |
| :--- | :--- | :--- |
| Họ và tên | Hiển thị trên toàn bộ CMS (header, Audit Log, dropdown RBAC) | Bắt buộc, không rỗng |
| Email | Định danh đăng nhập CMS duy nhất | Đúng định dạng email; **không trùng với bất kỳ tài khoản CMS nào đã có**; **không** cần kiểm tra trùng với email bệnh nhân (PostgreSQL) — 2 không gian định danh tách biệt, đăng nhập qua 2 cổng khác nhau (`/cms/login` vs `/login`), không có rủi ro thật nếu trùng |
| Vai trò | Quyết định ngay từ đầu nhân sự này thấy màn nào (ma trận ở [SCR-AD-03 §4](SCR-AD-03-PhanQuyenRBAC.md)) | Bắt buộc chọn đúng 1 trong 4 vai trò, không có giá trị mặc định (buộc Admin chọn có ý thức, không lỡ tay để mặc định) |
| Mật khẩu ban đầu | Cho tài khoản đăng nhập được ngay | Theo đúng chính sách mật khẩu dùng chung của hệ thống (tối thiểu 8 ký tự, có hoa, số, ký tự đặc biệt); nút "Tạo ngẫu nhiên" sinh 1 mật khẩu đạt chuẩn để Admin không phải tự nghĩ |

*Ghi chú kỹ thuật (ảnh hưởng trực tiếp tới việc hiện thực đúng màn này): kiểm tra trùng email và tạo tài khoản ở trên giả định mô hình **4 vai trò nằm trên 1 bảng tài khoản duy nhất**. Nếu bảng `doctors` + vai trò `DOCTOR` + `CmsAccountType.DOCTOR` chưa được dọn khỏi code trước khi làm màn này, màn tạo tài khoản sẽ buộc phải hỏi thêm "tạo ở bảng nào" — câu hỏi không có ý nghĩa nghiệp vụ, chỉ tồn tại vì nợ kỹ thuật cũ (xem ghi chú ở [SCR-AD-03 §4](SCR-AD-03-PhanQuyenRBAC.md)).*

## 5. Mật khẩu ban đầu — chọn phương án nào cho đồ án

> ## Quyết định: Admin đặt mật khẩu tạm, hay gửi email mời tự đặt?
> Đây là khác biệt cố ý so với SCR-AUTH-01 (xem §2) — 2 phương án thật:
>
> | Phương án | Cách làm | Chi phí hạ tầng | Rủi ro |
> | :--- | :--- | :--- | :--- |
> | **A — Admin đặt mật khẩu tạm (đề xuất)** | Admin tự nhập hoặc bấm "Tạo ngẫu nhiên", gửi cho nhân sự mới qua kênh ngoài hệ thống (chat nhóm, gặp trực tiếp — team 4 người, kênh này vốn đã có); nhân sự tự đổi sau khi đăng nhập lần đầu qua màn Đổi mật khẩu đã có sẵn ([SCR-AUTH-06](../01-XacThuc-TaiKhoan/SCR-AUTH-06-DoiMatKhau.md), route `/cms/change-password`) | **Không cần gì mới** — dùng đúng hạ tầng mật khẩu/đổi mật khẩu đã có | Admin biết mật khẩu tạm trong thời gian ngắn — chấp nhận được ở quy mô 4 người, đều là đồng nghiệp trực tiếp |
> | **B — Gửi email mời tự đặt mật khẩu (chuẩn bigtech)** | Tạo tài khoản ở trạng thái "chờ kích hoạt", gửi email chứa link tự đặt mật khẩu lần đầu (giống luồng quên mật khẩu) | **Cao hơn hẳn** — đã kiểm tra: CMS **hiện không có** endpoint quên/đặt lại mật khẩu nào (`forgot-password`/`reset-password` hiện chỉ tồn tại ở `/api/v1/auth/*` cho bệnh nhân). Cần thêm: 1 bảng token mời, 1 endpoint public mới (phải mở cổng riêng trong `SecurityConfig`), 1 mẫu email mới, 1 trang FE mới | Admin không biết mật khẩu người khác (đúng chuẩn), nhưng là hạ tầng hoàn toàn mới cho 1 nhu cầu dùng rất ít lần (toàn team 4 người) |
>
> **Đề xuất: Phương án A.** Rủi ro "Admin biết mật khẩu tạm" là rủi ro lý thuyết đúng chuẩn bigtech, nhưng ở quy mô 4 người tự quản lý máy/DB riêng (không phải nhân sự thật, không có HR/compliance thật đứng sau), chi phí xây phương án B không tương xứng lợi ích — nên dồn thời gian cho NFR có rủi ro thật hơn (audit log, Nghị định 13). Nếu sau này hệ thống lên môi trường có nhân sự ngoài nhóm thật, nên chuyển sang B.

## 6. Điều gì xảy ra sau khi tạo

Tài khoản **kích hoạt ngay** (khác SCR-AUTH-01 — xem §2, không cần bước xác thực email). Mật khẩu tạm hiển thị **đúng 1 lần** ngay trên màn hình thành công, kèm nút sao chép — sau khi Admin rời khỏi màn này, không có cách nào xem lại (chỉ có thể đặt lại mật khẩu mới — hệ thống chỉ lưu bản hash, không lưu mật khẩu dạng đọc được). Hành động tạo tài khoản ghi vào Audit Log (mã hành động mới trong `AuditAction`: `CMS_ACCOUNT_CREATE` — hiện enum này đã có `RBAC_ROLE_CHANGE` cho đổi vai trò nhưng chưa có mã riêng cho tạo tài khoản mới).

## 7. Trường hợp cần xử lý khéo

| Tình huống | Cách xử lý nên có | Mã lỗi gợi ý | Vì sao |
| :--- | :--- | :--- | :--- |
| Email đã tồn tại (ở bất kỳ vai trò nào) | Báo rõ "Email này đã có tài khoản CMS" — không cần giữ kín như màn quên mật khẩu phía bệnh nhân | `ERR_CMS_EMAIL_DUPLICATE` | Admin là người quản trị, không phải người lạ dò thông tin — nguyên tắc "giữ kín email có tồn tại" ở SCR-AUTH-04 không áp dụng ở đây |
| Bỏ trống họ tên/email/vai trò | Chặn submit, chỉ rõ từng trường thiếu | `ERR_REQUIRED_FIELD` | Giảm số lần thử sai |
| Mật khẩu tạm không đạt chính sách | Chặn ngay khi gõ, giống hành vi ở SCR-AUTH-01 §5 | `ERR_PASSWORD_POLICY` | Nhất quán toàn hệ thống, Admin không phải học quy tắc riêng cho màn này |
| Admin tạo tài khoản mang vai trò Admin | Cho phép, nhưng dùng đúng loại cảnh báo "nâng quyền" như SCR-AD-03 §5 vì bản chất là trao toàn quyền hệ thống ngay từ đầu | — | Nhất quán với nguyên tắc "cảnh báo rõ khi tạo ra 1 tài khoản Admin" dù là tạo mới hay đổi vai trò |

## 8. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Chỉ Admin vào được màn này | Route CMS giới hạn Admin, không vai trò nào khác thấy nút "Tạo tài khoản mới" | Tạo tài khoản + gán vai trò lần đầu có thể dẫn tới leo thang quyền nếu giao nhầm vai trò — giữ cùng mức kiểm soát với đổi vai trò ở SCR-AD-03 |
| Không tự đăng ký, không có "mời qua link công khai" | Khác hẳn SCR-AUTH-01 — không có đường dẫn công khai nào tới màn này | Tài khoản CMS đại diện quyền truy cập dữ liệu y tế của toàn bộ bệnh nhân — không thể để lộ ra ngoài vòng kiểm soát trực tiếp của Admin |
| Mật khẩu tạm chỉ hiển thị 1 lần | Không có API/màn nào "xem lại" mật khẩu cũ, chỉ có "đặt lại" | Giảm thời gian mật khẩu ở dạng đọc được tồn tại trên màn hình — lỡ không chép kịp, bắt buộc đặt mật khẩu mới (an toàn hơn lưu hiển thị được mãi) |
| Mọi tài khoản mới đều ghi Audit Log, không ngoại lệ | Tự động, cùng cơ chế với mọi hành động nhạy cảm khác | Xem `00-TongQuan.md` §5 và [SCR-AD-06](SCR-AD-06-NhatKyHoatDong.md) |

## 9. Tham chiếu

- Đổi vai trò tài khoản đã tồn tại (bước sau màn này trong vòng đời tài khoản): [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md)
- Đăng ký tài khoản bệnh nhân (mô hình tin cậy khác hẳn — xem §2): [01-XacThuc-TaiKhoan/SCR-AUTH-01-DangKy.md](../01-XacThuc-TaiKhoan/SCR-AUTH-01-DangKy.md)
- Đổi mật khẩu sau khi đăng nhập lần đầu: [01-XacThuc-TaiKhoan/SCR-AUTH-06-DoiMatKhau.md](../01-XacThuc-TaiKhoan/SCR-AUTH-06-DoiMatKhau.md)
- Mọi thao tác ở màn này đều ghi vào: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md)

## 10. Giới hạn đã biết (chưa thiết kế trong lần này)

Vòng đời tài khoản CMS hiện chỉ có **tạo** (màn này) và **đổi vai trò** ([SCR-AD-03](SCR-AD-03-PhanQuyenRBAC.md)) — chưa có màn/luồng **khoá hoặc gỡ tài khoản** khi 1 nhân sự rời nhóm. Ghi nhận như giới hạn đã biết, chưa thiết kế thêm trong phạm vi yêu cầu lần này.
