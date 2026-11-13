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

```text
┌───────────────────────────────────────────────────────────┐
│  Xu hướng tiêu hoá                   Khoảng thời gian [30 ngày▾]│
│                                                                 │
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
│  Vùng tô màu nhạt: mức 3-5 (bình thường)                        │
│                                                                 │
│  ⓘ Bạn có 3 lần ghi nhận mức 6-7 trong 7 ngày qua — nếu kèm     │
│    đau bụng hoặc kéo dài, hãy trò chuyện với AI để được tư vấn   │
│    thêm.                                      [Hỏi AI ngay →]  │
└───────────────────────────────────────────────────────────┘
```

Vùng tô màu nhạt ở mức 3-5 giúp bệnh nhân **nhìn phát biết ngay** điểm nào nằm trong/ngoài vùng bình thường, không cần tự nhớ ý nghĩa từng mức.

## 3. Nguyên tắc diễn giải

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Gợi ý khi có nhiều điểm bất thường gần đây | Chỉ **gợi ý trò chuyện với AI**, không tự kết luận tình trạng bệnh | Đây là màn hình hiển thị dữ liệu, không phải màn hình chẩn đoán — mọi diễn giải sâu hơn nên dẫn người dùng sang kênh có thể hỏi-đáp thêm (chat AI) thay vì đưa ra 1 câu kết luận tĩnh có thể sai ngữ cảnh |
| Không đưa ra kết luận kiểu "Bạn có thể bị IBS" | Tuyệt đối tránh | Tên 1 bệnh cụ thể đi kèm dữ liệu cá nhân, hiển thị như một khẳng định, dễ bị hiểu nhầm là chẩn đoán chính thức — vượt quá vai trò gợi ý sơ bộ của sản phẩm |
| Khoảng thời gian xem | 7/30/90 ngày, mặc định 30 | Đồng nhất với [SCR-PT-03](SCR-PT-03-LichSuAnUong.md) để bệnh nhân quen 1 kiểu điều khiển duy nhất trong toàn app |

## 4. Tham chiếu

- Ghi nhận dữ liệu: [SCR-PT-04-NhatKyBristol.md](SCR-PT-04-NhatKyBristol.md)
- Trò chuyện khi có bất thường: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
