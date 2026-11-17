# Tổng quan Phân hệ — Xác thực & Tài khoản

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | Xác thực & Tài khoản (bổ trợ — nền tảng bắt buộc cho mọi phân hệ khác) |
| **UC nguồn** | `UC0001 - UC0008 (8 UC)` |
| **Actor** | Bệnh nhân (chưa/đã đăng nhập), Hệ thống |
| **Tài liệu liên quan** | `02-NguoiDung-BenhNhan/` · `03-AIChatbot-RAG/` · `04-QuanTri-AdminCMS/` (UC0047 gán vai trò) · `05-ThongBao-Notification/` (UC0069/70 gửi email) |
| **Phiên bản** | v2.0 — viết lại theo trải nghiệm người dùng thực tế, không mô tả theo implementation |

> Tài liệu này được viết theo cách một BA tiếp cận bài toán thật: bắt đầu từ **người dùng là ai, họ cần gì, họ sẽ bối rối/bỏ cuộc ở đâu** — rồi mới suy ra quy tắc nghiệp vụ và thiết kế màn hình. Đây **không phải** tài liệu mô tả lại những gì phần mềm đang chạy hiện có; một số điểm ở đây có thể khác (và nên khác) so với bản dựng hiện tại, nếu trải nghiệm thực tế chỉ ra cách làm tốt hơn.

> **Chân dung người dùng cần hình dung khi đọc tài liệu này**: phần lớn là người Việt, nhiều độ tuổi (kể cả trung niên/lớn tuổi — nhóm hay gặp vấn đề tiêu hoá mạn tính), lần đầu dùng 1 "app sức khoẻ có AI", còn dè dặt về việc đưa thông tin sức khoẻ cho máy. **Mục tiêu của toàn bộ phân hệ Auth: đưa người dùng tới trải nghiệm AI tư vấn nhanh nhất và an toàn nhất có thể** — mỗi bước rườm rà ở đây là một người dùng tiềm năng rơi rụng trước khi kịp thấy giá trị thật của sản phẩm.

---

## 1. Danh sách màn hình & UC

| Mã màn hình | File | Tên màn hình | UC | 
| :--- | :--- | :--- | :--- |
| `SCR-AUTH-01` | [SCR-AUTH-01-DangKy.md](SCR-AUTH-01-DangKy.md) | Đăng ký tài khoản | `UC0001` |
| `SCR-AUTH-02` | [SCR-AUTH-02-XacThucEmail.md](SCR-AUTH-02-XacThucEmail.md) | Xác thực email | `UC0001` |
| `SCR-AUTH-03` | [SCR-AUTH-03-DangNhap.md](SCR-AUTH-03-DangNhap.md) | Đăng nhập | `UC0002`, `UC0008` |
| `SCR-AUTH-04` | [SCR-AUTH-04-QuenMatKhau.md](SCR-AUTH-04-QuenMatKhau.md) | Quên mật khẩu — nhập email | `UC0004` (bước 1) |
| `SCR-AUTH-05` | [SCR-AUTH-05-DatLaiMatKhau.md](SCR-AUTH-05-DatLaiMatKhau.md) | Đặt lại mật khẩu | `UC0004` (bước 2) |
| `SCR-AUTH-06` | [SCR-AUTH-06-DoiMatKhau.md](SCR-AUTH-06-DoiMatKhau.md) | Đổi mật khẩu | `UC0005` |
| `SCR-AUTH-07` | [SCR-AUTH-07-LichSuDangNhap.md](SCR-AUTH-07-LichSuDangNhap.md) | Lịch sử đăng nhập | `UC0007` |

**UC không nên có màn hình riêng** (gộp vào trải nghiệm của màn khác, vì tách ra sẽ chỉ làm phức tạp hoá hành trình người dùng mà không mang lại giá trị gì):

| UC | Tên | Vì sao không tách màn |
| :--- | :--- | :--- |
| `UC0003` | Đăng xuất | Người dùng không cần "1 màn hình" để đăng xuất — chỉ cần 1 hành động rõ ràng ở menu tài khoản, có xác nhận nhẹ để tránh bấm nhầm. |
| `UC0006` | Phân quyền RBAC cơ bản | Đây là quy tắc "ai được làm gì", không phải thứ bệnh nhân/Admin "thao tác" trên 1 màn hình riêng — nó thấm vào mọi màn hình khác (ẩn/hiện chức năng theo vai trò). UI để *gán* vai trò nằm ở `UC0047`. |

---

## 2. Hành trình người dùng (tổng quan)

