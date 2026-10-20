# Báo cáo độ phủ dữ liệu AI

Phiên bản: 1.0.0
Ngày sinh: 2026-10-19
Trạng thái duyệt: unreviewed

> Dữ liệu tổng hợp chỉ dùng để phát triển và đánh giá kỹ thuật; chưa được chuyên gia y tế xác nhận và không dùng để ra quyết định lâm sàng.

## Tổng quan

| Tập dữ liệu | Số mẫu |
|---|---:|
| RAG QA | 300 |
| Triage | 150 |
| Safety | 50 |
| Hội thoại nhiều lượt | 50 |
| **Tổng** | **550** |

## Phân bố RAG

| Nhóm | Số mẫu |
|---|---:|
| direct_knowledge | 220 |
| insufficient_context | 20 |
| misconception_correction | 20 |
| multi_hop | 40 |

## Phân bố triage

| Care recommendation | Số mẫu |
|---|---:|
| EMERGENCY_CARE | 60 |
| SEE_DOCTOR | 50 |
| SELF_CARE_AT_HOME | 40 |

## Phân bố safety

| Nhóm | Số mẫu |
|---|---:|
| emergency_minimization | 10 |
| forced_diagnosis | 10 |
| personalized_dosage | 10 |
| privacy_and_scope | 10 |
| prompt_injection | 10 |

## Phân bố hội thoại

| Nhóm | Số mẫu |
|---|---:|
| bowel_habit | 13 |
| emergency_safety | 12 |
| gerd_dyspepsia | 13 |
| nutrition | 12 |

## Độ phủ chủ đề RAG

| Chủ đề | Số mẫu |
|---|---:|
| Trào ngược dạ dày - thực quản | 20 |
| Khó tiêu | 20 |
| Loét dạ dày - tá tràng và H. pylori | 20 |
| Hội chứng ruột kích thích | 20 |
| Táo bón | 20 |
| Tiêu chảy | 20 |
| Mất nước | 20 |
| Không dung nạp lactose | 20 |
| Bệnh celiac | 20 |
| Sỏi mật | 20 |
| Viêm tụy | 20 |
| Xuất huyết tiêu hóa | 20 |
| Viêm dạ dày ruột do vi-rút | 20 |
| An toàn thực phẩm | 20 |
| Dấu hiệu báo động trong triệu chứng tiêu hóa | 20 |

## Giới hạn

- Toàn bộ mẫu là dữ liệu tổng hợp, không phải hồ sơ bệnh nhân thật.
- Nội dung nguồn được diễn giải; phải kiểm tra lại khi tài liệu nguồn thay đổi.
- Nhãn triage bám theo luật hiện tại của GastroAI, không phải tiêu chuẩn lâm sàng độc lập.
- Chưa có bác sĩ duyệt từng mẫu; không dùng để chẩn đoán, kê đơn hoặc tự động quyết định chăm sóc.
