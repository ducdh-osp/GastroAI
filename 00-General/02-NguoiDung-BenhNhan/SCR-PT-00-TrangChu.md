# SCR-PT-00 — Trang chủ & khung điều hướng

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Không có mã UC riêng trong đề cương — đây là khung điều hướng của toàn phân hệ, thay thế vai trò "chọn chức năng" trước đây gán cho thanh bên (sidebar) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Trang chủ (`/`) là **bảng tổng quan sức khoẻ** của bệnh nhân sau khi đăng nhập; việc chọn chức năng nằm ở **menu dropdown trên header**, có mặt ở mọi màn hình |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao bỏ sidebar, và vì sao Trang chủ là dashboard

Thanh bên cố định (sidebar) liệt kê hết 12+ mục cùng lúc có vấn đề thật: chiếm diện tích màn hình thường trực (đặc biệt bất tiện trên điện thoại — nơi phần lớn bệnh nhân Việt Nam dùng app), và biến mọi chức năng thành "ngang hàng" về mặt thị giác dù tần suất dùng rất khác nhau. Điều hướng được chuyển lên **header**: các chức năng gom thành 4 nhóm dropdown, bệnh nhân nhảy thẳng từ chức năng này sang chức năng khác mà không cần quay về Trang chủ.

Vì điều hướng đã nằm đủ trên header, Trang chủ không cần lặp lại 1 lưới ô bấm nữa — nó trở thành nơi bệnh nhân nhìn nhanh tình trạng của chính mình: chuỗi ngày ghi nhận, xu hướng Bristol, nhật ký ăn uống và nhạy cảm thực phẩm.

## 2. Khung header — cố định trên MỌI màn hình sau đăng nhập

Header xuất hiện xuyên suốt mọi màn hình bệnh nhân đã đăng nhập — kể cả khi đang ở sâu trong 1 chức năng (vd đang ghi nhật ký ăn uống).

```text
— Màn rộng (từ md trở lên) —
┌──────────────────────────────────────────────────────────────────────────────┐
│ 🌿 GastroAI  🏠 Trang chủ  Trò chuyện cùng AI ▾  Ghi nhận hằng ngày ▾          │
│              Xem lại & phân tích ▾  Hồ sơ & kiến thức ▾      💡[◯] 🔔³ (A) Nguyễn Văn A ▾│
└──────────────────────────────────────────────────────────────────────────────┘
                          │ (rê chuột hoặc bấm vào 1 nhóm)
                          ▼
                 ┌──────────────────────┐
                 │ 🍜 Nhật ký ăn uống     │
                 │ 🧪 Nhật ký Bristol     │
                 │ 💊 Nhắc uống thuốc     │
                 └──────────────────────┘

— Màn hẹp (điện thoại) —
┌───────────────────────────────────────────┐
│ 🌿 GastroAI   Chức năng ▾          🔔³ (A) ▾│
└───────────────────────────────────────────┘
   Chức năng ▾ → 🏠 Trang chủ
                 Trò chuyện cùng AI   ▸ (menu con)
                 Ghi nhận hằng ngày   ▸
                 Xem lại & phân tích  ▸
                 Hồ sơ & kiến thức    ▸
```

| Thành phần | Vị trí | Hành vi |
| :--- | :--- | :--- |
| Logo GastroAI | Góc trái | Bấm → về Trang chủ (`/`) từ bất kỳ màn nào |
| Mục "🏠 Trang chủ" | Đầu thanh điều hướng (màn rộng) | Về Trang chủ; được tô sáng khi đang ở `/` |
| 4 nhóm dropdown chức năng | Giữa header (màn rộng) | Rê chuột hoặc bấm để mở danh sách mục, bấm mục → chuyển thẳng tới màn đó. Nhóm chứa màn đang mở được tô sáng |
| Nút "Chức năng ▾" | Thay cho thanh điều hướng trên màn hẹp | Bấm mở 1 dropdown duy nhất: "Trang chủ" + 4 nhóm dạng menu con, cùng các mục như màn rộng |
| Công tắc giao diện sáng/tối | Bên phải (ẩn trên màn rất hẹp) | Bật/tắt chế độ tối |
| 🔔 Chuông thông báo | Bên phải, trước tên người dùng | Badge số chưa đọc, bấm mở [SCR-NT-01 Trung tâm thông báo](../05-ThongBao-Notification/SCR-NT-01-TrungTamThongBao.md) |
| Ảnh đại diện (chữ cái đầu) + tên + mũi tên | Ngoài cùng bên phải (tên ẩn trên màn hẹp) | Bấm mở dropdown: **Đổi mật khẩu** ([SCR-AUTH-06](../01-XacThuc-TaiKhoan/SCR-AUTH-06-DoiMatKhau.md)), **Đăng xuất** (`UC0003`) |

