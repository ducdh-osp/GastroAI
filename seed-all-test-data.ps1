<#
.SYNOPSIS
Tao bo du lieu test thuc te cho toan bo bang ung dung GastroAI.

.DESCRIPTION
Seed dong thoi MySQL gastroai_admin va PostgreSQL gastroai. Script chi lam moi
du lieu co dau hieu seed (email *.test@gastroai.vn, [SEED], seed: hoac seed://)
va khong truncate hay xoa du lieu khac. Co the chay lai nhieu lan ma khong nhan
ban du lieu seed.

Database phai duoc chay Flyway migration cua phien ban source hien tai truoc khi
seed. Mode Auto uu tien CLI local neu ket noi duoc, sau do tu tim container
PostgreSQL/MySQL dang chay.

.EXAMPLE
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\seed-all-test-data.ps1

.EXAMPLE
.\seed-all-test-data.ps1 -Mode Docker -PostgresContainer gastroai-postgres -MySqlContainer gastroai-mysql

.EXAMPLE
.\seed-all-test-data.ps1 -Mode Local -PostgresPort 5433 -MySqlPort 3307
#>

[CmdletBinding()]
param(
    [ValidateSet('Auto', 'Local', 'Docker')]
    [string]$Mode = 'Auto',

    [string]$PostgresHost = 'localhost',
    [int]$PostgresPort = 5432,
    [string]$PostgresDatabase = 'gastroai',
    [string]$PostgresUser = 'gastroai',
    [string]$PostgresPassword = 'gastroai',
    [string]$PostgresContainer = '',

    [string]$MySqlHost = 'localhost',
    [int]$MySqlPort = 3306,
    [string]$MySqlDatabase = 'gastroai_admin',
    [string]$MySqlUser = 'gastroai',
    [string]$MySqlPassword = 'gastroai',
    [string]$MySqlContainer = '',

    [switch]$SkipPostgres,
    [switch]$SkipMySql
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$tempDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ('gastroai-seed-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $tempDirectory

function Write-Step {
    param([string]$Message)
    Write-Host ('[GastroAI Seed] ' + $Message) -ForegroundColor Cyan
}

function Invoke-CheckedNative {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$FailureMessage = 'Lenh he thong that bai.'
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ($FailureMessage + ' Exit code: ' + $LASTEXITCODE)
    }
}

function Test-LocalPostgres {
    $psql = Get-Command psql -ErrorAction SilentlyContinue
    if ($null -eq $psql) {
        return $false
    }

    $oldPassword = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $PostgresPassword
        & $psql.Source -X -q -h $PostgresHost -p $PostgresPort -U $PostgresUser -d $PostgresDatabase -Atc 'SELECT 1' *> $null
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        $env:PGPASSWORD = $oldPassword
    }
}

function Test-LocalMySql {
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($null -eq $mysql) {
        return $false
    }

    $oldPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        & $mysql.Source --default-character-set=utf8mb4 --protocol=TCP --host=$MySqlHost --port=$MySqlPort --user=$MySqlUser --database=$MySqlDatabase --batch --skip-column-names --execute='SELECT 1' *> $null
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        $env:MYSQL_PWD = $oldPassword
    }
}

function Find-DockerContainer {
    param(
        [ValidateSet('Postgres', 'MySql')][string]$Kind,
        [string]$PreferredName
    )

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -eq $docker) {
        return $null
    }

    if (-not [string]::IsNullOrWhiteSpace($PreferredName)) {
        $exact = & $docker.Source ps --filter ('name=^/' + $PreferredName + '$') --format '{{.Names}}'
        if ($LASTEXITCODE -eq 0 -and ($exact | Where-Object { $_ -eq $PreferredName })) {
            return $PreferredName
        }
        throw ('Khong tim thay container dang chay: ' + $PreferredName)
    }

    $rows = & $docker.Source ps --format '{{.Names}}|{{.Image}}'
    if ($LASTEXITCODE -ne 0) {
        return $null
    }

    $pattern = if ($Kind -eq 'Postgres') { 'postgres|pgvector' } else { 'mysql|mariadb' }
    $match = $rows | Where-Object { $_ -match $pattern } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($match)) {
        return $null
    }
    return ($match -split '\|', 2)[0]
}

function Resolve-Runner {
    param(
        [ValidateSet('Postgres', 'MySql')][string]$Kind,
        [string]$PreferredContainer
    )

    if ($Mode -eq 'Local') {
        $localWorks = if ($Kind -eq 'Postgres') { Test-LocalPostgres } else { Test-LocalMySql }
        if (-not $localWorks) {
            throw ('Khong the ket noi ' + $Kind + ' bang CLI local. Hay kiem tra dich vu, cong va tai khoan.')
        }
        return [pscustomobject]@{ Type = 'Local'; Container = $null }
    }

    if ($Mode -eq 'Auto') {
        $localWorks = if ($Kind -eq 'Postgres') { Test-LocalPostgres } else { Test-LocalMySql }
        if ($localWorks) {
            return [pscustomobject]@{ Type = 'Local'; Container = $null }
        }
    }

    $container = Find-DockerContainer -Kind $Kind -PreferredName $PreferredContainer
    if (-not [string]::IsNullOrWhiteSpace($container)) {
        return [pscustomobject]@{ Type = 'Docker'; Container = $container }
    }

    throw ('Khong tim thay ket noi ' + $Kind + ' local hoac container Docker phu hop.')
}

function Invoke-PostgresFile {
    param(
        [Parameter(Mandatory = $true)]$Runner,
        [Parameter(Mandatory = $true)][string]$SqlFile
    )

    if ($Runner.Type -eq 'Local') {
        $psql = (Get-Command psql -ErrorAction Stop).Source
        $oldPassword = $env:PGPASSWORD
        try {
            $env:PGPASSWORD = $PostgresPassword
            Invoke-CheckedNative -FilePath $psql -Arguments @(
                '-X', '-v', 'ON_ERROR_STOP=1',
                '-h', $PostgresHost, '-p', [string]$PostgresPort,
                '-U', $PostgresUser, '-d', $PostgresDatabase,
                '-f', $SqlFile
            ) -FailureMessage 'Seed PostgreSQL that bai.'
        }
        finally {
            $env:PGPASSWORD = $oldPassword
        }
        return
    }

    $docker = (Get-Command docker -ErrorAction Stop).Source
    $containerPath = '/tmp/gastroai-seed-postgres.sql'
    Invoke-CheckedNative -FilePath $docker -Arguments @('cp', $SqlFile, ($Runner.Container + ':' + $containerPath)) -FailureMessage 'Khong the copy SQL vao container PostgreSQL.'
    try {
        Invoke-CheckedNative -FilePath $docker -Arguments @(
            'exec', '-e', ('PGPASSWORD=' + $PostgresPassword), $Runner.Container,
            'psql', '-X', '-v', 'ON_ERROR_STOP=1',
            '-U', $PostgresUser, '-d', $PostgresDatabase, '-f', $containerPath
        ) -FailureMessage 'Seed PostgreSQL trong Docker that bai.'
    }
    finally {
        & $docker exec $Runner.Container rm -f $containerPath *> $null
    }
}

