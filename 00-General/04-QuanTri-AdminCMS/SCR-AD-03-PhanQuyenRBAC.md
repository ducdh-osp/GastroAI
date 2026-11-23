# SCR-AD-03 — Phân quyền nhân sự nội bộ (RBAC)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0047` |
| Actor | Admin (duy nhất — xem và đổi vai trò người khác là hành động quản trị hệ thống, không giao cho vai trò nào khác, kể cả vai trò "thấy được nhiều màn") |
| Mục tiêu nghiệp vụ | Gán đúng 1 trong 4 vai trò cho từng nhân sự nội bộ — nền tảng để mọi màn hình khác biết "ai được thấy gì, làm gì" |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với 2 màn dễ nhầm

- **[SCR-AD-02](SCR-AD-02-QuanLyNguoiDung.md) — Quản lý người dùng (bệnh nhân)**: quản lý **bệnh nhân** (chỉ khoá/mở, không có khái niệm "vai trò"). Màn này quản lý **nhân sự nội bộ** — khái niệm khác hẳn: ở đây việc cần làm là xác định *mức độ quyền hạn*, không phải *cho phép truy cập hay không*.
- **[SCR-AD-14](SCR-AD-14-TaoTaiKhoanNhanSu.md) — Tạo tài khoản nhân sự CMS**: 2 màn này giải quyết **2 thời điểm khác nhau trong vòng đời 1 tài khoản nội bộ**. SCR-AD-14 là thời điểm *sinh ra* tài khoản (và gán vai trò lần đầu, bắt buộc chọn ngay lúc tạo). Màn này (SCR-AD-03) là nơi *đổi* vai trò của một tài khoản đã tồn tại — dùng khi ai đó chuyển việc trong team (vd từ Cộng tác viên nội dung lên Biên tập chuyên môn y khoa khi đủ tin tưởng). Không gộp 2 việc vào 1 màn vì "tạo mới" cần xử lý thêm mật khẩu ban đầu (xem SCR-AD-14 §5), một việc không liên quan gì tới "đổi vai trò".

## 2. Từ 2 vai trò lên 4 vai trò — xử lý trực diện mâu thuẫn với nguyên tắc cũ

> ## Quyết định đổi mô hình vai trò lần 2 (sau quyết định bỏ "Bác sĩ" ngày 2026-11-17)
> Phiên bản trước của màn này (v1.1) đặt nguyên tắc **"số vai trò tối thiểu — 2 vai trò"** làm nguyên tắc đầu tiên, với lý do: *"càng nhiều vai trò mập mờ, càng khó kiểm soát."* Yêu cầu nghiệp vụ mới (nâng lên ≥4 vai trò, có 1 vai trò xem được mọi màn, các vai trò còn lại xem khác nhau) **nghe như đi ngược nguyên tắc đó** — cần nói rõ đây **không phải tăng vai trò vì tăng**, mà là hệ quả của 2 ranh giới thật đã tồn tại trong cách team vận hành, chỉ chưa được đặt tên:
>
> 1. **Ranh giới "soạn" và "duyệt" nội dung** — tài liệu các màn [SCR-AD-10](SCR-AD-10-QuanLyFAQ.md)/[SCR-AD-11](SCR-AD-11-QuanLyBaiViet.md) từ trước đã ghi "Admin hoặc Biên tập chuyên môn y khoa (**soạn**)" tách biệt với "Biên tập chuyên môn y khoa (**duyệt**)" — khái niệm "soạn không cần quyền duyệt" đã có trong văn bản. Tách **Cộng tác viên nội dung** ra khỏi **Biên tập chuyên môn y khoa** là đặt tên đúng cho 1 ranh giới đã có, không phải vẽ thêm ranh giới mới.
> 2. **Ranh giới "vận hành/giám sát" và "quản trị hệ thống"** — Dashboard ([SCR-AD-05](SCR-AD-05-DashboardBaoCao.md)) và Giám sát cảnh báo khẩn cấp ([SCR-AD-13](SCR-AD-13-GiamSatCanhBao.md)) hiện bị ép vào nhóm "Admin-only" dù `00-TongQuan.md` §0 đã ghi rõ bước xác nhận escalate ở SCR-AD-13 **"không cần chuyên môn y tế... mọi việc ở đây là vận hành/xác nhận."** Nếu việc đó không cần chuyên môn y tế, nó cũng không cần *toàn quyền quản trị hệ thống* — dồn 2 việc khác hẳn nhau (xem số liệu/xác nhận cảnh báo vs. đổi phân quyền/xoá tài khoản) vào cùng 1 vai trò "Admin" chính là kiểu mập mờ nguyên tắc cũ muốn tránh, chỉ khác hướng: mập mờ vì *dồn quá nhiều việc vào 1 vai trò*, không phải vì *tạo vai trò lưng chừng*.
>
> **Nguyên tắc cũ vẫn đúng tinh thần, chỉ cần diễn giải lại cho chính xác**: vấn đề chưa bao giờ là *số lượng* vai trò, mà là *mỗi vai trò phải ứng với 1 ranh giới thật, kiểm tra được bằng câu hỏi "vai trò này có quyền này vì sao" — không phải "vì tiện".* "Bác sĩ" (đã bỏ) sai vì là danh xưng không có tập quyền thực sự khác biệt rõ với Biên tập chuyên môn y khoa ở quy mô đồ án. 2 vai trò mới dưới đây không rơi vào lỗi đó — mỗi vai trò có 1 cột "không được làm gì" trả lời được, xem §3.
>
> Giữ đúng tinh thần góc nhìn `ba-bigtech-10yr` đã áp dụng cho quyết định lần 1: **không thêm vai trò chỉ vì có người yêu cầu, chỉ thêm khi chỉ ra được ranh giới không thể gộp vào vai trò đã có.**

