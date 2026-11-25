# SCR-PT-03 — Lịch sử & biểu đồ ăn uống

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0012` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Cho bệnh nhân *nhìn lại* quá trình ăn uống của mình một cách trực quan — phần thưởng tinh thần cho việc ghi chép đều đặn |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Tab Xu hướng trong trang Nhật ký ăn uống

Tab Nhật ký ([SCR-PT-02](SCR-PT-02-NhatKyAnUong.md)) phục vụ ghi và sửa bữa ăn; tab **Xu hướng** phục vụ nhìn lại mức độ ghi chép theo thời gian. Hai tab nằm ngang hàng trong `FoodDiaryPage`, cùng với tab Nhạy cảm thực phẩm.

Đường dẫn trực tiếp là `/food-diary?tab=trend`. Route cũ `/food-diary-history` chỉ chuyển hướng tới đường dẫn này. Vào `/food-diary` không có tham số tab thì mặc định mở Nhật ký.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký ăn uống                                               │
│  [Nhật ký]    [Xu hướng]    [Nhạy cảm thực phẩm]                 │
│  Khoảng xem: [7 ngày] [30 ngày] [90 ngày]                        │
│  [Tổng bữa] [Số ngày có ghi] [TB mỗi ngày có ghi]                │
│   Số bữa ghi nhận theo ngày                                    │
│   ▃▅▇▅▃▆▇▆▅▃▇▆▅▆▇▃▅▆▇▅▆▇▅▃▆▇▅▆▇▅                              │
│   (biểu đồ cột, 1 cột/ngày)                                    │
│                                                                 │
│   Bấm một cột để xem nhật ký của ngày đó                        │
└───────────────────────────────────────────────────────────┘
```

Trong tab Xu hướng không có toggle con "Biểu đồ / Danh sách". Danh sách bữa ăn thuộc tab Nhật ký. Khi toàn bộ khoảng xem chưa có dữ liệu, hiển thị trạng thái trống.

## 3. Nguyên tắc hiển thị

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Khoảng thời gian xem | Cho chọn (7/30/90 ngày), mặc định 30 ngày | Đủ dài để thấy xu hướng, không quá dài gây rối biểu đồ trên màn hình nhỏ |
| Ngày không ghi gì | Số bữa của ngày bằng 0 | Đây là thiếu ghi nhận, không phải lỗi tải dữ liệu |
| Thống kê trung bình | Tổng bữa chia cho số ngày có ghi nhật ký; bằng 0 nếu chưa có ngày ghi | Tránh hiểu nhầm là trung bình trên toàn bộ khoảng 7/30/90 ngày |
| Liên kết tới chi tiết | Bấm một cột → chuyển về tab Nhật ký, lọc ngày đã chọn; chọn Sửa tại đúng bữa cần sửa | Một ngày có thể có nhiều bữa, nên không tự mở form của một bữa bất kỳ |

## 4. Tham chiếu

- Ghi nhận dữ liệu: [SCR-PT-02-NhatKyAnUong.md](SCR-PT-02-NhatKyAnUong.md)
- Theo dõi mức độ đều đặn: [SCR-PT-06-StreakTuanThu.md](SCR-PT-06-StreakTuanThu.md)
- Đối chiếu code: `router.tsx`, `FoodDiaryPage.tsx`, `FoodDiaryTrendTab.tsx`, `FoodDiaryTrendChart.tsx`.
