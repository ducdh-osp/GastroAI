# SCR-PT-05 — Xu hướng tiêu hoá theo thời gian

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0014` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Biến các mức Bristol rời rạc từng ngày thành 1 bức tranh xu hướng dễ hiểu — "dạo này tiêu hoá của tôi ổn định hay đang bất thường" |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần biểu đồ xu hướng, không chỉ danh sách

Một con số Bristol đơn lẻ ít ý nghĩa — giá trị thật nằm ở **xu hướng theo thời gian**: dao động quanh mức 4 là ổn định, trong khi liên tục trồi sụt giữa mức 1 và mức 7 là dấu hiệu rối loạn chức năng tiêu hoá đáng chú ý (gần với các tiêu chí phân loại rối loạn tiêu hoá chức năng kiểu hội chứng ruột kích thích — dù màn hình này không chẩn đoán, dữ liệu xu hướng chính là thứ giúp bác sĩ sau này đặt câu hỏi đúng hướng hơn).

## 2. Thiết kế màn hình

Đây là tab **Xu hướng** của trang Nhật ký Bristol, mở trực tiếp tại `/bristol?tab=trend`. Route cũ `/bristol-history` chuyển hướng tới tab này; `/bristol` mặc định mở tab Nhật ký.

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký Bristol                                               │
│  [Nhật ký]    [Xu hướng]                                       │
│  Khoảng xem: [7 ngày] [30 ngày] [90 ngày]                        │
│  [Tổng lần] [Tỷ lệ loại 3–5] [Ghi nhận gần nhất]                │
│  7 ┤                        ●                                  │
│  6 ┤              ●     ●       ●                               │
│  5 ┤        ●                        ●                          │
│  4 ┤  ●  ●          ●         ●           ●  ●                  │
│  3 ┤                                                            │
│  2 ┤                                                            │
│  1 ┤                                                            │
│    └──────────────────────────────────────────────────────    │
│     1/11        10/11        20/11        30/11                │
│                                                                 │
│  Bấm một điểm để xem nhật ký của ngày tương ứng                 │
└───────────────────────────────────────────────────────────┘
```

Biểu đồ hiện nối các lần ghi nhận, có nhãn mô tả ngắn trên trục dọc và tooltip; trục số trong sơ đồ trên chỉ minh hoạ thang 1–7. **Chưa có vùng nền tô mức 3–5 hoặc banner dẫn sang AI.** Thẻ "Trong khoảng loại 3–5" là tỷ lệ số lần thuộc nhóm này trên tổng số lần ghi trong khoảng xem. Khi chưa có dữ liệu, hiển thị trạng thái trống.

## 3. Nguyên tắc diễn giải

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Xem lại một thời điểm | Bấm điểm biểu đồ → tab Nhật ký lọc theo ngày tại Việt Nam của điểm đó | Xem tất cả lần ghi trong ngày rồi chọn mục cần sửa |
| Các thẻ thống kê | Hiện tổng lần, tỷ lệ loại 3–5, ghi nhận gần nhất | Mô tả dữ liệu đã ghi; không phải kết luận chẩn đoán |
| Không đưa ra kết luận kiểu "Bạn có thể bị IBS" | Tuyệt đối tránh | Tên 1 bệnh cụ thể đi kèm dữ liệu cá nhân, hiển thị như một khẳng định, dễ bị hiểu nhầm là chẩn đoán chính thức — vượt quá vai trò gợi ý sơ bộ của sản phẩm |
| Khoảng thời gian xem | 7/30/90 ngày, mặc định 30 | Đồng nhất với [SCR-PT-03](SCR-PT-03-LichSuAnUong.md) để bệnh nhân quen 1 kiểu điều khiển duy nhất trong toàn app |

## 4. Đề xuất chưa triển khai

- Tô nhạt vùng mức 3–5 để dễ đọc biểu đồ.
- Banner gợi ý mở chat khi có nhiều ghi nhận cần theo dõi. Ví dụ "3 lần mức 6–7 trong 7 ngày" trong mockup cũ chỉ là ý tưởng thiết kế, chưa phải quy tắc cảnh báo đã được hệ thống áp dụng. Cần xác định tiêu chí, nội dung và ngữ cảnh chuyển sang chat trước khi triển khai.

## 5. Tham chiếu

- Ghi nhận dữ liệu: [SCR-PT-04-NhatKyBristol.md](SCR-PT-04-NhatKyBristol.md)
- Trò chuyện khi có bất thường: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
- Đối chiếu code: `router.tsx`, `BristolPage.tsx`, `BristolTrendTab.tsx`, `BristolTrendChart.tsx`.
