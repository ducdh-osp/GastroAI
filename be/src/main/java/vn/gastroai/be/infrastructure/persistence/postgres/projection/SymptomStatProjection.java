package vn.gastroai.be.infrastructure.persistence.postgres.projection;

public interface SymptomStatProjection {
    String getSymptom();
    long getTotalCount();
    long getEmergencyCount();
}
