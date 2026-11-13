# SCR-PT-12 — Thư viện kiến thức tiêu hoá

| Thuộc tính | Giá trị |
| :--- | :--- |
| UC | `UC0026` |
| Actor | Bệnh nhân |
| Mục tiêu nghiệp vụ | Cho bệnh nhân chủ động đọc hiểu về bệnh lý tiêu hoá theo nhịp độ của riêng họ, không nhất thiết phải hỏi mới biết |
| Tài liệu tổng quan | [00-TongQuan.md](00-TongQuan.md) |

## 1. Khác biệt với Chat AI và FAQ

Có người thích hỏi trực tiếp (Chat), có người thích tra nhanh 1 câu (FAQ), và có người thích **đọc chủ động để tự tìm hiểu** trước khi cần hỏi gì — đặc biệt với bệnh mạn tính, nhiều bệnh nhân muốn hiểu sâu về tình trạng của mình theo thời gian, không chỉ xử lý từng câu hỏi rời rạc. Thư viện kiến thức phục vụ đúng nhóm nhu cầu này: bài viết dài hơn FAQ, có chiều sâu hơn, được tổ chức theo chủ đề để duyệt dần.

## 2. Thiết kế màn hình

```text
┌───────────────────────────────────────────────────────────┐
│  Thư viện kiến thức tiêu hoá        [ 🔍 Tìm bài viết ]        │
│                                                                 │
│  Chủ đề:  [Tất cả] [Dạ dày] [Đại tràng] [Chế độ ăn] [Thuốc]    │
│                                                                 │
│  ┌───────────────────┐  ┌───────────────────┐                │
│  │ [ảnh minh hoạ]       │  │ [ảnh minh hoạ]       │                │
│  │ Hiểu đúng về trào     │  │ Chế độ ăn low-FODMAP  │                │
│  │ ngược dạ dày-thực     │  │ là gì, có phù hợp     │                │
│  │ quản                  │  │ với bạn?               │                │
│  │ 5 phút đọc            │  │ 7 phút đọc             │                │
│  └───────────────────┘  └───────────────────┘                │
└───────────────────────────────────────────────────────────┘

— Trang chi tiết bài viết —
┌───────────────────────────────────────────────────────────┐
│  ← Quay lại                                                     │
│  Hiểu đúng về trào ngược dạ dày-thực quản                       │
│  Chủ đề: Dạ dày · Cập nhật 10/11/2026                           │
│  [ảnh minh hoạ lớn]                                              │
│  Nội dung bài viết...                                           │
│                                                                 │
│  Vẫn còn thắc mắc riêng về trường hợp của bạn?                 │
│       [   Hỏi AI về chủ đề này   ]                             │
└───────────────────────────────────────────────────────────┘
```

## 3. Nguyên tắc

| Yếu tố | Nguyên tắc | Vì sao |
| :--- | :--- | :--- |
| Phân loại theo chủ đề | Lọc nhanh bằng các nhãn chủ đề ở đầu trang, không bắt cuộn qua toàn bộ danh sách | Bệnh nhân thường chỉ quan tâm 1-2 chủ đề liên quan tới tình trạng của chính họ |
| Thời gian đọc ước tính | Hiện ngay trên mỗi bài | Giúp người đọc quyết định có đủ thời gian đọc ngay hay để dành — tôn trọng việc họ có thể đang đọc giữa lúc bận |
| Luôn có lối dẫn sang AI ở cuối bài | "Hỏi AI về chủ đề này" | Bài viết tĩnh trả lời được cái chung, nhưng câu hỏi "vậy trường hợp CỦA TÔI thì sao" chỉ AI (biết hồ sơ cá nhân) mới trả lời được — đây là cách nối giá trị tĩnh (bài viết) sang giá trị cá nhân hoá (AI) một cách tự nhiên |
| Nguồn nội dung | Do đội ngũ có chuyên môn y tế biên soạn/duyệt (`04-QuanTri-AdminCMS/`, UC0065), không phải AI tự sinh | Đây là nội dung giáo dục tĩnh, cùng 1 nội dung cho mọi người đọc — cần độ chính xác đã được con người kiểm chứng, khác với câu trả lời AI theo ngữ cảnh từng cuộc hỏi-đáp |

## 4. Tham chiếu

- Tra nhanh câu hỏi ngắn: [SCR-PT-11-FAQ.md](SCR-PT-11-FAQ.md)
- Hỏi riêng cho trường hợp cá nhân: [SCR-PT-08-ChatAI.md](SCR-PT-08-ChatAI.md)
- Quản lý nội dung: `04-QuanTri-AdminCMS/` (UC0065, UC0066)
