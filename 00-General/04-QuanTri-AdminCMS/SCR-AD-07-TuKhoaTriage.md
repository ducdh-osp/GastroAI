# SCR-AD-07 — Quản lý từ khoá sàng lọc khẩn cấp

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0054` |
| Actor | Biên tập chuyên môn y khoa (thêm/sửa/xoá), Admin (xem) |
| Mục tiêu nghiệp vụ | Cho đội ngũ chuyên môn tự cập nhật danh sách dấu hiệu nguy hiểm cần cảnh báo, mà không cần chờ đội kỹ thuật sửa code |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao màn hình này nên tồn tại, dù trông "nhỏ"

Ngôn ngữ bệnh nhân dùng để mô tả triệu chứng nguy hiểm rất đa dạng ("đi ngoài ra máu", "phân đen như hắc ín", "đi cầu màu đỏ tươi"...) — không ai liệt kê hết ngay từ đầu. Nếu danh sách này nằm cứng trong code, mỗi lần phát hiện 1 cách diễn đạt mới chưa bắt được, đội ngũ chuyên môn phải **chờ đội kỹ thuật** sửa và triển khai lại — chậm, trong khi đây lại là phần liên quan trực tiếp tới an toàn bệnh nhân, cần cập nhật được ngay khi phát hiện thiếu sót.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Từ khoá sàng lọc khẩn cấp              [ + Thêm từ khoá ]    │
│  Nhóm: [Tất cả ▾]  (13 nhóm cố định: Xuất huyết tiêu hoá,       │
│   Đau bụng cấp, Sụt cân bất thường, Khó nuốt...)                │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Từ khoá/câu nói          │ Nhóm                │ Thao tác    │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ đi ngoài ra máu           │ Xuất huyết tiêu hoá  │ [Sửa][Xoá]  │ │
│ │ phân đen như hắc ín       │ Xuất huyết tiêu hoá  │ [Sửa][Xoá]  │ │
│ │ đau bụng dữ dội đột ngột  │ Đau bụng cấp         │ [Sửa][Xoá]  │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Nhóm cố định, không tự tạo nhóm mới tuỳ ý | 13 nhóm y khoa đã xác định sẵn (xuất huyết tiêu hoá, đau bụng cấp, sụt cân, khó nuốt...), chỉ thêm/sửa/xoá **từ khoá bên trong** mỗi nhóm | Việc phân loại nhóm dấu hiệu nguy hiểm cần cơ sở y khoa nhất quán — để tự tạo nhóm tuỳ hứng dễ dẫn tới phân loại chồng chéo, khó đánh giá độ nhạy tổng thể sau này |
| Thêm từ khoá dễ dàng, nhưng xoá cần cân nhắc | Xoá nên có xác nhận rõ ràng, nói rõ "từ khoá này sẽ không còn được dùng để phát hiện dấu hiệu nguy hiểm nữa" | Xoá nhầm 1 từ khoá đang hoạt động tốt có thể làm giảm khả năng phát hiện nguy hiểm — hậu quả không hiện ngay mà chỉ lộ ra khi có 1 trường hợp thật bị bỏ sót |
| Thay đổi có hiệu lực ngay sau khi lưu, không cần chờ triển khai lại hệ thống | Cập nhật tức thời | Đúng mục tiêu ban đầu của màn hình này: phản ứng nhanh khi phát hiện thiếu sót, không phải quy trình chậm như sửa code |
| Từ khoá do AI/hệ thống tự đề xuất (nếu có, vd từ các câu hỏi bị bỏ sót trong đánh giá `UC0038`) **không tự động áp dụng** | Luôn vào danh sách "Đề xuất chờ duyệt" riêng, Biên tập chuyên môn y khoa xem và xác nhận trước khi chuyển thành từ khoá chính thức | Rủi ro 2 chiều nếu để tự động: bỏ sót cách nói dân dã thật sự nguy hiểm (vd "nôn ra dịch cà phê" = xuất huyết tiêu hoá), hoặc ngược lại tự thêm từ quá chung chung gây báo động giả tràn lan, làm giảm độ nhạy thật của cả hệ thống theo thời gian ("alert fatigue") |

## 4. Tham chiếu

- Cơ chế sử dụng các từ khoá này: `03-AIChatbot-RAG/00-TongQuan.md` §3.2
- Danh sách dấu hiệu bắt buộc phải có: `03-AIChatbot-RAG/00-TongQuan.md` §3.2 (bảng dấu hiệu tối thiểu)