function Invoke-MySqlFile {
    param(
        [Parameter(Mandatory = $true)]$Runner,
        [Parameter(Mandatory = $true)][string]$SqlFile
    )

    if ($Runner.Type -eq 'Local') {
        $mysql = (Get-Command mysql -ErrorAction Stop).Source
        $oldPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $MySqlPassword
            $clientPath = $SqlFile.Replace('\', '/')
            Invoke-CheckedNative -FilePath $mysql -Arguments @(
                '--default-character-set=utf8mb4', '--protocol=TCP',
                ('--host=' + $MySqlHost), ('--port=' + $MySqlPort),
                ('--user=' + $MySqlUser), ('--database=' + $MySqlDatabase),
                ('--execute=source ' + $clientPath)
            ) -FailureMessage 'Seed MySQL that bai.'
        }
        finally {
            $env:MYSQL_PWD = $oldPassword
        }
        return
    }

    $docker = (Get-Command docker -ErrorAction Stop).Source
    $containerPath = '/tmp/gastroai-seed-mysql.sql'
    Invoke-CheckedNative -FilePath $docker -Arguments @('cp', $SqlFile, ($Runner.Container + ':' + $containerPath)) -FailureMessage 'Khong the copy SQL vao container MySQL.'
    try {
        Invoke-CheckedNative -FilePath $docker -Arguments @(
            'exec', '-e', ('MYSQL_PWD=' + $MySqlPassword), $Runner.Container,
            'mysql', '--default-character-set=utf8mb4',
            ('--user=' + $MySqlUser), ('--database=' + $MySqlDatabase),
            ('--execute=source ' + $containerPath)
        ) -FailureMessage 'Seed MySQL trong Docker that bai.'
    }
    finally {
        & $docker exec $Runner.Container rm -f $containerPath *> $null
    }
}

function Invoke-MySqlScalar {
    param(
        [Parameter(Mandatory = $true)]$Runner,
        [Parameter(Mandatory = $true)][string]$Query
    )

    if ($Runner.Type -eq 'Local') {
        $mysql = (Get-Command mysql -ErrorAction Stop).Source
        $oldPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $MySqlPassword
            $output = & $mysql --default-character-set=utf8mb4 --protocol=TCP --host=$MySqlHost --port=$MySqlPort --user=$MySqlUser --database=$MySqlDatabase --batch --skip-column-names --execute=$Query
            if ($LASTEXITCODE -ne 0) {
                throw 'Khong the doc ID tai khoan CMS tu MySQL.'
            }
        }
        finally {
            $env:MYSQL_PWD = $oldPassword
        }
    }
    else {
        $docker = (Get-Command docker -ErrorAction Stop).Source
        $output = & $docker exec -e ('MYSQL_PWD=' + $MySqlPassword) $Runner.Container mysql --default-character-set=utf8mb4 --user=$MySqlUser --database=$MySqlDatabase --batch --skip-column-names --execute=$Query
        if ($LASTEXITCODE -ne 0) {
            throw 'Khong the doc ID tai khoan CMS tu MySQL Docker.'
        }
    }

    $number = $output | ForEach-Object { ([string]$_).Trim() } | Where-Object { $_ -match '^\d+$' } | Select-Object -Last 1
    if ([string]::IsNullOrWhiteSpace($number)) {
        throw ('Truy van MySQL khong tra ve ID hop le: ' + $Query)
    }
    return $number
}

$passwordHash = '$2a$10$f0l8woAFrVzD9WcP2uacte5eQwjqQb7eGcT2gIjA18XiUXJ/Q..TC'

$mySqlSql = @'
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO admins
    (email, password_hash, full_name, role, failed_login_attempts, locked_until, created_at, updated_at)
VALUES
    ('admin.test@gastroai.vn', '__PASSWORD_HASH__', 'Nguyễn Minh Anh - Quản trị viên', 'ADMIN', 0, NULL, UTC_TIMESTAMP() - INTERVAL 120 DAY, UTC_TIMESTAMP()),
    ('editor.test@gastroai.vn', '__PASSWORD_HASH__', 'Trần Thu Hà - Biên tập nội dung', 'CONTENT_EDITOR', 0, NULL, UTC_TIMESTAMP() - INTERVAL 90 DAY, UTC_TIMESTAMP())
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    full_name = VALUES(full_name),
    role = VALUES(role),
    failed_login_attempts = VALUES(failed_login_attempts),
    locked_until = VALUES(locked_until),
    updated_at = VALUES(updated_at);

INSERT INTO doctors
    (email, password_hash, full_name, role, failed_login_attempts, locked_until, created_at, updated_at)
VALUES
    ('doctor.test@gastroai.vn', '__PASSWORD_HASH__', 'BS. Lê Hoàng Nam', 'DOCTOR', 0, NULL, UTC_TIMESTAMP() - INTERVAL 100 DAY, UTC_TIMESTAMP()),
    ('doctor.locked.test@gastroai.vn', '__PASSWORD_HASH__', 'BS. Phạm Ngọc Lan - Tài khoản khóa', 'DOCTOR', 5, UTC_TIMESTAMP() + INTERVAL 2 DAY, UTC_TIMESTAMP() - INTERVAL 80 DAY, UTC_TIMESTAMP())
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    full_name = VALUES(full_name),
    role = VALUES(role),
    failed_login_attempts = VALUES(failed_login_attempts),
    locked_until = VALUES(locked_until),
    updated_at = VALUES(updated_at);

DELETE FROM admin_login_history
WHERE admin_id IN (
    SELECT id FROM admins WHERE email IN ('admin.test@gastroai.vn', 'editor.test@gastroai.vn')
);

DELETE FROM doctor_login_history
WHERE doctor_id IN (
    SELECT id FROM doctors WHERE email IN ('doctor.test@gastroai.vn', 'doctor.locked.test@gastroai.vn')
);

