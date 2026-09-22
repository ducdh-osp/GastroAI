# Authentication và RBAC Backend

Tài liệu này mô tả phần backend xác thực và phân quyền đã triển khai cho GastroAI.

## 1. Phạm vi đã triển khai

Backend dùng Spring Boot 4, Java 21 và kiến trúc hai cơ sở dữ liệu:

- PostgreSQL: dữ liệu bệnh nhân, bác sĩ, JWT và token bị thu hồi.
- MySQL: tài khoản quản trị viên và session admin.

Các cơ chế xác thực được tách riêng:

- Bệnh nhân/bác sĩ: Bearer JWT.
- Admin: `HttpSession` của Spring Servlet, không dùng JWT bệnh nhân.

## 2. Chức năng

### Đăng ký và xác thực email

Endpoint:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "patient@example.com",
  "password": "Passw0rd!",
  "fullName": "Nguyen Van A"
}
```

Mật khẩu được băm bằng BCrypt. Backend tạo token xác thực ngẫu nhiên, chỉ lưu SHA-256 của token và gửi link xác thực qua SMTP. Token có hiệu lực 24 giờ.

```http
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "token": "token-nhan-duoc-trong-email"
}
```

### Đăng nhập bệnh nhân

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "test@gastroai.vn",
  "password": "Passw0rd!"
}
```

Màn hình bệnh nhân dùng endpoint `/api/v1/auth/login` và chỉ chấp nhận tài khoản có role `PATIENT`:

- Bệnh nhân nhận JWT.
- Nếu dùng email bác sĩ hoặc admin ở cổng này, backend từ chối đăng nhập.

Sau khi đăng nhập, bệnh nhân được dẫn tới `/patient`.

Response bệnh nhân/bác sĩ trả về JWT, mã tài khoản, email, họ tên và role:

```json
{
  "token": "eyJ...",
  "patientId": 1,
  "email": "test@gastroai.vn",
  "fullName": "Benh nhan Test",
  "role": "PATIENT"
}
```

Gửi token ở các request cần đăng nhập:

```http
Authorization: Bearer <jwt>
```

JWT chứa `jti`, role và `tokenVersion`. Filter xác thực chữ ký, thời hạn, token bị thu hồi và phiên bản token hiện tại của tài khoản.

### Đăng xuất bệnh nhân/bác sĩ

```http
POST /api/v1/auth/logout
Authorization: Bearer <jwt>
```

JWT hiện tại được ghi vào bảng `revoked_tokens` và không thể dùng tiếp trước khi hết hạn.

### Đổi mật khẩu bệnh nhân/bác sĩ

```http
POST /api/v1/auth/change-password
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "currentPassword": "Passw0rd!",
  "newPassword": "NewPassw0rd!"
}
```

Sau khi đổi mật khẩu, `tokenVersion` tăng lên nên toàn bộ JWT cũ bị vô hiệu hóa.

### Đăng nhập bác sĩ và admin

Bác sĩ và admin dùng chung màn hình nhân sự, nhưng không có chức năng đăng ký. Endpoint là:

```http
POST /api/v1/auth/staff/login
```

- Bác sĩ hợp lệ nhận JWT và role `DOCTOR`.
- Admin hợp lệ nhận cookie session và role `ADMIN`.
- Cả hai được frontend dẫn tới cùng trang `/staff`.
- Admin được phép dùng toàn bộ route; bác sĩ chỉ thấy và truy cập chức năng được cấp.

### Admin session

Admin đăng nhập bằng endpoint nhân sự dùng chung với bác sĩ:

```http
POST /api/v1/auth/staff/login
Content-Type: application/json

{
  "email": "admin@gastroai.vn",
  "password": "Passw0rd!"
}
```

Client phải giữ cookie session trả về từ server. Các request admin gửi cookie đó, không gửi JWT bệnh nhân.

Frontend đã cấu hình Axios với `withCredentials: true`, vì vậy màn `/staff/login` giữ được cookie session admin.

Đăng xuất cũng dùng endpoint chung:

```http
POST /api/v1/auth/logout
Cookie: JSESSIONID=<session-id>
```

Đổi mật khẩu:

```http
POST /api/v1/auth/admin/change-password
Cookie: JSESSIONID=<session-id>
Content-Type: application/json

{
  "currentPassword": "Passw0rd!",
  "newPassword": "NewAdminPassw0rd!"
}
```

Sau khi đổi mật khẩu, session admin hiện tại bị hủy và phải đăng nhập lại.

## 3. RBAC

Ba role được hỗ trợ:

| Role | Ý nghĩa | Route được bảo vệ |
|---|---|---|
| `PATIENT` | Bệnh nhân | `/api/v1/patient/**` |
| `DOCTOR` | Bác sĩ | `/api/v1/doctor/**` |
| `ADMIN` | Quản trị viên | `/api/v1/admin/**` |

Quy tắc hiện tại:

- `/api/v1/patient/**`: chỉ `PATIENT`.
- `/api/v1/doctor/**`: `DOCTOR` hoặc `ADMIN`.
- `/api/v1/admin/**`: chỉ `ADMIN`.
- Các route khác ngoài route public yêu cầu đã xác thực.

RBAC được kiểm tra bởi Spring Security trong `SecurityConfig`; JWT role được nạp bởi `JwtAuthenticationFilter`, còn role admin được nạp từ `HttpSession` bởi `AdminSessionFilter`.

## 4. Migration và dữ liệu mẫu

PostgreSQL:

- `V1__init_patients.sql`: bảng bệnh nhân ban đầu.
- `V2__seed_test_patient.sql`: bệnh nhân mẫu.
- `V3__authentication_and_rbac.sql`: role, xác thực email, token version và revoked token; đồng thời thêm bác sĩ mẫu.

MySQL:

- `V1__init_admin_accounts.sql`: bảng admin và admin mẫu.

Dữ liệu mẫu dùng cho local:

| Loại | Email | Mật khẩu |
|---|---|---|
| Patient | `test@gastroai.vn` | `Passw0rd!` |
| Doctor | `doctor@gastroai.vn` | `Passw0rd!` |
| Admin | `admin@gastroai.vn` | `Passw0rd!` |

Đổi các mật khẩu này trước khi triển khai thật.

## 5. Cấu hình môi trường

Mặc định trong `src/main/resources/application.yml`, backend dùng:

- PostgreSQL: `localhost:5432/gastroai`.
- MySQL: `localhost:3306/gastroai_admin`.
- SMTP local: `localhost:1025`.
- Server: `http://localhost:8080`.

Các biến có thể override:

```env
POSTGRES_PORT=5432
MYSQL_PORT=3306
POSTGRES_USERNAME=gastroai
POSTGRES_PASSWORD=gastroai
MYSQL_USERNAME=gastroai
MYSQL_PASSWORD=gastroai
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_AUTH=false
SMTP_STARTTLS=false
MAIL_FROM=no-reply@gastroai.vn
VERIFICATION_BASE_URL=http://localhost:5173/verify-email
```

SMTP mặc định phù hợp với MailHog/Mailpit local. Khi dùng SMTP thật, đặt lại `SMTP_HOST`, port, username, password và các cờ auth/STARTTLS.

## 6. Cách chạy

### Nếu gặp lỗi `Access denied for user 'gastroai'@'localhost'`

Đây là lỗi kết nối MySQL, không phải lỗi compile Java. Trong log của Spring, hãy tìm dòng cuối cùng có dạng:

```text
Access denied for user 'gastroai'@'localhost' (using password: YES)
```

Ứng dụng đang dùng cấu hình mặc định sau trong `application.yml`:

```text
MySQL host: localhost
MySQL port: 3306
Database: gastroai_admin
Username: gastroai
Password: gastroai
```

Bạn cần tạo database, tạo user và cấp quyền bằng tài khoản quản trị MySQL (`root`). Mở một cửa sổ PowerShell/CMD khác và chạy:

Nếu lệnh `mysql` không được nhận diện, dùng đường dẫn mặc định của MySQL trên Windows:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p
```

Hoặc thêm thư mục `C:\Program Files\MySQL\MySQL Server 8.0\bin` vào `PATH`, rồi mở lại terminal.

Nhập mật khẩu root MySQL. Sau đó chạy các câu SQL sau trong màn hình MySQL:

```sql
CREATE DATABASE IF NOT EXISTS gastroai_admin
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'gastroai'@'localhost'
  IDENTIFIED BY 'gastroai';

ALTER USER 'gastroai'@'localhost'
  IDENTIFIED BY 'gastroai';

GRANT ALL PRIVILEGES ON gastroai_admin.*
  TO 'gastroai'@'localhost';

FLUSH PRIVILEGES;
```

Thoát MySQL:

```sql
exit
```

Kiểm tra user mới có đăng nhập được không:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" `
  -h localhost -P 3306 -u gastroai -p gastroai_admin
```

Khi được hỏi mật khẩu, nhập `gastroai`. Nếu vào được màn hình MySQL thì thông tin kết nối đã đúng; gõ `exit` để thoát.

Nếu MySQL của bạn chạy ở port khác, ví dụ `3307`, kiểm tra bằng:

```powershell
mysql -h localhost -P 3307 -u gastroai -p gastroai_admin
```

Sau đó chạy backend với đúng port:

```powershell
$env:MYSQL_PORT = "3307"
.\be\mvnw.cmd -f be/pom.xml spring-boot:run
```

Nếu bạn muốn dùng username/password khác thay vì `gastroai/gastroai`, đặt biến môi trường trước khi chạy:

```powershell
$env:MYSQL_USERNAME = "myuser"
$env:MYSQL_PASSWORD = "mypassword"
.\be\mvnw.cmd -f be/pom.xml spring-boot:run
```

Tương tự, có thể đặt `POSTGRES_USERNAME` và `POSTGRES_PASSWORD` cho PostgreSQL.

### Chuẩn bị PostgreSQL

Sau khi sửa MySQL, ứng dụng còn cần PostgreSQL. Đăng nhập bằng tài khoản quản trị PostgreSQL:

Nếu đã cài PostgreSQL nhưng lệnh `psql` không được nhận diện, dùng đường dẫn tương ứng, ví dụ:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres
```

