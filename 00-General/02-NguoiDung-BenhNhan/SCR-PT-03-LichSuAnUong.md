# SCR-PT-03 — Lịch sử & biểu đồ ăn uống

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0012` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Cho bệnh nhân *nhìn lại* quá trình ăn uống của mình một cách trực quan — phần thưởng tinh thần cho việc ghi chép đều đặn |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với màn "Ghi nhận" ([SCR-PT-02](SCR-PT-02-NhatKyAnUong.md))

Màn ghi nhận phục vụ tốc độ; màn này phục vụ **sự thấu hiểu**. Bệnh nhân mở màn này không phải để thêm dữ liệu mà để tự hỏi "dạo này mình ăn uống thế nào, có đều đặn không, có pattern gì không". Vì vậy ưu tiên biểu đồ hơn bảng liệt kê thô.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Lịch sử ăn uống                    Khoảng thời gian [30 ngày ▾]│
│                                                                 │
│   Số bữa ghi nhận theo ngày                                    │
│   ▃▅▇▅▃▆▇▆▅▃▇▆▅▆▇▃▅▆▇▅▆▇▅▃▆▇▅▆▇▅                              │
│   (biểu đồ cột, 1 cột/ngày)                                    │
│                                                                 │
│   ┌─────────┬──────────┐                                       │
│   │ Biểu đồ  │ Danh sách│                                       │
│   └─────────┴──────────┘                                       │
│   14/11  🍜 Phở bò · Đầy bụng nhẹ                               │
│   14/11  ☕ Bánh mì, cà phê                                     │
│   13/11  🍲 Cơm, canh chua, cá kho                              │
│   ...                                                           │
└───────────────────────────────────────────────────────────┘
```

Mặc định hiện biểu đồ trước (không phải danh sách) — đúng với mục tiêu "nhìn lại cho dễ hiểu" hơn là tra cứu từng dòng.

## 3. Nguyên tắc hiển thị

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Khoảng thời gian xem | Cho chọn (7/30/90 ngày), mặc định 30 ngày | Đủ dài để thấy xu hướng, không quá dài gây rối biểu đồ trên màn hình nhỏ |
| Ngày không ghi gì | Hiển thị là khoảng trống/cột thấp, không phải lỗi | Bệnh nhân cần thấy rõ "mình đã bỏ lỡ ngày nào" một cách trung lập, không cảm giác bị chấm điểm |
| Liên kết tới chi tiết | Bấm vào 1 ngày/1 mục trong danh sách → mở lại đúng bữa ăn đó để sửa nếu cần | Không nên là màn hình "chỉ đọc" tuyệt đối — đôi khi xem lại mới nhớ ra ghi thiếu |

## 4. Tham chiếu

- Ghi nhận dữ liệu: [SCR-PT-02-NhatKyAnUong.md](SCR-PT-02-NhatKyAnUong.md)
- Theo dõi mức độ đều đặn: [SCR-PT-06-StreakTuanThu.md](SCR-PT-06-StreakTuanThu.md)