INSERT INTO admin_login_history
    (admin_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label)
SELECT id, UTC_TIMESTAMP() - INTERVAL 2 HOUR, 'SUCCESS', NULL, '192.168.1.10',
       'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0', 'Chrome trên Windows'
FROM admins WHERE email = 'admin.test@gastroai.vn'
UNION ALL
SELECT id, UTC_TIMESTAMP() - INTERVAL 3 DAY, 'FAILURE', 'BAD_CREDENTIALS', '192.168.1.10',
       'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0', 'Chrome trên Windows'
FROM admins WHERE email = 'admin.test@gastroai.vn'
UNION ALL
SELECT id, UTC_TIMESTAMP() - INTERVAL 1 DAY, 'SUCCESS', NULL, '10.10.0.25',
       'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15', 'Safari trên macOS'
FROM admins WHERE email = 'editor.test@gastroai.vn';

INSERT INTO doctor_login_history
    (doctor_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label)
SELECT id, UTC_TIMESTAMP() - INTERVAL 5 HOUR, 'SUCCESS', NULL, '172.16.10.21',
       'Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) Safari/604.1', 'Safari trên iPad'
FROM doctors WHERE email = 'doctor.test@gastroai.vn'
UNION ALL
SELECT id, UTC_TIMESTAMP() - INTERVAL 1 HOUR, 'BLOCKED', 'ACCOUNT_LOCKED', '172.16.10.22',
       'Mozilla/5.0 (Linux; Android 15) Chrome/140.0 Mobile', 'Chrome trên Android'
FROM doctors WHERE email = 'doctor.locked.test@gastroai.vn'
UNION ALL
SELECT id, UTC_TIMESTAMP() - INTERVAL 2 HOUR, 'FAILURE', 'BAD_CREDENTIALS', '172.16.10.22',
       'Mozilla/5.0 (Linux; Android 15) Chrome/140.0 Mobile', 'Chrome trên Android'
FROM doctors WHERE email = 'doctor.locked.test@gastroai.vn';

COMMIT;

SELECT 'admins' AS seed_table, COUNT(*) AS seed_rows
FROM admins WHERE email LIKE '%.test@gastroai.vn'
UNION ALL
SELECT 'doctors', COUNT(*) FROM doctors WHERE email LIKE '%.test@gastroai.vn'
UNION ALL
SELECT 'admin_login_history', COUNT(*) FROM admin_login_history
WHERE admin_id IN (SELECT id FROM admins WHERE email LIKE '%.test@gastroai.vn')
UNION ALL
SELECT 'doctor_login_history', COUNT(*) FROM doctor_login_history
WHERE doctor_id IN (SELECT id FROM doctors WHERE email LIKE '%.test@gastroai.vn');
'@
$mySqlSql = $mySqlSql.Replace('__PASSWORD_HASH__', $passwordHash)

$postgresSql = @'
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO patients
    (email, password_hash, full_name, role, email_verified,
     verification_token_hash, verification_token_expires_at,
     password_reset_token_hash, password_reset_token_expires_at,
     token_version, failed_login_attempts, locked_until, phone,
     admin_locked, admin_locked_at, created_at, updated_at)
VALUES
    ('patient01.test@gastroai.vn', '__PASSWORD_HASH__', 'Nguyễn Văn An', 'PATIENT', TRUE,
     NULL, NULL, repeat('b', 64), now() - interval '1 hour',
     0, 0, NULL, '0901234567', FALSE, NULL, now() - interval '180 days', now()),
    ('patient02.test@gastroai.vn', '__PASSWORD_HASH__', 'Trần Thị Bình', 'PATIENT', TRUE,
     NULL, NULL, NULL, NULL,
     1, 0, NULL, '0912345678', TRUE, now() - interval '3 days', now() - interval '150 days', now()),
    ('patient03.unverified.test@gastroai.vn', '__PASSWORD_HASH__', 'Lê Quốc Cường', 'PATIENT', FALSE,
     repeat('a', 64), now() + interval '7 days', NULL, NULL,
     0, 0, NULL, '0923456789', FALSE, NULL, now() - interval '2 days', now()),
    ('patient04.locked.test@gastroai.vn', '__PASSWORD_HASH__', 'Phạm Thu Dung', 'PATIENT', TRUE,
     NULL, NULL, NULL, NULL,
     0, 5, now() + interval '2 days', '0934567890', FALSE, NULL, now() - interval '60 days', now())
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    email_verified = EXCLUDED.email_verified,
    verification_token_hash = EXCLUDED.verification_token_hash,
    verification_token_expires_at = EXCLUDED.verification_token_expires_at,
    password_reset_token_hash = EXCLUDED.password_reset_token_hash,
    password_reset_token_expires_at = EXCLUDED.password_reset_token_expires_at,
    token_version = EXCLUDED.token_version,
    failed_login_attempts = EXCLUDED.failed_login_attempts,
    locked_until = EXCLUDED.locked_until,
    phone = EXCLUDED.phone,
    admin_locked = EXCLUDED.admin_locked,
    admin_locked_at = EXCLUDED.admin_locked_at,
    updated_at = now();

