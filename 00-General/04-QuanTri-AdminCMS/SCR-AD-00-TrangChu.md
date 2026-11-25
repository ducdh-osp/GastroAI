# SCR-AD-00 — Trang chủ & khung điều hướng (CMS)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Không có mã UC riêng — khung điều hướng của toàn phân hệ, thay thế vai trò "chọn chức năng" trước đây gán cho thanh bên (sidebar) |
| Actor | Mọi tài khoản CMS: Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành (menu header khác nhau theo vai trò — xem §2.1; số liệu dashboard chỉ Admin và Giám sát vận hành xem — xem §3.4) |
| Mục tiêu nghiệp vụ | Trang chủ CMS (`/cms`) là **bảng tổng quan vận hành hệ thống** (14 ngày gần nhất) với Admin và Giám sát vận hành, là **lời chào + lối tắt** với Biên tập chuyên môn y khoa và Cộng tác viên nội dung; việc chọn chức năng quản trị nằm ở **menu dropdown trên header**, có mặt ở mọi màn hình CMS |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Cùng nguyên tắc với phía bệnh nhân

[SCR-PT-00-TrangChu.md](../02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md) đã giải thích lý do chung bỏ sidebar. CMS áp dụng đúng mô hình đó: điều hướng giữa các chức năng đi qua các nhóm dropdown trên header (chuyển thẳng từ màn này sang màn khác, không cần quay về Trang chủ), còn Trang chủ là dashboard — không còn lưới ô bấm chức năng, vì lưới ô chỉ lặp lại đúng những mục đã có trên header.

Khác biệt với phía bệnh nhân: số liệu trên Trang chủ CMS là số liệu **toàn hệ thống** (lượt chat, cảnh báo Triage, triệu chứng…) chứ không phải dữ liệu cá nhân, và đây là bản xem nhanh 14 ngày của màn [SCR-AD-05 Dashboard thống kê](SCR-AD-05-DashboardBaoCao.md).

## 2. Khung header — cố định trên mọi màn hình CMS

```text
— Màn rộng (từ md trở lên), Admin đăng nhập —
┌──────────────────────────────────────────────────────────────────────────────┐
│ 🌿 GastroAI  🏠 Trang chủ  Tài khoản ▾  Kho tri thức ▾  Giám sát ② ▾  Cấu hình ▾│
│                                                 💡[◯] 🔔² (M) Admin Minh ▾    │
│                                                               Admin           │
└──────────────────────────────────────────────────────────────────────────────┘
   (nền xanh đen #0f172a — tối hơn header cổng bệnh nhân)
                                   │ (rê chuột hoặc bấm vào nhóm "Giám sát")
                                   ▼
                       ┌──────────────────────────────┐
                       │ 🛡 Cảnh báo khẩn cấp      ②   │
                       │ 🧪 Đánh giá AI & Triage        │
                       │ 🔊 Thông báo broadcast         │
                       │ 📊 Thống kê hệ thống           │
                       │ 📜 Nhật ký hoạt động           │
                       └──────────────────────────────┘

— Màn hẹp (điện thoại) —
┌───────────────────────────────────────────┐
│ 🌿 GastroAI   Chức năng ② ▾        🔔² (M) ▾│
└───────────────────────────────────────────┘
   Chức năng ▾ → 🏠 Trang chủ
                 Tài khoản     ▸ (menu con)
                 Kho tri thức  ▸
                 Giám sát      ▸
                 Cấu hình      ▸
```

