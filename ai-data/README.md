# Bộ dữ liệu AI tiếng Việt cho GastroAI

Bộ pilot này phục vụ hai mục đích: tạo corpus tiếng Việt để nạp vào RAG và tạo dữ liệu đánh giá tự động cho luồng hỏi đáp, triage và an toàn.

> Toàn bộ dữ liệu là dữ liệu tổng hợp, có trạng thái `unreviewed`, chưa được chuyên gia y tế xác nhận và không dùng để ra quyết định lâm sàng.

## Thành phần

| Thành phần | Quy mô | Mục đích |
|---|---:|---|
| `corpus/*.md` | 15 tài liệu | Nguồn kiến thức đã diễn giải để đưa vào RAG |
| `datasets/rag-evaluation.jsonl` | 300 mẫu | Kiểm tra truy xuất, dẫn nguồn, tổng hợp và từ chối khi thiếu dữ kiện |
| `datasets/triage-evaluation.jsonl` | 150 mẫu | Kiểm tra đúng enum và luật `TriageService` hiện tại |
| `datasets/safety-evaluation.jsonl` | 50 mẫu | Kiểm tra kê đơn/liều, ép chẩn đoán, cấp cứu, prompt injection và riêng tư |
| `datasets/multi-turn-conversations.jsonl` | 50 mẫu | Kiểm tra hội thoại nhiều lượt và giữ ngữ cảnh |

Phân bố triage: 60 `EMERGENCY_CARE`, 50 `SEE_DOCTOR`, 40 `SELF_CARE_AT_HOME`. Hai dấu hiệu `UNEXPLAINED_WEIGHT_LOSS` và `DIFFICULT_OR_PAINFUL_SWALLOWING` được gắn `SEE_DOCTOR`, không tự động gắn cấp cứu khi đứng riêng lẻ.

## Sinh lại và kiểm định

Yêu cầu Python 3.10 trở lên; không cần cài thư viện ngoài.

    python ai-data/scripts/generate_dataset.py
    python ai-data/scripts/validate_dataset.py

Generator có tính xác định: chạy lại từ cùng phiên bản mã sẽ tạo cùng nội dung. `manifest.json` lưu số lượng và SHA-256 của bốn file JSONL. Validator kiểm tra UTF-8, mojibake, số lượng, ID trùng, nội dung trùng, PII cơ bản, nguồn, enum, split và phân bố nhãn.

## Chia lại development/test

Không sửa file gốc; ghi kết quả vào thư mục mới:

    python ai-data/scripts/split_dataset.py ai-data/datasets/rag-evaluation.jsonl --output-dir ai-data/splits

Có thể truyền nhiều file JSONL cùng lúc và đổi tỷ lệ bằng `--development-ratio`.

## Nạp corpus vào RAG

Backend hiện chỉ nhận PDF/DOCX. Script import đóng gói từng file Markdown thành DOCX OpenXML tối giản rồi gọi API upload. Mặc định script chỉ dry-run:

    python ai-data/scripts/import_rag_corpus.py --base-url http://localhost:8080

Để upload thật bằng phiên đăng nhập CMS:

    python ai-data/scripts/import_rag_corpus.py --base-url http://localhost:8080 --cookie "JSESSIONID=..." --execute

Nếu môi trường dùng JWT có thể thay bằng `--bearer-token`. Có thể thêm `--keep-docx ai-data/exported-docx` để giữ bản DOCX mang sang máy khác. Thư mục export không bắt buộc phải commit.

Sau khi upload, backend sẽ tự chạy pipeline `documents -> chunks -> embeddings`; repository không chứa API key hay vector embedding.

## Mang sang máy khác

Sao chép nguyên thư mục `ai-data`. Trên máy nhận, chạy validator trước. Nếu chỉ cần đánh giá mô hình thì dùng trực tiếp bốn file JSONL. Nếu cần nạp RAG thì khởi động backend, đăng nhập CMS bằng tài khoản có quyền upload tài liệu, lấy cookie phiên và chạy importer với `--execute`.

## Quy tắc sử dụng

- Không chứa bệnh án thật hoặc thông tin định danh thật.
- Không dùng mẫu làm đơn thuốc, hướng dẫn liều cá nhân hoặc thay thế nhân viên y tế.
- Cần bác sĩ duyệt từng mẫu trước khi dùng cho tính năng ảnh hưởng chăm sóc thực tế.
- Khi tài liệu nguồn thay đổi, phải rà soát lại corpus và expected answer liên quan.
- Xem `LICENSES.md` về nguồn bên ngoài và điều kiện tái sử dụng.
