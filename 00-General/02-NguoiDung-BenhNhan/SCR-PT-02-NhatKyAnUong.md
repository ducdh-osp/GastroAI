# SCR-PT-02 — Nhật ký ăn uống

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0011` (ghi nhận), `UC0020` (khôi phục mục đã xoá — phần ăn uống) |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Ghi lại "ăn gì, khi nào" nhanh và đều đặn — đây là 1 trong 2 nguồn dữ liệu chính (cùng với Bristol) để AI phát hiện mối liên hệ thực phẩm-triệu chứng sau này |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao "nhanh" quan trọng hơn "đầy đủ"

Bệnh nhân ghi nhật ký ăn uống **ngay sau bữa ăn**, thường đang bận, đang ở ngoài, đang cầm điện thoại 1 tay. Nếu form bắt nhập chi tiết dinh dưỡng, cân nặng khẩu phần, calo... họ sẽ ghi vài ngày rồi bỏ. Mục tiêu nghiệp vụ thật sự không phải "sổ nhật ký dinh dưỡng chính xác tuyệt đối" mà là **đủ dữ liệu, đều đặn, trong thời gian dài** để sau này nhìn ra quy luật — vì vậy form phải ưu tiên tốc độ nhập hơn độ chi tiết.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Nhật ký ăn uống                    [ + Ghi bữa ăn mới ]       │
│  ┌─────────┬──────────┐                                       │
│  │Danh sách │ Thùng rác│                                       │
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
│                    Phụ )             │
│  Món ăn          [___________________] │
│  Triệu chứng sau ăn (không bắt buộc)  │
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
| Thời điểm ăn | Có, mặc định "bây giờ" | Hầu hết trường hợp ghi ngay sau khi ăn — mặc định đúng giúp bỏ qua 1 bước thao tác |
| Loại bữa | Có | Giúp biểu đồ sau này phân biệt được bữa chính/phụ, hữu ích khi tìm quy luật |
| Món ăn | Có (chỉ cần mô tả tự do, không bắt chọn từ danh sách đóng) | Ẩm thực Việt Nam đa dạng tới mức một danh sách món ăn cố định sẵn sẽ luôn thiếu — để nhập tự do, AI xử lý văn bản tự do tốt hơn là bắt bệnh nhân tìm món không có trong danh sách rồi bỏ cuộc |
| Triệu chứng sau ăn | Không | Không phải bữa ăn nào cũng gây triệu chứng — bắt buộc trường này sẽ khiến nhiều người bỏ qua ghi nhật ký vì "không biết viết gì" |

## 4. Xoá & khôi phục (`UC0020`)

Bấm "Xoá" không xoá vĩnh viễn ngay — chuyển vào tab "Thùng rác", có thể khôi phục trong một khoảng thời gian hợp lý (đủ để bệnh nhân nhận ra lỡ tay, không cần giữ mãi mãi vì đây chỉ là lưới an toàn cho thao tác nhầm, không phải kho lưu trữ). Ngay sau khi xoá, nên có **thông báo nhỏ kèm nút "Hoàn tác" xuất hiện ngay tại chỗ** trong vài giây — hầu hết trường hợp bấm nhầm được sửa ngay tại đây, không cần mở tab thùng rác.

## 5. Vì sao dữ liệu này quan trọng hơn vẻ ngoài của nó

Mỗi bữa ăn ghi lại là 1 điểm dữ liệu cho tính năng "Hồ sơ nhạy cảm thực phẩm" ([SCR-PT-10](SCR-PT-10-NhayCamThucPham.md)) — càng ghi đều, AI càng có cơ sở để nhận ra "cứ ăn món X là vài giờ sau đầy bụng". Nên có thiết kế khuyến khích duy trì (xem [SCR-PT-06-StreakTuanThu.md](SCR-PT-06-StreakTuanThu.md)) thay vì chỉ là 1 cái form CRUD vô cảm.

## 6. Tham chiếu

- Xem lại theo thời gian: [SCR-PT-03-LichSuAnUong.md](SCR-PT-03-LichSuAnUong.md)
- Dùng để phân tích: [SCR-PT-10-NhayCamThucPham.md](SCR-PT-10-NhayCamThucPham.md)
