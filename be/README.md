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

Cần cả Postgres (db `gastroai`) và MySQL (db `gastroai_admin`) chạy sẵn, user/pass `gastroai`/`gastroai` cho cả 2.

```bash
# Nếu Postgres/MySQL máy m không ở cổng mặc định (vd bị app khác chiếm cổng):
POSTGRES_PORT=5433 MYSQL_PORT=3307 mvn spring-boot:run
# Bình thường thì chỉ cần:
mvn spring-boot:run
```

Tài khoản test có sẵn (seed ở migration `V2__seed_test_patient.sql`): `test@gastroai.vn` / `Passw0rd!`.
