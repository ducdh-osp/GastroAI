package vn.gastroai.be.infrastructure.persistence.postgres.projection;

public interface DailyChatStatProjection {
    String getDay();
    long getTotalMessages();
    long getEmergencyMessages();
    long getPatientMessages();
    long getBotMessages();
}
