# Tổng quan Phân hệ — Người dùng (Bệnh nhân)

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Phân hệ** | Người dùng (Bệnh nhân) — lõi |
| **UC nguồn** | `UC0009 - UC0027 (19 UC)` |
| **Actor** | Bệnh nhân |
| **Tài liệu liên quan** | `01-XacThuc-TaiKhoan/` (lối vào) · `03-AIChatbot-RAG/` (phần "bộ não" đứng sau màn Chat) · `05-ThongBao-Notification/` (các nhắc nhở) |
| **Phiên bản** | v1.0 — viết theo trải nghiệm người dùng thực tế |

> Đây là phân hệ mà bệnh nhân **sống trong đó mỗi ngày**: ghi nhật ký, theo dõi triệu chứng, uống thuốc đúng giờ, và trò chuyện với AI khi có thắc mắc. Khác với phân hệ Auth (chỉ ghé qua rồi rời đi), ở đây mục tiêu không phải "tốc độ" mà là **duy trì thói quen** — một bệnh nhân tiêu hoá mạn tính (IBS, viêm dạ dày, trào ngược...) chỉ thu được giá trị thật nếu họ còn quay lại ghi nhật ký sau tuần thứ 3, không phải chỉ dùng thử 1 lần rồi bỏ.

> **Sợi chỉ đỏ xuyên suốt phân hệ này: biến dữ liệu bệnh nhân tự ghi (ăn gì, đi ngoài thế nào, uống thuốc chưa) thành thứ có ý nghĩa — qua AI. Không có phân hệ này, AI ở `03-AIChatbot-RAG/` chỉ trả lời chung chung; có đủ dữ liệu theo dõi, AI mới cá nhân hoá được: biết bệnh nhân đã thử món gì, phát hiện món ăn hay đi kèm triệu chứng, biết họ có đang quên uống thuốc hay không. Mọi màn hình dưới đây nên được thiết kế để khuyến khích ghi đều đặn — vì chính nhật ký đều đặn đó mới là "nguyên liệu" để AI tư vấn trở nên thông minh hơn theo thời gian, cho riêng từng người.**

---

## 1. Danh sách màn hình & UC

| Mã màn hình | File | Tên màn hình | UC |
| :--- | :--- | :--- | :--- |
| `SCR-PT-01` | [SCR-PT-01-HoSoBenhLy.md](SCR-PT-01-HoSoBenhLy.md) | Hồ sơ bệnh lý cá nhân | `UC0009`, `UC0010` |
| `SCR-PT-02` | [SCR-PT-02-NhatKyAnUong.md](SCR-PT-02-NhatKyAnUong.md) | Nhật ký ăn uống | `UC0011`, `UC0020` (phần ăn uống) |
| `SCR-PT-03` | [SCR-PT-03-LichSuAnUong.md](SCR-PT-03-LichSuAnUong.md) | Lịch sử & biểu đồ ăn uống | `UC0012` |
| `SCR-PT-04` | [SCR-PT-04-NhatKyBristol.md](SCR-PT-04-NhatKyBristol.md) | Nhật ký tiêu hoá (thang Bristol) | `UC0013`, `UC0020` (phần Bristol) |
| `SCR-PT-05` | [SCR-PT-05-XuHuongBristol.md](SCR-PT-05-XuHuongBristol.md) | Xu hướng tiêu hoá theo thời gian | `UC0014` |
| `SCR-PT-06` | [SCR-PT-06-StreakTuanThu.md](SCR-PT-06-StreakTuanThu.md) | Chuỗi ngày ghi nhật ký & tỷ lệ tuân thủ | `UC0025` |
| `SCR-PT-07` | [SCR-PT-07-NhacThuoc.md](SCR-PT-07-NhacThuoc.md) | Nhắc uống thuốc | `UC0015`, `UC0016` |
| `SCR-PT-08` | [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md) | Trò chuyện với AI | `UC0017` (+ liên quan `UC0035/42/43` ở `03-AIChatbot-RAG/`) |
| `SCR-PT-09` | [SCR-PT-09-LichSuChat.md](SCR-PT-09-LichSuChat.md) | Lịch sử trò chuyện | `UC0018`, `UC0027` |
| `SCR-PT-10` | [SCR-PT-10-NhayCamThucPham.md](SCR-PT-10-NhayCamThucPham.md) | Hồ sơ nhạy cảm thực phẩm | `UC0022`, `UC0023` |
| `SCR-PT-11` | [SCR-PT-11-FAQ.md](SCR-PT-11-FAQ.md) | Câu hỏi thường gặp | `UC0021` |
| `SCR-PT-12` | [SCR-PT-12-ThuVienKienThuc.md](SCR-PT-12-ThuVienKienThuc.md) | Thư viện kiến thức tiêu hoá | `UC0026` |

