# SCR-AD-00 — Trang chủ & khung điều hướng (CMS)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Không có mã UC riêng — khung điều hướng của toàn phân hệ, thay thế vai trò "chọn chức năng" trước đây gán cho thanh bên (sidebar) |
| Actor | Admin, Biên tập chuyên môn y khoa (mỗi vai trò thấy bộ ô khác nhau — xem §3) |
| Mục tiêu nghiệp vụ | Nơi duy nhất để chọn chức năng quản trị sau khi đăng nhập CMS |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Cùng nguyên tắc với phía bệnh nhân, áp cho người dùng thành thạo hơn

[SCR-PT-00-TrangChu.md](../02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md) đã giải thích lý do chung bỏ sidebar. Khác biệt duy nhất ở đây: người dùng CMS là nhân sự nội bộ dùng lặp lại hằng ngày, nên Trang chủ CMS có thể **dày đặc thông tin hơn** (nhiều ô hơn, không cần icon to như phía bệnh nhân) — nhưng vẫn giữ nguyên tắc cốt lõi: không menu cố định bên cạnh, mọi điều hướng qua đây.

## 2. Khung header — cố định trên mọi màn hình CMS

```text
┌───────────────────────────────────────────────────────────┐
│ 🌿 GastroAI — Quản trị      (tông màu tối, khác cổng bệnh nhân)   🔔² Admin Minh ▾│
└───────────────────────────────────────────────────────────┘
```

| Thành phần | Hành vi |
| :--- | :--- |
| Icon/logo GastroAI | Bấm → về Trang chủ CMS, từ bất kỳ màn nào |
| 🔔 Chuông thông báo | Mở trung tâm thông báo CMS (`SCR-NT-01`, audience Admin) — chỉ hiện với Admin, vì chỉ Admin nhận thông báo hệ thống (xem `05-ThongBao-Notification/`) |
| Tên người dùng + mũi tên | Dropdown: **Đổi mật khẩu**, **Lịch sử đăng nhập**, **Đăng xuất** — đúng cơ chế `01-XacThuc-TaiKhoan/`, chỉ khác route `/cms/...` |

## 3. Thiết kế màn Trang chủ — số ô thay đổi theo vai trò

```text
— Admin đăng nhập —                      — Biên tập chuyên môn y khoa đăng nhập —
┌─────────────────────────────┐         ┌─────────────────────────────┐
│ Chào Admin Minh                 │         │ Chào BS. Thu Hà (biên tập)       │
│                                   │         │                                   │
│ Tài khoản                        │         │ Kho tri thức                     │
│ ┌──────────┐                     │         │ ┌──────────┐┌──────────┐         │
│ │👤 Quản lý  │                     │         │ │📄 Tài liệu ││📚 Bài viết │         │
│ │  người dùng│                     │         │ │  y khoa    ││  kiến thức │         │
│ └──────────┘                     │         │ └──────────┘└──────────┘         │
│ ┌──────────┐                     │         │ ┌──────────┐┌──────────┐         │
│ │🔑 Phân quyền│                     │         │ │🏷 Chủ đề    ││❓ FAQ      │         │
│ │  nhân sự   │                     │         │ │  bài viết  ││            │         │
│ └──────────┘                     │         │ └──────────┘└──────────┘         │
│                                   │         │ ┌──────────┐┌──────────────┐    │
│ Kho tri thức                     │         │ │💊 Danh mục ││🚨 Từ khoá      │    │
│ ┌──────────┐┌──────────┐         │         │ │  chuẩn hoá ││  sàng lọc khẩn│    │
│ │📄 Tài liệu ││📚 Bài viết │         │         │ └──────────┘│  cấp          │    │
│ │  y khoa    ││  kiến thức │         │         │             └──────────────┘    │
│ └──────────┘└──────────┘         │         └─────────────────────────────┘
│ ┌──────────┐┌──────────┐         │
│ │🏷 Chủ đề    ││❓ FAQ      │         │
│ │  bài viết  ││            │         │
│ └──────────┘└──────────┘         │
│ ┌──────────┐┌──────────────┐    │
│ │💊 Danh mục ││🚨 Từ khoá      │    │
│ │  chuẩn hoá ││  sàng lọc khẩn│    │
│ └──────────┘│  cấp          │    │
│             └──────────────┘    │
│                                   │
│ Giám sát                         │
│ ┌──────────────┐┌──────────┐    │
│ │🚨 Cảnh báo     ││📊 Dashboard│    │
│ │  khẩn cấp      ││  thống kê  │    │
│ └──────────────┘└──────────┘    │
│ ┌──────────────┐                 │
│ │📜 Nhật ký        │                 │
│ │  hoạt động       │                 │
│ └──────────────┘                 │
│                                   │
│ Cấu hình                         │
│ ┌──────────────┐                 │
│ │⚙ Cấu hình       │                 │
│ │  RAG/Triage      │                 │
│ └──────────────┘                 │
└─────────────────────────────┘
```

## 4. Nguyên tắc thiết kế

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Chỉ hiện ô thuộc quyền của vai trò đang đăng nhập | Biên tập chuyên môn y khoa không thấy nhóm "Tài khoản", "Giám sát", "Cấu hình" | Đúng ma trận phân quyền đã định nghĩa ở từng màn (vd [SCR-AD-02](SCR-AD-02-QuanLyNguoiDung.md), [SCR-AD-08](SCR-AD-08-CauHinhRAGTriage.md)) — không hiện ô dẫn tới nơi không có quyền vào, tránh gây hiểu nhầm "có quyền mà không thấy" |
| Giữ đúng 4 nhóm cũ từng có trên sidebar | Tài khoản / Người dùng / Kho tri thức / Giám sát / Cấu hình | Nhân sự nội bộ đã quen cách phân nhóm này — đổi cách điều hướng (bỏ sidebar) không cần đổi luôn cách tổ chức thông tin, giảm chi phí học lại |
| Không có ô "Đánh giá AI & Triage" | Báo cáo chất lượng AI (`UC0037`/`UC0038`) hiện chưa có màn hình riêng trong tài liệu này (xem `03-AIChatbot-RAG/00-TongQuan.md` §2.5, §3.5) | Trang chủ chỉ dẫn tới màn đã có đặc tả — không tạo ô dẫn tới nơi chưa thiết kế |

## 5. Tham chiếu

- Mọi màn đích: xem [00-TongQuan.md](00-TongQuan.md) §1 danh sách màn hình
- Khung tương đương phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md`
