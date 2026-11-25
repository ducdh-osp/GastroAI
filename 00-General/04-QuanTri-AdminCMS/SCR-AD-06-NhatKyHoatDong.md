# SCR-AD-06 — Nhật ký hoạt động hệ thống (Audit Log)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0052` (ghi nhật ký), `UC0053` (lọc/tìm), `UC0063` (cảnh báo bất thường), `UC0064` (xuất file) |
| Actor | Admin |
| Mục tiêu nghiệp vụ | Trả lời được câu hỏi "ai đã làm gì, khi nào" với mọi thao tác nhạy cảm trong hệ thống — nền tảng để điều tra sự cố và duy trì trách nhiệm giải trình |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao đây là 1 trong những màn hình quan trọng nhất, dù ít khi mở tới

Với một hệ thống xử lý dữ liệu y tế, câu hỏi "ai đã xem/sửa dữ liệu của bệnh nhân X vào lúc nào" phải luôn trả lời được — không chỉ khi có sự cố mới đi tìm cách ghi lại. Màn hình này ít khi được mở trong vận hành hàng ngày, nhưng **khi cần tới, nó phải tuyệt đối đầy đủ và đáng tin cậy** — không có "có thể thiếu 1 vài bản ghi" chấp nhận được ở đây.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký hoạt động hệ thống                                    │
│ ┌─────────┬──────────────┐                                    │
│ │ Nhật ký  │ Quy tắc cảnh  │                                    │
│ │          │ báo bất thường │                                    │
│ └─────────┴──────────────┘                                    │
│  Hành động[▾] Người dùng[▾] Loại dữ liệu[▾] Từ ngày-Đến ngày    │
│                                           [🔍 Lọc] [Xuất ⬇]     │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Thời gian │ Người thực hiện │ Hành động       │ Đối tượng    │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ 14/11 09:20│ BS. Lê Văn C    │ Sửa tài liệu     │ Phác đồ H.pylori│
│ │ 14/11 09:05│ Admin Chính     │ Đổi phân quyền   │ Phạm Thị D    │ │
│ │ 13/11 22:40│ Admin Chính     │ Khoá tài khoản   │ Bệnh nhân #342│ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Tab "Quy tắc cảnh báo bất thường" —
┌───────────────────────────────────────────────────────────┐
│  Cảnh báo khi (giá trị mặc định, Admin chỉnh được):           │
│  ☑ 1 tài khoản xem dữ liệu từ [20] bệnh nhân khác nhau trở lên │
│     trong vòng [10] phút                                        │
│  ☑ 1 tài khoản đăng nhập/thao tác từ [4] địa chỉ IP khác nhau  │
│     trở lên trong vòng [1440] phút (24 giờ)                     │
│  … (6 quy tắc còn lại — xem bảng bên dưới)                      │
└───────────────────────────────────────────────────────────┘
```

Mockup trên chỉ minh hoạ 2 quy tắc. Trên màn thật, tab này là 1 bảng liệt kê **đủ 8 quy tắc mặc định** (cột: Quy tắc, Theo dõi, Ngưỡng cảnh báo `[ngưỡng] / [số phút]`, Bật, Sửa lần cuối, Lưu/Hoàn tác). Mọi quy tắc so sánh "**từ ngưỡng trở lên**" (`số đếm >= ngưỡng`), đếm theo từng tài khoản trong khung thời gian lùi tính từ thời điểm thao tác. Giá trị mặc định ban đầu (V11, V12, V16):

| Mã quy tắc | Tên | Đếm gì | Ngưỡng mặc định | Khung thời gian mặc định |
| :--- | :--- | :--- | :--- | :--- |
| `MASS_PATIENT_VIEW` | Xem dữ liệu nhiều bệnh nhân | Số bệnh nhân khác nhau bị xem | 20 | 10 phút |
| `MASS_DOCUMENT_DELETE` | Xoá tài liệu hàng loạt | Số lượt xoá tài liệu / xoá phiên bản tài liệu | 5 | 10 phút |
| `MASS_DOCUMENT_UNPUBLISH` | Gỡ tài liệu hàng loạt | Số lượt gỡ tài liệu | 5 | 10 phút |
| `MASS_PATIENT_LOCK` | Khoá/mở khoá nhiều bệnh nhân | Số bệnh nhân khác nhau bị khoá/mở khoá | 10 | 10 phút |
| `MASS_ROLE_CHANGE` | Đổi vai trò nhiều tài khoản | Số tài khoản khác nhau bị đổi vai trò | 5 | 10 phút |
| `MULTI_IP` | Đăng nhập/thao tác từ nhiều địa chỉ IP | Số địa chỉ IP khác nhau (mọi thao tác, không định vị địa lý) | 4 | 1440 phút (24 giờ) |
| `MASS_CONTENT_DELETE` | Xoá nội dung hàng loạt | Số lượt xoá nội dung | 5 | 10 phút |
| `MASS_CONTENT_UNPUBLISH` | Gỡ nội dung hàng loạt | Số lượt gỡ nội dung | 5 | 10 phút |

Ô "số phút" nhận từ 1 đến 1440, ô ngưỡng từ 1 đến 1000. Ngoài 8 quy tắc trên, thao tác **khôi phục dữ liệu hệ thống** luôn sinh cảnh báo ngay (cố định trong code, không nằm trong bảng quy tắc nên không tắt/chỉnh được).

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Ghi nhận **mọi** thao tác nhạy cảm, không chọn lọc | Sửa tài liệu, đổi phân quyền, khoá/mở tài khoản, truy cập dữ liệu bệnh nhân — không có danh sách "thao tác nhỏ không cần ghi" | Một log thiếu sót làm mất hoàn toàn giá trị khi cần điều tra — không biết trước thao tác nào sẽ quan trọng cho tới khi cần tới nó |
| Không bao giờ cho sửa/xoá bản ghi log đã có | Chỉ đọc, không có nút sửa/xoá ở bất kỳ dòng nào | Một nhật ký có thể bị chỉnh sửa không còn giá trị làm bằng chứng — đây là nguyên tắc bất biến (immutable) bắt buộc với log kiểm tra |
| Cảnh báo bất thường phải **cấu hình được**, không hardcode | Ngưỡng (số lần truy cập, khoảng thời gian) chỉnh được qua giao diện | Ngưỡng hợp lý thay đổi theo quy mô vận hành thực tế (hệ thống nhỏ 4 người làm khác hệ thống có nhiều nhân sự) — không nên phải sửa code mỗi khi cần điều chỉnh |
| Xuất file giữ nguyên bộ lọc đang áp dụng | Giống nguyên tắc ở [SCR-AD-05](SCR-AD-05-DashboardBaoCao.md) | Phục vụ đúng mục đích điều tra 1 sự việc cụ thể, không phải luôn xuất toàn bộ log |

## 4. Tham chiếu

- Mọi màn hình khác trong phân hệ này đều ghi dữ liệu vào đây — xem phần "Tham chiếu" ở từng màn liên quan.
