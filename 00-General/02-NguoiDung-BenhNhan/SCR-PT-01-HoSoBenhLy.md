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

> ⚠️ **Tình trạng thật (xem cảnh báo chi tiết ở §3)**: đoạn trên mô tả *mục tiêu thiết kế*, chưa phải *hiện trạng* — pipeline chat AI hiện chưa đọc dữ liệu hồ sơ này khi trả lời. Giữ nguyên mục tiêu ở đây vì đây vẫn là lý do màn hình này tồn tại, nhưng đừng hiểu nhầm là đã hoạt động.

## 2. Thiết kế màn hình

Chia theo nhóm thông tin, đúng thứ tự thật trên form:

```text
┌───────────────────────────────────────────────────────────┐
│  Hồ sơ bệnh lý của bạn                                        │
│  Khai báo thông tin cơ bản, tiền sử bệnh, dị ứng và thuốc      │
│  không cần lịch nhắc để trợ lý AI tham khảo. Đây là thông tin  │
│  tham khảo, không thay thế chẩn đoán y tế.         [Xuất PDF ⬇]│
│                                                                 │
│  Ngày sinh ______   Giới tính ▾   Chiều cao __cm  Cân nặng __kg│
│                                                                 │
│  Bệnh nền đang mắc                                             │
│    [ Chọn hoặc nhập bệnh lý, có gợi ý từ danh mục ]            │
│    (vd: Viêm dạ dày, Hội chứng ruột kích thích, Trào ngược...) │
│                                                                 │
│  Tiền sử phẫu thuật                                            │
│    [ textarea tự do, mỗi mục 1 dòng hoặc ngăn cách bằng dấu  ] │
│    [ phẩy — vd: Cắt túi mật 2021, Nội soi cắt polyp 2023     ] │
│                                                                 │
│  Dị ứng                                                        │
│    [ textarea tự do — vd: Hải sản, Penicillin...             ] │
│                                                                 │
│  Thuốc không cần lịch nhắc (tự khai)                           │
│    [ Chọn hoặc nhập thuốc, có gợi ý từ danh mục ]              │
│    ⓘ Chỉ nhập thuốc KHÔNG cần nhắc trong app. Mục này độc lập │
│      — không tự tạo/cập nhật/tắt lịch nhắc. Thuốc có liều cần │
│      nhắc → quản lý ở "Nhắc uống thuốc" (màn khác, có cấu trúc│
│      Tên thuốc·Liều·Khung giờ riêng)                           │
│                                                                 │
│  Chế độ ăn đặc biệt / không dung nạp                           │
│    [ textarea tự do — vd: không dung nạp lactose, ăn chay    ] │
│                                                                 │
│  Ghi chú thêm về tiền sử bệnh                                  │
│    [ textarea tự do — thông tin khác chưa có mục riêng       ] │
│                                                                 │
│  ⚠ Liên hệ khẩn cấp                                            │
│    Họ tên người liên hệ ______  SĐT ______  Mối quan hệ ______ │
│    ⓘ Chỉ dùng khi hệ thống phát hiện dấu hiệu nguy hiểm mà      │
│      không liên lạc được trực tiếp với bạn — Admin sẽ TỰ GỌI   │
│      số này bằng điện thoại cá nhân, hệ thống không tự gọi     │
│                                                                 │
│                              [     Lưu thay đổi     ]          │
└───────────────────────────────────────────────────────────┘
```

8 nhóm trên là đúng những gì đã code (không phải mockup lý tưởng hoá): "Dị ứng" và "Chế độ ăn đặc biệt / không dung nạp" là 2 field tách riêng — không gộp (lý do lâm sàng ở §3); "Tiền sử phẫu thuật" và "Ghi chú thêm về tiền sử bệnh" tách riêng khỏi "Bệnh nền đang mắc"; "Thuốc không cần lịch nhắc" chỉ là tag tự do, không có cột Liều dùng/Tần suất riêng (cấu trúc 3 cột đó thuộc tính năng Nhắc uống thuốc khác).

Lần đầu vào màn này (chưa khai báo gì), nên có 1 banner nhẹ nhàng giải thích lý do cần khai báo — tránh cảm giác "lại một form hành chính nữa phải điền".

## 3. Vì sao các nhóm thông tin này, không phải nhóm khác

