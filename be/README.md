# be — Backend GastroAI

Spring Boot 4.1, Java 21, Maven. Kiến trúc layered + CQRS-nhẹ, tham khảo cấu trúc package của dự án CSDL-CC (FPT/OSP) — đổi GraphQL→REST và dùng feature của GastroAI thay vì nghiệp vụ công chứng.

## Package nghĩa là gì

```
vn.gastroai.be
├── api/<feature>/            REST controller — nhận request, validate, gọi application layer, trả response.
│                              KHÔNG chứa business logic.
├── api/GlobalExceptionHandler.java   Bắt exception toàn cục (401 sai mật khẩu, 400 validate lỗi...)
│
├── application/<feature>/    Service xử lý nghiệp vụ (vd AuthService) — nơi business logic thật sự nằm.
├── application/commands/     Object đại diện 1 "lệnh" thay đổi state (vd LoginCommand). Kiểu CQRS.
├── application/queries/      Object đại diện 1 "truy vấn" đọc dữ liệu. Chưa dùng — để dành query phức tạp sau.
├── application/readmodel/    Object trả về sau khi xử lý xong (vd AuthResult).
├── application/port/         Interface application cần, infrastructure implement — giữ application không
│                              phụ thuộc trực tiếp framework/thư viện ngoài. Chưa dùng, để dành.
├── application/session/      Quản lý phiên làm việc. Chưa dùng.
├── application/support/      Helper dùng chung tầng application. Chưa dùng.
│
├── domain/<feature>/         Entity JPA — model nghiệp vụ ánh xạ DB (vd domain/auth/Patient.java).
│
├── infrastructure/persistence/postgres/   Spring Data JPA repository cho datasource Postgres (vd PatientRepository).
├── infrastructure/persistence/mysql/      Spring Data JPA repository cho datasource MySQL. Rỗng —
│                                           chưa code UC nào bên Admin.
│                                           (Tách theo datasource vì 2 EntityManagerFactory không thể
│                                           cùng scan chung 1 package repository.)
├── infrastructure/security/      JwtService (sinh/verify JWT).
├── infrastructure/external/      Client gọi service ngoài (LLM API...). Chưa dùng.
├── infrastructure/{cache,export,filestorage,observability,scheduler,search,web,audit}/  Chưa dùng, để dành.
│
├── config/                   Spring @Configuration — khai bean hạ tầng (khác application/, đây không phải
│                              nghiệp vụ). PostgresConfig + MysqlConfig (mỗi cái tự khai datasource/JPA/
│                              Flyway riêng — kiến trúc đa cơ sở dữ liệu, mục 6 đề cương), SecurityConfig
│                              (CORS, password encoder, filter chain).
│
└── filter/<feature>/         Servlet filter riêng từng phân hệ (log, chặn request...). Chưa dùng.
```

## Luồng hoạt động — ví dụ UC0002 (Đăng nhập)

1. FE gửi `POST /api/v1/auth/login` với `{ email, password }`.
2. `api/auth/AuthController.login()` nhận request, Spring validate theo annotation trong `LoginRequest` (`@NotBlank`, `@Email`).
3. Controller bọc thành `application/commands/LoginCommand`, gọi `application/auth/AuthService.login()`.
4. `AuthService` tra `domain/auth/Patient` qua `infrastructure/persistence/postgres/PatientRepository.findByEmail()`.
5. So khớp mật khẩu bằng `PasswordEncoder` (bcrypt, bean khai trong `config/SecurityConfig`).
6. Đúng mật khẩu → gọi `infrastructure/security/JwtService.generateToken()` sinh JWT.
7. Trả `application/readmodel/AuthResult` → controller convert sang `api/auth/AuthResponse` → về FE.
8. Sai mật khẩu → `AuthService` ném `BadCredentialsException` → `api/GlobalExceptionHandler` bắt, trả HTTP 401.

Datasource/JPA/Flyway auto-config mặc định của Spring Boot bị tắt trong `GastroApplication.java` (`@SpringBootApplication(exclude = ...)`) vì được khai thủ công trong `config/PostgresConfig` và `config/MysqlConfig` (kiến trúc đa cơ sở dữ liệu, mục 6 đề cương). `MysqlConfig` hiện chưa quản lý entity nào (chưa code UC bên Admin) — chỉ khởi tạo kết nối + Flyway để sẵn sàng.

## Chạy local

### Setup lần đầu (mỗi máy làm 1 lần)

Cần cài sẵn PostgreSQL + MySQL (không dùng Docker). Sau đó tạo user + database — **clone code không tự tạo cái này**, ai cũng phải tự chạy 1 lần trên máy mình, không thì app báo lỗi `Access denied for user 'gastroai'@'localhost'`.