## 3. Danh sách 4 vai trò & ranh giới

| Vai trò | Mô tả ngắn | Được làm gì (đặc trưng) | Không được làm gì (ranh giới) |
| :--- | :--- | :--- | :--- |
| **Admin** | Quản trị hệ thống — toàn quyền | Xem **tất cả** màn CMS; quản lý tài khoản bệnh nhân, RBAC, tạo tài khoản CMS mới, cấu hình hệ thống, xem Audit Log | *(không có ranh giới — vai trò duy nhất không bị giới hạn, đúng yêu cầu "1 vai trò xem tất cả")* |
| **Biên tập chuyên môn y khoa** | Chịu trách nhiệm cuối cùng về nội dung y tế hiển thị cho bệnh nhân hoặc nạp cho AI | Soạn **và duyệt/xuất bản** tài liệu RAG, bài viết, FAQ, chủ đề, danh mục chuẩn hoá, từ khoá Triage | Không quản lý tài khoản người dùng/nhân sự, không xem Dashboard/Audit Log/cấu hình hệ thống |
| **Cộng tác viên nội dung** *(mới)* | Hỗ trợ soạn nội dung không đòi hỏi thẩm định y khoa sâu — vd nhập liệu, viết nháp, chuẩn hoá văn phong | Soạn **nháp** bài viết, FAQ; quản lý chủ đề bài viết (gắn thẻ, rủi ro thấp) | **Không có quyền duyệt/xuất bản** (nội dung do vai trò này soạn luôn ở trạng thái "chờ duyệt" — chỉ Biên tập chuyên môn y khoa hoặc Admin bấm xuất bản được); không đụng Kho tài liệu y khoa (RAG) hay Từ khoá Triage — 2 nơi ảnh hưởng trực tiếp an toàn/độ chính xác của AI |
| **Giám sát vận hành** *(mới)* | Theo dõi hệ thống có "chạy đúng" không — số liệu, cảnh báo — không phải chuyên môn y tế, không phải quản trị nhân sự | Xem Dashboard thống kê; xem và xác nhận escalate ở Giám sát cảnh báo khẩn cấp | Không xem Audit Log (giám sát Audit Log là giám sát *con người*, không phải giám sát *hệ thống* — cố tình giữ Admin-exclusive để không mở rộng "ai được xem ai đã làm gì" ngoài nhu cầu thật); không đụng nội dung, không đụng tài khoản |

*Tên kỹ thuật cho enum `CmsRole` (xem ghi chú code ở §4): `ADMIN`, `CONTENT_EDITOR` (giữ nguyên), `CONTENT_AUTHOR` (Cộng tác viên nội dung), `OPERATIONS` (Giám sát vận hành).*

## 4. Ma trận quyền theo màn hình