| Nhóm (field thật) | Vì sao cần cho AI tư vấn tiêu hoá |
| :--- | :--- |
| Ngày sinh · Giới tính · Chiều cao · Cân nặng | Tuổi và giới tính thay đổi ngưỡng tham chiếu lâm sàng (độ tuổi nên lưu ý triệu chứng báo động, thiên hướng giới tính của một số bệnh lý); chiều cao/cân nặng dùng tính BMI — béo phì là yếu tố nguy cơ đã chứng minh của GERD |
| Bệnh nền đang mắc | Một gợi ý đúng cho người khoẻ mạnh có thể sai hoàn toàn với người có bệnh nền (vd khuyên ăn nhiều chất xơ không phù hợp lúc đang viêm loét cấp hoặc đang hẹp ruột do Crohn) |
| Tiền sử phẫu thuật | **Tách riêng khỏi "bệnh nền" vì lý do lâm sàng thật**: phẫu thuật tiêu hoá để lại thay đổi giải phẫu/sinh lý vĩnh viễn, khác bản chất "đang mắc bệnh" — vd sau cắt túi mật dễ tiêu chảy do rối loạn tiêu mỡ, sau cắt dạ dày có thể gặp hội chứng dumping, sau cắt đoạn đại tràng thay đổi tần suất đi cầu "bình thường mới" mà AI cần hiểu để không nhầm là triệu chứng bất thường |
| Dị ứng | **Ranh giới an toàn tuyệt đối, không phải gợi ý tuỳ chọn**: dị ứng thật là phản ứng miễn dịch, có thể gây mề đay, phù mạch, sốc phản vệ — AI không gợi ý món/nhóm thực phẩm đã khai dị ứng dù một lần hay lượng nhỏ |
| Thuốc không cần lịch nhắc / tự khai | Giúp AI tránh lời khuyên mâu thuẫn/tương tác với thuốc đang dùng (vd nước bưởi với thuốc chuyển hoá qua CYP3A4). **Độc lập hoàn toàn với "Nhắc uống thuốc"** — chỉ là thuốc tự khai không cần nhắc, không đồng bộ 2 chiều |
| Chế độ ăn đặc biệt / không dung nạp | **Khác bản chất với Dị ứng nên tách field riêng**: không dung nạp (vd lactose) là vấn đề men/tiêu hoá, thường phụ thuộc liều lượng; kiêng khem tự chọn (chay, tôn giáo) không phải vấn đề an toàn. AI nên "cân nhắc, đề xuất thay thế" ở nhóm này, không áp mức nghiêm ngặt tuyệt đối như Dị ứng |
| Ghi chú thêm về tiền sử bệnh | Catch-all cho thông tin chưa có field riêng (vd đang mang thai/cho con bú, hút thuốc/rượu bia, tiền sử gia đình — xem §3.1) |
| Liên hệ khẩn cấp | Không phục vụ AI tư vấn — phục vụ lớp an toàn cuối ([04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md](../04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md)): khi hệ thống phát hiện dấu hiệu nguy hiểm mà không liên lạc được bệnh nhân, đây là số Admin sẽ **tự gọi bằng điện thoại cá nhân** — hệ thống không tự động gọi ai |

> ⚠️ **Hạn chế thật quan trọng hơn cả mockup, phát hiện khi rà soát code**: hồ sơ bệnh lý (dị ứng, bệnh nền, thuốc đang dùng) **hiện chưa được pipeline chat AI sử dụng** — `RagQueryService` chỉ build system prompt từ tài liệu RAG truy xuất + file đính kèm, không nạp bất kỳ field nào ở màn này vào ngữ cảnh. Toàn bộ lý do nghiệp vụ ở §1 ("AI biết bệnh nhân đang dị ứng gì... câu trả lời cá nhân hoá") mô tả một khả năng **chưa tồn tại** — màn này hiện chỉ thực sự phục vụ 2 việc: xuất PDF mang đi khám (§5) và cung cấp số liên hệ khẩn cấp cho Triage. Cần xác nhận với dev liệu đây là việc còn dang dở hay đã có kế hoạch khác, trước khi đầu tư thêm field mới cho mục đích "cá nhân hoá AI" vốn chưa hoạt động.

### 3.1. Khuyến nghị bổ sung (chưa có trong code — đề xuất cho vòng sau, không bắt buộc ngay)