Ngoài ra, màn **Xuất nhật ký sức khoẻ ra PDF** (`UC0019`) không phải 1 trang riêng mà là **1 hành động** (nút "Xuất PDF") có thể gọi từ nhiều nơi (hồ sơ bệnh lý, lịch sử ăn uống, xu hướng Bristol) — mô tả trong [SCR-PT-01-HoSoBenhLy.md](SCR-PT-01-HoSoBenhLy.md) §5 vì đó là điểm vào tự nhiên nhất (hồ sơ tổng hợp).

**UC không có màn hình riêng:**

| UC | Tên | Vì sao |
| :--- | :--- | :--- |
| `UC0019` | Xuất nhật ký sức khoẻ ra PDF | Là 1 nút hành động, không phải màn hình — xem `SCR-PT-01` §5 |
| `UC0020` | Khôi phục nhật ký đã xoá nhầm | Xuất hiện như 1 tab "Thùng rác" *bên trong* cả `SCR-PT-02` (ăn uống) lẫn `SCR-PT-04` (Bristol) — không có "nhà riêng", vì bản chất nó là tính năng phụ trợ đi kèm từng loại nhật ký |
| `UC0024` | Nhắc ghi nhật ký nếu quên | Là một thông báo đẩy (notification), không phải màn hình bệnh nhân chủ động mở — xem `05-ThongBao-Notification/` |

---

## 2. Hành trình sử dụng hằng ngày (tổng quan)

```text
                    [ Bệnh nhân mở app buổi sáng ]
                                  │
                   ┌──────────────┼──────────────┐
                   ▼              ▼              ▼
            "Vừa ăn sáng    "Có triệu chứng  "Có thắc mắc
             xong"           muốn ghi lại"    muốn hỏi"
                   │              │              │
                   ▼              ▼              ▼
           [ SCR-PT-02 ]   [ SCR-PT-04 ]   [ SCR-PT-08 ]
            Nhật ký ăn      Nhật ký         Chat với AI ───┐
            uống            Bristol              │         │
                   │              │                │   (AI phát hiện dấu
                   │              │                │    hiệu nguy hiểm?)
                   ▼              ▼                │         │
         (Đã đủ dữ liệu    (Đã đủ dữ liệu           │         ▼
          vài tuần?)        vài tuần?)              │  → 03-AIChatbot-RAG/
                   │              │                 │    (cảnh báo khẩn cấp)
                   ▼              ▼                 │
           [ SCR-PT-10 ]   [ SCR-PT-05 ]            │
            Hồ sơ nhạy      Xu hướng                 │
            cảm thực        tiêu hoá                 │
            phẩm (AI tự                              │
            phát hiện mẫu                            │
            hình)                                    │
                                                      ▼
                                          AI trả lời có trích dẫn,
                                          gợi ý câu hỏi tiếp theo
                                                      │
                                          ┌───────────┴───────────┐
                                          ▼                       ▼
                                   [ SCR-PT-09 ]           "Chưa rõ, muốn
                                   Xem/xuất lại              đọc thêm"
                                   phiên chat cũ                  │
                                                                   ▼
                                                           [ SCR-PT-12 ] /
                                                           [ SCR-PT-11 ]
                                                           Thư viện / FAQ

      [ Mỗi khi tới giờ uống thuốc — bất kể đang ở màn nào ]
                         [ SCR-PT-07 ]
                      Nhắc & xác nhận đã uống

      [ Mở lại sau vài tuần dùng đều ]
                   [ SCR-PT-06 ]
            "Bạn đã duy trì 14 ngày liên tục!"
             (củng cố động lực tiếp tục ghi nhật ký)
```