Các nhóm và mục trong menu điều hướng (nguồn: `PATIENT_NAV_GROUPS` trong `fe/src/components/layout/patientNav.ts`):

| Nhóm | Mục | Màn đích |
| :--- | :--- | :--- |
| Trò chuyện cùng AI | Trò chuyện với AI | `/chat` |
| | Kiểm tra triệu chứng | `/symptom-assessment` |
| Ghi nhận hằng ngày | Nhật ký ăn uống | `/food-diary` |
| | Nhật ký Bristol | `/bristol` |
| | Nhắc uống thuốc | `/medications` |
| Xem lại & phân tích | Lịch sử ăn uống | `/food-diary-history` |
| | Xu hướng Bristol | `/bristol-history` |
| | Nhạy cảm thực phẩm | `/food-sensitivity` |
| Hồ sơ & kiến thức | Hồ sơ bệnh lý | `/medical-profile` |
| | Hỏi đáp thường gặp | `/faq` |
| | Bài viết tiêu hoá | `/articles` |

Không có mục "Nhận diện món ăn" vì `UC0044` đã cắt khỏi phạm vi.

## 3. Thiết kế màn Trang chủ (dashboard)

Các khối xếp dọc theo đúng thứ tự dưới đây. Hai thẻ biểu đồ Bristol / Nhật ký ăn uống nằm **2 cột cạnh nhau trên màn rộng** (từ `lg`), **1 cột xếp chồng trên điện thoại**; các khối còn lại luôn chiếm hết chiều ngang.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ (header — xem §2)                                                       │
├──────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ ❤  Tổng quan hôm nay                                                │ │
│ │    Xin chào, Nguyễn Văn A 👋                                        │ │
│ │    Theo dõi đều đặn giúp bạn nhận ra thay đổi trong sức khỏe tiêu   │ │
│ │    hóa và cung cấp thông tin hữu ích hơn khi trao đổi với bác sĩ.   │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│  Thói quen hằng ngày                                  [30 ngày qua ▾]  │
│  Tiến độ theo dõi sức khỏe                                             │
│  Mỗi ngày có ít nhất một ghi nhận ăn uống hoặc Bristol sẽ được tính    │
│  là một ngày duy trì.                                                  │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Hôm nay bạn chưa ghi nhật ký           [☕ Ghi bữa ăn] [🧪 Ghi Bristol]│ │
│ │ Thêm một ghi nhận để tiếp tục chuỗi 5 ngày đang duy trì.           │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│   (thẻ nhắc này chỉ hiện khi hôm nay CHƯA có ghi nhận nào)             │
│ ┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐ │
│ │🔥 Chuỗi ngày duy trì│ │🏆 Kỷ lục duy trì    │ │✅ Mức độ ghi nhật ký│ │
│ │   5 ngày           │ │   14 ngày          │ │   63.3%            │ │
│ │ Hãy ghi một mục    │ │ Tính trong 365 ngày│ │ 19/30 ngày có ghi  │ │
│ │ trong hôm nay.     │ │ gần nhất.          │ │ nhận.              │ │
│ └────────────────────┘ └────────────────────┘ └────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ 📅 Lịch ghi nhật ký                                   19/30 ngày    │ │
│ │ 30 ngày gần nhất                                                    │ │
│ │ [██████████████████████████░░░░░░░░░░░░░░░░] 63%                    │ │
│ │ ■■□■■■□□■■■■■□■■■□■■■□□■■■■□■■   (mỗi ô = 1 ngày, rê chuột xem số   │ │
│ │                                   bữa ăn / lần Bristol của ngày đó)│ │
│ │ ■ Đã ghi   □ Chưa ghi                                               │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌───────────────────────────────┐ ┌───────────────────────────────┐ │
│ │ Xu hướng Bristol               │ │ Nhật ký ăn uống                │ │
│ │ (14 ngày gần nhất)             │ │ (14 ngày gần nhất)             │ │
│ │ 7┤                             │ │ 3┤      ▇                      │ │
│ │ 4┤  ●──●     ●──●──●           │ │ 2┤ ▇  ▇ ▇    ▇ ▇               │ │
│ │ 1┤        ●                    │ │ 1┤ ▇▇ ▇ ▇ ▇  ▇ ▇ ▇             │ │
│ │   13/11 ……………………… 26/11        │ │   13/11 ……………………… 26/11        │ │
│ │ (đường gãy, ngày chưa ghi để   │ │ (cột = số bữa ghi trong ngày)  │ │
│ │  trống — không nối qua)        │ │                                │ │
│ └───────────────────────────────┘ └───────────────────────────────┘ │
│   ↑ 2 cột trên màn rộng (lg) — 1 cột xếp chồng trên điện thoại        │
│                                                                        │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Nhạy cảm thực phẩm trong 90 ngày                    [Xem đầy đủ]   │ │
│ │ Tỷ lệ bữa ăn kèm triệu chứng tính chung: 25%                        │ │
│ │                          ┆Mức nền                                   │ │
│ │ Sữa         ███████████████████████  70%   (đỏ: cao hơn hẳn mức nền)│ │
│ │ Đồ chiên    ███████████████  40%           (xanh: hơi cao hơn)      │ │
│ │ Cơm         ██████  20%                    (xám: không cao hơn)     │ │
│ │ (tối đa 6 món, thanh ngang 0–100%)                                  │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.1. Nội dung từng khối

