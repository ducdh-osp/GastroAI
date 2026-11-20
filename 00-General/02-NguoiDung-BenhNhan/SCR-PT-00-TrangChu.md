# SCR-PT-00 — Trang chủ & khung điều hướng

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | Không có mã UC riêng trong đề cương — đây là khung điều hướng của toàn phân hệ, thay thế vai trò "chọn chức năng" trước đây gán cho thanh bên (sidebar) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Nơi **duy nhất** để chọn chức năng sau khi đăng nhập — không còn menu cố định liệt kê sẵn mọi chức năng ở cạnh màn hình |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao đổi từ sidebar sang Trang chủ

Thanh bên cố định (sidebar) liệt kê hết 12+ mục cùng lúc có vấn đề thật: chiếm diện tích màn hình thường trực (đặc biệt bất tiện trên điện thoại — nơi phần lớn bệnh nhân Việt Nam dùng app), và biến mọi chức năng thành "ngang hàng" về mặt thị giác dù tần suất dùng rất khác nhau (nhật ký ăn uống mở mỗi ngày, xuất PDF có khi cả tháng mới cần 1 lần). Chuyển sang mô hình **Trang chủ làm điểm xuất phát**: đăng nhập xong luôn về đây, chọn đúng việc cần làm, xong việc quay lại đây để chọn việc tiếp theo — giống cách mở 1 app trên điện thoại hơn là dùng 1 dashboard quản trị.

## 2. Khung header — cố định trên MỌI màn hình sau đăng nhập

Khác với Trang chủ (chỉ là 1 màn cụ thể), header dưới đây xuất hiện xuyên suốt mọi màn hình bệnh nhân đã đăng nhập — kể cả khi đang ở sâu trong 1 chức năng (vd đang ghi nhật ký ăn uống), để luôn có đường quay về mà không cần bấm "Quay lại" nhiều lần.

```text
┌───────────────────────────────────────────────────────────┐
│ 🌿 GastroAI          (giữa trống/tên trang hiện tại)   🔔³ Nguyễn Văn A ▾│
└───────────────────────────────────────────────────────────┘
```

| Thành phần | Vị trí | Hành vi |
| :--- | :--- | :--- |
| Icon/logo GastroAI | Góc trái | Bấm vào → luôn quay về Trang chủ, từ bất kỳ màn nào. Đây là cách duy nhất để "thoát" khỏi 1 chức năng quay lại chọn việc khác, thay cho việc click mục khác trên sidebar như trước |
| 🔔 Chuông thông báo | Góc phải, trước tên người dùng | Badge số chưa đọc, bấm mở [SCR-NT-01 Trung tâm thông báo](../05-ThongBao-Notification/SCR-NT-01-TrungTamThongBao.md) |
| Tên người dùng + mũi tên | Ngoài cùng bên phải | Bấm mở dropdown: **Đổi mật khẩu** ([SCR-AUTH-06](../01-XacThuc-TaiKhoan/SCR-AUTH-06-DoiMatKhau.md)), **Đăng xuất** (`UC0003`) |

Không còn khái niệm "menu bên trái" ở bất kỳ đâu trong phân hệ bệnh nhân — mọi điều hướng giữa các chức năng đi qua Trang chủ.

## 3. Thiết kế màn Trang chủ

```text
┌───────────────────────────────────────────────────────────┐
│ 🌿 GastroAI                                        🔔³ Nguyễn Văn A ▾│
├───────────────────────────────────────────────────────────┤
│  Chào buổi sáng, Văn A 👋                                      │
│  🔥 Streak hiện tại: 14 ngày         💊 Còn 1 liều thuốc hôm nay│
│                                                                 │
│  Trò chuyện cùng AI                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐          │
│  │💬 Trò     │ │🕑 Lịch sử │ │📋 Kiểm tra triệu       │          │
│  │  chuyện   │ │  trò chuyện│ │  chứng có cấu trúc     │          │
│  └──────────┘ └──────────┘ └──────────────────────┘          │
│                                                                 │
│  Ghi nhận hằng ngày                                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                       │
│  │🍜 Nhật ký │ │💩 Nhật ký │ │💊 Nhắc     │                       │
│  │  ăn uống  │ │  Bristol  │ │  uống thuốc│                       │
│  └──────────┘ └──────────┘ └──────────┘                       │
│                                                                 │
│  Xem lại & phân tích                                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐      │
│  │📊 Lịch sử │ │📈 Xu hướng │ │🔥 Streak & │ │🔍 Nhạy cảm    │      │
│  │  ăn uống  │ │  Bristol  │ │  tuân thủ │ │  thực phẩm    │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘      │
│                                                                 │
│  Hồ sơ & kiến thức                                              │
│  ┌──────────────┐ ┌──────────┐ ┌──────────────┐                │
│  │📋 Hồ sơ bệnh   │ │❓ FAQ     │ │📚 Thư viện     │                │
│  │  lý            │ │          │ │  kiến thức     │                │
│  └──────────────┘ └──────────┘ └──────────────┘                │
└───────────────────────────────────────────────────────────┘
```

## 4. Nguyên tắc thiết kế

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Nhóm theo **ý định sử dụng**, không theo thứ tự UC | 4 nhóm: Trò chuyện cùng AI / Ghi nhận hằng ngày / Xem lại & phân tích / Hồ sơ & kiến thức | Bệnh nhân nghĩ theo "tôi muốn làm gì" chứ không theo cấu trúc UC trong đề cương — nhóm sai sẽ khiến họ phải đoán chức năng nằm ở đâu |
| Có dòng tóm tắt nhanh ở đầu trang (streak, nhắc thuốc còn lại) | Lấy dữ liệu thật từ [SCR-PT-06](SCR-PT-06-StreakTuanThu.md) và [SCR-PT-07](SCR-PT-07-NhacThuoc.md) | Trang chủ thuần ô bấm (không có gì sống động) cảm giác như 1 app launcher vô hồn — vài con số cá nhân hoá khiến nó cảm giác như "nhà của tôi", đúng tinh thần khuyến khích quay lại mỗi ngày đã nêu ở `00-TongQuan.md` §4.1 |
| "Trò chuyện cùng AI" luôn là nhóm đầu tiên, góc trên trái | Thứ tự cố định, không đổi theo hành vi dùng | Đây là giá trị cốt lõi của sản phẩm — không nên để thuật toán "ưu tiên theo tần suất dùng" đẩy nó xuống dưới |
| Mỗi ô chỉ 1 dòng tên ngắn, có icon dễ nhận | Không nhồi mô tả dài trong ô | Đây là màn hình để *quét nhanh rồi bấm*, không phải màn hình để đọc |
| Không có ô nào dẫn tới chức năng đã cắt khỏi phạm vi | Không có ô "Chụp ảnh món ăn" (`UC0044` đã cắt) | Trang chủ phản ánh đúng những gì thật sự làm được, không hứa hẹn tính năng không tồn tại |

## 5. Tham chiếu

- Mọi màn đích của các ô: xem [00-TongQuan.md](00-TongQuan.md) §1 danh sách màn hình
- Thông báo: `05-ThongBao-Notification/SCR-NT-01-TrungTamThongBao.md`
- Khung tương đương phía CMS: `04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md`