## 3. Vì sao các màn hình được nhóm như trên

- **Ghi nhận (`SCR-PT-02`, `SCR-PT-04`, `SCR-PT-07`)**: thao tác lặp lại hằng ngày — phải nhanh, ít bước, có thể làm "trong lúc đang bận" (vừa ăn xong, vừa định đi ngủ). Đây là nhóm màn hình quyết định bệnh nhân có **duy trì** thói quen hay bỏ cuộc sau vài ngày.
- **Nhìn lại (`SCR-PT-03`, `SCR-PT-05`, `SCR-PT-06`)**: không cần nhanh, ngược lại cần **đẹp và dễ hiểu** — đây là lúc bệnh nhân thu hoạch thành quả của việc ghi chép đều đặn, cảm nhận được "tôi đang kiểm soát được bệnh của mình".
- **Hiểu sâu hơn nhờ AI (`SCR-PT-10`)**: lớp giá trị cao nhất — biến dữ liệu thô (đã ghi ở nhóm "Ghi nhận") thành insight không tự tay phân tích được. Đây là nơi rõ nhất để "khoe" sức mạnh AI của sản phẩm, vì nó làm được việc mà ghi tay/excel không thể làm: tự động dò ra món ăn nào hay đi kèm triệu chứng.
- **Trò chuyện & tra cứu (`SCR-PT-08`, `SCR-PT-09`, `SCR-PT-11`, `SCR-PT-12`)**: khi bệnh nhân chủ động cần câu trả lời — AI trả lời trực tiếp (chat) hoặc tự tra cứu trước (FAQ/thư viện) nếu câu hỏi đơn giản, đỡ phải chờ.

---

## 4. Quy tắc nghiệp vụ dùng chung

### 4.1 Nguyên tắc "ghi nhanh, xem đẹp"

Mọi màn hình **ghi nhận** dữ liệu (ăn uống, Bristol, xác nhận thuốc) nên tối ưu cho thao tác bằng 1 tay trên điện thoại, trong dưới 15 giây — đây là lúc dễ bỏ cuộc nhất nếu rườm rà. Mọi màn hình **xem lại** (lịch sử, xu hướng, streak) nên ưu tiên biểu đồ trực quan hơn là bảng số liệu khô khan — bệnh nhân không phải nhân viên nhập liệu, họ cần *cảm nhận* được tiến triển, không chỉ đọc số.

### 4.2 Góc nhìn chuyên môn tiêu hoá cần giữ xuyên suốt (`ba-gastro-10yr`)