```text
                      [ Người dùng mới nghe về GastroAI ]
                                    │
                         ( Mở app lần đầu )
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                                 ▼
          "Tôi đã có tài khoản"              "Tôi là người mới"
                    │                                 │
                    ▼                                 ▼
             [ SCR-AUTH-03 ]                   [ SCR-AUTH-01 ]
              Đăng nhập                         Đăng ký — chỉ hỏi
                    │                            đúng 3 thứ cần thiết
       ┌────────────┼────────────┐                     │
       ▼            ▼             ▼                     ▼
  (Quên mật    (Nhập sai quá  (Vào được)      "Kiểm tra email để kích hoạt"
   khẩu)        nhiều lần —            │                 │
       │        hệ thống tạm           │                 ▼
       ▼        khoá để bảo vệ,        │          [ SCR-AUTH-02 ]
 [SCR-AUTH-04]  không phải phạt)       │          Xác thực email — 1 cú
       │              │                │          nhấp, không cần nhớ gì
       ▼              ▼                │                 │
 [SCR-AUTH-05]   Thông báo rõ còn       │         ┌───────┴───────┐
  Đặt mật khẩu    bao lâu, không       │         ▼               ▼
  mới, xong là     mập mờ              │    (Link cũ,       (Thành công
  đăng nhập được                       │     đã quá hạn)       ngay)
       │                                │         │               │
       └────────────────────────────────┼─────────┘               │
                                         │    "Gửi lại giúp tôi"   │
                                         │    (không bắt nhập lại  │
                                         │     từ đầu)             │
                                         ▼                         ▼
                              [ Đã đăng nhập — bắt đầu trò chuyện với AI ]
                                     (→ 02-NguoiDung-BenhNhan/)
                                         │
                          ┌──────────────┼──────────────┐
                          ▼              ▼               ▼
                   "Đổi mật khẩu"  "Ai từng đăng    "Thoát ra"
                    khi cần          nhập vào tài        │
                          │          khoản của tôi?"      ▼
                          ▼              │           Xác nhận nhẹ
                   [SCR-AUTH-06]   [SCR-AUTH-07]     rồi về SCR-AUTH-03
```

## 3. Mô tả hành trình & lý do nghiệp vụ

