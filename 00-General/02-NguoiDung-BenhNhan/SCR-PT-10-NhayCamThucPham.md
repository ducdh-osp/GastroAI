# SCR-PT-10 — Hồ sơ nhạy cảm thực phẩm

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0022` (phân tích tương quan), `UC0023` (tổng hợp thành hồ sơ) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Tự động phát hiện mối liên hệ giữa món ăn và triệu chứng mà bản thân bệnh nhân khó nhận ra nếu chỉ tự đọc lại nhật ký bằng mắt thường |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Đây là nơi "sức mạnh AI" thể hiện rõ nhất ngoài màn chat

Một bệnh nhân ghi nhật ký ăn uống + Bristol đều đặn trong vài tuần sẽ có hàng chục, hàng trăm điểm dữ liệu — **không ai tự ngồi đối chiếu bằng tay nổi**. Đây chính xác là việc máy làm tốt hơn người: tự động quét toàn bộ lịch sử, tìm ra món/nhóm thực phẩm nào **hay xuất hiện trước các lần có triệu chứng**, rồi trình bày lại thành một bức tranh dễ hiểu. Không có tính năng này, 2 nhật ký ở `SCR-PT-02`/`SCR-PT-04` chỉ là 2 cuốn sổ ghi chép — có tính năng này, chúng trở thành dữ liệu có thể hành động.

Đây cũng là tính năng dễ bị hiểu sai thành "chẩn đoán" nhất trong toàn phân hệ — nên phần kiểm soát kỳ vọng (§3) quan trọng không kém phần hiển thị kết quả.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Hồ sơ nhạy cảm thực phẩm của bạn                              │
│  ⓘ Đây là gợi ý thống kê từ chính nhật ký của bạn, không phải  │
│    kết luận y khoa. Hãy trao đổi với bác sĩ trước khi loại bỏ  │
│    hẳn một nhóm thực phẩm khỏi chế độ ăn.                       │
│                                                                 │
│  Những món/nhóm thực phẩm hay đi kèm triệu chứng:               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 🌶 Đồ cay            Xuất hiện cùng triệu chứng 7/9 lần      │ │
│  │                      [Xem 7 lần ghi nhận liên quan →]       │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │ 🥛 Sữa & chế phẩm sữa  Xuất hiện cùng triệu chứng 4/11 lần   │ │
│  │                      [Xem 4 lần ghi nhận liên quan →]       │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                                 │
│  Cần thêm dữ liệu để phân tích: Hải sản (mới ghi 2 lần)         │
│                                                                 │
│        [  Hỏi AI về kết quả này  ]                             │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc kiểm soát kỳ vọng — quan trọng nhất của màn hình này

| Nguyên tắc | Cách thể hiện | Vì sao |
| :--- | :--- | :--- |
| Luôn hiện **bằng chứng thô** đứng sau mỗi kết luận | "Xem N lần ghi nhận liên quan" dẫn thẳng tới đúng các bữa ăn/triệu chứng cụ thể đã ghi | Bệnh nhân phải tự thấy được "à, đúng là có 7 lần như vậy thật" — không chỉ tin vào 1 con số % hiện ra từ đâu không rõ. Đây cũng là cách giảm thiểu rủi ro từ suy luận thống kê đơn giản: cho người đọc tự đánh giá mẫu dữ liệu, không "đóng gói" kết luận như một sự thật tuyệt đối |
| Không dùng ngôn ngữ khẳng định ("gây ra", "là nguyên nhân") | Dùng "hay xuất hiện cùng", "có liên quan tới" | "Tương quan không phải nhân quả" — đồ cay xuất hiện cùng triệu chứng nhiều lần có thể vì bệnh nhân ăn cay thường xuyên (trùng hợp tần suất), không nhất thiết là nguyên nhân thật sự |
| Luôn có khung cảnh báo giới hạn ở đầu trang, không phải chữ nhỏ ở cuối | Đặt ngay dưới tiêu đề, không ẩn | Đây là thông tin quan trọng ngang với chính kết quả phân tích — nếu đặt ở cuối trang, phần lớn người dùng sẽ không đọc tới |
| Yêu cầu đủ dữ liệu tối thiểu trước khi kết luận | Món mới ghi 1-2 lần hiện riêng ở mục "cần thêm dữ liệu", không trộn chung | Vài lần trùng hợp ngẫu nhiên không nên được trình bày ngang hàng với 1 mẫu dữ liệu đủ lớn — tránh đưa ra gợi ý vội vàng có thể khiến bệnh nhân loại bỏ nhầm 1 nhóm thực phẩm quan trọng (rủi ro dinh dưỡng, đặc biệt với bệnh nhân mạn tính vốn đã cần chế độ ăn cân bằng, càng kiêng khem tuỳ tiện càng dễ thiếu chất) |
| Luôn có lối dẫn sang hỏi AI trực tiếp | Nút "Hỏi AI về kết quả này" | Một bảng số liệu tĩnh dễ bị hiểu sai một mình — để bệnh nhân hỏi thêm ngay tại chỗ ("đồ cay có thật sự là vấn đề của tôi không, tôi nên làm gì") giúp AI bổ sung ngữ cảnh, giảm nguy cơ họ tự diễn giải sai rồi hành động theo cách không phù hợp |

## 4. Khi nào tính năng này "chưa sẵn sàng" để hiển thị

Nếu bệnh nhân mới dùng app vài ngày, chưa có đủ dữ liệu nhật ký, màn hình nên hiện trạng thái "Hãy ghi nhật ký đều đặn hơn để nhận gợi ý chính xác" thay vì cố hiển thị 1 kết quả yếu dựa trên quá ít dữ liệu — một kết quả vội vàng, sai, sẽ phá vỡ niềm tin vào toàn bộ tính năng AI của sản phẩm ngay từ lần đầu dùng thử.

## 5. Tham chiếu

- Nguồn dữ liệu: [SCR-PT-02-NhatKyAnUong.md](SCR-PT-02-NhatKyAnUong.md), [SCR-PT-04-NhatKyBristol.md](SCR-PT-04-NhatKyBristol.md)
- Hỏi sâu hơn: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
