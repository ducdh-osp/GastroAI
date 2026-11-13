# SCR-PT-04 — Nhật ký tiêu hoá (thang Bristol)

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0013` (ghi nhận), `UC0020` (khôi phục mục đã xoá — phần Bristol) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Ghi lại tình trạng đi tiêu theo 1 thang đo chuẩn y khoa, đủ đơn giản để ghi mỗi ngày nhưng đủ chính xác để có ý nghĩa lâm sàng |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao phải dùng đúng thang Bristol, không tự chế

Thang phân loại phân Bristol (Bristol Stool Chart) là thang 7 mức được dùng rộng rãi trong lâm sàng tiêu hoá — không phải một thang "cảm nhận" tự do kiểu "bình thường/hơi lỏng/rất lỏng". Lý do bắt buộc dùng đúng thang chuẩn:

- Đây là **ngôn ngữ chung** giữa bệnh nhân và bác sĩ — nếu bệnh nhân mang nhật ký này tới phòng khám, bác sĩ đọc hiểu ngay "mức 6" nghĩa là gì, không cần diễn giải lại.
- 7 mức này phản ánh **thời gian di chuyển qua ruột** (mức 1 = di chuyển chậm/táo bón, mức 7 = di chuyển rất nhanh/tiêu chảy) — đây là thông tin có giá trị chẩn đoán, không chỉ là mô tả định tính.
- Tự chế 1 thang riêng (vd "1-5 sao") sẽ làm mất hoàn toàn giá trị lâm sàng của dữ liệu, biến nhật ký thành ghi chép cảm tính vô nghĩa với bác sĩ.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký tiêu hoá                    [ + Ghi nhận mới ]        │
│  ┌─────────┬──────────┐                                       │
│  │Danh sách │ Thùng rác│                                       │
│  └─────────┴──────────┘                                       │
│  Hôm nay, 08:15 — Mức 4 (Bình thường)         [Sửa] [Xoá]      │
│  Hôm qua, 21:40 — Mức 6 (Hơi lỏng)            [Sửa] [Xoá]      │
└───────────────────────────────────────────────────────────┘

— Form ghi nhận mới —
┌─────────────────────────────────────┐
│  Ghi nhận tình trạng tiêu hoá           │
│                                          │
│  Chọn mức phù hợp nhất:                 │
│  ┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐
│  │ 1  ││ 2  ││ 3  ││ 4  ││ 5  ││ 6  ││ 7  │
│  │🔵🔵││🔵🔵││ 🌽 ││ 🌭 ││ 🟤 ││ 🥣 ││ 💧 │  ← icon minh hoạ
│  └────┘└────┘└────┘└────┘└────┘└────┘└────┘
│   Táo        Bình thường       Tiêu chảy
│   bón                                    │
│                                          │
│  Triệu chứng kèm theo (không bắt buộc)  │
│  [___________________________________]  │
│       [     Lưu     ]                   │
└─────────────────────────────────────┘
```

Luôn hiển thị **cả 7 mức cùng lúc** dưới dạng minh hoạ trực quan (không phải dropdown ẩn) — bệnh nhân cần so sánh hình ảnh để chọn đúng mức, không phải nhớ định nghĩa bằng chữ.

## 3. Ý nghĩa 7 mức (dùng nhất quán ở mọi nơi hiển thị trong app)

| Mức | Nhóm | Ý nghĩa |
| :-: | :--- | :--- |
| 1 | Táo bón | Cục cứng rời rạc, khó đi |
| 2 | Táo bón | Hình xúc xích nhưng lổn nhổn |
| 3 | Bình thường (hơi khô) | Hình xúc xích có vết nứt trên bề mặt |
| 4 | Bình thường (lý tưởng) | Hình xúc xích/rắn, mềm mịn |
| 5 | Bình thường (hơi lỏng) | Cục mềm, rìa rõ ràng |
| 6 | Tiêu chảy nhẹ | Mảnh bông xốp, rìa nham nhở |
| 7 | Tiêu chảy | Hoàn toàn lỏng, không có dạng rắn |

## 4. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Ghi nhận mức 1, 2, 6, hoặc 7 nhiều lần liên tục trong thời gian ngắn | Không tự ý cảnh báo y tế ngay tại màn này (đây là nơi ghi chép, không phải nơi chẩn đoán) — nhưng dữ liệu này cần được AI nhìn thấy khi bệnh nhân chat để có thể chủ động hỏi thêm | Táo bón/tiêu chảy kéo dài là dấu hiệu cần theo dõi, nhưng cảnh báo dồn dập ngay tại màn ghi chép sẽ gây hoang mang không cần thiết cho 1 bất thường đơn lẻ — việc diễn giải nên để dành cho AI tư vấn có ngữ cảnh đầy đủ hơn |
| Xoá/khôi phục | Giống hệt nguyên tắc ở [SCR-PT-02](SCR-PT-02-NhatKyAnUong.md) §4 | Nhất quán trải nghiệm giữa 2 loại nhật ký |

## 5. Tham chiếu

- Xem xu hướng theo thời gian: [SCR-PT-05-XuHuongBristol.md](SCR-PT-05-XuHuongBristol.md)
- Dấu hiệu nguy hiểm cần phân biệt với ghi nhận thường quy (máu trong phân, đau dữ dội...): `03-AIChatbot-RAG/` (lớp sàng lọc an toàn)
