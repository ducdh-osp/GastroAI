package vn.gastroai.be.infrastructure.backup;


public record RestoreResult(
        boolean postgresOk,
        String postgresError,
        boolean mysqlOk,
        String mysqlError
) {
    public boolean allOk() {
        return postgresOk && mysqlOk;
    }
}