| Khối | Nguồn dữ liệu (API) | Hiển thị |
| :--- | :--- | :--- |
| Thẻ chào | Thông tin đăng nhập | Dòng nhỏ "Tổng quan hôm nay", tiêu đề "Xin chào, `<họ tên>` 👋" (chưa có họ tên thì dùng email), 1 câu khuyến khích theo dõi đều đặn |
| Tiến độ theo dõi sức khỏe | `GET /patient/adherence?days=…` | Bộ chọn khoảng thời gian **7 / 30 / 90 ngày qua** (mặc định 30). Gồm thẻ nhắc (khi hôm nay chưa ghi), 3 thẻ số liệu (Chuỗi ngày duy trì, Kỷ lục duy trì — tính trong 365 ngày gần nhất, Mức độ ghi nhật ký %) và thẻ "Lịch ghi nhật ký" (thanh tiến độ + mỗi ngày 1 ô màu). 1 ngày được tính là "đã ghi" khi có ít nhất 1 ghi nhận ăn uống **hoặc** Bristol. Chi tiết nghiệp vụ streak: [SCR-PT-06](SCR-PT-06-StreakTuanThu.md) |
| Xu hướng Bristol (14 ngày gần nhất) | `GET /patient/bristol-logs/trend?days=14` | Biểu đồ đường, trục dọc cố định loại 1–7, trục ngang luôn đủ 14 ngày (dd/m). Ngày không ghi để trống, đường không nối qua ngày đó |
| Nhật ký ăn uống (14 ngày gần nhất) | `GET /patient/food-diary/trend?days=14` | Biểu đồ cột số bữa đã ghi mỗi ngày, trục ngang luôn đủ 14 ngày; ngày không ghi = 0 |
| Nhạy cảm thực phẩm trong 90 ngày | `GET /patient/food-diary/sensitivity-profile?days=90` | Dòng "Tỷ lệ bữa ăn kèm triệu chứng tính chung: X%", rồi biểu đồ thanh ngang tối đa 6 món có tỷ lệ kèm triệu chứng cao nhất, có vạch đứt "Mức nền". Màu thanh: đỏ khi cao hơn mức nền từ 20 điểm % trở lên, xanh teal khi cao hơn mức nền dưới 20 điểm, xám khi không cao hơn. Rê chuột xem "X% (a/b bữa)" |

### 3.2. Trạng thái đặc biệt

