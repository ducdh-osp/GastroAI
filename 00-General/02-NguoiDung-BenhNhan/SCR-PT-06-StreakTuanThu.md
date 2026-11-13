# SCR-PT-06 — Chuỗi ngày ghi nhật ký & tỷ lệ tuân thủ

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0025` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Tạo động lực để bệnh nhân duy trì thói quen ghi nhật ký — vì chính sự đều đặn đó mới làm cho AI tư vấn ngày càng hiểu họ hơn |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần 1 màn hình chỉ để "động viên"

Bài toán lớn nhất của mọi app theo dõi sức khoẻ không phải là tính năng ghi nhận (dễ làm) mà là **giữ chân người dùng ghi đều đặn** (khó làm). Sau 1-2 tuần mới lạ, phần lớn người dùng rơi vào trạng thái "quên mất mình đang dùng app này". Màn hình này đóng vai trò giống các app gamification sức khoẻ phổ biến — cho bệnh nhân thấy **nỗ lực của họ được ghi nhận bằng con số cụ thể**, biến việc ghi nhật ký từ "nghĩa vụ" thành "thành tích muốn giữ".

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Chuỗi ngày ghi nhật ký                                       │
│  Một ngày được tính là "đã ghi" nếu bạn có ít nhất 1 lần ghi   │
│  nhật ký ăn uống hoặc Bristol.                                  │
│                                                                 │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐              │
│  │ 🔥 Chuỗi     │  │ 🏆 Dài nhất  │  │ Tỷ lệ tuân   │              │
│  │   hiện tại   │  │              │  │   thủ         │              │
│  │   14 ngày    │  │   21 ngày    │  │   82%          │              │
│  └────────────┘  └────────────┘  └────────────┘              │
│                                                                 │
│  ████████████████████████████████░░░░░░░░░░░░░░              │
│                                                                 │
│  30 ngày gần nhất                                               │
│  ▪▪▪▪▪▪▫▪▪▪▪▪▪▪▪▫▪▪▪▪▪▪▪▪▫▪▪▪▪▪   (▪ = có ghi, ▫ = chưa ghi)    │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc thiết kế động lực

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Ghi nhận **nỗ lực**, không chỉ kết quả hoàn hảo | Chỉ cần ghi 1 trong 2 loại nhật ký (ăn uống **hoặc** Bristol) là tính "có ghi" trong ngày, không bắt ghi đủ cả 2 | Yêu cầu càng khắt khe, bệnh nhân càng dễ nản khi lỡ 1 ngày không ghi đủ |
| Không trừng phạt khi đứt chuỗi | Khi chuỗi hiện tại về 0, hiển thị trung lập ("Bắt đầu chuỗi mới hôm nay"), không có thông điệp tiêu cực | Một bệnh nhân mạn tính có thể có lý do chính đáng để gián đoạn (ốm nặng hơn, nằm viện...) — app trách móc sẽ khiến họ bỏ hẳn thay vì quay lại |
| Ưu tiên "chuỗi dài nhất từng đạt" hiển thị song song với chuỗi hiện tại | Luôn hiện cả 2 số | Dù chuỗi hiện tại bị đứt, thành tích cũ vẫn là động lực để cố gắng phá kỷ lục của chính mình, thay vì cảm giác "mất hết công sức" |
| Tỷ lệ tuân thủ tính theo khoảng dài hơn (vd 30-90 ngày) | Không chỉ nhìn 7 ngày gần nhất | Phản ánh đúng thói quen dài hạn — điều thật sự quan trọng với bệnh mạn tính cần theo dõi liên tục, không phải phong trào ngắn hạn |

## 4. Tham chiếu

- Nguồn dữ liệu: [SCR-PT-02-NhatKyAnUong.md](SCR-PT-02-NhatKyAnUong.md), [SCR-PT-04-NhatKyBristol.md](SCR-PT-04-NhatKyBristol.md)
- Nhắc nhở khi sắp đứt chuỗi: `UC0024`, xem `05-ThongBao-Notification/`
