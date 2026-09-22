package vn.gastroai.be.domain.auth;

/** Roles stored in PostgreSQL. ADMIN is stored only in the MySQL admin_accounts database. */
public enum UserRole {
    PATIENT,
    DOCTOR
}
