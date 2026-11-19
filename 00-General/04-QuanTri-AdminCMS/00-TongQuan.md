# Tổng quan Phân hệ — Quản trị (Admin/CMS)

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | Quản trị (Admin/CMS) — lõi |
| **UC nguồn** | `UC0045 - UC0066 (22 UC)` |
| **Actor** | Admin, Biên tập chuyên môn y khoa (2 vai trò nội bộ — không còn "Bác sĩ", xem lý do ngay dưới) |
| **Tài liệu liên quan** | `01-XacThuc-TaiKhoan/` (cơ chế đăng nhập/mật khẩu dùng chung) · `02-NguoiDung-BenhNhan/` (nơi nội dung do phân hệ này quản lý hiển thị ra) · `03-AIChatbot-RAG/` (nơi dữ liệu nạp vào được AI sử dụng) |
| **Phiên bản** | v1.1 — bỏ vai trò "Bác sĩ", xem §0 |

> ## 0. Quyết định đổi mô hình vai trò (2026-11-17)
> Hệ thống chuyển sang xoay quanh chatbot AI — AI đảm nhận phần lớn việc tư vấn trực tiếp cho bệnh nhân, nên vai trò "Bác sĩ" như một tài khoản vận hành CMS riêng biệt **không còn cần thiết và không thực tế** với quy mô đồ án 4 người (không có bác sĩ thật trong team để "đóng vai" tài khoản này). Quyết định này đã được đánh giá qua 2 góc nhìn trước khi áp dụng:
> - **Góc nhìn quy trình/RBAC** (`ba-bigtech-10yr`): Doctor không chỉ là 1 role UI mà là 1 account type/bảng riêng ở tầng dữ liệu — bỏ hẳn, không gộp mập mờ vào Admin, để RBAC vẫn còn ý nghĩa thật trong đồ án. Giữ **2 role: Admin + Biên tập chuyên môn y khoa**.
> - **Góc nhìn an toàn lâm sàng** (`ba-gastro-10yr`): không cần dựng lại vai trò "Bác sĩ" vận hành (phi thực tế), **nhưng bắt buộc giữ lại bước xác nhận nội dung y khoa có đối chiếu nguồn kiểm chứng** ở 3 nơi rủi ro cao — kho tài liệu RAG (`SCR-AD-04`), từ khoá Triage (`SCR-AD-07`), FAQ/bài viết (`SCR-AD-10`/`SCR-AD-11`) — chỉ đổi danh xưng người thực hiện, không bỏ bước duyệt. Và màn giám sát cảnh báo khẩn cấp (`SCR-AD-13`) phải đổi thành kịch bản vận hành cố định, không để Admin tự phán đoán y khoa khi không có ai chuyên môn.
>
> **Nguyên tắc trung thực học thuật xuyên suốt từ đây**: nội dung y tế trong hệ thống (tài liệu RAG, FAQ, bài viết) là **do nhóm biên soạn/đối chiếu từ tài liệu y khoa đã công bố**, không phải do bác sĩ hành nghề thẩm định — mọi nơi trước đây ghi "Bác sĩ duyệt" nay ghi rõ "Biên tập chuyên môn y khoa" với đúng nghĩa này.

> Người dùng của phân hệ này **khác hẳn** bệnh nhân: đây là đội ngũ nội bộ (Admin, Biên tập chuyên môn y khoa) — người dùng thành thạo, lặp lại thao tác hàng ngày, cần **hiệu quả và chính xác** hơn là cần được "thuyết phục" hay "trấn an" như bệnh nhân. Triết lý thiết kế vì vậy đổi khác: ưu tiên bảng dữ liệu dày đặc thông tin, thao tác hàng loạt, bộ lọc mạnh — thay vì giao diện tối giản từng bước như phân hệ bệnh nhân.

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
| `UC0003`, `UC0005`, `UC0007` | Đăng xuất / Đổi mật khẩu / Lịch sử đăng nhập | Nhân sự CMS dùng **đúng cơ chế nghiệp vụ** đã mô tả ở `01-XacThuc-TaiKhoan/` — chỉ khác route (`/cms/...`) và tông màu giao diện tách biệt khỏi cổng bệnh nhân. Không lặp lại tài liệu. |
| `UC0057` | Sao lưu định kỳ CSDL | Tác vụ nền tự động (lịch chạy định kỳ), không có màn hình — xem §4 |
| `UC0058` | Khôi phục dữ liệu từ backup | Thao tác kỹ thuật do đội vận hành thực hiện trực tiếp khi có sự cố, không qua giao diện web — xem §4 |

---

## 2. Vì sao các màn hình được nhóm như trên — đặc biệt là "Kho tài liệu y khoa"

Khác với phân hệ bệnh nhân (mỗi UC gần như 1 màn riêng để giữ đơn giản), ở đây **nhiều UC gộp vào 1 màn** vì đó là cách nhân viên vận hành thực tế làm việc: khi một tài liệu y khoa cần cập nhật, người quản lý tài liệu muốn **thấy luôn trong 1 màn**: tải lên, xem phiên bản cũ, theo dõi đang xử lý tới đâu, tìm lại tài liệu cũ, xoá nếu lỗi thời — tách thành 5 màn riêng biệt (đúng theo 5 mã UC) sẽ bắt họ nhảy qua lại liên tục giữa các màn cho cùng 1 công việc, hiệu quả làm việc giảm hẳn. Nguyên tắc tương tự áp dụng cho "Nhật ký hoạt động" (xem + lọc + cảnh báo bất thường + xuất file đều là các góc nhìn khác nhau trên **cùng một bộ dữ liệu log**, không phải 4 nghiệp vụ tách biệt).