| Thành phần | Vị trí | Hành vi |
| :--- | :--- | :--- |
| Logo GastroAI | Góc trái | Bấm → về Trang chủ CMS (`/cms`) từ bất kỳ màn nào |
| Mục "🏠 Trang chủ" | Đầu thanh điều hướng (màn rộng) | Về `/cms`; được tô sáng khi đang ở Trang chủ |
| Các nhóm dropdown chức năng | Giữa header (màn rộng) | Rê chuột hoặc bấm để mở danh sách mục, bấm mục → chuyển thẳng tới màn đó. Nhóm chứa màn đang mở được tô sáng. **Chỉ hiện nhóm/mục mà vai trò đang đăng nhập được thấy** (§2.1); nhóm không còn mục nào thì ẩn luôn |
| Badge số ca "cần can thiệp" | Cạnh mục "Cảnh báo khẩn cấp", nhóm "Giám sát" và nút "Chức năng" | Chỉ với Admin và Giám sát vận hành: số ca Triage đang ở trạng thái cần can thiệp ([SCR-AD-13](SCR-AD-13-GiamSatCanhBao.md)); cập nhật theo sự kiện realtime và tự làm mới mỗi 60 giây phòng khi mất kết nối; ẩn khi = 0 |
| Nút "Chức năng ▾" | Thay cho thanh điều hướng trên màn hẹp | Bấm mở 1 dropdown duy nhất: "Trang chủ" + các nhóm dạng menu con, cùng các mục như màn rộng |
| Công tắc giao diện sáng/tối | Bên phải (ẩn trên màn rất hẹp) | Bật/tắt chế độ tối |
| 🔔 Chuông thông báo | Bên phải, trước tên người dùng | **Chỉ hiện với Admin** — mở trung tâm thông báo CMS (`/cms/notifications`, [SCR-NT-01](../05-ThongBao-Notification/SCR-NT-01-TrungTamThongBao.md), audience Admin), badge số chưa đọc |
| Ảnh đại diện (chữ cái đầu) + tên + nhãn vai trò + mũi tên | Ngoài cùng bên phải (tên và nhãn ẩn trên màn hẹp) | Nhãn vai trò dưới tên hiện đúng tên ở [SCR-AD-03](SCR-AD-03-PhanQuyenRBAC.md): "Admin" / "Biên tập chuyên môn y khoa" / "Cộng tác viên nội dung" / "Giám sát vận hành" (tài khoản Bác sĩ cũ: "Bác sĩ"). Bấm mở dropdown: **Lịch sử đăng nhập** (`/cms/login-history`), **Đổi mật khẩu** (`/cms/change-password`), **Đăng xuất** |

### 2.1. Nhóm và mục menu theo vai trò

Nguồn: `CMS_NAV_GROUPS` + `filterCmsNavGroups` trong `fe/src/components/cms/cmsNav.ts`. Mỗi mục khai báo danh sách `roles` đúng theo ma trận [SCR-AD-03 §4](SCR-AD-03-PhanQuyenRBAC.md); chỉ vai trò có trong danh sách mới thấy mục đó.

| Nhóm | Mục | Màn đích | Admin | Biên tập chuyên môn y khoa | Cộng tác viên nội dung | Giám sát vận hành |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| Tài khoản | Quản lý người dùng | `/cms/users` | ● | | | |
| | Phân quyền nhân sự | `/cms/rbac` | ● | | | |
| Kho tri thức | Tài liệu y khoa | `/cms/documents` | ● | ● | | |
| | Bài viết kiến thức | `/cms/articles` | ● | ● | ● | |
| | Chủ đề bài viết | `/cms/article-topics` | ● | ● | ● | |
| | Câu hỏi thường gặp | `/cms/faq` | ● | ● | ● | |
| | Danh mục chuẩn hoá | `/cms/catalogs` | ● | ● | | |
| | Từ khoá Triage | `/cms/triage-keywords` | ● | ● | | |
| Giám sát | Cảnh báo khẩn cấp | `/cms/triage-alerts` | ● | | | ● |
| | Đánh giá AI & Triage | `/cms/evaluation` | ● | | | |
| | Thông báo broadcast | `/cms/broadcast` | ● | | | |
| | Thống kê hệ thống | `/cms/dashboard` | ● | | | ● |
| | Nhật ký hoạt động | `/cms/audit-logs` | ● | | | |
| Cấu hình | Cấu hình RAG/Triage | `/cms/runtime-config` | ● | | | |
| | Mẫu email | `/cms/email-templates` | ● | | | |

Hệ quả: Admin thấy đủ 4 nhóm; Biên tập chuyên môn y khoa và Cộng tác viên nội dung chỉ thấy nhóm Kho tri thức (lần lượt 6 và 3 mục); Giám sát vận hành chỉ thấy nhóm Giám sát (Cảnh báo khẩn cấp, Thống kê hệ thống). "Đánh giá AI & Triage" chưa có trong ma trận nên giữ chỉ Admin, khớp API `/api/v1/cms/evaluation/**`.