| Màn hình | Admin | Biên tập chuyên môn y khoa | Cộng tác viên nội dung | Giám sát vận hành |
| :--- | :---: | :---: | :---: | :---: |
| SCR-AD-00 Trang chủ | ● | ● | ● | ● |
| SCR-AD-01 Đăng nhập quản trị | ● | ● | ● | ● |
| SCR-AD-02 Quản lý người dùng (bệnh nhân) | ● | | | |
| SCR-AD-03 Phân quyền nhân sự (màn này) | ● | | | |
| SCR-AD-04 Kho tài liệu y khoa | ● | ● | | |
| SCR-AD-05 Dashboard thống kê | ● | | | ● |
| SCR-AD-06 Nhật ký hoạt động | ● | | | |
| SCR-AD-07 Từ khoá Triage | ● | ● | | |
| SCR-AD-08 Cấu hình RAG/Triage | ● | | | |
| SCR-AD-09 Danh mục chuẩn hoá | ● | ● | | |
| SCR-AD-10 Quản lý FAQ | ● | ● (soạn+duyệt) | ● (soạn) | |
| SCR-AD-11 Quản lý bài viết | ● | ● (soạn+duyệt) | ● (soạn) | |
| SCR-AD-12 Chủ đề bài viết | ● | ● | ● | |
| SCR-AD-13 Giám sát cảnh báo khẩn cấp | ● (xác nhận) | | | ● (xác nhận) |
| SCR-AD-14 Tạo tài khoản nhân sự CMS | ● *(duy nhất)* | | | |

**Lưu ý đọc ma trận — 2 trục khác nhau, đừng nhầm**: bảng trên là ranh giới **ở cấp màn hình** (được mở màn hay không). SCR-AD-10/11 cho thấy có thêm 1 trục **ranh giới theo hành động trong cùng 1 màn** ("soạn" khác "duyệt/xuất bản") — trục này được đặc tả trong chính SCR-AD-10/SCR-AD-11, không lặp lại ở đây. "●" ở ô (Cộng tác viên nội dung, SCR-AD-10) nghĩa là *mở được màn, thấy được nút "Lưu bản nháp"* — không nghĩa là thấy nút "Duyệt & xuất bản".

> ## Quyết định kỹ thuật: ma trận "cứng" (hard-code theo vai trò) hay bảng quyền động trong DB?
> Yêu cầu nghiệp vụ dùng từ "có thể cấu hình được" cho 3 vai trò còn lại — cần làm rõ đây có 2 cách hiểu, chi phí khác nhau hẳn:
>
> | Phương án | Mô tả | Chi phí | Phù hợp khi |
> | :--- | :--- | :--- | :--- |
> | **A — 4 vai trò cố định, mỗi vai trò 1 bộ màn hard-code** | Ma trận ở §4 viết thẳng vào code: nối dài danh sách `hasAnyRole(...)` trong `SecurityConfig.java` (BE) + nối dài `roles: [...]` trong `cmsNav.ts` (FE, cơ chế này **đã có sẵn** cho 2 vai trò hiện tại) + thêm 1 route guard tham số hoá theo vai trò ở `router.tsx`. Đổi ma trận = sửa code, build lại | Thấp — đúng cách hệ thống **đang** làm cho 2 vai trò hiện tại, chỉ mở rộng số dòng | Đồ án 4 người, vai trò đổi vài lần trong lúc làm chứ không đổi liên tục lúc vận hành thật |
> | **B — Ma trận quyền động, Admin tự cấu hình role↔màn qua UI** | Thêm bảng `role_screen_permission` (role, màn, có quyền), 1 màn Admin bật/tắt từng ô, và **toàn bộ** `requestMatchers` cứng ở `SecurityConfig.java` phải đổi thành tra bảng động (vd 1 `AuthorizationManager` tuỳ biến) — ảnh hưởng mọi endpoint CMS, không chỉ RBAC | Cao — đụng lớp bảo mật trung tâm toàn hệ thống; còn phải thiết kế thêm vì "soạn/duyệt" là quyền theo **hành động**, không phải theo **màn** (bảng role↔màn không tự nhiên biểu diễn được, cần thêm 1 chiều role↔hành động) | Hệ thống thật, nhiều vai trò đổi quyền thường xuyên không qua được đội dev |
>
> **Đề xuất cho đồ án: Phương án A.** Phần phân quyền hiện tại (`SecurityConfig.java` dùng `requestMatchers().hasAnyRole()`, `cmsNav.ts` dùng `roles: string[]`) **đã là Phương án A cho 2 vai trò** — mở rộng lên 4 vai trò chỉ nối dài danh sách đã có, không xây mới. Phương án B tốn công xây 1 permission-engine cho lợi ích mà team 4 người, tự kiểm soát deploy, không cần — thời gian đó nên dồn cho các NFR thật đang thiếu. Nếu muốn **demo** được khái niệm "ma trận cấu hình được" mà không trả chi phí B: hiển thị bảng ở §4 dưới dạng **chỉ xem** (đúng dữ liệu đang hard-code), kèm chú thích "thay đổi ma trận cần qua dev, không chỉnh trực tiếp trên UI" — vẫn trung thực, không giả vờ có 1 UI chỉnh sửa không tồn tại.

