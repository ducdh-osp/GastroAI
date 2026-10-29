package vn.gastroai.be.infrastructure.persistence.postgres.projection;

public interface TriageStatusStatProjection {
    String getStatus();
    long getCount();
}
