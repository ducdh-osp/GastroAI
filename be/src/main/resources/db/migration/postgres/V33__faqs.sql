-- UC0062: Quản lý câu hỏi thường gặp (FAQ) trong CMS
-- UC0021: Tra cứu và xem FAQ phía bệnh nhân

CREATE TABLE faqs (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(80) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_faqs_category ON faqs (category);
CREATE INDEX idx_faqs_published ON faqs (published);
CREATE INDEX idx_faqs_sort_order ON faqs (sort_order);

-- Dữ liệu mẫu y khoa tiêu hóa chất lượng cao cho FAQ ban đầu
INSERT INTO faqs (question, answer, category, sort_order, published)
VALUES
(
    'Trào ngược dạ dày thực quản (GERD) có những dấu hiệu điển hình nào?',
    'Các dấu hiệu điển hình bao gồm:
- **Ợ nóng**: Cảm giác nóng rát sau xương ức, lan lên cổ họng, thường xuất hiện sau khi ăn hoặc khi nằm xuống.
- **Ợ chua, ợ hơi**: Cảm giác dịch vị chua hoặc đắng trào lên miệng.
- **Nuốt nghẹn, vướng họng**: Cảm giác có khối vướng ở cổ họng (cảm giác globus).
- **Khàn tiếng, ho mạn tính**: Do dịch acid gây kích ứng thanh quản, thường nặng hơn vào buổi sáng.
- **Đau tức ngực không do tim**: Cần được thăm khám để loại trừ bệnh lý tim mạch trước khi kết luận.',
    'Trào ngược dạ dày thực quản',
    10,
    true
),
(
    'Tôi nên làm gì khi bị đau dạ dày cấp tính tại nhà?',
    'Khi xuất hiện cơn đau thượng vị cấp:
1. **Nghỉ ngơi**: Nằm nghiêng sang bên trái hoặc kê cao đầu giường 15-20cm để giảm áp lực lên cơ hoành.
2. **Uống nước ấm**: Uống từng ngụm nhỏ nước ấm để làm dịu niêm mạc. Tránh nước quá nóng hoặc quá lạnh.
3. **Tránh ăn thực phẩm kích ứng**: Tuyệt đối không dùng ớt, tiêu, cà phê, rượu bia, nước ngọt có ga hoặc đồ chua.
4. **Ăn nhẹ thức ăn dễ tiêu**: Như cháo loãng, bánh mì mềm để thấm bớt acid dịch vị.
5. **Cảnh báo nguy hiểm**: Nếu có dấu hiệu sốt cao, nôn ra máu, đi ngoài phân đen sệt như bã cà phê, hoặc đau dữ dội lan ra sau lưng, cần đến ngay cơ sở y tế gần nhất.',
    'Bệnh dạ dày - tá tràng',
    20,
    true
),
(
    'Hội chứng ruột kích thích (IBS) có nguy hiểm không và làm sao để kiểm soát?',
    '**Hội chứng ruột kích thích (IBS)** là một rối loạn chức năng đường tiêu hóa lành tính, không gây tổn thương thực thể ở ruột và **không làm tăng nguy cơ ung thư đại trực tràng**.

Tuy nhiên, IBS ảnh hưởng nhiều đến chất lượng cuộc sống. Để kiểm soát triệu chứng:
- Áp dụng chế độ ăn **Low-FODMAP**: Giảm các loại đường dễ lên men (hành tây, tỏi, đậu, sữa bò, táo, dưa hấu...).
- Bổ sung chất xơ hòa tan (yến mạch, chuối, psyllium).
- Giảm stress và căng thẳng: Tập yoga, thiền, duy trì giấc ngủ đủ 7-8 tiếng/ngày.
- Ghi nhật ký ăn uống để xác định thực phẩm kích phát cơn đau hoặc tiêu chảy/táo bón.',
    'Bệnh lý đại tràng',
    30,
    true
),
(
    'Thang điểm phân Bristol dùng để làm gì trong theo dõi tiêu hóa?',
    'Thang điểm phân Bristol (Bristol Stool Form Scale) phân loại phân người thành 7 nhóm:
- **Loại 1 & 2**: Phân cục cứng hoặc lổn nhổn hạt — Dấu hiệu của **táo bón**.
- **Loại 3 & 4**: Phân mềm mượt hình xúc xích hoặc chuối — **Phân lý tưởng, tiêu hóa khỏe mạnh**.
- **Loại 5**: Từng miếng mềm với bờ viền rõ — Có xu hướng thiếu chất xơ.
- **Loại 6 & 7**: Phân lỏng, xốp hoặc toàn nước — Dấu hiệu của **tiêu chảy**.

Việc ghi nhật ký Bristol hàng ngày giúp bác sĩ đánh giá chính xác tốc độ lưu chuyển qua đại tràng và mức độ đáp ứng điều trị.',
    'Theo dõi sức khỏe',
    40,
    true
),
(
    'Khi nào người bệnh tiêu hóa cần đi khám cấp cứu ngay?',
    'Bạn cần đến ngay phòng cấp cứu nếu xuất hiện bất kỳ dấu hiệu báo động (Red Flags) nào sau đây:
- **Nôn ra máu tươi hoặc dịch màu bã cà phê**.
- **Đi ngoài phân đen nhánh, mùi tanh nồng hoặc đi ngoài ra máu tươi nhiều**.
- **Đau bụng dữ dội, liên tục, bụng cứng như gỗ**.
- **Sút cân nhanh chóng không rõ nguyên nhân** (giảm >5% trọng lượng cơ thể trong 1-3 tháng).
- **Nuốt nghẹn tăng dần** (từ nghẹn thức ăn đặc chuyển sang nghẹn cả thức ăn lỏng).
- **Sốt cao kèm vàng da, vàng mắt**.',
    'Cảnh báo nguy hiểm',
    50,
    true
);
