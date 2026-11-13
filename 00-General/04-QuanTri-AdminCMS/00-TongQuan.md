# Tổng quan Phân hệ — Quản trị (Admin/CMS)

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | Quản trị (Admin/CMS) — lõi |
| **UC nguồn** | `UC0045 - UC0066 (22 UC)` |
| **Actor** | Admin, Bác sĩ (2 vai trò chính dùng cổng này — có cả Biên tập nội dung cho vài màn) |
| **Tài liệu liên quan** | `01-XacThuc-TaiKhoan/` (cơ chế đăng nhập/mật khẩu dùng chung) · `02-NguoiDung-BenhNhan/` (nơi nội dung do Admin quản lý hiển thị ra) · `03-AIChatbot-RAG/` (nơi dữ liệu Admin nạp vào được AI sử dụng) |
| **Phiên bản** | v1.0 — viết theo trải nghiệm người dùng thực tế |

> Người dùng của phân hệ này **khác hẳn** bệnh nhân: đây là đội ngũ nội bộ (Admin, Bác sĩ) — người dùng thành thạo, lặp lại thao tác hàng ngày, cần **hiệu quả và chính xác** hơn là cần được "thuyết phục" hay "trấn an" như bệnh nhân. Triết lý thiết kế vì vậy đổi khác: ưu tiên bảng dữ liệu dày đặc thông tin, thao tác hàng loạt, bộ lọc mạnh — thay vì giao diện tối giản từng bước như phân hệ bệnh nhân.

> **Vai trò của phân hệ này với "sức mạnh AI" không phải là sử dụng AI trực tiếp, mà là *nuôi* và *giám sát* AI**: đây là nơi nạp tài liệu y khoa cho AI học, cấu hình cách AI hoạt động, và theo dõi AI có đang trả lời đúng/an toàn hay không. Một AI tư vấn tốt chỉ bền vững nếu đội ngũ vận hành có đủ công cụ để liên tục kiểm tra và cải thiện nó — đó là lý do phân hệ này quan trọng không kém phân hệ AI, dù không "hào nhoáng" bằng.

---

## 1. Danh sách màn hình & UC

| Mã màn hình | File | Tên màn hình | UC |
| :--- | :--- | :--- | :--- |
| `SCR-AD-01` | [SCR-AD-01-DangNhapQuanTri.md](SCR-AD-01-DangNhapQuanTri.md) | Đăng nhập quản trị | `UC0045` |
| `SCR-AD-02` | [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md) | Quản lý người dùng (bệnh nhân) | `UC0046` |
| `SCR-AD-03` | [SCR-AD-03-PhanQuyenRBAC.md](SCR-AD-03-PhanQuyenRBAC.md) | Phân quyền nhân sự nội bộ | `UC0047` |
| `SCR-AD-04` | [SCR-AD-04-KhoTaiLieuYKhoa.md](SCR-AD-04-KhoTaiLieuYKhoa.md) | Kho tài liệu y khoa | `UC0048`, `UC0049`, `UC0050`, `UC0055`, `UC0061` |
| `SCR-AD-05` | [SCR-AD-05-DashboardBaoCao.md](SCR-AD-05-DashboardBaoCao.md) | Dashboard thống kê & báo cáo | `UC0051`, `UC0056` |
| `SCR-AD-06` | [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md) | Nhật ký hoạt động hệ thống (Audit Log) | `UC0052`, `UC0053`, `UC0063`, `UC0064` |
| `SCR-AD-07` | [SCR-AD-07-TuKhoaTriage.md](SCR-AD-07-TuKhoaTriage.md) | Quản lý từ khoá sàng lọc khẩn cấp | `UC0054` |
| `SCR-AD-08` | [SCR-AD-08-CauHinhRAGTriage.md](SCR-AD-08-CauHinhRAGTriage.md) | Cấu hình tham số RAG/Triage | `UC0059` |
| `SCR-AD-09` | [SCR-AD-09-DanhMucChuanHoa.md](SCR-AD-09-DanhMucChuanHoa.md) | Danh mục chuẩn hoá (triệu chứng, thuốc) | `UC0060` |
| `SCR-AD-10` | [SCR-AD-10-QuanLyFAQ.md](SCR-AD-10-QuanLyFAQ.md) | Quản lý FAQ | `UC0062` |
| `SCR-AD-11` | [SCR-AD-11-QuanLyBaiViet.md](SCR-AD-11-QuanLyBaiViet.md) | Quản lý bài viết kiến thức | `UC0065` |
| `SCR-AD-12` | [SCR-AD-12-ChuDeBaiViet.md](SCR-AD-12-ChuDeBaiViet.md) | Quản lý chủ đề bài viết | `UC0066` |
| `SCR-AD-13` | [SCR-AD-13-GiamSatCanhBao.md](SCR-AD-13-GiamSatCanhBao.md) | Giám sát cảnh báo khẩn cấp | *(phía nhận của `UC0036`, không có mã UC riêng)* |

