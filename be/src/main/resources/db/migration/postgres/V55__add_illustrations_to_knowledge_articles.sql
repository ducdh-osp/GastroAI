-- V55: Thêm hình ảnh minh họa y khoa vào nội dung các bài viết kiến thức

-- Bài 1: Trào ngược dạ dày thực quản
UPDATE knowledge_articles
SET content = '![Sơ đồ cơ chế trào ngược acid dịch vị từ dạ dày lên thực quản qua cơ thắt thực quản dưới](/images/articles/gerd-mechanism.svg)

' || content
WHERE slug = 'trao-nguoc-da-day-thuc-quan' AND content NOT LIKE '%/images/articles/%';

-- Bài 2: Hội chứng ruột kích thích (IBS)
UPDATE knowledge_articles
SET content = '![Sơ đồ trục Não - Ruột (Gut-Brain Axis) và các phân nhóm hội chứng ruột kích thích](/images/articles/ibs-gut-brain.svg)

' || content
WHERE slug = 'hoi-chung-ruot-kich-thich' AND content NOT LIKE '%/images/articles/%';

-- Bài 3: Viêm dạ dày và bệnh lý niêm mạc dạ dày
UPDATE knowledge_articles
SET content = '![Cấu trúc lớp niêm mạc bảo vệ dạ dày và các yếu tố gây viêm niêm mạc](/images/articles/gastritis-mucosa.svg)

' || content
WHERE slug = 'viem-da-day-va-benh-ly-niem-mac' AND content NOT LIKE '%/images/articles/%';

-- Bài 4: Loét dạ dày-tá tràng
UPDATE knowledge_articles
SET content = '![Vị trí hình thành ổ loét niêm mạc tại dạ dày và hành tá tràng](/images/articles/peptic-ulcer.svg)

' || content
WHERE slug = 'loet-da-day-ta-trang' AND content NOT LIKE '%/images/articles/%';

-- Bài 5: Bệnh Celiac
UPDATE knowledge_articles
SET content = '![So sánh cấu trúc nhung mao ruột non bình thường và tình trạng teo nhung mao ở bệnh Celiac](/images/articles/celiac-villi.svg)

' || content
WHERE slug = 'benh-celiac' AND content NOT LIKE '%/images/articles/%';

-- Bài 6: Tiêu chảy: bù dịch và nhận biết nguy hiểm
UPDATE knowledge_articles
SET content = '![Nguyên tắc bù nước điện giải Oresol và đánh giá các mức độ mất nước](/images/articles/diarrhea-dehydration.svg)

' || content
WHERE slug = 'tieu-chay-va-phong-mat-nuoc' AND content NOT LIKE '%/images/articles/%';

-- Bài 7: Táo bón
UPDATE knowledge_articles
SET content = '![Tư thế ngồi đại tiện sinh lý 35 độ và 3 trụ cột điều trị cải thiện táo bón](/images/articles/constipation-habits.svg)

' || content
WHERE slug = 'tao-bon' AND content NOT LIKE '%/images/articles/%';

-- Bài 8: Theo dõi ợ nóng trong 30 ngày
UPDATE knowledge_articles
SET content = '![Bảng theo dõi nhật ký thời điểm và mức độ ợ nóng sau các bữa ăn](/images/articles/heartburn-tracker.svg)

## Hướng dẫn theo dõi ợ nóng

Ghi lại bữa ăn, giờ ngủ và mức độ ợ nóng giúp nhận biết xu hướng và thực phẩm kích hoạt cá nhân.
- Ghi nhận thời điểm xuất hiện triệu chứng sau ăn hoặc khi nằm ngủ.
- Đánh giá mức độ bỏng rát theo thang điểm từ 1 đến 10.
- Mang nhật ký theo dõi đến trao đổi cùng bác sĩ để tối ưu hóa phác đồ điều trị.'
WHERE slug = 'demo-30d-theo-doi-o-nong';

-- Bài 9: Sử dụng thang Bristol hằng ngày
UPDATE knowledge_articles
SET content = '![Thang phân loại dạng phân Bristol từ Type 1 đến Type 7](/images/articles/bristol-scale.svg)

## Hướng dẫn sử dụng thang Bristol

Thang phân loại Bristol giúp chuẩn hóa mô tả khuôn phân và đánh giá tốc độ lưu chuyển qua ruột:
- **Type 1 & 2**: Dấu hiệu phân khô cứng, gợi ý tình trạng táo bón.
- **Type 3 & 4**: Khuôn phân lý tưởng nhất, đại diện cho hệ tiêu hóa khỏe mạnh.
- **Type 5, 6 & 7**: Phân nát, bèo nhèo hoặc lỏng toàn nước, cảnh báo tiêu chảy.'
WHERE slug = 'demo-30d-nhat-ky-bristol';

-- Bài 10: Chia nhỏ bữa ăn
UPDATE knowledge_articles
SET content = '![Lịch phân bổ các bữa ăn nhỏ trong ngày giúp giảm áp lực cho dạ dày](/images/articles/small-frequent-meals.svg)

## Lợi ích của việc chia nhỏ bữa ăn

