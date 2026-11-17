# SCR-PT-01 — Hồ sơ bệnh lý cá nhân

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0009` (khai báo lần đầu), `UC0010` (cập nhật), `UC0019` (xuất PDF — mô tả ở §5) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Thu thập đủ bối cảnh y tế cá nhân để AI tư vấn được *cá nhân hoá* thay vì trả lời chung chung cho mọi người |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao màn hình này quyết định chất lượng AI tư vấn

Một chatbot y tế không biết gì về bệnh nhân chỉ có thể trả lời như một cuốn cẩm nang tra cứu chung. Khi AI biết bệnh nhân **đang dị ứng gì, đang dùng thuốc gì, có bệnh nền gì** — câu trả lời chuyển từ "thông tin chung về viêm dạ dày" sang "lưu ý cho đúng bạn, người đang dùng thuốc X". Vì vậy, dù đây là 1 form khai báo (nghe có vẻ nhàm chán), nó chính là nơi "nạp nhiên liệu cá nhân hoá" cho toàn bộ trải nghiệm AI phía sau.

Khác với form đăng ký (phải tối giản tối đa), form này được khai báo **sau khi đã tin tưởng sản phẩm** — nên có thể hỏi sâu hơn, miễn là sắp xếp hợp lý, không dồn hết vào 1 màn dài gây ngợp.

## 2. Thiết kế màn hình

Chia theo nhóm thông tin, mỗi nhóm 1 khối gọn, có thể mở/thu để không ngợp mắt:

```text
┌───────────────────────────────────────────────────────────┐
│  Hồ sơ bệnh lý của bạn                                        │
│  Thông tin này giúp AI tư vấn chính xác và an toàn hơn cho     │
│  riêng bạn.                                       [Xuất PDF ⬇]│
│                                                                 │
│  ▾ Tiền sử bệnh lý tiêu hoá                                   │
│    [ + Thêm bệnh lý ]  (vd: Viêm dạ dày, Hội chứng ruột kích   │
│     thích, Trào ngược dạ dày-thực quản...)                    │
│                                                                 │
│  ▾ Dị ứng & không dung nạp thực phẩm                          │
│    [ + Thêm dị ứng ]  (vd: Lactose, Gluten, Hải sản...)        │
│                                                                 │
│  ▾ Thuốc đang sử dụng                                         │
│    [ + Thêm thuốc ]  Tên thuốc · Liều dùng · Tần suất          │
│                                                                 │
│  ▾ Thông tin khác                                              │
│    Chiều cao · Cân nặng (hỗ trợ tính các chỉ số liên quan)     │
│                                                                 │
│  ▾ Liên hệ khẩn cấp                                            │
│    Họ tên · Số điện thoại · Mối quan hệ với bạn                 │
│    ⓘ Chỉ dùng khi hệ thống phát hiện dấu hiệu nguy hiểm mà      │
│      không liên lạc được trực tiếp với bạn                       │
│                                                                 │
│                              [     Lưu thay đổi     ]          │
└───────────────────────────────────────────────────────────┘
```

Lần đầu vào màn này (chưa khai báo gì), nên có 1 banner nhẹ nhàng giải thích lý do cần khai báo — tránh cảm giác "lại một form hành chính nữa phải điền".

## 3. Vì sao các nhóm thông tin này, không phải nhóm khác

| Nhóm | Vì sao cần cho AI tư vấn tiêu hoá |
| :--- | :--- |
| Tiền sử bệnh lý | Một gợi ý đúng cho người khoẻ mạnh có thể sai hoàn toàn với người đang có bệnh nền (vd gợi ý ăn nhiều chất xơ không phù hợp lúc đang viêm loét cấp) |
| Dị ứng/không dung nạp | AI tuyệt đối không nên gợi ý món ăn/nhóm thực phẩm mà bệnh nhân đã khai là dị ứng — đây là ranh giới an toàn, không phải gợi ý tuỳ chọn |
| Thuốc đang dùng | Một số thuốc tiêu hoá có khung giờ uống quan trọng (trước/sau ăn) và có thể tương tác với nhau — biết bệnh nhân đang dùng gì giúp AI tránh đưa lời khuyên mâu thuẫn với phác đồ bác sĩ đã kê |
| Liên hệ khẩn cấp | Không phục vụ AI tư vấn — phục vụ lớp an toàn cuối cùng ([04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md](../04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md)): khi hệ thống phát hiện dấu hiệu nguy hiểm mà không liên lạc được trực tiếp với bệnh nhân trong thời gian ngắn, đây là nơi duy nhất để escalate thay vì bỏ cuộc |

## 4. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Chưa khai báo gì, vào màn lần đầu | Hiện trạng thái rỗng thân thiện, không phải bảng trống trơn khó hiểu | Tránh cảm giác "màn hình bị lỗi" |
| Thêm/sửa/xoá 1 mục trong danh sách (bệnh lý/dị ứng/thuốc) | Lưu ngay, không cần vào 1 màn "chỉnh sửa" riêng biệt | Đây là những danh sách ngắn, tách thành luồng Tạo/Sửa riêng chỉ làm chậm thao tác |
| Khai báo lần đầu và cập nhật sau này | Cùng 1 màn hình, không tách 2 trải nghiệm khác nhau | Với người dùng, "khai báo" và "cập nhật" là cùng một việc nhìn từ xa — tách biệt chỉ tạo thêm khái niệm phải học |

## 5. Xuất hồ sơ sức khoẻ ra PDF (`UC0019`)

Nút "Xuất PDF" đặt ngay trên màn hồ sơ — vì đây là nơi tổng hợp đầy đủ nhất. Bản PDF nên gộp cả hồ sơ bệnh lý, một khoảng thời gian nhật ký ăn uống/Bristol gần đây, để bệnh nhân **mang thẳng tới phòng khám** khi gặp bác sĩ — đây là giá trị thực tế lớn: bác sĩ khám 5-10 phút không có thời gian hỏi lại cả quá trình, một bản tóm tắt sẵn giúp buổi khám hiệu quả hơn hẳn. Nên cho chọn khoảng thời gian muốn xuất (vd 30 ngày gần nhất) thay vì luôn xuất toàn bộ lịch sử — tránh tài liệu quá dài không ai đọc hết tại phòng khám.

## 6. Tham chiếu

- Dữ liệu ở đây là nền tảng cho: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md), [SCR-PT-10-NhayCamThucPham.md](SCR-PT-10-NhayCamThucPham.md)