Nếu thư mục PostgreSQL không tồn tại và port `5432` không lắng nghe, PostgreSQL chưa được cài/chạy. Có thể cài PostgreSQL 16 rồi chọn chạy Windows Service, hoặc dùng Docker Desktop. Khi Docker Desktop đã chạy:

```powershell
docker run --name gastroai-postgres `
  -e POSTGRES_USER=gastroai `
  -e POSTGRES_PASSWORD=gastroai `
  -e POSTGRES_DB=gastroai `
  -p 5432:5432 `
  -d postgres:16
```

Lệnh Docker chỉ chạy được sau khi Docker Desktop đã khởi động.

Chạy:

```sql
CREATE USER gastroai WITH PASSWORD 'gastroai';
CREATE DATABASE gastroai OWNER gastroai;
```

Nếu user/database đã tồn tại thì dùng:

```sql
ALTER USER gastroai WITH PASSWORD 'gastroai';
```

Kiểm tra kết nối:

```powershell
psql -h localhost -p 5432 -U gastroai -d gastroai
```

Nhập mật khẩu `gastroai`. Nếu PostgreSQL chạy port khác, đặt biến trước khi chạy backend:

```powershell
$env:POSTGRES_PORT = "5433"
```

### Thứ tự chạy đầy đủ trên Windows

Mở ba cửa sổ terminal:

1. Cửa sổ 1: bảo đảm PostgreSQL đang chạy.
2. Cửa sổ 2: bảo đảm MySQL đang chạy.
3. Cửa sổ 3: chạy backend bằng Maven Wrapper.

Kiểm tra nhanh port có đang lắng nghe:

```powershell
Test-NetConnection localhost -Port 5432
Test-NetConnection localhost -Port 3306
```

Kết quả cần có `TcpTestSucceeded : True` cho cả hai port.

### Chuẩn bị database

Tạo hai database và user trùng với cấu hình:

- PostgreSQL database: `gastroai`, user/password: `gastroai` / `gastroai`.
- MySQL database: `gastroai_admin`, user/password: `gastroai` / `gastroai`.

Đảm bảo SMTP server đang lắng nghe ở `localhost:1025`, hoặc override bằng biến môi trường.

### Khởi động backend

Từ thư mục repository:

```powershell
.\be\mvnw.cmd -f be/pom.xml spring-boot:run
```

Nếu database dùng port khác:

```powershell
$env:POSTGRES_PORT = "5433"
$env:MYSQL_PORT = "3307"
.\be\mvnw.cmd -f be/pom.xml spring-boot:run
```

Flyway sẽ tự chạy migration cho từng database khi ứng dụng khởi động.

## 7. Cách build và test

Compile không chạy test:

```powershell
.\be\mvnw.cmd -f be/pom.xml -DskipTests compile
```

Chạy toàn bộ test:

```powershell
.\be\mvnw.cmd -f be/pom.xml test
```

Test smoke thủ công bằng PowerShell:

```powershell
$base = "http://localhost:8080"

# Đăng nhập bệnh nhân
$login = Invoke-RestMethod "$base/api/v1/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"email":"test@gastroai.vn","password":"Passw0rd!"}'

# Gọi endpoint cần JWT
$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-WebRequest "$base/api/v1/auth/logout" -Method Post -Headers $headers

# Đăng nhập admin bằng endpoint chung và giữ cookie session
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-RestMethod "$base/api/v1/auth/login" `
  -Method Post `
  -WebSession $session `
  -ContentType "application/json" `
  -Body '{"email":"admin@gastroai.vn","password":"Passw0rd!"}'

Invoke-WebRequest "$base/api/v1/auth/logout" `
  -Method Post `
  -WebSession $session
```

## 8. Lưu ý khi triển khai

- Bắt buộc thay `JWT_SECRET` bằng secret ngẫu nhiên, đủ dài và quản lý bằng secret manager.
- Không dùng tài khoản seed trong môi trường production.
- SMTP local mặc định chỉ để phát triển; production cần SMTP thật và HTTPS.
- CORS hiện chỉ cho phép `http://localhost:5173`; cần đổi theo domain frontend thật.
- Cần tạo thêm controller nghiệp vụ dưới `/api/v1/patient/**`, `/api/v1/doctor/**` hoặc `/api/v1/admin/**` để áp dụng RBAC cho các use case cụ thể.
- Test tích hợp đầy đủ cần PostgreSQL, MySQL và SMTP đang chạy; lệnh compile không yêu cầu các dịch vụ này.