**UC không có màn hình riêng (giải thích ngay dưới):**

| UC | Tên | Ghi chú |
| :--- | :--- | :--- |
| `UC0003`, `UC0005`, `UC0007` | Đăng xuất / Đổi mật khẩu / Lịch sử đăng nhập | Admin/Bác sĩ dùng **đúng cơ chế nghiệp vụ** đã mô tả ở `01-XacThuc-TaiKhoan/` — chỉ khác route (`/cms/...`) và tông màu giao diện tách biệt khỏi cổng bệnh nhân. Không lặp lại tài liệu. |
| `UC0057` | Sao lưu định kỳ CSDL | Tác vụ nền tự động (lịch chạy định kỳ), không có màn hình — xem §4 |
| `UC0058` | Khôi phục dữ liệu từ backup | Thao tác kỹ thuật do đội vận hành thực hiện trực tiếp khi có sự cố, không qua giao diện web — xem §4 |

---

## 2. Vì sao các màn hình được nhóm như trên — đặc biệt là "Kho tài liệu y khoa"

Khác với phân hệ bệnh nhân (mỗi UC gần như 1 màn riêng để giữ đơn giản), ở đây **nhiều UC gộp vào 1 màn** vì đó là cách nhân viên vận hành thực tế làm việc: khi một tài liệu y khoa cần cập nhật, người quản lý tài liệu muốn **thấy luôn trong 1 màn**: tải lên, xem phiên bản cũ, theo dõi đang xử lý tới đâu, tìm lại tài liệu cũ, xoá nếu lỗi thời — tách thành 5 màn riêng biệt (đúng theo 5 mã UC) sẽ bắt họ nhảy qua lại liên tục giữa các màn cho cùng 1 công việc, hiệu quả làm việc giảm hẳn. Nguyên tắc tương tự áp dụng cho "Nhật ký hoạt động" (xem + lọc + cảnh báo bất thường + xuất file đều là các góc nhìn khác nhau trên **cùng một bộ dữ liệu log**, không phải 4 nghiệp vụ tách biệt).

## 3. Chân dung người dùng cần hình dung

- **Admin**: toàn quyền — quản lý người dùng, phân quyền, cấu hình hệ thống, xem thống kê toàn diện.
- **Bác sĩ**: góc nhìn chuyên môn — quản lý nội dung y khoa (tài liệu, bài viết, FAQ, từ khoá Triage), xem cảnh báo khẩn cấp, nhưng **không** có quyền quản lý tài khoản người dùng hay phân quyền hệ thống (đây là ranh giới quan trọng: chuyên môn y tế không đồng nghĩa với quyền quản trị hệ thống).
- **Biên tập nội dung** (vai trò hẹp hơn): chỉ soạn thảo bài viết/FAQ, không có quyền ở các màn nhạy cảm hơn (người dùng, phân quyền, audit log, cấu hình hệ thống).

