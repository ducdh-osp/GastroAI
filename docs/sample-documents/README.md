# Tài liệu mẫu để test UC0031/032 (số hóa tài liệu / RAG)

3 file PDF thật, nguồn công khai từ Bệnh viện Nhân Dân 115 (đăng trên benhvientanphu.vn, mục
"phác đồ điều trị"), dùng để test luồng upload → trích xuất → chia chunk → sinh embedding →
RAG trả lời có trích nguồn. Không phải tài sản của dự án, chỉ để mỗi máy dev tự nạp vào DB
local của mình (mỗi người có DB riêng, không dùng chung).

- `ibs-benhvientanphu.pdf` — Hội chứng ruột kích thích (IBS)
- `viem-da-day.pdf` — Viêm dạ dày
- `viem-dai-trang-man.pdf` — Viêm đại tràng mạn

## Cách nạp vào DB local

```bash
# 1. Đăng nhập CMS (Admin), lưu session
curl -c cookies.txt -b cookies.txt -X POST http://localhost:8080/api/v1/cms/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gastroai.vn","password":"1","role":"ADMIN"}'

# 2. Upload từng file (lặp lại cho mỗi file)
curl -c cookies.txt -b cookies.txt -X POST http://localhost:8080/api/v1/documents \
  -F "file=@docs/sample-documents/ibs-benhvientanphu.pdf"
```

Đợi vài giây rồi kiểm tra `documents.status` chuyển sang `DONE` (query DB trực tiếp, chưa có
API xem trạng thái — xem UC0061) là dùng được cho RAG.