**Ghi chú kỹ thuật phát hiện khi review (chưa sửa code, cần biết trước khi hiện thực)**:
- `DOCTOR` (vai trò "Bác sĩ" đã cắt khỏi sản phẩm từ 2026-11-17) vẫn còn sống ở 4 lớp: bảng `doctors` riêng (MySQL, `CHECK role = 'DOCTOR'`), `CmsRbacService.assignRole` vẫn xử lý nhánh này, nhiều `requestMatchers(...).hasAnyRole("ADMIN","DOCTOR",...)` trong `SecurityConfig.java`, và FE (`cmsNav.ts`, `CmsHeader.tsx`, `CmsRbacPage.tsx` còn map nhãn "Bác sĩ"). `CmsRbacPage.tsx` đã tự khoá cứng dropdown cho tài khoản `DOCTOR` (không đổi được role) nhưng chưa dọn hẳn. Nên dọn `DOCTOR`/`Doctor`/`CmsAccountType.DOCTOR` **trước hoặc cùng lúc** với việc thêm 2 vai trò mới — nếu không, `CmsAccountType` (hiện chỉ có `{ADMIN, DOCTOR}`, vì `CONTENT_EDITOR` chỉ là 1 giá trị `role` trong bảng `admins`, không phải 1 loại tài khoản riêng) sẽ buộc màn SCR-AD-14 phải hỏi "tạo ở bảng nào" — câu hỏi vô nghĩa về nghiệp vụ.
- **Bug đã phát hiện, nên sửa khi làm lại ma trận lần này**: `SecurityConfig.java` dòng quy định `/api/v1/cms/triage-keywords/**` hiện chỉ `hasAnyRole("ADMIN","DOCTOR")` — **thiếu `CONTENT_EDITOR`**, dù tài liệu [SCR-AD-07](SCR-AD-07-TuKhoaTriage.md) giao việc "thêm/sửa/xoá" từ khoá Triage cho Biên tập chuyên môn y khoa. Lệch giữa tài liệu và code từ trước khi có đợt đổi này.

## 5. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Phân quyền nhân sự          [+ Tạo tài khoản mới] [ 🔍 Tìm ] │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên              │ Email          │ Vai trò hiện tại      │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Phạm Thị D        │ d@gastroai.vn  │ [ Biên tập chuyên    │ │
│ │                    │                │   môn y khoa     ▾ ] │ │
│ │ Lê Văn E           │ e@gastroai.vn  │ [ Cộng tác viên      │ │
│ │                    │                │   nội dung       ▾ ] │ │
│ │ Hoàng Thị F        │ f@gastroai.vn  │ [ Giám sát vận hành ▾]│ │
│ │ Admin Chính        │ admin@...      │ [ Admin          ▾ ]  │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Khi đổi vai trò (lên Admin) —
┌─────────────────────────────────┐
│  Đổi vai trò cho "Phạm Thị D"?       │
│  Từ: Biên tập chuyên môn y khoa →    │
│       Admin                           │
│  ⚠ Admin có toàn quyền hệ thống,     │
│    bao gồm cả quản lý tài khoản và   │
│    xem nhật ký hoạt động.             │
│       [Huỷ]  [  Xác nhận đổi  ]      │
└─────────────────────────────────┘