1. **Đăng ký (`UC0001` → `SCR-AUTH-01`)**: người mới chỉ nên bị hỏi những gì **thật sự cần** để tạo tài khoản (tên, email, mật khẩu) — mọi thông tin khác (hồ sơ bệnh lý, thói quen ăn uống...) để dành cho sau khi đã vào app, lúc người dùng đã thấy giá trị và sẵn sàng đầu tư thời gian khai báo. Hỏi quá nhiều ngay từ đầu là nguyên nhân phổ biến nhất khiến người dùng bỏ cuộc trước khi dùng thử.
2. **Xác thực email (`UC0001` → `SCR-AUTH-02`)**: bước này tồn tại vì lý do bảo mật (chống tạo tài khoản ảo, đảm bảo liên lạc được khi có cảnh báo y tế quan trọng) chứ **không phải** để làm khó người dùng — nên phải tối giản tới mức "bấm 1 link, xong". Nếu người dùng quay lại sau khi link hết hạn, họ không nên phải nhớ lại và nhập lại cả form đăng ký — chỉ cần 1 nút "gửi lại".
3. **Đăng nhập (`UC0002` → `SCR-AUTH-03`)**: màn hình được ghé qua **thường xuyên nhất** trong toàn bộ hành trình — phải nhanh, không có yếu tố gây xao nhãng. Mọi lỗi (sai mật khẩu, tài khoản chưa kích hoạt, tài khoản bị khoá) cần có **đường ra rõ ràng ngay tại chỗ**, không bắt người dùng tự đoán phải làm gì tiếp.
4. **Bảo vệ khỏi đoán mật khẩu (`UC0008`, gắn trên `SCR-AUTH-03`)**: đây là tính năng **bảo vệ người dùng**, không phải hình phạt — vì vậy phải luôn cho người dùng thật biết rõ "còn bao lâu nữa" thay vì chỉ nói "thử lại sau", và thời gian khoá phải đủ ngắn để không biến 1 lần gõ nhầm phím thành cả buổi tối không vào được tài khoản.
5. **Đăng xuất (`UC0003`)**: hành động đơn giản, nhưng với 1 app chứa dữ liệu sức khoẻ cá nhân, nên có xác nhận nhẹ (tránh bấm nhầm khi đang thao tác vội trên điện thoại) và dọn sạch phiên làm việc đó ngay lập tức.
6. **Quên mật khẩu bước 1 (`UC0004` → `SCR-AUTH-04`)**: với một app sức khoẻ, việc để lộ "email này có tồn tại trong hệ thống hay không" là rò rỉ thông tin riêng tư (ai đó có thể đang dùng GastroAI để theo dõi bệnh lý tiêu hoá — một thông tin tế nhị). Vì vậy màn hình này **luôn phản hồi như nhau**, bất kể email có tồn tại hay không.
7. **Quên mật khẩu bước 2 (`UC0004` → `SCR-AUTH-05`)**: đặt mật khẩu mới xong nên **tự động đưa thẳng tới màn đăng nhập** với thông báo rõ ràng "đặt lại thành công, đăng nhập bằng mật khẩu mới" — tránh người dùng hoang mang không biết bước tiếp theo là gì.
8. **Đổi mật khẩu (`UC0005` → `SCR-AUTH-06`)`**: khác bước 7 ở chỗ người dùng **đang đăng nhập**, nên cần xác nhận lại mật khẩu hiện tại trước khi cho đổi — đây là bước xác thực lại danh tính (re-authentication) cho một thao tác nhạy cảm, phòng trường hợp ai đó dùng chung máy/thiết bị chưa đăng xuất.
9. **Lịch sử đăng nhập (`UC0007` → `SCR-AUTH-07`)**: tính năng **tạo niềm tin** — với dữ liệu sức khoẻ, người dùng cần cảm thấy họ có thể tự kiểm tra "có ai khác đang vào tài khoản của tôi không" bất cứ lúc nào, không phải chờ liên hệ hỗ trợ.
10. **Phân quyền (`UC0006`)**: không phải một hành trình người dùng trải nghiệm trực tiếp — là quy tắc nền để các phân hệ khác biết "ai được thấy gì".
11. **Kết thúc hành trình Auth**: ngay khi đăng nhập thành công, người dùng nên được đưa thẳng tới nơi họ có thể bắt đầu trò chuyện với AI hoặc tiếp tục việc đang dang dở — đây là bàn giao sang `02-NguoiDung-BenhNhan/`.

---

## 4. Quy tắc nghiệp vụ dùng chung

### 4.1 Vòng đời trạng thái tài khoản

| Trạng thái | Mô tả từ góc nhìn người dùng |
| :--- | :--- |
| Chưa kích hoạt | Vừa đăng ký, chưa bấm link xác thực email. Không vào được app — nhưng luôn có đường gửi lại email, không bao giờ là ngõ cụt. |
| Hoạt động bình thường | Đã xác thực, dùng app như bình thường. |
| Tạm khoá do nhập sai nhiều lần | Tình trạng **tạm thời**, tự hết sau một khoảng thời gian ngắn — người dùng luôn được biết chính xác còn bao lâu. |
| Bị vô hiệu hoá | Admin chủ động khoá (vi phạm, yêu cầu của chính người dùng...). Không tự mở lại được bằng "quên mật khẩu" — phải có người quản trị can thiệp, vì đây là quyết định của con người chứ không phải lỗi hệ thống. |

### 4.2 Nguyên tắc đặt mật khẩu

Một mật khẩu đủ mạnh để bảo vệ dữ liệu sức khoẻ (tối thiểu 8 ký tự, có chữ hoa, có số, có ký tự đặc biệt) — nhưng yêu cầu phải được giải thích **ngay khi gõ**, không phải dưới dạng lỗi khô khan sau khi bấm submit. Nguyên tắc **không cho dùng lại mật khẩu cũ** áp dụng cho cả hai nơi đổi mật khẩu (quên/đổi) — vì nếu mật khẩu cũ từng bị lộ ở đâu đó, cho phép quay lại dùng nó là vô hiệu hoá chính hành động "đổi mật khẩu vì lý do an toàn".

### 4.3 Nguyên tắc bảo vệ khỏi đoán mật khẩu

Khoá tạm thời sau một số lần nhập sai liên tiếp — đủ để chặn kẻ dò mật khẩu tự động, nhưng đủ ngắn để không biến thành trải nghiệm tồi tệ cho người dùng thật chỉ đang lọng cọng gõ sai. Nguyên tắc vàng: **thà một kẻ xấu phải chờ thêm vài phút, còn hơn một bệnh nhân thật không vào được tài khoản đúng lúc cần tra cứu thông tin sức khoẻ gấp.**

### 4.4 Nguyên tắc về thời hạn các đường link gửi qua email

Link xác thực tài khoản có thể "sống" lâu hơn (người dùng có thể không mở email ngay) vì hậu quả nếu bị lộ chỉ là kích hoạt hộ 1 tài khoản chưa có dữ liệu gì. Link đặt lại mật khẩu phải "sống" ngắn hơn hẳn, vì hậu quả nếu bị lộ là **chiếm được quyền truy cập vào dữ liệu sức khoẻ đã có sẵn** của người dùng — rủi ro cao hơn hẳn nên cửa sổ thời gian phải hẹp hơn hẳn.

### 4.5 Những yêu cầu không được bỏ sót (góc nhìn `ba-bigtech-10yr`)

- **Quyền riêng tư dữ liệu y tế**: tài khoản là cửa vào dữ liệu sức khoẻ cá nhân — thuộc nhóm dữ liệu cá nhân nhạy cảm theo Nghị định 13/2023/NĐ-CP. Mọi thông tin xác thực (mật khẩu, token) không bao giờ được hiển thị/ghi lại ở bất kỳ đâu ngoài đúng mục đích xác thực.
- **Nhật ký kiểm tra (Audit)**: mọi lần đăng nhập — kể cả thất bại — cần được ghi nhận, vì đây là cơ sở để vừa phục vụ người dùng tự kiểm tra (`UC0007`), vừa phục vụ điều tra sự cố bảo mật về sau.
- **Trải nghiệm ổn định dù hệ thống phụ tải cao**: đăng nhập là chức năng được gọi nhiều nhất — không được phép chậm hơn cảm nhận "tức thì" của người dùng, kể cả khi hệ thống đang xử lý nhiều yêu cầu AI nặng cùng lúc.
- **Không để lộ thông tin qua sự khác biệt nhỏ**: tốc độ phản hồi, nội dung lỗi... giữa "email không tồn tại" và "email tồn tại nhưng sai mật khẩu" phải giống nhau, nếu không sẽ trở thành công cụ dò xem ai đang là người dùng của GastroAI.

---

## 5. Ma trận phân quyền

| Hành động | Khách | Bệnh nhân | Admin |
| :--- | :---: | :---: | :---: |
| Đăng ký/Đăng nhập/Quên mật khẩu | ✓ | — | ✓ (lối vào riêng, tách biệt khỏi cổng bệnh nhân) |
| Xác thực email | ✓ | ✓ | N/A |
| Đổi mật khẩu của chính mình | — | ✓ | ✓ |
| Xem lịch sử đăng nhập của chính mình | — | ✓ | ✓ |
| Xem lịch sử đăng nhập của **người khác** | — | ✕ | ✕ (chỉ qua báo cáo kiểm tra hệ thống tổng hợp, `UC0052`) |

**Nguyên tắc phạm vi dữ liệu**: không ai — kể cả Admin thao tác nhầm URL/tham số — có thể nhìn thấy lịch sử đăng nhập của người khác qua màn `UC0007`; đây là dữ liệu tuyệt đối riêng tư của từng cá nhân.

---

## 6. Traceability (UC ↔ Màn hình)

| UC | Màn hình | Ghi chú |
| :--- | :--- | :--- |
| `UC0001` | `SCR-AUTH-01`, `SCR-AUTH-02` | Đăng ký + kích hoạt là 1 hành trình liền mạch, 2 màn |
| `UC0002` | `SCR-AUTH-03` | |
| `UC0003` | *(hành động trong menu)* | |
| `UC0004` | `SCR-AUTH-04`, `SCR-AUTH-05` | Quên mật khẩu là 1 hành trình 2 bước |
| `UC0005` | `SCR-AUTH-06` | |
| `UC0006` | *(quy tắc nền, không có màn)* | UI gán vai trò → `UC0047` |
| `UC0007` | `SCR-AUTH-07` | |
| `UC0008` | `SCR-AUTH-03` | Biểu hiện như 1 trạng thái lỗi trên màn đăng nhập |

---

## 7. Tham chiếu

- `docs/masterplan-phan-he-usecase.xlsx` — sheet "Use case chi tiết", UC0001-UC0008.
- Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân.
- `02-NguoiDung-BenhNhan/`, `03-AIChatbot-RAG/`, `04-QuanTri-AdminCMS/`, `05-ThongBao-Notification/`.

## 8. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung | Người cập nhật |
| :--- | :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo (file gộp, mô tả theo implementation) | BA |
| v1.1 | 2026-11-14 | Tách theo màn hình thành thư mục riêng | BA |
| v2.0 | 2026-11-14 | Viết lại hoàn toàn theo trải nghiệm người dùng thực tế thay vì mô tả theo code |
| v2.1 | 2026-11-17 | Bỏ vai trò "Bác sĩ" khỏi cổng CMS (xem lý do ở `04-QuanTri-AdminCMS/00-TongQuan.md`) — chỉ còn "Admin" ở ma trận phân quyền §5 | BA |