## 4. Sao lưu & khôi phục dữ liệu (`UC0057`, `UC0058`)

Đây là tác vụ **bảo vệ dữ liệu y tế nhạy cảm khỏi mất mát** — chạy nền theo lịch định kỳ, không cần (và không nên) có giao diện web để kích hoạt thủ công hàng ngày, vì mục tiêu là tự động, nhất quán, không phụ thuộc việc có ai nhớ bấm nút hay không. Khôi phục dữ liệu (`UC0058`) chỉ xảy ra khi có sự cố thật sự (mất/hỏng dữ liệu) — đây là thao tác hệ trọng, rủi ro cao nếu làm sai, nên cố tình **không** có nút "Khôi phục" dễ bấm nhầm trên giao diện web; đội vận hành thực hiện trực tiếp, có kiểm soát chặt, khi thật sự cần.

## 5. Những yêu cầu không được bỏ sót (`ba-bigtech-10yr`)

- **Ranh giới quyền hạn rõ ràng giữa Admin và Bác sĩ**: Bác sĩ có chuyên môn y tế nhưng không nên có toàn quyền hệ thống — nhầm lẫn 2 khái niệm "giỏi chuyên môn" và "được tin cậy quản trị hệ thống" là lỗi thiết kế phân quyền phổ biến.
- **Mọi thao tác nhạy cảm đều phải để lại dấu vết**: sửa tài liệu, đổi phân quyền, truy cập dữ liệu người dùng — tất cả phải ghi vào Audit Log ([SCR-AD-06](SCR-AD-06-NhatKyHoatDong.md)), không có ngoại lệ "thao tác nhỏ không cần ghi".
- **Dữ liệu bệnh nhân hiển thị ở đây vẫn là dữ liệu nhạy cảm**: dù người xem là Admin/Bác sĩ (không phải chính bệnh nhân), màn [SCR-AD-02](SCR-AD-02-QuanLyNguoiDung.md) vẫn phải tuân thủ nguyên tắc tối thiểu hoá hiển thị — chỉ hiện thông tin cần thiết cho mục đích quản lý tài khoản, không hiện toàn bộ nhật ký sức khoẻ cá nhân một cách không cần thiết.

## 6. Traceability (UC ↔ Màn hình)

| UC | Màn hình |
| :--- | :--- |
| `UC0045` | `SCR-AD-01` |
| `UC0046` | `SCR-AD-02` |
| `UC0047` | `SCR-AD-03` |
| `UC0048`, `UC0049`, `UC0050`, `UC0055`, `UC0061` | `SCR-AD-04` |
| `UC0051`, `UC0056` | `SCR-AD-05` |
| `UC0052`, `UC0053`, `UC0063`, `UC0064` | `SCR-AD-06` |
| `UC0054` | `SCR-AD-07` |
| `UC0057`, `UC0058` | *(tác vụ nền, không có màn)* |
| `UC0059` | `SCR-AD-08` |
| `UC0060` | `SCR-AD-09` |
| `UC0062` | `SCR-AD-10` |
| `UC0065` | `SCR-AD-11` |
| `UC0066` | `SCR-AD-12` |

## 7. Tham chiếu

- `01-XacThuc-TaiKhoan/` — cơ chế đăng nhập/mật khẩu dùng chung cho Admin/Bác sĩ.
- `02-NguoiDung-BenhNhan/` — nơi nội dung (bài viết, FAQ, tài liệu) do phân hệ này quản lý hiển thị ra cho bệnh nhân.
- `03-AIChatbot-RAG/` — nơi tài liệu nạp vào ở `SCR-AD-04` được AI sử dụng, và nơi `SCR-AD-07`/`SCR-AD-08` điều khiển hành vi Triage/RAG.

## 8. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo, viết theo trải nghiệm người dùng thực tế |
