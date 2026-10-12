CREATE TABLE knowledge_articles (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    category VARCHAR(80) NOT NULL,
    reading_minutes INTEGER NOT NULL CHECK (reading_minutes BETWEEN 1 AND 30),
    content TEXT NOT NULL,
    source_name VARCHAR(200) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_checked_at DATE NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_articles_published_order
    ON knowledge_articles(published, sort_order, title);

INSERT INTO knowledge_articles
    (slug, title, summary, category, reading_minutes, content, source_name, source_url, source_checked_at, sort_order)
VALUES
('trao-nguoc-da-day-thuc-quan', 'Trào ngược dạ dày thực quản: hiểu đúng về triệu chứng',
 'Ợ nóng và trào ngược có thể gặp ở nhiều người; GERD được cân nhắc khi triệu chứng lặp lại gây khó chịu hoặc dẫn đến biến chứng.',
 'Trào ngược dạ dày', 4,
 $article$## Trào ngược là gì?

Trào ngược dạ dày thực quản (GER) xảy ra khi dịch hoặc thức ăn trong dạ dày đi ngược lên thực quản. GERD là tình trạng trào ngược kéo dài, tái diễn gây triệu chứng đáng kể hoặc biến chứng. Chỉ có triệu chứng giống trào ngược chưa đủ để tự kết luận mình mắc GERD.

## Dấu hiệu thường gặp

Ợ nóng (cảm giác nóng rát sau xương ức) và ợ/trớ dịch lên họng hoặc miệng là những biểu hiện thường gặp. Một số người có thể buồn nôn, khó nuốt, đau ngực, ho kéo dài hoặc khàn tiếng; biểu hiện có thể khác nhau giữa từng người.

## Đánh giá và chăm sóc

Bác sĩ thường bắt đầu bằng việc hỏi triệu chứng và tiền sử. Nếu có dấu hiệu biến chứng, nghi ngờ nguyên nhân khác hoặc triệu chứng không cải thiện, bác sĩ có thể chỉ định kiểm tra thêm. Một số thay đổi lối sống có thể giúp giảm triệu chứng, nhưng yếu tố khởi phát khác nhau ở mỗi người. Ghi lại thời điểm ăn, nằm và lúc xuất hiện triệu chứng có thể giúp trao đổi với nhân viên y tế.

Không tự dùng thuốc kéo dài hoặc thay đổi thuốc đang được kê. Hãy trao đổi với bác sĩ/dược sĩ về lựa chọn phù hợp với tình trạng và các thuốc khác bạn đang dùng.

## Khi nào cần đi khám sớm?

Đau ngực, khó nuốt hoặc nuốt đau, nôn kéo dài, nôn ra máu/dịch giống bã cà phê, phân đen như hắc ín hoặc sụt cân không rõ nguyên nhân cần được nhân viên y tế đánh giá. Đau ngực dữ dội hoặc kèm khó thở cần gọi cấp cứu.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/acid-reflux-ger-gerd-adults', DATE '2026-10-08', 10),
('hoi-chung-ruot-kich-thich', 'Hội chứng ruột kích thích (IBS)',
 'IBS là nhóm triệu chứng tái diễn liên quan đau bụng và thay đổi thói quen đại tiện; bác sĩ đánh giá theo triệu chứng và loại trừ nguyên nhân khác khi cần.',
 'Đại tràng và ruột', 4,
 $article$## IBS là gì?

Hội chứng ruột kích thích (IBS) là nhóm triệu chứng thường gồm đau bụng tái diễn cùng thay đổi số lần hoặc dạng phân. Người bệnh có thể thiên về tiêu chảy (IBS-D), táo bón (IBS-C), hoặc xen kẽ cả hai (IBS-M). IBS không đồng nghĩa với tổn thương nhìn thấy được ở đường tiêu hoá.

## Triệu chứng và đánh giá

Đau bụng có thể liên quan đến việc đi tiêu; đầy hơi và thay đổi đại tiện cũng thường được ghi nhận. Nhiều tình trạng khác có triệu chứng tương tự, vì vậy không thể xác định IBS chỉ bằng một biểu hiện hoặc tự chẩn đoán. Bác sĩ sẽ xem xét kiểu triệu chứng, tiền sử và khám; đôi khi cần xét nghiệm để tìm hoặc loại trừ bệnh khác.

## Quản lý triệu chứng

Hướng xử trí được cá nhân hoá. Bác sĩ có thể trao đổi về chế độ ăn, sinh hoạt, thuốc hoặc các hỗ trợ khác. Thực phẩm ảnh hưởng đến mỗi người không giống nhau; nhật ký ăn uống và triệu chứng có thể hữu ích khi thảo luận. Không nên tự loại bỏ nhiều nhóm thực phẩm hoặc tự dùng thuốc kéo dài khi chưa được tư vấn.

## Khi nào cần gặp nhân viên y tế?

Triệu chứng mới xuất hiện, kéo dài, ảnh hưởng sinh hoạt hoặc thay đổi rõ rệt nên được đánh giá. Đi ngoài ra máu, phân đen, đau dữ dội, nôn kéo dài hoặc sụt cân không chủ ý cần được kiểm tra sớm vì có thể liên quan nguyên nhân khác.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/irritable-bowel-syndrome', DATE '2026-10-08', 20),
('viem-da-day-va-benh-ly-niem-mac', 'Viêm dạ dày và bệnh lý niêm mạc dạ dày',
 'Viêm dạ dày và bệnh lý niêm mạc đều ảnh hưởng lớp lót dạ dày nhưng không hoàn toàn giống nhau; nguyên nhân quyết định cách đánh giá và xử trí.',
 'Dạ dày', 4,
 $article$## Hai thuật ngữ này có nghĩa gì?

Viêm dạ dày là tình trạng lớp niêm mạc dạ dày bị viêm. Gastropathy (bệnh lý niêm mạc dạ dày) mô tả tổn thương lớp niêm mạc nhưng có ít hoặc không có viêm. Hai tình trạng có thể không gây triệu chứng; nếu có, triệu chứng có thể giống khó tiêu. Không thể phân biệt chúng chỉ dựa vào cảm giác đau bụng.

## Nguyên nhân và cách đánh giá

Nhiễm vi khuẩn *Helicobacter pylori* là một nguyên nhân thường gặp của viêm dạ dày. Một số thuốc, trong đó có thuốc kháng viêm không steroid, cũng có thể liên quan tổn thương niêm mạc. Bác sĩ sẽ xem xét thuốc đang dùng, bệnh sử và triệu chứng; tuỳ trường hợp có thể cần xét nghiệm máu, phân, hơi thở hoặc nội soi và sinh thiết.

## Điều trị

Điều trị phụ thuộc vào loại tổn thương và nguyên nhân. Nếu phát hiện *H. pylori*, bác sĩ sẽ lựa chọn phác đồ phù hợp; không tự dùng kháng sinh hoặc ngừng thuốc kê toa. Hãy cung cấp cho bác sĩ danh sách thuốc, thực phẩm bổ sung và tiền sử dị ứng.

## Dấu hiệu cần chú ý

Nôn ra máu hoặc chất giống bã cà phê, phân đen như hắc ín, choáng/ngất hay đau bụng dữ dội cần được chăm sóc y tế khẩn cấp.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/gastritis-gastropathy', DATE '2026-10-08', 30),
('loet-da-day-ta-trang', 'Loét dạ dày-tá tràng',
 'Ổ loét là vết tổn thương ở niêm mạc dạ dày hoặc tá tràng; hai nguyên nhân phổ biến là nhiễm H. pylori và thuốc kháng viêm không steroid.',
 'Dạ dày', 4,
 $article$## Loét dạ dày-tá tràng là gì?

Đây là vết loét ở lớp lót dạ dày hoặc tá tràng. Một số người có đau/khó chịu vùng bụng, nhanh no, buồn nôn, đầy bụng hoặc ợ hơi; cũng có người ít triệu chứng. Đau bụng đơn thuần không xác nhận có ổ loét.

## Nguyên nhân và chẩn đoán

Hai nguyên nhân phổ biến là nhiễm *H. pylori* và sử dụng thuốc kháng viêm không steroid (NSAID), chẳng hạn ibuprofen hoặc naproxen. Bác sĩ có thể hỏi bệnh sử, rà soát thuốc và chỉ định xét nghiệm tìm *H. pylori*, nội soi đường tiêu hoá trên hoặc phương pháp khác để xác định nguyên nhân.

## Điều trị

Điều trị thường nhằm làm lành ổ loét và xử lý nguyên nhân. Nếu liên quan *H. pylori*, cần dùng đúng phác đồ do bác sĩ kê và hoàn tất theo hướng dẫn. Không tự ngừng aspirin hoặc thuốc kê toa; hãy hỏi người kê đơn để cân nhắc lợi ích và nguy cơ. Không có một chế độ ăn đặc biệt được khuyến nghị chung để chữa hoặc phòng loét.

## Biến chứng cần cấp cứu

Nôn ra máu, chất nôn giống bã cà phê, phân đen/đỏ, choáng hoặc đau bụng đột ngột dữ dội có thể là dấu hiệu chảy máu hay thủng ổ loét. Hãy đến cơ sở cấp cứu ngay.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/peptic-ulcers-stomach-ulcers', DATE '2026-10-08', 40),
('benh-celiac', 'Bệnh Celiac và xét nghiệm chẩn đoán',
 'Celiac là bệnh miễn dịch do gluten kích hoạt, có thể làm tổn thương ruột non; cần xét nghiệm y tế để chẩn đoán thay vì tự loại gluten trước đó.',
 'Dinh dưỡng và hấp thu', 4,
 $article$## Celiac là bệnh gì?

Bệnh Celiac là bệnh tiêu hoá và miễn dịch mạn tính: gluten trong lúa mì, lúa mạch và lúa mạch đen kích hoạt phản ứng miễn dịch làm tổn thương ruột non. Bệnh khác với dị ứng lúa mì hoặc tình trạng nhạy cảm gluten không do Celiac. Một số người có triệu chứng tiêu hoá, triệu chứng ngoài đường ruột hoặc ít biểu hiện.

## Chẩn đoán

Triệu chứng không đủ để kết luận Celiac vì có thể giống IBS hoặc không dung nạp lactose. Bác sĩ xem xét bệnh sử, khám và thường dùng xét nghiệm máu; một số trường hợp cần sinh thiết ruột non hoặc kiểm tra khác.

**Không tự bắt đầu chế độ ăn không gluten trước khi được đánh giá.** Việc loại gluten trước xét nghiệm có thể làm sai lệch kết quả. Nếu đang ăn không gluten, hãy nói với bác sĩ trước khi làm xét nghiệm.

## Sống chung với bệnh

Khi đã được xác nhận chẩn đoán, điều trị thường gồm chế độ ăn không gluten lâu dài và theo dõi dinh dưỡng. Bác sĩ hoặc chuyên gia dinh dưỡng có thể hướng dẫn đọc nhãn và lựa chọn thực phẩm đủ chất, phù hợp với nhu cầu cá nhân.

## Khi nào nên trao đổi với bác sĩ?

Hãy hỏi nhân viên y tế nếu có triệu chứng kéo dài, thiếu máu hoặc sụt cân không rõ nguyên nhân, người thân ruột thịt mắc Celiac, hoặc nghi ngờ phản ứng với gluten. Không tự chẩn đoán dựa trên việc thấy đỡ hơn khi thay đổi chế độ ăn.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/celiac-disease/diagnosis', DATE '2026-10-08', 50),
('tieu-chay-va-phong-mat-nuoc', 'Tiêu chảy: bù dịch và nhận biết dấu hiệu nguy hiểm',
 'Tiêu chảy thường là phân lỏng nhiều lần; ưu tiên bù dịch phù hợp và theo dõi các dấu hiệu mất nước, chảy máu hoặc diễn tiến nặng.',
 'Đại tràng và ruột', 4,
 $article$## Tiêu chảy là gì?

Tiêu chảy thường được mô tả là phân lỏng/nước từ ba lần mỗi ngày trở lên, hoặc nhiều hơn mức bình thường của chính bạn. Có thể kèm mót đi tiêu, đau quặn bụng, buồn nôn hoặc mất kiểm soát đại tiện. Nhiễm virus, ngộ độc thực phẩm, tác dụng phụ của thuốc và một số bệnh mạn tính là những nguyên nhân có thể gặp.

## Chăm sóc và theo dõi

Tiêu chảy làm mất nước và chất điện giải. Bù lại lượng dịch là quan trọng; dung dịch bù nước đường uống có thể phù hợp trong một số tình huống. Trẻ nhỏ, người cao tuổi, người suy giảm miễn dịch hoặc có bệnh nền nên hỏi nhân viên y tế về cách bù dịch. Không tự cho trẻ dùng thuốc cầm tiêu chảy.

Ghi lại số lần đi tiêu, thời gian mắc, thức ăn/đồ uống và thuốc đang dùng để cung cấp cho bác sĩ nếu cần. Không tự dùng kháng sinh; thuốc điều trị phụ thuộc nguyên nhân.

## Khi nào cần liên hệ bác sĩ ngay?

Lú lẫn hoặc li bì, nôn nhiều, đau bụng/trực tràng dữ dội, dấu hiệu mất nước, phân có máu hoặc đen cần được đánh giá sớm. Người lớn cũng nên liên hệ bác sĩ nếu tiêu chảy kéo dài hơn hai ngày, sốt cao hoặc đi phân lỏng từ sáu lần/ngày. Trẻ sơ sinh và trẻ nhỏ có nguy cơ mất nước nhanh hơn.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/diarrhea/symptoms-causes', DATE '2026-10-08', 60),
('tao-bon', 'Táo bón: thói quen đại tiện và khi cần thăm khám',
 'Táo bón có thể biểu hiện bằng đi tiêu ít, phân khô/cứng hoặc khó đi; tần suất bình thường khác nhau giữa mỗi người.',
 'Đại tràng và ruột', 4,
 $article$## Nhận biết táo bón

Táo bón có thể gồm đi tiêu ít hơn ba lần một tuần, phân khô/cứng/vón cục, phải rặn hoặc cảm giác đi chưa hết. Thói quen đại tiện khác nhau giữa mọi người; thay đổi so với mức bình thường của bạn cũng đáng lưu ý.

## Những việc có thể hỗ trợ

Chất xơ trong thức ăn, đủ nước, hoạt động thể lực đều đặn và dành thời gian đi vệ sinh khi có nhu cầu có thể hỗ trợ một số người. Nếu tăng chất xơ, nên tăng từ từ và uống đủ dịch phù hợp với tình trạng sức khoẻ. Một số bệnh hoặc thuốc cũng có thể góp phần gây táo bón.

Không tự ngừng thuốc kê toa. Thuốc nhuận tràng không phù hợp với tất cả mọi người; hỏi bác sĩ hoặc dược sĩ trước khi dùng, đặc biệt khi triệu chứng kéo dài hoặc bạn có bệnh nền.

## Khi nào nên đi khám?

Hãy trao đổi với bác sĩ khi táo bón không cải thiện với tự chăm sóc hoặc kéo dài. Bác sĩ có thể hỏi về thuốc, chế độ ăn và thói quen đại tiện, khám và quyết định có cần xét nghiệm để tìm nguyên nhân hay không. Cần được đánh giá ngay nếu táo bón kèm chảy máu trực tràng hoặc máu trong phân, đau bụng liên tục, không trung tiện được, nôn, sốt, đau lưng dưới hoặc sụt cân không chủ ý.

*Bài viết cung cấp thông tin phổ thông, không thay thế chẩn đoán hay tư vấn cá nhân.*$article$,
 'NIDDK (National Institute of Diabetes and Digestive and Kidney Diseases, NIH)',
 'https://www.niddk.nih.gov/health-information/digestive-diseases/constipation/symptoms-causes', DATE '2026-10-08', 70);