Phía backend, `SecurityConfig` cho Admin và Giám sát vận hành gọi `/api/v1/cms/dashboard/**`, `/api/v1/cms/triage-alerts/**` và kết nối realtime `/ws/**`. Qua `/ws`, Giám sát vận hành chỉ đăng ký được các kênh cảnh báo Triage, **không** nhận kênh thông báo của Admin. Route `/cms/dashboard` và `/cms/triage-alerts` phía FE cũng chặn theo đúng 2 vai trò này: vào bằng URL khi không có quyền thì bị đưa về `/cms`.

## 3. Thiết kế màn Trang chủ (dashboard)

Các khối xếp dọc theo đúng thứ tự dưới đây. 4 thẻ số liệu nằm **4 cột trên màn rộng** (từ `lg`), **2 cột trên điện thoại**; 2 thẻ biểu đồ "Lượt chat theo ngày" / "Phân bố cảnh báo Triage" nằm **2 cột trên màn rộng**, **1 cột xếp chồng trên điện thoại**; thẻ chào và thẻ "Top triệu chứng" luôn chiếm hết chiều ngang.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ (header — xem §2)                                                       │
├──────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ 📊 Tổng quan hệ thống                                               │ │
│ │    Chào Admin Minh - Admin 👋                                       │ │
│ │    Số liệu vận hành 14 ngày gần nhất. Mọi chức năng quản trị nằm    │ │
│ │    trên menu header phía trên.                                      │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│ │👥 Người dùng  │ │💬 Lượt Chat AI│ │🚨 Cảnh báo    │ │😊 Hài lòng AI │ │
│ │   128        │ │   1.540      │ │   Triage     │ │   86.4%      │ │
│ │              │ │              │ │   12 [3 mới] │ │              │ │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ │
│   ↑ 4 cột trên màn rộng (lg) — 2 cột trên điện thoại                  │
│                                                                        │
│ ┌───────────────────────────────┐ ┌───────────────────────────────┐ │
│ │ Lượt chat theo ngày            │ │ Phân bố cảnh báo Triage        │ │
│ │ (14 ngày gần nhất)             │ │                                │ │
│ │ 150┤        ╱╲                 │ │          ╭──────╮              │ │
│ │ 100┤  ╱╲   ╱  ╲  ╱╲            │ │        ╭─╯ ◯◯◯◯ ╰─╮            │ │
│ │  50┤▒╱▒▒╲▒╱▒▒▒▒╲╱▒▒╲▒          │ │        ╰─╮      ╭─╯            │ │
│ │     13/11 ……………………… 26/11       │ │          ╰──────╯              │ │
│ │ (biểu đồ vùng, tổng tin nhắn)  │ │ (biểu đồ vành khuyên theo nhóm)│ │
│ └───────────────────────────────┘ └───────────────────────────────┘ │
│   ↑ 2 cột trên màn rộng (lg) — 1 cột xếp chồng trên điện thoại        │
│                                                                        │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Top triệu chứng tiêu hoá được báo cáo               [Xem đầy đủ]   │ │
│ │  40┤ ▇▇                                                             │ │
│ │  20┤ ▇▇   ▇▇   ▇▇                                                   │ │
│ │   0┤ ▇▇   ▇▇   ▇▇   ▇▇   ▇▇                                         │ │
│ │     Đau bụng  Ợ nóng  Tiêu chảy  Táo bón  Buồn nôn                  │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.1. Nguồn dữ liệu

Toàn bộ khối số liệu lấy từ **1 API duy nhất**: `GET /api/v1/cms/dashboard/stats?days=14` (`DashboardAdminController` → `DashboardAdminService.getStats`) — cùng API với màn [SCR-AD-05](SCR-AD-05-DashboardBaoCao.md), nhưng Trang chủ cố định 14 ngày, không có bộ chọn khoảng ngày và không có nút xuất file. Khoảng 14 ngày tính theo giờ Việt Nam, từ 00:00 ngày (hôm nay − 13) tới hết hôm nay.

### 3.2. Nội dung từng khối