**Linux/Mac** (Terminal):
```bash
sudo -u postgres psql -c "CREATE USER gastroai WITH PASSWORD 'gastroai';"
sudo -u postgres psql -c "CREATE DATABASE gastroai OWNER gastroai;"
sudo -u postgres psql -d gastroai -c "CREATE EXTENSION IF NOT EXISTS vector;"

sudo mysql -e "CREATE USER 'gastroai'@'%' IDENTIFIED BY 'gastroai'; CREATE DATABASE gastroai_admin; GRANT ALL ON gastroai_admin.* TO 'gastroai'@'%'; FLUSH PRIVILEGES;"
```

**Windows** (Command Prompt / PowerShell — không có `sudo`, sẽ hỏi mật khẩu user `root`/`postgres` đặt lúc cài):
```
mysql -u root -p -e "CREATE USER 'gastroai'@'%' IDENTIFIED BY 'gastroai'; CREATE DATABASE gastroai_admin; GRANT ALL ON gastroai_admin.* TO 'gastroai'@'%'; FLUSH PRIVILEGES;"

psql -U postgres -c "CREATE USER gastroai WITH PASSWORD 'gastroai';"
psql -U postgres -c "CREATE DATABASE gastroai OWNER gastroai;"
psql -U postgres -d gastroai -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

> `CREATE EXTENSION vector` (pgvector, dùng cho kho tri thức RAG — GĐ2) **bắt buộc chạy bằng
> user superuser** (`postgres`), user ứng dụng `gastroai` không có quyền này — Flyway sẽ không
> tự bật được, phải làm tay bước trên trước khi chạy app, nếu không migration `V7` sẽ lỗi.
> Nếu báo thiếu `vector.control` file, nghĩa là chưa cài package pgvector cho Postgres —
> Linux: `sudo apt install postgresql-<version>-pgvector` (xem version bằng
> `psql --version`); Windows/Mac: cài qua [pgvector releases](https://github.com/pgvector/pgvector#installation)
> hoặc dùng bản Postgres đã tích hợp sẵn (vd Postgres.app trên Mac).
Nếu báo `'mysql'`/`'psql' is not recognized` thì do chưa có trong PATH — dùng đường dẫn đầy đủ, ví dụ `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"` / `"C:\Program Files\PostgreSQL\16\bin\psql.exe"`. Ngại dòng lệnh thì mở **MySQL Workbench** / **pgAdmin** (thường cài kèm sẵn) → mở Query tab → dán câu SQL bên trong dấu `"..."` rồi chạy.

> Dùng `'gastroai'@'%'` (mọi host) thay vì `'gastroai'@'localhost'` — MySQL coi `localhost` và `127.0.0.1` là 2 host khác nhau khi cấp quyền, mà JDBC (app Java) thường connect qua `127.0.0.1` nên dùng đúng `'localhost'` hay bị `Access denied` dù gõ đúng mật khẩu. Nếu đã lỡ tạo user kiểu `'gastroai'@'localhost'` và vẫn bị lỗi này, chạy `DROP USER IF EXISTS 'gastroai'@'localhost';` rồi tạo lại theo lệnh trên.

Nếu máy đã có Postgres/MySQL chạy port khác 5432/3306 (vd trùng port với service khác đang chạy), đổi port lúc kết nối cho khớp, rồi dùng `POSTGRES_PORT`/`MYSQL_PORT` khi chạy app (xem dưới).

### Chạy app

```bash
# Nếu Postgres/MySQL máy m không ở cổng mặc định (vd bị app khác chiếm cổng):
POSTGRES_PORT=5433 MYSQL_PORT=3307 mvn spring-boot:run
# Bình thường thì chỉ cần:
mvn spring-boot:run
```

Từ GĐ2 (RAG) trở đi, cần thêm biến môi trường `GEMINI_API_KEY` (lấy free tại
[Google AI Studio](https://aistudio.google.com) → icon chìa khóa → Create API key — chọn
project mặc định, đừng gắn Cloud project có bật billing) trước khi chạy app, ví dụ:
```bash
GEMINI_API_KEY=AIza... mvn spring-boot:run
```
Thiếu biến này thì các API liên quan chat/tài liệu y khoa sẽ lỗi lúc gọi tới, còn lại (đăng
nhập, quản lý người dùng...) vẫn chạy bình thường.

Lần đầu chạy app, Flyway sẽ tự tạo bảng + seed dữ liệu test theo các file trong `db/migration/`.

Tài khoản test có sẵn (seed ở migration `V2__seed_test_patient.sql`): `test@gastroai.vn` / `Passw0rd!`.
