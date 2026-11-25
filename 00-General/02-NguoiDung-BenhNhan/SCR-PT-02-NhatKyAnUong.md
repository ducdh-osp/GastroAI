# SCR-PT-02 — Nhật ký ăn uống

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0011` (ghi nhận), `UC0020` (khôi phục mục đã xoá — phần ăn uống) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Ghi lại "ăn gì, khi nào" nhanh và đều đặn — cùng với Bristol, đây là nguồn dữ liệu cho thống kê tương quan thực phẩm và triệu chứng |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao "nhanh" quan trọng hơn "đầy đủ"

Bệnh nhân ghi nhật ký ăn uống **ngay sau bữa ăn**, thường đang bận, đang ở ngoài, đang cầm điện thoại 1 tay. Nếu form bắt nhập chi tiết dinh dưỡng, cân nặng khẩu phần, calo... họ sẽ ghi vài ngày rồi bỏ. Mục tiêu nghiệp vụ thật sự không phải "sổ nhật ký dinh dưỡng chính xác tuyệt đối" mà là **đủ dữ liệu, đều đặn, trong thời gian dài** để sau này nhìn ra quy luật — vì vậy form phải ưu tiên tốc độ nhập hơn độ chi tiết.

## 2. Thiết kế màn hình

Trang `/food-diary` có ba tab ngang hàng: **Nhật ký**, **Xu hướng**, **Nhạy cảm thực phẩm**. Trong tab Nhật ký có lựa chọn **Nhật ký / Thùng rác**. SCR-PT-03 mô tả tab Xu hướng trên cùng trang.

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký ăn uống                    [ + Ghi bữa ăn mới ]       │
│  [Nhật ký]    [Xu hướng]    [Nhạy cảm thực phẩm]                 │
│  ┌─────────┬──────────┐                                       │
│  │ Nhật ký  │ Thùng rác│                                       │
│  └─────────┴──────────┘                                       │
│  Hôm nay                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ 🍜 12:30 · Bữa trưa                                         │ │
│  │ Phở bò, 1 ly trà đá                                         │ │
│  │ Triệu chứng sau ăn: Đầy bụng nhẹ          [Sửa] [Xoá]       │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ ☕ 07:15 · Bữa sáng                                         │ │
│  │ Bánh mì, cà phê sữa                       [Sửa] [Xoá]       │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Form ghi bữa ăn mới (mở dạng lớp phủ, không chuyển trang) —
┌─────────────────────────────────┐
│  Ghi bữa ăn mới                     │
│  Thời điểm ăn   [ Bây giờ ▾ ]       │
│  Loại bữa        ( Sáng/Trưa/Tối/   │
│                    Phụ/Khác )         │
│  Món ăn          [___________________] │
│  Triệu chứng sau ăn (không bắt buộc)  │
│                  [___________________] │
│  Thời gian khởi phát (phút, tuỳ chọn) │
│                  [___________________] │
│  Ghi chú (không bắt buộc)              │
│                  [___________________] │
│       [     Lưu     ]                  │
└─────────────────────────────────┘
```

Mở dạng lớp phủ (không chuyển hẳn sang trang khác) để ghi xong quay lại ngay danh sách, không mất ngữ cảnh.

## 3. Nguyên tắc cho từng trường

| Trường | Bắt buộc? | Lý do |
| :--- | :--- | :--- |
| Thời điểm ăn | Có, mặc định "bây giờ", không ở tương lai | Hỗ trợ ghi ngay sau bữa ăn hoặc nhập lại bữa trước đó |
| Loại bữa | Có; Sáng/Trưa/Tối/Phụ/Khác, mặc định Khác | Tương ứng `BREAKFAST/LUNCH/DINNER/SNACK/OTHER`; lựa chọn Khác giúp ghi bữa không thuộc bốn nhóm còn lại |
| Món ăn | Có (chỉ cần mô tả tự do, không bắt chọn từ danh sách đóng) | Ẩm thực Việt Nam đa dạng tới mức một danh sách món ăn cố định sẵn sẽ luôn thiếu — để nhập tự do, AI xử lý văn bản tự do tốt hơn là bắt bệnh nhân tìm món không có trong danh sách rồi bỏ cuộc |
| Triệu chứng sau ăn | Không | Không phải bữa ăn nào cũng gây triệu chứng — bắt buộc trường này sẽ khiến nhiều người bỏ qua ghi nhật ký vì "không biết viết gì" |
| Thời gian khởi phát (`symptomOnsetMinutes`) | Không; số phút từ bữa ăn đến khi xuất hiện triệu chứng | Để trống nếu không biết hoặc không ghi nhận triệu chứng; giá trị 0 có nghĩa khởi phát ngay, khác với để trống. Form cho nhập 0–10.080 phút; API hiện kiểm tra số nguyên không âm |
| Ghi chú | Không, tối đa 5.000 ký tự | Lưu thêm khẩu phần hoặc bối cảnh bữa ăn |

Món ăn tối đa 500 ký tự; triệu chứng sau ăn tối đa 1.000 ký tự. Trường thời gian khởi phát hiện được lưu cùng nhật ký; việc lưu trường này không đồng nghĩa chat AI tự đọc dữ liệu nhật ký.

## 4. Xoá & khôi phục (`UC0020`)

Bấm "Xoá" chuyển mục vào Thùng rác trong tab Nhật ký. Có thể khôi phục trong thời hạn cấu hình, mặc định 30 ngày. Sau khi xoá, thông báo có nút **Hoàn tác** để khôi phục ngay. Đây là hành vi đã có của UC0020, dùng chung nguyên tắc với nhật ký Bristol.

## 5. Vì sao dữ liệu này quan trọng hơn vẻ ngoài của nó

Mỗi bữa ăn là một điểm dữ liệu cho [Nhạy cảm thực phẩm](SCR-PT-10-NhayCamThucPham.md). Tính năng hiện dùng thống kê tương quan nhật ký ăn uống/Bristol, không gọi AI để suy luận nguyên nhân. Ghi đều giúp có đủ dữ liệu để xem lại; kết quả tương quan không xác nhận món ăn gây bệnh. Chat AI hiện chưa tự nạp nhật ký, xem [SCR-PT-08](SCR-PT-08-ChatAI.md).

## 6. Tham chiếu

- Xem lại theo thời gian: [SCR-PT-03-LichSuAnUong.md](SCR-PT-03-LichSuAnUong.md)
- Dùng để phân tích: [SCR-PT-10-NhayCamThucPham.md](SCR-PT-10-NhayCamThucPham.md)
- Đối chiếu code: `FoodDiaryPage.tsx`, `FoodDiaryEntryModal.tsx`, `FoodDiaryEntryRequest.java`, `MealType.java`, `FoodDiaryJournalTab.tsx`, `FoodDiaryService.java`.