| Field đề xuất | Vì sao nên có |
| :--- | :--- |
| Thai kỳ / đang cho con bú | **Ưu tiên cao nhất** — nhiều thuốc/lời khuyên tiêu hoá chống chỉ định thai kỳ (vd một số phác đồ diệt H. pylori); thai kỳ cũng gây GERD/táo bón sinh lý cần phân biệt với bệnh lý. Thiếu field này, AI có thể đưa lời khuyên thuốc không an toàn mà không biết |
| Mức độ nặng / giai đoạn bệnh nền (vd đang lui bệnh hay đang bùng phát) | "Bệnh nền đang mắc" hiện chỉ là tag tên bệnh, không có trạng thái hoạt động — "viêm loét đại tràng lui bệnh" (nên ăn đủ xơ) và "đang bùng phát" (nên hạn chế xơ không tan) cần lời khuyên ngược nhau |
| Hút thuốc / rượu bia | Rượu làm nặng viêm/loét dạ dày, viêm tuỵ, tương tác xấu với NSAID (tăng nguy cơ xuất huyết tiêu hoá); thuốc lá cản trở lành loét, liên quan tiến triển Crohn |
| Tiền sử gia đình (ung thư đại trực tràng, polyp) | Ảnh hưởng tuổi/tần suất nên tầm soát được khuyến nghị |

> Các field trên chưa có trong code — nêu như khuyến nghị nghiệp vụ dựa trên chuyên môn tiêu hoá, không phải lỗi cần sửa gấp. Nên xác nhận lại pipeline AI đã thực sự đọc dữ liệu hồ sơ bệnh lý khi trả lời chat hay chưa (xem cảnh báo ngay trên) trước khi đầu tư thêm field mới, kẻo thêm dữ liệu mà AI không dùng tới.

## 4. Hành vi cần có

| Tình huống | Hành vi đúng | Vì sao |
| :--- | :--- | :--- |
| Chưa khai báo gì, vào màn lần đầu | Hiện trạng thái rỗng thân thiện, không phải bảng trống trơn khó hiểu | Tránh cảm giác "màn hình bị lỗi" |
| Thêm/sửa/xoá 1 mục trong danh sách (bệnh lý/dị ứng/thuốc) | Lưu ngay, không cần vào 1 màn "chỉnh sửa" riêng biệt | Đây là những danh sách ngắn, tách thành luồng Tạo/Sửa riêng chỉ làm chậm thao tác |
| Khai báo lần đầu và cập nhật sau này | Cùng 1 màn hình, không tách 2 trải nghiệm khác nhau | Với người dùng, "khai báo" và "cập nhật" là cùng một việc nhìn từ xa — tách biệt chỉ tạo thêm khái niệm phải học |

## 5. Xuất hồ sơ sức khoẻ ra PDF (`UC0019`)

Nút "Xuất PDF" đặt ngay trên màn hồ sơ — vì đây là nơi tổng hợp đầy đủ nhất. Bản PDF nên gộp cả hồ sơ bệnh lý, một khoảng thời gian nhật ký ăn uống/Bristol gần đây, để bệnh nhân **mang thẳng tới phòng khám** khi gặp bác sĩ — đây là giá trị thực tế lớn: bác sĩ khám 5-10 phút không có thời gian hỏi lại cả quá trình, một bản tóm tắt sẵn giúp buổi khám hiệu quả hơn hẳn. Nên cho chọn khoảng thời gian muốn xuất (vd 30 ngày gần nhất) thay vì luôn xuất toàn bộ lịch sử — tránh tài liệu quá dài không ai đọc hết tại phòng khám.

## 6. Tham chiếu

- Dữ liệu ở đây **dự kiến** là nền tảng cho [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md) — nhưng như cảnh báo ở §3, pipeline chat AI hiện chưa thực sự đọc field nào ở màn này. `SCR-PT-10-NhayCamThucPham.md` (tương quan món ăn/triệu chứng) cũng KHÔNG dùng dữ liệu hồ sơ bệnh lý — tính năng đó chỉ dựa trên nhật ký ăn uống + nhật ký Bristol (`FoodSymptomCorrelationService`), không liên quan tới màn này; đã bỏ link cũ vì không chính xác.
- Liên hệ khẩn cấp dùng bởi: [04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md](../04-QuanTri-AdminCMS/SCR-AD-13-GiamSatCanhBao.md)
- Xuất PDF dùng bởi: `HealthReportService.java` (đã verify khớp §5)
