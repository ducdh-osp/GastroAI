# SCR-NT-03 — Gửi thông báo broadcast

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0075` |
| Actor | Admin |
| Mục tiêu nghiệp vụ | Gửi 1 thông báo chung tới nhiều/toàn bộ bệnh nhân cùng lúc — bảo trì hệ thống, tài liệu y khoa mới, thông báo quan trọng khác |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao cần tách riêng khỏi các thông báo cá nhân hoá khác

Mọi thông báo khác trong hệ thống (nhắc thuốc, cảnh báo Triage, nhắc nhật ký) đều **gửi riêng cho từng cá nhân dựa trên dữ liệu của chính họ**. Broadcast thì ngược lại — cùng 1 nội dung, gửi cho **nhiều người cùng lúc theo chủ đích của Admin**, không dựa trên dữ liệu cá nhân. Đây là hành động có khả năng ảnh hưởng tới rất nhiều người cùng lúc, cần quy trình thận trọng hơn hẳn 1 thông báo cá nhân thông thường.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Gửi thông báo broadcast                                       │
│  Đối tượng nhận                                                │
│  ( • ) Toàn bộ bệnh nhân        ( ) Chọn nhóm cụ thể            │
│                                                                 │
│  Tiêu đề     [_________________________________________]      │
│  Nội dung    [_________________________________________]      │
│              [_________________________________________]      │
│                                                                 │
│  ⓘ Ước tính gửi tới 342 bệnh nhân (đã trừ những người tắt      │
│    "Thông báo hệ thống" trong cài đặt cá nhân)                  │
│                                                                 │
│       [  Xem trước  ]              [    Gửi ngay    ]          │
└───────────────────────────────────────────────────────────┘

— Xác nhận trước khi gửi thật —
┌─────────────────────────────────┐
│  Xác nhận gửi broadcast               │
│  Gửi tới 342 bệnh nhân. Hành động này │
│  không thể thu hồi sau khi gửi.       │
│       [Huỷ]   [  Xác nhận gửi  ]     │
└─────────────────────────────────┘
```

## 3. Nguyên tắc

| Nguyên tắc | Cách áp dụng | Vì sao |
| :--- | :--- | :--- |
| Luôn hiện số lượng người sẽ nhận trước khi gửi | "Ước tính gửi tới 342 bệnh nhân" | Gửi nhầm/gửi thiếu kiểm soát quy mô tác động là rủi ro lớn — Admin cần biết chính xác quy mô trước khi quyết định |
| Tôn trọng lựa chọn đã tắt thông báo hệ thống | Tự động trừ ra khỏi số lượng ước tính | Nhất quán với nguyên tắc ở [SCR-NT-01](SCR-NT-01-TrungTamThongBao.md) — broadcast không phải ngoại lệ được phép bỏ qua lựa chọn cá nhân |
| Có bước xác nhận riêng, nói rõ "không thể thu hồi" | Popup xác nhận trước khi gửi thật | Đây là hành động một chiều, ảnh hưởng số đông — không nên dễ bấm nhầm như các thao tác thông thường khác |
| Cho xem trước giao diện thông báo sẽ nhận được | Nút "Xem trước" | Giảm rủi ro gửi đi nội dung có lỗi định dạng/lỗi chính tả cho hàng trăm người cùng lúc |

## 4. Tham chiếu

- Nơi bệnh nhân nhận và xem lại: [SCR-NT-01-TrungTamThongBao.md](SCR-NT-01-TrungTamThongBao.md)
- Mọi lượt gửi broadcast được ghi vào: `04-QuanTri-AdminCMS/SCR-AD-06-NhatKyHoatDong.md`