INSERT INTO revoked_tokens (jti, expires_at, revoked_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', now() + interval '1 day', now() - interval '2 hours'),
    ('00000000-0000-0000-0000-000000000102', now() - interval '1 day', now() - interval '3 days')
ON CONFLICT (jti) DO UPDATE SET
    expires_at = EXCLUDED.expires_at,
    revoked_at = EXCLUDED.revoked_at;

DELETE FROM patient_login_history
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');

INSERT INTO patient_login_history
    (patient_id, attempted_at, outcome, failure_reason, ip_address, user_agent, device_label)
SELECT id, now() - interval '2 hours', 'SUCCESS', NULL, '192.168.1.20',
       'Mozilla/5.0 (Linux; Android 15) Chrome/140.0 Mobile', 'Chrome trên Android'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '4 days', 'FAILURE', 'BAD_CREDENTIALS', '192.168.1.20',
       'Mozilla/5.0 (Linux; Android 15) Chrome/140.0 Mobile', 'Chrome trên Android'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '1 day', 'BLOCKED', 'ADMIN_LOCKED', '192.168.1.30',
       'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) Mobile', 'Safari trên iPhone'
FROM patients WHERE email = 'patient02.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '30 minutes', 'BLOCKED', 'ACCOUNT_LOCKED', '10.0.0.44',
       'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/140.0', 'Chrome trên Windows'
FROM patients WHERE email = 'patient04.locked.test@gastroai.vn';

INSERT INTO medical_profiles
    (patient_id, medical_history, allergies, current_medications,
     date_of_birth, gender, height_cm, weight_kg, chronic_conditions,
     past_surgeries, dietary_restrictions, version, created_at, updated_at)
SELECT id,
       'Trào ngược dạ dày tái phát khoảng 2 năm, triệu chứng tăng sau bữa tối.',
       '["Hải sản có vỏ"]',
       '["Omeprazole 20 mg khi có chỉ định"]',
       DATE '1991-05-16', 'MALE', 170, 68,
       '["GERD"]', '[]', '["Hạn chế thức ăn cay", "Không dung nạp lactose nhẹ"]',
       0, now() - interval '170 days', now()
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id,
       'Hội chứng ruột kích thích thể táo bón, theo dõi ngoại trú.',
       '[]',
       '["Psyllium 5 g mỗi ngày"]',
       DATE '1986-11-02', 'FEMALE', 158, 54,
       '["IRRITABLE_BOWEL_SYNDROME"]', '["Mổ ruột thừa năm 2010"]', '["Chế độ Low-FODMAP có hướng dẫn"]',
       0, now() - interval '140 days', now()
FROM patients WHERE email = 'patient02.test@gastroai.vn'
UNION ALL
SELECT id,
       'Chưa ghi nhận bệnh tiêu hóa mạn tính.',
       '["Penicillin"]',
       '[]',
       DATE '2000-03-24', 'MALE', 175, 72,
       '[]', '[]', '[]',
       0, now() - interval '2 days', now()
FROM patients WHERE email = 'patient03.unverified.test@gastroai.vn'
ON CONFLICT (patient_id) DO UPDATE SET
    medical_history = EXCLUDED.medical_history,
    allergies = EXCLUDED.allergies,
    current_medications = EXCLUDED.current_medications,
    date_of_birth = EXCLUDED.date_of_birth,
    gender = EXCLUDED.gender,
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    chronic_conditions = EXCLUDED.chronic_conditions,
    past_surgeries = EXCLUDED.past_surgeries,
    dietary_restrictions = EXCLUDED.dietary_restrictions,
    version = 0,
    updated_at = now();

DELETE FROM symptom_assessments
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');
DELETE FROM triage_alerts
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');
DELETE FROM chat_sessions
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');
DELETE FROM medication_reminders
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');
DELETE FROM food_diary_entries
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');
DELETE FROM bristol_logs
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn');

INSERT INTO food_diary_entries
    (patient_id, eaten_at, description, notes, meal_type, symptoms_after_meal,
     symptom_onset_minutes, deleted_at, version, created_at, updated_at)
SELECT id, now() - interval '6 hours', 'Cháo yến mạch, chuối và sữa không lactose',
       'Ăn chậm, khẩu phần vừa.', 'BREAKFAST', NULL, NULL, NULL::timestamptz, 0,
       now() - interval '6 hours', now() - interval '6 hours'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '1 day 5 hours', 'Cơm gạo lứt, cá hấp và rau luộc',
       'Không dùng nước chấm cay.', 'LUNCH', 'Đầy hơi nhẹ', 45, NULL, 0,
       now() - interval '1 day 4 hours', now() - interval '1 day 4 hours'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '2 days 2 hours', 'Phở bò ít béo',
       'Ăn tối muộn hơn thường lệ.', 'DINNER', 'Ợ nóng', 30, NULL, 0,
       now() - interval '2 days 1 hour', now() - interval '2 days 1 hour'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '8 days', 'Cà phê sữa và bánh ngọt',
       '[SEED] Bản ghi mẫu đang ở thùng rác.', 'SNACK', 'Đau thượng vị', 20,
       now() - interval '2 days', 0, now() - interval '8 days', now() - interval '2 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '7 hours', 'Bún cá và rau xanh',
       'Khẩu phần nhỏ.', 'LUNCH', 'Chướng bụng', 60, NULL, 0,
       now() - interval '6 hours', now() - interval '6 hours'
FROM patients WHERE email = 'patient02.test@gastroai.vn';

INSERT INTO bristol_logs
    (patient_id, logged_at, bristol_type, notes, deleted_at, version, created_at, updated_at)
SELECT id, now() - interval '1 day', 4, 'Bình thường, không đau bụng.', NULL::timestamptz, 0, now() - interval '1 day', now() - interval '1 day'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '3 days', 6, 'Phân lỏng sau bữa ăn nhiều dầu mỡ.', NULL, 0, now() - interval '3 days', now() - interval '3 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '10 days', 2, '[SEED] Bản ghi Bristol trong thùng rác.', now() - interval '2 days', 0, now() - interval '10 days', now() - interval '2 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '2 days', 2, 'Phân cứng, phải rặn.', NULL, 0, now() - interval '2 days', now() - interval '2 days'
FROM patients WHERE email = 'patient02.test@gastroai.vn'
UNION ALL
SELECT id, now() - interval '5 days', 1, 'Táo bón rõ.', NULL, 0, now() - interval '5 days', now() - interval '5 days'
FROM patients WHERE email = 'patient02.test@gastroai.vn';

INSERT INTO medication_reminders
    (patient_id, medicine_name, dosage, active, start_date, end_date,
     instructions, version, created_at, updated_at)
SELECT id, 'Omeprazole', '20 mg', TRUE, current_date - 30, current_date + 30,
       'Uống trước bữa sáng 30 phút theo đơn bác sĩ.', 0, now() - interval '30 days', now()
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, 'Men vi sinh', '1 gói', TRUE, current_date - 14, NULL,
       'Uống sau bữa ăn, bảo quản theo hướng dẫn.', 0, now() - interval '14 days', now()
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, 'Psyllium', '5 g', FALSE, current_date - 60, current_date - 5,
       'Pha với nhiều nước; lịch mẫu đã kết thúc.', 0, now() - interval '60 days', now()
FROM patients WHERE email = 'patient02.test@gastroai.vn';

INSERT INTO medication_reminder_times (reminder_id, time_of_day, sort_order)
SELECT r.id, TIME '07:00', 0 FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Omeprazole'
UNION ALL
SELECT r.id, TIME '08:00', 0 FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Men vi sinh'
UNION ALL
SELECT r.id, TIME '20:00', 1 FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Men vi sinh'
UNION ALL
SELECT r.id, TIME '21:00', 0 FROM medication_reminders r
JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient02.test@gastroai.vn' AND r.medicine_name = 'Psyllium';

INSERT INTO medication_confirmations
    (reminder_id, confirmed_at, confirmation_date, scheduled_time)
SELECT r.id, (current_date - 1) + TIME '07:05', current_date - 1, TIME '07:00'
FROM medication_reminders r JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Omeprazole'
UNION ALL
SELECT r.id, (current_date - 2) + TIME '07:10', current_date - 2, TIME '07:00'
FROM medication_reminders r JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Omeprazole'
UNION ALL
SELECT r.id, (current_date - 1) + TIME '20:12', current_date - 1, TIME '20:00'
FROM medication_reminders r JOIN patients p ON p.id = r.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND r.medicine_name = 'Men vi sinh';

DELETE FROM documents WHERE source LIKE 'seed://%';

WITH root_document AS (
    INSERT INTO documents
        (title, source, status, error_message, full_text, processing_stage, source_url,
         content_hash, uploaded_at, updated_at, root_id, version_no, is_current, unpublished_at)
    VALUES
        ('[SEED] Hướng dẫn theo dõi trào ngược tại nhà - phiên bản 1',
         'seed://rag/gerd-guide-v1.pdf', 'DONE', NULL,
         'Tài liệu hướng dẫn ghi nhận bữa ăn, triệu chứng ợ nóng và các dấu hiệu cần đi khám.',
         'DONE', 'https://example.test/gastroai/gerd-guide-v1',
         repeat('1', 64), now() - interval '90 days', now() - interval '60 days',
         NULL, 1, FALSE, NULL)
    RETURNING id
)
INSERT INTO documents
    (title, source, status, error_message, full_text, processing_stage, source_url,
     content_hash, uploaded_at, updated_at, root_id, version_no, is_current, unpublished_at)
SELECT '[SEED] Hướng dẫn theo dõi trào ngược tại nhà - phiên bản 2',
       'seed://rag/gerd-guide-v2.pdf', 'DONE', NULL,
       'Theo dõi thời điểm ăn, tư thế nằm, mức độ ợ nóng và đáp ứng với kế hoạch điều trị. Gọi 115 khi đau ngực dữ dội hoặc khó thở.',
       'DONE', 'https://example.test/gastroai/gerd-guide-v2',
       repeat('2', 64), now() - interval '60 days', now() - interval '55 days',
       id, 2, TRUE, NULL
FROM root_document;

INSERT INTO documents
    (title, source, status, error_message, full_text, processing_stage, source_url,
     content_hash, uploaded_at, updated_at, root_id, version_no, is_current, unpublished_at)
VALUES
    ('[SEED] Tài liệu lỗi xử lý', 'seed://rag/broken-scan.pdf', 'ERROR',
     'Không thể trích xuất văn bản từ tệp ảnh bị hỏng.', NULL, 'ERROR', NULL,
     repeat('3', 64), now() - interval '3 days', now() - interval '3 days',
     NULL, 1, FALSE, NULL),
    ('[SEED] Tài liệu đã gỡ xuất bản', 'seed://rag/unpublished-diet.pdf', 'DONE',
     NULL, 'Hướng dẫn khẩu phần nhỏ và nhật ký thực phẩm cho người bệnh tiêu hóa.',
     'DONE', 'https://example.test/gastroai/unpublished-diet',
     repeat('4', 64), now() - interval '120 days', now() - interval '20 days',
     NULL, 1, FALSE, now() - interval '20 days');

INSERT INTO chunks (document_id, chunk_index, content, token_count, created_at)
SELECT id, 0,
       'Trào ngược có thể tăng sau bữa ăn lớn hoặc khi nằm ngay sau ăn. Hãy ghi nhật ký để nhận biết yếu tố khởi phát.',
       34, now() - interval '55 days'
FROM documents WHERE source = 'seed://rag/gerd-guide-v2.pdf'
UNION ALL
SELECT id, 1,
       'Đau ngực dữ dội, khó thở, nôn ra máu hoặc phân đen là dấu hiệu cần được đánh giá cấp cứu.',
       31, now() - interval '55 days'
FROM documents WHERE source = 'seed://rag/gerd-guide-v2.pdf'
UNION ALL
SELECT id, 0,
       'Khẩu phần nhỏ, đủ nước và theo dõi phản ứng cá nhân với thực phẩm có thể hỗ trợ trao đổi cùng bác sĩ.',
       30, now() - interval '100 days'
FROM documents WHERE source = 'seed://rag/unpublished-diet.pdf';

INSERT INTO embeddings (chunk_id, embedding, model, created_at)
SELECT id,
       ('[' || array_to_string(array_fill(0.001::numeric, ARRAY[3072]), ',') || ']')::vector,
       'gemini-embedding-2', now() - interval '55 days'
FROM chunks
WHERE document_id = (SELECT id FROM documents WHERE source = 'seed://rag/gerd-guide-v2.pdf')
UNION ALL
SELECT id,
       ('[' || array_to_string(array_fill(0.002::numeric, ARRAY[3072]), ',') || ']')::vector,
       'gemini-embedding-2', now() - interval '100 days'
FROM chunks
WHERE document_id = (SELECT id FROM documents WHERE source = 'seed://rag/unpublished-diet.pdf');

INSERT INTO chat_sessions (patient_id, title, created_at, updated_at)
SELECT id, '[SEED] Theo dõi trào ngược sau bữa tối', now() - interval '5 days', now() - interval '4 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, '[SEED] Đau bụng và phân đen cần cấp cứu', now() - interval '2 days', now() - interval '2 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, '[SEED] Tư vấn táo bón kéo dài', now() - interval '7 days', now() - interval '6 days'
FROM patients WHERE email = 'patient02.test@gastroai.vn';

INSERT INTO chat_messages
    (session_id, sender, content, sources, related_questions, emergency, matched_groups, created_at)
SELECT id, 'patient', 'Tôi thường bị ợ nóng sau bữa tối, nên theo dõi thế nào?', NULL, NULL, FALSE, NULL, created_at + interval '1 minute'
FROM chat_sessions WHERE title = '[SEED] Theo dõi trào ngược sau bữa tối'
UNION ALL
SELECT id, 'assistant',
       'Bạn có thể ghi lại giờ ăn, món ăn, thời điểm nằm và mức độ ợ nóng. Tránh tự thay đổi thuốc đang được kê.',
       '[{"documentTitle":"[SEED] Hướng dẫn theo dõi trào ngược tại nhà - phiên bản 2","snippet":"Ghi nhật ký để nhận biết yếu tố khởi phát."}]',
       '["Dấu hiệu nào cần đi khám sớm?","Tôi nên ghi nhật ký ăn uống ra sao?"]',
       FALSE, NULL, created_at + interval '2 minutes'
FROM chat_sessions WHERE title = '[SEED] Theo dõi trào ngược sau bữa tối'
UNION ALL
SELECT id, 'patient', 'Tôi gửi ghi chú triệu chứng kèm theo.', NULL, NULL, FALSE, NULL, created_at + interval '3 minutes'
FROM chat_sessions WHERE title = '[SEED] Theo dõi trào ngược sau bữa tối'
UNION ALL
SELECT id, 'patient', 'Tôi đau bụng dữ dội và đi ngoài phân đen từ sáng nay.', NULL, NULL, TRUE,
       '["XUAT_HUYET_TIEU_HOA","DAU_BUNG_CAP_TINH"]', created_at + interval '1 minute'
FROM chat_sessions WHERE title = '[SEED] Đau bụng và phân đen cần cấp cứu'
UNION ALL
SELECT id, 'assistant',
       'Đây là dấu hiệu cảnh báo khẩn cấp. Hãy gọi 115 hoặc đến cơ sở cấp cứu ngay, không tự lái xe nếu đang choáng.',
       NULL, '[]', TRUE, '["XUAT_HUYET_TIEU_HOA","DAU_BUNG_CAP_TINH"]', created_at + interval '2 minutes'
FROM chat_sessions WHERE title = '[SEED] Đau bụng và phân đen cần cấp cứu'
UNION ALL
SELECT id, 'patient', 'Tôi bị táo bón hơn một tuần và thường xuyên đầy bụng.', NULL, NULL, FALSE, NULL, created_at + interval '1 minute'
FROM chat_sessions WHERE title = '[SEED] Tư vấn táo bón kéo dài'
UNION ALL
SELECT id, 'assistant',
       'Triệu chứng kéo dài nên được bác sĩ đánh giá. Trong lúc chờ khám, hãy uống đủ nước và tiếp tục ghi nhật ký Bristol.',
       NULL, '["Khi nào táo bón cần cấp cứu?"]', FALSE, NULL, created_at + interval '2 minutes'
FROM chat_sessions WHERE title = '[SEED] Tư vấn táo bón kéo dài';

INSERT INTO message_ratings (message_id, rating, rated_at)
SELECT id, 'HELPFUL', created_at + interval '1 hour'
FROM chat_messages
WHERE sender = 'assistant' AND content LIKE 'Bạn có thể ghi lại giờ ăn%'
UNION ALL
SELECT id, 'UNHELPFUL', created_at + interval '2 hours'
FROM chat_messages
WHERE sender = 'assistant' AND content LIKE 'Triệu chứng kéo dài nên được bác sĩ đánh giá%';

INSERT INTO chat_attachments
    (message_id, stored_name, original_filename, content_type, size_bytes, created_at)
SELECT id, 'seed-chat-note.txt', 'ghi-chu-trieu-chung.txt', 'text/plain', __ATTACHMENT_SIZE__, created_at
FROM chat_messages
WHERE sender = 'patient' AND content = 'Tôi gửi ghi chú triệu chứng kèm theo.';

INSERT INTO symptom_assessments
    (patient_id, primary_symptom, primary_symptom_detail, duration, reported_severity,
     activity_impact, progression, patient_group, warning_signs, severity_level,
     emergency, matched_groups, reason_codes, requires_clinician_review,
     care_recommendation, assessed_at)
SELECT id, 'HEARTBURN', 'Ợ nóng sau bữa tối, giảm khi ngồi thẳng.',
       'ONE_TO_THREE_DAYS', 'MILD', 'NONE', 'STABLE', 'ADULT', '[]', 'MILD',
       FALSE, '[]', '["MILD_NO_RED_FLAGS"]', FALSE, 'SELF_CARE_AT_HOME', now() - interval '5 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT id, 'CONSTIPATION', 'Táo bón kéo dài hơn một tuần.',
       'MORE_THAN_THREE_DAYS', 'MODERATE', 'SOME_LIMITATION', 'STABLE', 'ADULT', '[]', 'MODERATE',
       FALSE, '[]', '["DURATION_OVER_THREE_DAYS"]', TRUE, 'SEE_DOCTOR', now() - interval '7 days'
FROM patients WHERE email = 'patient02.test@gastroai.vn'
UNION ALL
SELECT id, 'ABDOMINAL_PAIN', 'Đau bụng dữ dội kèm phân đen.',
       'LESS_THAN_24_HOURS', 'SEVERE', 'PREVENTS_NORMAL_ACTIVITY', 'WORSENING', 'ADULT',
       '["BLACK_OR_BLOODY_STOOL","SUDDEN_SEVERE_ABDOMINAL_PAIN"]', 'SEVERE',
       TRUE, '["XUAT_HUYET_TIEU_HOA","DAU_BUNG_CAP_TINH"]',
       '["BLACK_OR_BLOODY_STOOL","SUDDEN_SEVERE_ABDOMINAL_PAIN"]', TRUE,
       'EMERGENCY_CARE', now() - interval '2 days'
FROM patients WHERE email = 'patient01.test@gastroai.vn';

INSERT INTO triage_alerts
    (patient_id, session_id, message_id, message_content, matched_groups, status,
     claimed_by_type, claimed_by_id, claimed_at, resolved_at, occurred_at, updated_at,
     resolved_by_type, resolved_by_id)
SELECT p.id, s.id, m.id, m.content, m.matched_groups, 'NEW',
       NULL, NULL, NULL, NULL::timestamptz, m.created_at, now(), NULL, NULL::bigint
FROM patients p
JOIN chat_sessions s ON s.patient_id = p.id AND s.title = '[SEED] Đau bụng và phân đen cần cấp cứu'
JOIN chat_messages m ON m.session_id = s.id AND m.sender = 'patient'
WHERE p.email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT p.id, NULL, NULL, '[SEED] Bệnh nhân báo nôn nhiều và có dấu hiệu mất nước.',
       '["MAT_NUOC_NANG"]', 'IN_PROGRESS',
       'ADMIN', __SEED_ADMIN_ID__, now() - interval '3 hours', NULL,
       now() - interval '4 hours', now() - interval '3 hours', NULL, NULL
FROM patients p WHERE p.email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT p.id, NULL, NULL, '[SEED] Đau bụng đã được bác sĩ liên hệ và hướng dẫn đi khám.',
       '["DAU_BUNG_CAP_TINH"]', 'RESOLVED',
       'DOCTOR', __SEED_DOCTOR_ID__, now() - interval '3 days',
       now() - interval '2 days 20 hours', now() - interval '3 days 1 hour',
       now() - interval '2 days 20 hours', 'ADMIN', __SEED_ADMIN_ID__
FROM patients p WHERE p.email = 'patient02.test@gastroai.vn';

INSERT INTO catalog_items (type, code, name, sort_order, active, triage_code)
VALUES
    ('SYMPTOM', 'SEED_EARLY_SATIETY', '[SEED] Mau no sau khi ăn', 900, TRUE, NULL),
    ('CONDITION', 'SEED_FUNCTIONAL_DYSPEPSIA', '[SEED] Khó tiêu chức năng', 900, TRUE, NULL),
    ('MEDICATION', 'SEED_ALGINATE', '[SEED] Alginate', 900, FALSE, NULL)
ON CONFLICT (type, code) DO UPDATE SET
    name = EXCLUDED.name,
    sort_order = EXCLUDED.sort_order,
    active = EXCLUDED.active,
    triage_code = EXCLUDED.triage_code;

INSERT INTO article_topics (name, sort_order, created_at, updated_at)
VALUES
    ('[SEED] Dinh dưỡng thực hành', 900, now(), now()),
    ('[SEED] Theo dõi triệu chứng', 910, now(), now())
ON CONFLICT (name) DO UPDATE SET
    sort_order = EXCLUDED.sort_order,
    updated_at = now();

INSERT INTO knowledge_articles
    (slug, title, summary, category, reading_minutes, content, source_name, source_url,
     source_checked_at, published, sort_order, medical_reviewer, medical_reviewed_at,
     version, created_at, updated_at)
VALUES
    ('seed-nhat-ky-an-uong-va-trieu-chung',
     '[SEED] Cách ghi nhật ký ăn uống và triệu chứng',
     'Mẫu theo dõi thời điểm ăn, khẩu phần và phản ứng tiêu hóa để trao đổi với nhân viên y tế.',
     '[SEED] Theo dõi triệu chứng', 4,
     E'## Nội dung cần ghi\n\nGhi thời điểm ăn, thành phần chính, khẩu phần và triệu chứng xuất hiện sau ăn. Nhật ký hỗ trợ trao đổi, không tự xác định chẩn đoán.',
     'GastroAI Test Data', 'https://example.test/gastroai/food-diary', current_date - 5,
     TRUE, 900, 'BS. Lê Hoàng Nam', now() - interval '5 days', 0, now() - interval '10 days', now()),
    ('seed-khau-phan-nho-ban-nhap',
     '[SEED] Khẩu phần nhỏ cho người hay đầy bụng - bản nháp',
     'Bản nháp dùng để kiểm thử quy trình biên tập và duyệt y khoa.',
     '[SEED] Dinh dưỡng thực hành', 3,
     E'## Bản nháp\n\nNội dung đang chờ bác sĩ kiểm duyệt trước khi xuất bản.',
     'GastroAI Test Data', 'https://example.test/gastroai/draft-small-meals', current_date - 2,
     FALSE, 910, NULL, NULL, 0, now() - interval '2 days', now())
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    category = EXCLUDED.category,
    reading_minutes = EXCLUDED.reading_minutes,
    content = EXCLUDED.content,
    source_name = EXCLUDED.source_name,
    source_url = EXCLUDED.source_url,
    source_checked_at = EXCLUDED.source_checked_at,
    published = EXCLUDED.published,
    sort_order = EXCLUDED.sort_order,
    medical_reviewer = EXCLUDED.medical_reviewer,
    medical_reviewed_at = EXCLUDED.medical_reviewed_at,
    version = 0,
    updated_at = now();

DELETE FROM faqs WHERE question LIKE '[SEED]%';

INSERT INTO faqs
    (question, answer, category, sort_order, published, version,
     medical_reviewer, medical_reviewed_at, created_at, updated_at)
VALUES
    ('[SEED] Nhật ký ăn uống nên ghi những thông tin nào?',
     'Nên ghi giờ ăn, món ăn, khẩu phần, triệu chứng sau ăn và thời gian triệu chứng bắt đầu. Mang nhật ký khi đi khám để bác sĩ có thêm thông tin.',
     'Theo dõi sức khỏe', 900, TRUE, 0, 'BS. Lê Hoàng Nam', now() - interval '3 days',
     now() - interval '5 days', now()),
    ('[SEED] Tôi có nên tự loại bỏ hoàn toàn một nhóm thực phẩm?',
     'Đây là bản nháp để kiểm thử. Việc loại bỏ kéo dài một nhóm thực phẩm nên được trao đổi với nhân viên y tế hoặc chuyên gia dinh dưỡng.',
     'Dinh dưỡng', 910, FALSE, 0, NULL, NULL, now() - interval '2 days', now());

INSERT INTO triage_keywords (group_code, keyword, created_at, updated_at)
VALUES ('DAU_BUNG_CAP_TINH', 'dau bung cap seed test', now(), now())
ON CONFLICT (group_code, keyword) DO UPDATE SET updated_at = now();

DELETE FROM notifications WHERE dedupe_key LIKE 'seed:%';

INSERT INTO notifications
    (recipient_type, recipient_id, notification_type, title, message,
     source_type, source_id, action_url, dedupe_key, created_at)
SELECT 'PATIENT', p.id, 'JOURNAL_REMINDER', '[SEED] Nhắc ghi nhật ký hôm nay',
       'Bạn chưa ghi nhận bữa tối. Hãy bổ sung nếu thuận tiện.',
       'FOOD_DIARY', NULL, '/journal/food', 'seed:patient01:journal-reminder',
       now() - interval '6 hours'
FROM patients p WHERE p.email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT 'PATIENT', p.id, 'SYSTEM', '[SEED] Hồ sơ y tế đã được cập nhật',
       'Thông tin hồ sơ mẫu đã được lưu thành công.',
       'MEDICAL_PROFILE', mp.id, '/profile/medical', 'seed:patient01:profile-updated',
       now() - interval '2 days'
FROM patients p JOIN medical_profiles mp ON mp.patient_id = p.id
WHERE p.email = 'patient01.test@gastroai.vn'
UNION ALL
SELECT 'ADMIN', NULL, 'TRIAGE_ALERT', '[SEED] Có cảnh báo triage mới',
       'Bệnh nhân Nguyễn Văn An báo đau bụng dữ dội và phân đen.',
       'TRIAGE_ALERT', ta.id, '/cms/triage-alerts', 'seed:admin:triage-alert',
       ta.occurred_at
FROM triage_alerts ta
JOIN patients p ON p.id = ta.patient_id
WHERE p.email = 'patient01.test@gastroai.vn' AND ta.status = 'NEW';

INSERT INTO notification_states
    (notification_id, recipient_type, recipient_id, read_at, deleted_at)
SELECT n.id, 'PATIENT', p.id, NULL, NULL::timestamptz
FROM notifications n
JOIN patients p ON p.email = 'patient01.test@gastroai.vn'
WHERE n.dedupe_key = 'seed:patient01:journal-reminder'
UNION ALL
SELECT n.id, 'PATIENT', p.id, now() - interval '1 day', NULL
FROM notifications n
JOIN patients p ON p.email = 'patient01.test@gastroai.vn'
WHERE n.dedupe_key = 'seed:patient01:profile-updated'
UNION ALL
SELECT n.id, 'ADMIN', __SEED_ADMIN_ID__, NULL, NULL
FROM notifications n
WHERE n.dedupe_key = 'seed:admin:triage-alert';

INSERT INTO app_runtime_config
    (id, rag_top_k, rag_similarity_threshold, triage_realtime_alerts_enabled,
     updated_at, updated_by_type, updated_by_id)
VALUES (1, 5, NULL, TRUE, now(), NULL, NULL)
ON CONFLICT (id) DO NOTHING;

COMMIT;

SELECT 'patients' AS seed_table, COUNT(*) AS seed_rows
FROM patients WHERE email LIKE '%.test@gastroai.vn'
UNION ALL SELECT 'medical_profiles', COUNT(*) FROM medical_profiles
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'food_diary_entries', COUNT(*) FROM food_diary_entries
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'bristol_logs', COUNT(*) FROM bristol_logs
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'medication_reminders', COUNT(*) FROM medication_reminders
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'chat_sessions', COUNT(*) FROM chat_sessions
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'documents', COUNT(*) FROM documents WHERE source LIKE 'seed://%'
UNION ALL SELECT 'triage_alerts', COUNT(*) FROM triage_alerts
WHERE patient_id IN (SELECT id FROM patients WHERE email LIKE '%.test@gastroai.vn')
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications WHERE dedupe_key LIKE 'seed:%'
ORDER BY seed_table;
'@

try {
    $attachmentDirectory = Join-Path $scriptRoot 'be\storage\chat-attachments'
    $null = New-Item -ItemType Directory -Path $attachmentDirectory -Force
    $attachmentPath = Join-Path $attachmentDirectory 'seed-chat-note.txt'
    $attachmentContent = "GastroAI seed attachment`r`nTrieu chung: o nong sau bua toi.`r`nMuc do: 4/10.`r`n"
    [System.IO.File]::WriteAllText($attachmentPath, $attachmentContent, $utf8NoBom)
    $attachmentSize = (Get-Item -LiteralPath $attachmentPath).Length

    $adminId = '0'
    $doctorId = '0'

    if (-not $SkipMySql) {
        Write-Step 'Dang tim ket noi MySQL...'
        $mySqlRunner = Resolve-Runner -Kind MySql -PreferredContainer $MySqlContainer
        Write-Step ('MySQL se chay bang ' + $mySqlRunner.Type + $(if ($mySqlRunner.Container) { ' (' + $mySqlRunner.Container + ')' } else { '' }))

        $mySqlFile = Join-Path $tempDirectory 'seed-mysql.sql'
        [System.IO.File]::WriteAllText($mySqlFile, $mySqlSql, $utf8NoBom)
        Invoke-MySqlFile -Runner $mySqlRunner -SqlFile $mySqlFile

        $adminId = Invoke-MySqlScalar -Runner $mySqlRunner -Query "SELECT id FROM admins WHERE email = 'admin.test@gastroai.vn' LIMIT 1"
        $doctorId = Invoke-MySqlScalar -Runner $mySqlRunner -Query "SELECT id FROM doctors WHERE email = 'doctor.test@gastroai.vn' LIMIT 1"
        Write-Step ('Da seed MySQL. Admin ID=' + $adminId + ', Doctor ID=' + $doctorId)
    }
    else {
        Write-Warning 'Bo qua MySQL: cac lien ket CMS trong PostgreSQL se dung ID 0 de phuc vu du lieu mau.'
    }

    if (-not $SkipPostgres) {
        Write-Step 'Dang tim ket noi PostgreSQL...'
        $postgresRunner = Resolve-Runner -Kind Postgres -PreferredContainer $PostgresContainer
        Write-Step ('PostgreSQL se chay bang ' + $postgresRunner.Type + $(if ($postgresRunner.Container) { ' (' + $postgresRunner.Container + ')' } else { '' }))

        $finalPostgresSql = $postgresSql.Replace('__PASSWORD_HASH__', $passwordHash)
        $finalPostgresSql = $finalPostgresSql.Replace('__ATTACHMENT_SIZE__', [string]$attachmentSize)
        $finalPostgresSql = $finalPostgresSql.Replace('__SEED_ADMIN_ID__', [string]$adminId)
        $finalPostgresSql = $finalPostgresSql.Replace('__SEED_DOCTOR_ID__', [string]$doctorId)

        $postgresFile = Join-Path $tempDirectory 'seed-postgres.sql'
        [System.IO.File]::WriteAllText($postgresFile, $finalPostgresSql, $utf8NoBom)
        Invoke-PostgresFile -Runner $postgresRunner -SqlFile $postgresFile
        Write-Step 'Da seed PostgreSQL.'
    }

    Write-Host ''
    Write-Host 'Hoan tat seed du lieu GastroAI.' -ForegroundColor Green
    Write-Host 'Mat khau chung: Test@123456'
    Write-Host 'CMS Admin: admin.test@gastroai.vn (chon Admin)'
    Write-Host 'CMS Content Editor: editor.test@gastroai.vn (chon Admin)'
    Write-Host 'CMS Doctor: doctor.test@gastroai.vn (chon Bac si)'
    Write-Host 'Patient: patient01.test@gastroai.vn'
    Write-Host 'Tai khoan trang thai dac biet: patient02, patient03.unverified, patient04.locked, doctor.locked'
    Write-Host ('Tep dinh kem mau: ' + $attachmentPath)
}
finally {
    if (Test-Path -LiteralPath $tempDirectory) {
        Remove-Item -LiteralPath $tempDirectory -Recurse -Force
    }
}