— Khi đổi vai trò (giữa 3 vai trò còn lại) —
┌─────────────────────────────────┐
│  Đổi vai trò cho "Lê Văn E"?          │
│  Từ: Cộng tác viên nội dung →        │
│       Giám sát vận hành               │
│       [Huỷ]  [  Xác nhận đổi  ]      │
└─────────────────────────────────┘
```

Nút **"+ Tạo tài khoản mới"** dẫn sang [SCR-AD-14](SCR-AD-14-TaoTaiKhoanNhanSu.md) — màn này không tự tạo tài khoản, chỉ đổi vai trò tài khoản đã tồn tại (xem §1). Hộp cảnh báo khi nâng lên Admin giữ nguyên như bản cũ; đổi vai trò giữa 3 vai trò còn lại (không chạm mức Admin) không cần cảnh báo nặng — mức rủi ro thấp hơn hẳn, hỏi xác nhận đơn giản là đủ, tránh "báo động giả" khiến Admin lướt qua mọi cảnh báo mà không đọc.

## 6. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Mỗi vai trò ứng với 1 ranh giới kiểm tra được, không phải thêm cho tiện | 4 vai trò ở §3, mỗi vai trò có 1 cột "không được làm gì" — nếu không điền được cột đó, không nên tách vai trò | Đây là lý do thật khiến "Bác sĩ" bị bỏ trước đây — số lượng vai trò không phải vấn đề, vai trò không có ranh giới mới là vấn đề (xem §2) |
| Chỉ Admin tạo được tài khoản CMS và đổi được vai trò người khác | Màn này và SCR-AD-14 đều giới hạn Actor = Admin, không có vai trò nào khác chạm tới | Nếu 1 vai trò thấp hơn có thể tự cấp quyền (cho mình hoặc người khác), toàn bộ ma trận ở §4 mất ý nghĩa — nguyên tắc chống leo thang quyền (privilege escalation), bắt buộc với bất kỳ hệ thống RBAC thật |
| Cảnh báo rõ khi nâng quyền lên Admin | Xác nhận kèm giải thích cụ thể Admin có thể làm gì; 3 vai trò còn lại chỉ cần xác nhận đơn giản | Nâng quyền lên mức cao nhất là quyết định quan trọng — không nên để xảy ra "lỡ tay" chỉ vì thao tác dropdown quá dễ dàng; đồng thời không lạm dụng cảnh báo cho mọi thay đổi khiến Admin có thói quen bấm qua mà không đọc |
| Vai trò mới không mặc định chạm dữ liệu y tế cá nhân hoá của bệnh nhân | Cộng tác viên nội dung và Giám sát vận hành — cả 2 đều **không** có màn nào hiển thị hồ sơ bệnh lý/nhật ký sức khoẻ cá nhân của 1 bệnh nhân cụ thể (khác SCR-AD-02, vẫn Admin-exclusive) | Nguyên tắc tối thiểu hoá dữ liệu nhạy cảm theo Nghị định 13/2023/NĐ-CP (dữ liệu y tế là dữ liệu cá nhân nhạy cảm) — mỗi khi thêm 1 vai trò mới trong tương lai, phải trả lời được câu hỏi này trước khi gán màn hình cho nó, không riêng lần này |
| Mọi thay đổi vai trò và mọi tài khoản mới đều ghi vào Audit Log | Tự động, không cần Admin tự nhớ ghi chú — cả hành động ở màn này (đổi role) và ở SCR-AD-14 (tạo mới) | Đổi phân quyền và tạo tài khoản nội bộ đều là thao tác nhạy cảm bắt buộc phải truy vết — xem `00-TongQuan.md` §5 và [SCR-AD-06](SCR-AD-06-NhatKyHoatDong.md) |

## 7. Tham chiếu

- Quản lý tài khoản bệnh nhân (khác khái niệm): [SCR-AD-02-QuanLyNguoiDung.md](SCR-AD-02-QuanLyNguoiDung.md)
- Tạo tài khoản nhân sự mới (bước trước màn này trong vòng đời tài khoản): [SCR-AD-14-TaoTaiKhoanNhanSu.md](SCR-AD-14-TaoTaiKhoanNhanSu.md)
- Lịch sử thay đổi phân quyền: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md)
- Số ô hiển thị ở Trang chủ theo vai trò (cần cập nhật đồng bộ khi màn này đổi — chưa làm, xem TODO ở `bao-cao-chuong1-draft.md`): [SCR-AD-00-TrangChu.md](SCR-AD-00-TrangChu.md)