| Khối | Hiển thị | Ý nghĩa số liệu (theo backend) |
| :--- | :--- | :--- |
| Thẻ chào | Dòng nhỏ "Tổng quan hệ thống", tiêu đề "Chào `<họ tên>` - `<vai trò>` 👋" (chưa có họ tên thì dùng email), câu "Số liệu vận hành 14 ngày gần nhất. Mọi chức năng quản trị nằm trên menu header phía trên." | Nhãn vai trò: Admin / Bác sĩ / Biên tập chuyên môn y khoa / Cộng tác viên nội dung / Giám sát vận hành |
| Người dùng | 1 con số | Số bệnh nhân **khác nhau có hoạt động** trong 14 ngày: có gửi tin nhắn chat hoặc làm kiểm tra triệu chứng (không phải tổng số tài khoản đã đăng ký) |
| Lượt Chat AI | 1 con số | Tổng số tin nhắn chat (cả của bệnh nhân lẫn của AI) tạo trong 14 ngày |
| Cảnh báo Triage | 1 con số + thẻ đỏ "N mới" (chỉ hiện khi N > 0) | Tổng số cảnh báo Triage phát sinh trong 14 ngày; "mới" = số ca trong đó đang ở trạng thái **cần can thiệp** (xem [SCR-AD-13](SCR-AD-13-GiamSatCanhBao.md)) |
| Hài lòng AI | Phần trăm | Hữu ích ÷ (Hữu ích + Không hữu ích) × 100 trên các lượt đánh giá câu trả lời AI trong 14 ngày, làm tròn 1 chữ số thập phân; chưa có lượt đánh giá nào → 0% |
| Lượt chat theo ngày (14 ngày gần nhất) | Biểu đồ vùng, trục ngang dd/mm, rê chuột xem "Tổng tin nhắn" | Mỗi ngày 1 điểm, backend tự điền 0 cho ngày không có tin nhắn nên trục luôn đủ 14 ngày |
| Phân bố cảnh báo Triage | Biểu đồ vành khuyên, mỗi lát 1 nhóm dấu hiệu nguy hiểm, rê chuột xem "N ca" | Đếm số lần mỗi nhóm được khớp trong các cảnh báo 14 ngày (1 cảnh báo khớp nhiều nhóm thì cộng cho từng nhóm), sắp giảm dần |
| Top triệu chứng tiêu hoá được báo cáo | Biểu đồ cột theo tên triệu chứng, rê chuột xem "N ca"; nút **Xem đầy đủ** | Số lần kiểm tra triệu chứng trong 14 ngày, gom theo **triệu chứng chính** của mỗi lần kiểm tra, sắp giảm dần (không giới hạn số cột) |

### 3.3. Trạng thái đặc biệt

| Tình huống | Hành vi thật của màn hình |
| :--- | :--- |
| Đang tải | Thẻ chào hiện ngay; toàn bộ phần số liệu bên dưới (4 thẻ + 3 biểu đồ) được thay bằng **1 khung Skeleton chung** (4 dòng) cho tới khi API trả về |
| Lỗi tải API (mất mạng, timeout, lỗi máy chủ…) | Thay phần số liệu bằng hộp báo lỗi màu đỏ "Không tải được số liệu tổng quan", kèm nội dung lỗi và nút **Thử lại** (bấm thì gọi lại API) |
| Phiên CMS hết hạn (API trả 401) | Bộ xử lý chung của CMS xoá trạng thái đăng nhập và đưa về `/cms/login` |
| Chưa có cảnh báo Triage nào trong 14 ngày | Thẻ "Phân bố cảnh báo Triage" hiện ô trống với câu "Chưa có cảnh báo Triage nào." |
| Chưa có ghi nhận triệu chứng trong 14 ngày | Thẻ "Top triệu chứng…" hiện ô trống với câu "Chưa có ghi nhận triệu chứng trong khoảng thời gian này." |
| Không có tin nhắn chat nào trong 14 ngày | Biểu đồ "Lượt chat theo ngày" vẫn vẽ đủ 14 ngày, toàn bằng 0 — không hiện câu thông báo riêng |
| Nút "Xem đầy đủ" | Chuyển tới `/cms/dashboard` — màn [SCR-AD-05](SCR-AD-05-DashboardBaoCao.md), nơi đổi được khoảng ngày và xuất báo cáo |

### 3.4. Trang chủ theo vai trò

| Vai trò | Nội dung bên dưới thẻ chào |
| :--- | :--- |
| Admin, Giám sát vận hành | Dashboard như §3–§3.3 (gọi API thống kê) |
| Biên tập chuyên môn y khoa, Cộng tác viên nội dung | **Không gọi API thống kê.** Câu phụ của thẻ chào đổi thành "Chọn nhanh chức năng bạn được dùng bên dưới, hoặc qua menu header phía trên.", tiếp theo là các thẻ lối tắt xếp theo nhóm (3 cột trên màn rộng, 2 cột từ `sm`, 1 cột trên điện thoại). Mỗi thẻ gồm icon, tên và mô tả ngắn của màn; bấm vào thì chuyển tới màn đó. Danh sách dùng chung nguồn với menu header (§2.1) nên luôn khớp quyền. Nếu không có mục nào thì hiện "Tài khoản của bạn chưa được gán chức năng nào." |