| Tình huống | Hành vi thật của màn hình |
| :--- | :--- |
| Đang tải 3 thẻ biểu đồ (Bristol, Ăn uống, Nhạy cảm thực phẩm) | Mỗi thẻ hiện khung Skeleton (3 dòng) cho tới khi API của thẻ đó trả về; 3 thẻ tải độc lập với nhau |
| Đang tải khối Tiến độ theo dõi / đổi khoảng 7–30–90 ngày | Khối hiện biểu tượng xoay (Spin) phủ lên nội dung |
| Chưa có dữ liệu Bristol / ăn uống trong 14 ngày | Thẻ vẫn vẽ đủ trục và lưới 14 ngày: Bristol không có điểm nào, cột ăn uống toàn bằng 0 — không hiện câu thông báo riêng |
| Lỗi tải Bristol / ăn uống | Xử lý như không có dữ liệu (biểu đồ rỗng), không hiện thông báo lỗi |
| Chưa đủ dữ liệu nhạy cảm thực phẩm (API trả `hasEnoughData = false`, hoặc danh sách món tương quan rỗng) | Thẻ hiện câu: "Chưa đủ dữ liệu để phân tích — ghi thêm nhật ký ăn uống kèm triệu chứng để AI gợi ý món ăn hay đi kèm triệu chứng của riêng bạn." |
| Lỗi tải nhạy cảm thực phẩm | Thẻ hiện "Không tải được dữ liệu" kèm câu "Đã có lỗi khi tải phân tích nhạy cảm thực phẩm. Vui lòng thử lại." và nút **Thử lại** — bấm thì thẻ về Skeleton và gọi lại API |
| Lỗi tải Tiến độ theo dõi | Hiện thông báo lỗi màu đỏ phía trên khối ("Không thể tải tiến độ theo dõi sức khỏe…") |
| Nút "Xem đầy đủ" (thẻ Nhạy cảm thực phẩm) | Chuyển tới `/food-sensitivity` — màn [SCR-PT-10](SCR-PT-10-NhayCamThucPham.md) |
| Hôm nay **chưa** có ghi nhận, chuỗi = 0 | Thẻ nhắc "Hôm nay bạn chưa ghi nhật ký" + câu "Thêm một ghi nhận để bắt đầu thói quen theo dõi từ hôm nay." + 2 nút **Ghi bữa ăn** (→ `/food-diary`) và **Ghi Bristol** (→ `/bristol`). Thẻ "Chuỗi ngày duy trì" hiện "0 ngày" kèm "Hãy ghi một mục trong hôm nay." |
| Hôm nay **chưa** có ghi nhận, chuỗi > 0 | Thẻ nhắc như trên nhưng câu là "Thêm một ghi nhận để tiếp tục chuỗi N ngày đang duy trì." Thẻ chuỗi hiện "N ngày" kèm "Hãy ghi một mục trong hôm nay." |
| Hôm nay **đã** có ghi nhận | Không hiện thẻ nhắc; thẻ chuỗi hiện "N ngày" kèm "Hôm nay đã có ghi nhận." |

## 4. Nguyên tắc thiết kế

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Điều hướng ở header, Trang chủ chỉ để xem tổng quan | Không có lưới ô bấm chức năng trên Trang chủ; mọi chức năng chọn qua 4 nhóm dropdown trên header | Header có mặt ở mọi màn nên bệnh nhân chuyển thẳng giữa các chức năng, không phải quay về Trang chủ; 1 lưới ô ở Trang chủ chỉ lặp lại đúng menu đó |
| Nhóm menu theo **ý định sử dụng**, không theo thứ tự UC | 4 nhóm: Trò chuyện cùng AI / Ghi nhận hằng ngày / Xem lại & phân tích / Hồ sơ & kiến thức | Bệnh nhân nghĩ theo "tôi muốn làm gì" chứ không theo cấu trúc UC trong đề cương |
| "Trò chuyện cùng AI" luôn là nhóm đầu tiên | Thứ tự nhóm cố định, không đổi theo hành vi dùng | Đây là giá trị cốt lõi của sản phẩm |
| Trang chủ hiển thị số liệu cá nhân thật | Chuỗi ngày duy trì, xu hướng Bristol, nhật ký ăn uống, nhạy cảm thực phẩm — lấy từ dữ liệu bệnh nhân tự ghi | Vài con số cá nhân hoá khiến Trang chủ cảm giác như "nhà của tôi", đúng tinh thần khuyến khích quay lại mỗi ngày đã nêu ở `00-TongQuan.md` §4.1 |
| Có lối tắt ghi nhận ngay khi hôm nay chưa ghi | Thẻ nhắc kèm 2 nút Ghi bữa ăn / Ghi Bristol | Giữ chuỗi ngày duy trì không bị đứt chỉ với 1 lần bấm |
| Biểu đồ luôn giữ đủ khung 14 ngày | Vẽ đủ trục/lưới kể cả khi chưa có dữ liệu | Tránh vỡ bố cục khi bệnh nhân mới chưa ghi ngày nào |
| Không có mục dẫn tới chức năng đã cắt khỏi phạm vi | Không có "Chụp ảnh món ăn" (`UC0044` đã cắt) | Giao diện phản ánh đúng những gì thật sự làm được |

## 5. Tham chiếu

- Mọi màn đích của menu: xem [00-TongQuan.md](00-TongQuan.md) §1 danh sách màn hình
- Streak & tuân thủ: [SCR-PT-06-StreakTuanThu.md](SCR-PT-06-StreakTuanThu.md)
- Thông báo: `05-ThongBao-Notification/SCR-NT-01-TrungTamThongBao.md`
- Khung tương đương phía CMS: `04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md`
- Code: `fe/src/pages/patient/HomePage.tsx`, `fe/src/components/home/HealthTrackingProgress.tsx`, `fe/src/components/layout/AppHeader.tsx`, `fe/src/components/layout/patientNav.ts`