- **Thang Bristol (1-7)** là thang phân loại phân chuẩn lâm sàng, không phải thang "cảm nhận chủ quan" — mọi nơi hiển thị (ghi nhận lẫn xem xu hướng) nên dùng đúng 7 mức kèm mô tả/hình minh hoạ chuẩn, không tự chế thang riêng. Mức 1-2 (táo bón), 3-4 (bình thường), 5-7 (tiêu chảy/lỏng) là vùng phân loại bệnh nhân cần hiểu để tự theo dõi, không chỉ là 1 con số.
- **"Phân tích tương quan" (`UC0022`) không phải chẩn đoán** — một món ăn xuất hiện cùng lúc với triệu chứng nhiều lần chỉ là *gợi ý thống kê đơn giản*, chưa đủ để kết luận "món này gây bệnh". Mọi nơi hiển thị kết quả loại này **bắt buộc phải có khung diễn giải rõ ràng về giới hạn này** — tránh bệnh nhân hiểu nhầm thành lời khuyên y khoa chắc chắn rồi tự loại bỏ cả nhóm thực phẩm quan trọng (nguy cơ thiếu dinh dưỡng nếu làm theo "gợi ý AI" một cách mù quáng, nhất là với các bệnh mạn tính như IBD cần dinh dưỡng đầy đủ).
- **Tuân thủ dùng thuốc (medication adherence)** là vấn đề thực tế lớn nhất với bệnh mạn tính — bệnh nhân hay tự ý bỏ liều giữa đợt, tự đổi giờ uống, hoặc ngại báo với ai là mình quên. Màn nhắc thuốc nên **trung lập, không phán xét** khi bệnh nhân bỏ lỡ 1 liều (không có thông điệp kiểu "Bạn đã bỏ lỡ!" mang tính trách móc), vì cảm giác tội lỗi thường khiến người bệnh mạn tính né tránh mở app tiếp, chứ không giúp họ tuân thủ tốt hơn.
- **Ranh giới của AI tư vấn**: mọi nơi AI đưa ra gợi ý liên quan tới sức khoẻ (phân tích tương quan, trả lời chat) đều cần nhất quán một thông điệp: đây là **gợi ý sơ bộ**, không thay thế chẩn đoán của bác sĩ — đặc biệt quan trọng với các triệu chứng có thể trùng lắp giữa bệnh nhẹ và bệnh nguy hiểm (vd đau thượng vị có thể là viêm dạ dày thông thường hoặc dấu hiệu tim mạch).

### 4.3 Những yêu cầu không được bỏ sót (`ba-bigtech-10yr`)

- **Dữ liệu sức khoẻ là dữ liệu nhạy cảm** (Nghị định 13/2023/NĐ-CP) — nhật ký ăn uống, tiêu hoá, hồ sơ bệnh lý chỉ bệnh nhân đó xem được, không có "chế độ chia sẻ công khai" nào trong toàn phân hệ này.
- **Không mất dữ liệu do thao tác nhầm**: mọi hành động xoá (nhật ký ăn uống, Bristol) phải có thể khôi phục trong một khoảng thời gian hợp lý — bệnh nhân dùng điện thoại, bấm nhầm là chuyện thường, không nên là mất vĩnh viễn ngay lập tức.
- **Nhất quán dữ liệu giữa các màn**: 1 bản ghi nhật ký bị xoá ở màn "Ghi nhận" phải biến mất khỏi mọi biểu đồ/thống kê liên quan ngay (lịch sử, xu hướng, streak) — không để lệch số liệu giữa các màn.

---

## 5. Traceability (UC ↔ Màn hình)

| UC | Màn hình |
| :--- | :--- |
| `UC0009`, `UC0010` | `SCR-PT-01` |
| `UC0011` | `SCR-PT-02` |
| `UC0012` | `SCR-PT-03` |
| `UC0013` | `SCR-PT-04` |
| `UC0014` | `SCR-PT-05` |
| `UC0015`, `UC0016` | `SCR-PT-07` |
| `UC0017` | `SCR-PT-08` |
| `UC0018`, `UC0027` | `SCR-PT-09` |
| `UC0019` | *(hành động, `SCR-PT-01` §5)* |
| `UC0020` | *(tab phụ trong `SCR-PT-02` và `SCR-PT-04`)* |
| `UC0021` | `SCR-PT-11` |
| `UC0022`, `UC0023` | `SCR-PT-10` |
| `UC0024` | *(thông báo, `05-ThongBao-Notification/`)* |
| `UC0025` | `SCR-PT-06` |
| `UC0026` | `SCR-PT-12` |

## 6. Tham chiếu

- `01-XacThuc-TaiKhoan/` — lối vào phân hệ này.
- `03-AIChatbot-RAG/` — "bộ não" đứng sau `SCR-PT-08`.
- `05-ThongBao-Notification/` — các nhắc nhở (`UC0024`, `UC0016` khi tới giờ).
- Nghị định 13/2023/NĐ-CP, thang Bristol Stool Chart.

## 7. Lịch sử thay đổi

| Phiên bản | Ngày | Nội dung |
| :--- | :--- | :--- |
| v1.0 | 2026-11-14 | Khởi tạo, viết theo trải nghiệm người dùng thực tế |