Chia nhỏ khẩu phần thành 5 - 6 bữa trong ngày giúp dạ dày không phải căng giãn quá mức:
- Giảm áp lực trong khoang dạ dày, hạn chế dịch acid trào ngược lên ngực.
- Dễ tiêu hóa, hấp thu dưỡng chất đều đặn và tránh hạ đường huyết xa bữa.
- Không ăn quá no trong bữa tối và không ăn sát giờ đi ngủ.'
WHERE slug = 'demo-30d-chia-nho-bua-an';

-- Bài 11: Nhận biết dấu hiệu mất nước
UPDATE knowledge_articles
SET content = '![Thang đo màu sắc nước tiểu và các dấu hiệu cảnh báo mất nước cơ thể](/images/articles/dehydration-signs.svg)

## Dấu hiệu nhận biết mất nước cơ thể

Mất nước thường xảy ra khi tiêu chảy cấp, nôn trớ hoặc sốt cao:
- **Màu nước tiểu**: Vàng nhạt trong là đủ nước; vàng sẫm hoặc màu hổ phách cảnh báo cần bù nước ngay.
- **Khô miệng & khát**: Môi nứt nẻ, nước bọt quánh, mắt trũng.
- **Dấu hiệu véo da**: Khi véo da mu bàn tay, nếp véo da trở lại vị trí ban đầu rất chậm.'
WHERE slug = 'demo-30d-nhan-biet-mat-nuoc';

-- Bài 12: Thói quen sau bữa ăn
UPDATE knowledge_articles
SET content = '![Những thói quen tốt sau bữa ăn giúp giảm trào ngược và hỗ trợ tiêu hóa](/images/articles/post-meal-habits.svg)

## Thói quen lành mạnh sau bữa ăn

Những thói quen đơn giản sau khi ăn có tác động lớn đến sức khỏe dạ dày:
- **Nên làm**: Giữ tư thế ngồi thẳng hoặc đi bộ nhẹ nhàng 15-30 phút để trọng lực hỗ trợ đưa thức ăn xuống ruột.
- **Tránh tuyệt đối**: Nằm ngay sau ăn, tập thể dục gắng sức, gập bụng hoặc hút thuốc lá, uống trà đậm.'
WHERE slug = 'demo-30d-thoi-quen-sau-an';

-- Bài 13: Theo dõi tuân thủ thuốc
UPDATE knowledge_articles
SET content = '![Thời điểm vàng sử dụng các nhóm thuốc đường tiêu hóa và lịch nhắc thuốc](/images/articles/medication-adherence.svg)

## Hướng dẫn tuân thủ thuốc tiêu hóa

Hiệu quả điều trị bệnh tiêu hóa phụ thuộc rất lớn vào việc uống thuốc đúng thời điểm:
- **Thuốc giảm tiết acid (PPI)**: Uống 30 - 60 phút trước bữa ăn sáng.
- **Thuốc kháng acid dạng sữa / gói**: Uống sau ăn 1 - 2 tiếng hoặc khi có cơn đau rát.
- **Tuân thủ liệu trình**: Không tự ý ngừng thuốc ngay khi vừa dứt triệu chứng vì niêm mạc cần thời gian tái tạo sẹo.'
WHERE slug = 'demo-30d-theo-doi-thuoc';

-- Bài 14: Giấc ngủ và triệu chứng tiêu hóa
UPDATE knowledge_articles
SET content = '![Tư thế nằm ngủ nghiêng trái nâng cao đầu giường giúp hạn chế trào ngược ban đêm](/images/articles/sleep-posture-gerd.svg)

## Giấc ngủ và trào ngược dạ dày ban đêm

Trào ngược ban đêm gây ho khan, viêm họng và làm gián đoạn giấc ngủ:
- **Nằm nghiêng bên trái**: Giữ dạ dày ở vị trí thấp hơn ngã ba thực quản, ngăn acid trôi ngược lên.
- **Nâng cao đầu giường 15 - 20 cm**: Sử dụng gối nêm chống trào ngược hoặc nâng chân đầu giường.
- **Ăn tối cách giờ ngủ ít nhất 3 tiếng**: Đảm bảo dạ dày đã làm rỗng trước khi nằm.'
WHERE slug = 'demo-30d-chat-luong-giac-ngu';

-- Bài 15: Khi nào cần đi khám sớm
UPDATE knowledge_articles
SET content = '![Các dấu hiệu cảnh báo cờ đỏ bệnh tiêu hóa cần thăm khám y tế ngay](/images/articles/red-flags-triage.svg)

## Dấu hiệu cảnh báo cờ đỏ (Red Flags)

Hãy đến ngay cơ sở y tế hoặc gọi cấp cứu nếu bạn gặp bất kỳ dấu hiệu nào dưới đây:
- **Cấp cứu khẩn cấp (115)**: Nôn ra máu, đi ngoài phân đen như bã cà phê, đau bụng dữ dội bụng cứng như gỗ.
- **Cần khám chuyên khoa sớm**: Nuốt nghẹn, sút cân nhanh không rõ nguyên nhân, sốt kéo dài kèm thiếu máu.'
WHERE slug = 'demo-30d-khi-nao-di-kham';

