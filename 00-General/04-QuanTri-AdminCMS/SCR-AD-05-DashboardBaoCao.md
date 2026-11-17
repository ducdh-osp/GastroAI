# SCR-AD-05 — Dashboard thống kê & báo cáo

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0051` (dashboard), `UC0056` (xuất báo cáo) |
| Actor | Admin |
| Mục tiêu nghiệp vụ | Cho Admin nắm được "sức khoẻ vận hành" của toàn hệ thống trong 1 cái nhìn — không phải để bệnh nhân xem |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần dashboard riêng cho Admin, khác với số liệu bệnh nhân tự xem

Bệnh nhân xem tiến triển **của chính họ** ([SCR-PT-06-StreakTuanThu.md](../02-NguoiDung-BenhNhan/SCR-PT-06-StreakTuanThu.md)). Admin cần xem **toàn cảnh hệ thống**: có bao nhiêu lượt chat, triệu chứng nào được hỏi nhiều nhất (giúp biết nên bổ sung tài liệu gì), Triage được kích hoạt bao nhiêu lần (giúp đánh giá mức độ nghiêm trọng của nhóm bệnh nhân đang dùng hệ thống). Đây là công cụ ra quyết định vận hành, không phải công cụ động viên cá nhân.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Dashboard thống kê              Khoảng [30 ngày▾] [Xuất ⬇]  │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐│
│ │ Lượt chat     │ │ Cảnh báo     │ │ Bệnh nhân    │ │ Tỷ lệ đánh   ││
│ │ 1,284         │ │ Triage        │ │ hoạt động    │ │ giá hữu ích  ││
│ │               │ │ 12            │ │ 342           │ │ 87%          ││
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘│
│                                                                 │
│  Triệu chứng được hỏi nhiều nhất                                │
│  Đầy bụng ████████████████ 234                                 │
│  Trào ngược ████████████ 189                                   │
│  Táo bón ████████ 112                                          │
│                                                                 │
│  Lượt chat theo ngày                                            │
│  ▃▅▇▅▃▆▇▆▅▃▇▆▅▆▇ (biểu đồ đường/cột theo thời gian)             │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Số liệu phải dẫn tới hành động, không chỉ để "biết" | "Triệu chứng được hỏi nhiều nhất" nên gợi ý trực tiếp: nội dung tài liệu nào cần bổ sung/cập nhật | Một dashboard chỉ để ngắm không mang lại giá trị vận hành — mục tiêu là giúp Admin/Biên tập chuyên môn y khoa *quyết định* nên làm gì tiếp theo |
| Số lượt cảnh báo Triage cần đặt cạnh ngữ cảnh | Không chỉ hiện con số, nên dẫn thẳng sang [SCR-AD-13-GiamSatCanhBao.md](SCR-AD-13-GiamSatCanhBao.md) để xem chi tiết | Một con số tăng đột biến có ý nghĩa khác hẳn nếu không biết chi tiết đứng sau nó là gì |
| Xuất báo cáo giữ nguyên bộ lọc đang xem | Xuất đúng khoảng thời gian/phạm vi đang hiển thị trên màn, không phải luôn xuất toàn bộ | Người xuất báo cáo thường đã lọc đúng thứ họ cần trình bày (vd báo cáo tháng cho cấp trên) — xuất khác với những gì đang xem trên màn hình gây nhầm lẫn |

## 4. Tham chiếu

- Chi tiết cảnh báo Triage: [SCR-AD-13-GiamSatCanhBao.md](SCR-AD-13-GiamSatCanhBao.md)
- Đánh giá chất lượng AI (khác với thống kê vận hành): `03-AIChatbot-RAG/00-TongQuan.md` §2.5, §3.5
