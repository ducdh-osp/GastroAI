# SCR-AD-04 — Kho tài liệu y khoa

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0048` (tải lên), `UC0049` (thay thế tài liệu), `UC0050` (xoá), `UC0055` (tìm kiếm), `UC0061` (theo dõi trạng thái xử lý) |
| Actor | Admin (tải lên, xử lý kỹ thuật), Biên tập chuyên môn y khoa (xác nhận nội dung trước khi AI dùng — xem §3.1b) |
| Mục tiêu nghiệp vụ | Đây là nơi "nạp kiến thức" cho AI — chất lượng và độ mới của tài liệu ở đây quyết định trực tiếp chất lượng câu trả lời AI đưa ra cho bệnh nhân |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Vì sao màn hình này gộp 5 UC — và vì sao điều đó đúng

Người quản lý tài liệu làm việc với **cùng 1 danh sách tài liệu**, chỉ khác hành động: tải mới, thay bằng bản mới, xoá, tìm lại, xem đang xử lý tới đâu. Tách thành 5 màn riêng sẽ buộc họ liên tục nhảy qua lại cho cùng 1 công việc duy nhất ("quản lý kho tài liệu"). Đây là ví dụ rõ nhất trong toàn bộ tài liệu này về nguyên tắc "cùng 1 màn hình nếu người dùng thực sự trải nghiệm chúng như 1 việc".

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Kho tài liệu y khoa    [🔍 Tìm theo tên/nguồn] [+ Tải lên]   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Tên tài liệu         │ Trạng thái xử lý │ Cập nhật │ Thao tác│ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ Hướng dẫn điều trị     │ ✓ Đang dùng cho AI │ 10/11    │ [···]  │ │
│ │ viêm dạ dày mạn tính   │                    │          │        │ │
│ │ Phác đồ H. pylori      │ 🔎 Chờ xác nhận nội │ 14/11    │ [···]  │ │
│ │                        │   dung (đã xử lý    │          │        │ │
│ │                        │   kỹ thuật xong)     │          │        │ │
│ │ Cẩm nang IBS 2024       │ ⚠ Lỗi xử lý        │ 13/11    │ [···]  │ │
│ └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘

— Menu [···] mở ra —
┌─────────────────────────┐
│ Tải bản thay thế          │
│ Xoá tài liệu               │
└─────────────────────────┘
```

Không còn "lịch sử phiên bản" — mỗi chủ đề tài liệu chỉ có đúng 1 bản ghi hiển thị, đại diện cho nội dung đang dùng (hoặc sắp dùng) cho AI. Tải bản thay thế là hành động duy nhất để cập nhật nội dung, không tạo thêm dòng mới trong danh sách.

## 3. Nguyên tắc cho từng nhóm thao tác

### 3.1 Tải lên & xử lý (`UC0048`, `UC0061`)

| Nguyên tắc | Vì sao |
| :--- | :--- |
| Luôn hiện rõ tài liệu đang ở bước nào (đang trích xuất / đang chia đoạn / đang tạo vector / hoàn tất / lỗi) | Quy trình biến 1 file thành dữ liệu AI dùng được mất thời gian — không hiện trạng thái rõ ràng khiến người tải lên không biết "xong chưa, có cần làm gì thêm không", dễ tải trùng lặp vì tưởng bị lỗi |
| Khi xử lý lỗi, nêu rõ lý do có thể hiểu được (không phải mã lỗi kỹ thuật) | Không phải ai dùng màn này cũng có nền tảng kỹ thuật sâu — "Không đọc được nội dung file, có thể do file ảnh scan chất lượng thấp" hữu ích hơn nhiều so với mã lỗi hệ thống |
| Tài liệu đang xử lý **chưa** được AI sử dụng để trả lời bệnh nhân | Tránh tình trạng AI trích dẫn một tài liệu còn dang dở, chưa xử lý xong, có thể thiếu/sai nội dung |

### 3.1b Xác nhận nội dung trước khi kích hoạt cho AI — bước bắt buộc, không phải tuỳ chọn

Xử lý kỹ thuật xong (trích xuất, chia đoạn, tạo vector) **không có nghĩa là nội dung đã đúng**. Một người chỉ kiểm tra kỹ thuật (file đọc được, định dạng đúng) không đủ khả năng phát hiện 1 tài liệu y khoa đã lỗi thời hay mâu thuẫn với tài liệu khác — ví dụ thực tế: một phác đồ diệt H. pylori bản cũ (3 thuốc chuẩn) có thể đã lỗi thời so với khuyến cáo mới hơn (ưu tiên phác đồ 4 thuốc ở vùng có tỷ lệ kháng kháng sinh cao) — sai sót này chỉ lộ ra *sau khi* AI đã trích dẫn sai cho bệnh nhân, quá muộn để phòng ngừa.