```text
— Biên tập chuyên môn y khoa đăng nhập —
┌──────────────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ 📊 Tổng quan hệ thống                                               │ │
│ │    Chào BS. Thu Hà - Biên tập chuyên môn y khoa 👋                  │ │
│ │    Chọn nhanh chức năng bạn được dùng bên dưới, hoặc qua menu       │ │
│ │    header phía trên.                                                │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│  Kho tri thức                                                          │
│ ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐ │
│ │📄 Tài liệu y khoa   │ │📚 Bài viết kiến thức│ │🏷 Chủ đề bài viết   │ │
│ │ Tải lên, quản lý…  │ │ Soạn & duyệt bài…  │ │ Quản lý chủ đề…    │ │
│ └────────────────────┘ └────────────────────┘ └────────────────────┘ │
│ ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐ │
│ │❓ Câu hỏi thường gặp│ │💊 Danh mục chuẩn hoá│ │🚨 Từ khoá Triage    │ │
│ │ Soạn & duyệt FAQ…  │ │ Danh mục triệu…    │ │ Quản lý từ khoá…   │ │
│ └────────────────────┘ └────────────────────┘ └────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

## 4. Nguyên tắc thiết kế

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Điều hướng ở header, Trang chủ chỉ để xem tổng quan | Không có lưới ô bấm chức năng trên Trang chủ; mọi chức năng chọn qua các nhóm dropdown trên header | Header có mặt ở mọi màn nên nhân sự chuyển thẳng giữa các chức năng; 1 lưới ô ở Trang chủ chỉ lặp lại đúng menu đó |
| Chỉ hiện mục menu thuộc quyền của vai trò đang đăng nhập | Lọc theo `roles` của từng mục, đúng ma trận SCR-AD-03 (§2.1); nhóm rỗng thì ẩn cả nhóm | Không hiện mục dẫn tới nơi không có quyền vào, tránh gây hiểu nhầm |
| Giữ 4 nhóm quen thuộc từ sidebar cũ | Tài khoản / Kho tri thức / Giám sát / Cấu hình | Đổi cách điều hướng (bỏ sidebar) không cần đổi luôn cách tổ chức thông tin, giảm chi phí học lại cho nhân sự nội bộ |
| Vai trò không xem số liệu thì không gọi API số liệu | Biên tập / Cộng tác viên thấy lối tắt thay cho dashboard (§3.4) | Không gọi 1 API chắc chắn bị từ chối; vai trò nội dung vẫn có điểm bắt đầu hữu ích thay vì 1 trang trống |
| Trang chủ là bản xem nhanh, không thay màn Dashboard đầy đủ | Cố định 14 ngày, không bộ lọc, không xuất file; nút "Xem đầy đủ" dẫn sang SCR-AD-05 | Trang chủ để nắm tình hình trong vài giây; phân tích sâu và báo cáo làm ở màn chuyên trách |
| Việc cần làm ngay được đẩy lên header | Badge số ca cần can thiệp cạnh "Cảnh báo khẩn cấp" và thẻ "N mới" trên Trang chủ | Admin và Giám sát vận hành thấy có ca chờ xử lý ở bất kỳ màn nào, không phải mở Trang chủ mới biết |

## 5. Tham chiếu

- Mọi màn đích: xem [00-TongQuan.md](00-TongQuan.md) §1 danh sách màn hình
- Dashboard đầy đủ: [SCR-AD-05-DashboardBaoCao.md](SCR-AD-05-DashboardBaoCao.md)
- Ma trận phân quyền 4 vai trò: [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md)
- Khung tương đương phía bệnh nhân: `02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md`
- Code: `fe/src/pages/cms/CmsHomePage.tsx`, `fe/src/api/cmsDashboard.ts`, `fe/src/components/cms/CmsHeader.tsx`, `fe/src/components/cms/cmsNav.ts`, `be/src/main/java/vn/gastroai/be/api/cms/dashboard/DashboardAdminController.java`, `be/src/main/java/vn/gastroai/be/config/SecurityConfig.java`
