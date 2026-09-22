# Backend đã triển khai và luồng hoạt động

Tài liệu này chỉ mô tả phần backend đã code; không mô tả phần frontend.

## 1. Những chức năng đã triển khai

### Bệnh nhân

- Đăng ký bằng email, mật khẩu và họ tên.
- Mật khẩu được băm bằng BCrypt.
- Gửi email xác thực; token chỉ lưu dạng SHA-256 và hết hạn sau 24 giờ.
- Đăng nhập bằng JWT.
- Đăng xuất bằng cách thu hồi JWT hiện tại.
- Đổi mật khẩu; tăng `tokenVersion` để vô hiệu hóa JWT cũ.

Endpoint:

```text
POST /api/v1/auth/register
POST /api/v1/auth/verify-email
POST /api/v1/auth/login
POST /api/v1/auth/logout
POST /api/v1/auth/change-password
```

`/api/v1/auth/login` chỉ dành cho role `PATIENT`. Bác sĩ và admin không thể dùng endpoint này.

### Bác sĩ

- Không có đăng ký public.
- Đăng nhập tại cổng nhân sự bằng JWT.
- Role là `DOCTOR`.
- Được phép dùng các route `/api/v1/doctor/**`.

```text
POST /api/v1/auth/staff/login
```

### Admin

- Không có đăng ký public.
- Đăng nhập cùng cổng nhân sự với bác sĩ.
- Dữ liệu tài khoản nằm ở MySQL.
- Xác thực bằng `HttpSession`, không dùng JWT.
- Có quyền đầy đủ ở các route được cấu hình.

```text
POST /api/v1/auth/staff/login
POST /api/v1/auth/logout
POST /api/v1/auth/admin/change-password
```

## 2. RBAC

Ba role được hỗ trợ:

```text
PATIENT  -> /api/v1/patient/**
DOCTOR   -> /api/v1/doctor/**
ADMIN    -> /api/v1/admin/**
```

Admin được phép truy cập các route nghiệp vụ cần quyền admin. Bác sĩ không được truy cập route admin. Kiểm tra quyền nằm trong `SecurityConfig`, không phụ thuộc vào việc frontend có ẩn menu hay không.

## 3. Phân tách hai database

### PostgreSQL

Chứa bảng `patients`, role bệnh nhân/bác sĩ, token xác thực email, `token_version` và bảng `revoked_tokens`.

Migration:

```text
be/src/main/resources/db/migration/postgres/V1__init_patients.sql
be/src/main/resources/db/migration/postgres/V2__seed_test_patient.sql
be/src/main/resources/db/migration/postgres/V3__authentication_and_rbac.sql
```

### MySQL

Chứa bảng `admin_accounts` và dữ liệu admin.

Migration:

```text
be/src/main/resources/db/migration/mysql/V1__init_admin_accounts.sql
```

## 4. Luồng chức năng: bác sĩ đăng nhập

### Bước 1: Request

```http
POST /api/v1/auth/staff/login
Content-Type: application/json

{
  "email": "doctor@gastroai.vn",
  "password": "Passw0rd!"
}
```

### Bước 2: Controller

`AuthController.staffLogin()` nhận request, validate dữ liệu và gọi `AuthService.login()`.

### Bước 3: Truy vấn tài khoản

`AuthService` dùng `PatientRepository.findByEmail()` để tìm tài khoản trong PostgreSQL. Bản ghi phải có:

```text
role = DOCTOR
```

Nếu email không tồn tại hoặc mật khẩu sai, backend trả HTTP `401`.

### Bước 4: Kiểm tra mật khẩu

`PasswordEncoder.matches()` so sánh mật khẩu gửi lên với BCrypt hash trong database. Bác sĩ cũng phải có `emailVerified = true`.

### Bước 5: Sinh JWT

`JwtService` tạo JWT có các claim:

```text
jti, sub, email, role, version, iat, exp
```

Response có dạng:

```json
{
  "token": "eyJ...",
  "patientId": 2,
  "email": "doctor@gastroai.vn",
  "fullName": "Bac si GastroAI",
  "role": "DOCTOR"
}
```

### Bước 6: Gọi route cần quyền bác sĩ

Client gửi:

```http
Authorization: Bearer <jwt>
```

`JwtAuthenticationFilter` kiểm tra chữ ký, thời hạn, `jti` có bị thu hồi không và `tokenVersion` có khớp database không. Nếu hợp lệ, filter tạo authority `ROLE_DOCTOR`.

`SecurityConfig` cho phép `ROLE_DOCTOR` vào `/api/v1/doctor/**`. Nếu gọi `/api/v1/admin/**`, Spring Security trả HTTP `403 Forbidden`.

## 5. Luồng chức năng: admin đăng nhập

Admin cũng gửi request tới `/api/v1/auth/staff/login`. Nếu tài khoản không phải bác sĩ, controller tìm email trong MySQL qua `AdminAccountRepository`.

1. Kiểm tra tài khoản admin có `active = true`.
2. So sánh mật khẩu BCrypt.
3. Tạo `HttpSession` và đổi session id.
4. Lưu `ADMIN_ID` và `ADMIN_EMAIL` vào session.
5. Trả role `ADMIN`; response không có JWT.

Request sau đó gửi cookie `JSESSIONID`. `AdminSessionFilter` đọc cookie/session và tạo authority `ROLE_ADMIN`.

## 6. Các file backend chính

```text
be/src/main/java/vn/gastroai/be/api/auth/AuthController.java
be/src/main/java/vn/gastroai/be/api/admin/AdminAuthController.java
be/src/main/java/vn/gastroai/be/application/auth/AuthService.java
be/src/main/java/vn/gastroai/be/application/auth/EmailVerificationService.java
be/src/main/java/vn/gastroai/be/config/SecurityConfig.java
be/src/main/java/vn/gastroai/be/infrastructure/security/JwtService.java
be/src/main/java/vn/gastroai/be/infrastructure/security/JwtAuthenticationFilter.java
be/src/main/java/vn/gastroai/be/infrastructure/security/AdminSessionFilter.java
be/src/main/java/vn/gastroai/be/domain/auth/Patient.java
be/src/main/java/vn/gastroai/be/domain/auth/RevokedToken.java
be/src/main/java/vn/gastroai/be/domain/auth/UserRole.java
be/src/main/java/vn/gastroai/be/domain/admin/AdminAccount.java
```

## 7. Build và chạy backend

```powershell
.\be\mvnw.cmd -f be/pom.xml -DskipTests compile
.\be\mvnw.cmd -f be/pom.xml spring-boot:run
```

Khởi động thành công khi log có:

```text
Tomcat started on port 8080
Started GastroApplication
```
