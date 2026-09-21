# fe — Frontend GastroAI

Vite + React 19 + TypeScript + Tailwind CSS v4 + Ant Design 6. Cấu trúc tham khảo dự án CSDL-CC (FPT/OSP) — bỏ `graphql/` (BE dùng REST) và `i18n/` (chỉ tiếng Việt, ngoài scope đề cương).

## Package nghĩa là gì

```
src/
├── api/               Hàm gọi API backend qua axios, 1 file/feature (hiện có auth.ts).
├── components/<feature>/   Component UI riêng từng phân hệ. Hiện rỗng — điền khi code UI thật.
├── components/layout/, components/brand/   Component dùng chung (header, logo...). Chưa dùng.
├── hooks/             Custom React hook dùng chung nhiều nơi. Chưa dùng.
├── layouts/           Khung trang tổng (sidebar, header) bọc quanh pages. Chưa dùng.
├── lib/               Tiện ích kỹ thuật thuần, không phải business (vd lib/axios.ts — axios instance).
├── pages/<feature>/   Component trang hoàn chỉnh ứng với 1 route (vd pages/auth/LoginPage.tsx).
├── routes/            Khai báo router (router.tsx) + route guard (ProtectedRoute.tsx).
├── stores/            State dùng chung toàn app, hiện dùng React Context (không thêm thư viện state
│                      ngoài vì chưa cần) — authStore.tsx giữ JWT + thông tin người dùng đang đăng nhập.
├── styles/            CSS dùng chung ngoài phạm vi 1 component. Chưa dùng.
└── test/              Test setup. Chưa dùng.
```

## Luồng hoạt động — ví dụ UC0002 (Đăng nhập)

1. User vào `/login` → `routes/router.tsx` render `pages/auth/LoginPage.tsx`.
2. Submit form (Ant Design `Form`) → gọi `useAuth().login()` từ `stores/authStore.tsx`.
3. `authStore` gọi `api/auth.ts` → `login()` → `apiClient.post('/auth/login', ...)` (axios instance khai ở `lib/axios.ts`, baseURL đọc từ biến môi trường `VITE_API_BASE_URL`).
4. Nhận `AuthResponse` (token, email, fullName) → lưu vào `localStorage` + state của `AuthProvider`.
5. `LoginPage` gọi `navigate('/')` → `routes/ProtectedRoute.tsx` thấy `isAuthenticated=true` → render `pages/patient/HomePage.tsx`.
6. BE trả lỗi (401 sai mật khẩu) → `LoginPage` bắt lỗi trong `catch`, hiển thị `Alert` đỏ, không điều hướng.

## Chạy local

```bash
cp .env.example .env   # nếu BE không chạy ở localhost:8080 thì sửa VITE_API_BASE_URL trong .env
npm run dev
```