## 3. Chân dung người dùng cần hình dung

- **Admin**: toàn quyền vận hành — quản lý người dùng, phân quyền, cấu hình hệ thống, xem thống kê toàn diện. Với cảnh báo khẩn cấp ([SCR-AD-13](SCR-AD-13-GiamSatCanhBao.md)), hệ thống tự động xử lý phần lớn (push, SMS, gọi tự động) — Admin chỉ xuất hiện ở **đúng 1 bước**: xác nhận escalate tới người liên hệ khẩn cấp khi mọi kênh tự động đều không có phản hồi. Không cần chuyên môn y tế để làm vai trò này — mọi việc ở đây là vận hành/xác nhận, không phải phán đoán lâm sàng.
- **Biên tập chuyên môn y khoa**: soạn & duyệt nội dung y tế hiển thị cho bệnh nhân hoặc nạp cho AI học (tài liệu RAG, bài viết, FAQ, từ khoá Triage) — làm việc này bằng cách **đối chiếu với tài liệu y khoa đã công bố**, không phải bằng tư cách bác sĩ hành nghề thẩm định. Không có quyền quản lý tài khoản người dùng hay phân quyền hệ thống (ranh giới quan trọng: biên tập nội dung chuyên môn không đồng nghĩa với quyền quản trị hệ thống).

## 4. Sao lưu & khôi phục dữ liệu (`UC0057`, `UC0058`)

Đây là tác vụ **bảo vệ dữ liệu y tế nhạy cảm khỏi mất mát** — chạy nền theo lịch định kỳ, không cần (và không nên) có giao diện web để kích hoạt thủ công hàng ngày, vì mục tiêu là tự động, nhất quán, không phụ thuộc việc có ai nhớ bấm nút hay không. Khôi phục dữ liệu (`UC0058`) chỉ xảy ra khi có sự cố thật sự (mất/hỏng dữ liệu) — đây là thao tác hệ trọng, rủi ro cao nếu làm sai, nên cố tình **không** có nút "Khôi phục" dễ bấm nhầm trên giao diện web; đội vận hành thực hiện trực tiếp, có kiểm soát chặt, khi thật sự cần.

## 5. Những yêu cầu không được bỏ sót (`ba-bigtech-10yr`)

- **Ranh giới quyền hạn rõ ràng giữa Admin và Biên tập chuyên môn y khoa**: biên tập nội dung chuyên môn tốt không nên đồng nghĩa với toàn quyền hệ thống — nhầm lẫn 2 khái niệm "giỏi nội dung" và "được tin cậy quản trị hệ thống" là lỗi thiết kế phân quyền phổ biến.
- **Không tự xưng thẩm quyền y tế mà hệ thống không có**: vì không còn vai trò "Bác sĩ" vận hành, mọi nội dung y tế công khai (FAQ, bài viết, tài liệu RAG) phải ghi rõ là do nhóm biên soạn/đối chiếu tài liệu y khoa đã công bố — tuyệt đối không dùng ngôn từ ngụ ý "đã được bác sĩ thẩm định" nếu thực tế không có. Đây là yêu cầu trung thực học thuật, không chỉ là tiểu tiết câu chữ.
- **Mọi thao tác nhạy cảm đều phải để lại dấu vết**: sửa tài liệu, đổi phân quyền, truy cập dữ liệu người dùng — tất cả phải ghi vào Audit Log ([SCR-AD-06](SCR-AD-06-NhatKyHoatDong.md)), không có ngoại lệ "thao tác nhỏ không cần ghi".
- **Dữ liệu bệnh nhân hiển thị ở đây vẫn là dữ liệu nhạy cảm**: dù người xem là nhân sự CMS (không phải chính bệnh nhân), màn [SCR-AD-02](SCR-AD-02-QuanLyNguoiDung.md) vẫn phải tuân thủ nguyên tắc tối thiểu hoá hiển thị — chỉ hiện thông tin cần thiết cho mục đích quản lý tài khoản, không hiện toàn bộ nhật ký sức khoẻ cá nhân một cách không cần thiết.

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

- `01-XacThuc-TaiKhoan/` — cơ chế đăng nhập/mật khẩu dùng chung cho nhân sự CMS.
- `02-NguoiDung-BenhNhan/` — nơi nội dung (bài viết, FAQ, tài liệu) do phân hệ này quản lý hiển thị ra cho bệnh nhân.
- `03-AIChatbot-RAG/` — nơi tài liệu nạp vào ở `SCR-AD-04` được AI sử dụng, và nơi `SCR-AD-07`/`SCR-AD-08` điều khiển hành vi Triage/RAG.

## 8. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo, viết theo trải nghiệm người dùng thực tế |
| v1.1 | 2026-11-17 | Bỏ vai trò "Bác sĩ" (xem §0) — đổi mô hình vai trò thành Admin + Biên tập chuyên môn y khoa; viết lại SCR-AD-01/03/04/07/10/11/13 |
| v1.2 | 2026-11-19 | Viết lại `SCR-AD-13` theo hướng AI-first (review lại qua `ba-bigtech-10yr` + `ba-gastro-10yr`): hệ thống tự động hoá push/SMS/gọi tự động, Admin chỉ còn 1 bước xác nhận escalate duy nhất thay vì tự tay gọi điện theo kịch bản như v1.1 |