Vì vậy, sau khi xử lý kỹ thuật xong, tài liệu vào trạng thái **"Chờ xác nhận nội dung"** — **Biên tập chuyên môn y khoa** đối chiếu với nguồn y khoa đã công bố (phác đồ, hướng dẫn điều trị chính thức) rồi mới bấm "Xác nhận dùng cho AI". Chỉ sau bước này tài liệu mới chuyển "Đang dùng cho AI". Đây không cần một bác sĩ hành nghề thật — nhưng bắt buộc là một bước đối chiếu nguồn có ý thức, không phải "xử lý xong kỹ thuật là coi như xong".

Nếu đây là bản **thay thế** cho 1 tài liệu đã có (không phải tài liệu hoàn toàn mới), thao tác "Xác nhận dùng cho AI" còn làm thêm 1 việc: xoá vĩnh viễn bản cũ (xem nguyên tắc ở §3.2) — nên trước khi bấm, Biên tập cần chắc chắn bản mới đã đúng, vì sau bước này không còn đường lùi.

### 3.2 Thay thế tài liệu (`UC0049`)

Có chủ ý **không làm theo kiểu "lưu lịch sử phiên bản"** (kiểu Google Docs — giữ lại mọi bản v1, v2, v3...). Mỗi chủ đề tài liệu chỉ có đúng 1 nội dung "thật" tại 1 thời điểm — tải bản mới là **thay thế**, không phải cộng thêm.

| Nguyên tắc | Vì sao |
| :--- | :--- |
| Tải bản mới cho tài liệu đã có = thay thế, không giữ song song nhiều bản | Đơn giản hoá cho người quản lý: không cần nhớ/phân biệt nhiều bản của cùng 1 chủ đề, thao tác luôn là "sửa cái hiện tại" |
| Trong lúc bản mới đang xử lý/chờ xác nhận nội dung, bản cũ **vẫn tiếp tục được AI dùng bình thường** | AI không được phép mất kiến thức về chủ đề đó dù chỉ tạm thời — tránh khoảng trống dữ liệu trong lúc chờ xử lý bản mới (có thể mất vài phút tới vài giờ) |
| Bản cũ chỉ bị xoá đúng thời điểm Biên tập chuyên môn y khoa bấm "Xác nhận dùng cho AI" cho bản mới (xem §3.1b) | Đây là thời điểm duy nhất hệ thống chắc chắn bản mới đã sẵn sàng thay thế — xoá sớm hơn (ngay lúc tải lên) có thể xoá nhầm trong khi bản mới còn lỗi hoặc chưa xử lý xong |
| Xoá bản cũ nghĩa là xoá **vĩnh viễn** toàn bộ document + chunk + embedding liên quan, không chỉ ẩn/đổi trạng thái | Không giữ lịch sử phiên bản nên phải dọn sạch — tránh chunk/embedding "mồ côi" còn sót trong kho vector, có thể vô tình vẫn bị RAG truy xuất trúng |
| Không có chức năng khôi phục bản cũ sau khi đã thay thế | Đánh đổi thật của việc bỏ lịch sử phiên bản: thao tác đơn giản hơn, nhưng mất khả năng rollback — màn hình cần cảnh báo rõ đây là hành động không thể hoàn tác trước khi Biên tập xác nhận |

### 3.3 Xoá tài liệu (`UC0050`)

| Nguyên tắc | Vì sao |
| :--- | :--- |
| Cảnh báo rõ hậu quả trước khi xoá (AI sẽ không còn trích dẫn được tài liệu này) **và nêu rõ không thể khôi phục** | Đây là thao tác ảnh hưởng trực tiếp chất lượng trả lời AI cho mọi bệnh nhân sau đó. Vì không còn giữ lịch sử phiên bản (§3.2), xoá ở đây là vĩnh viễn — cảnh báo cần mạnh hơn 1 dialog Có/Không thông thường (vd bắt gõ lại tên tài liệu để xác nhận) |

### 3.4 Tìm kiếm (`UC0055`)

| Nguyên tắc | Vì sao |
| :--- | :--- |
| Tìm theo tên, nguồn, thời điểm cập nhật | Khi kho tài liệu lớn dần theo thời gian, cuộn tay tìm sẽ không còn khả thi — đây là tính năng "chịu tải tốt khi dữ liệu lớn", không chỉ hữu ích ngay từ đầu |

## 4. Tham chiếu

- Cơ chế xử lý tài liệu (số hoá, chia đoạn, tạo vector): `03-AIChatbot-RAG/00-TongQuan.md` §2.4
- Mọi thao tác ở màn này đều ghi vào: [SCR-AD-06-NhatKyHoatDong.md](SCR-AD-06-NhatKyHoatDong.md)
