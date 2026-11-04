package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.triage.TriageAlertMessage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface TriageAlertMessageRepository extends JpaRepository<TriageAlertMessage, Long> {

    /** So tin theo tung canh bao, dung cho danh sach CMS (tranh N+1 truy van). */
    interface AlertMessageCount {
        Long getAlertId();
        long getTotal();
    }

    List<TriageAlertMessage> findByAlertIdOrderByCreatedAtAscIdAsc(Long alertId);

    @Query("SELECT count(m) FROM TriageAlertMessage m "
            + "WHERE m.alertId = :alertId AND m.senderType <> 'PATIENT'")
    long countStaffMessages(@Param("alertId") Long alertId);

    @Query("SELECT m.alertId AS alertId, count(m) AS total FROM TriageAlertMessage m "
            + "WHERE m.alertId IN :alertIds AND m.senderType <> 'PATIENT' GROUP BY m.alertId")
    List<AlertMessageCount> countStaffMessagesByAlert(@Param("alertIds") Collection<Long> alertIds);

    @Query("SELECT m.alertId AS alertId, count(m) AS total FROM TriageAlertMessage m "
            + "WHERE m.alertId IN :alertIds AND m.senderType = 'PATIENT' AND m.readAt IS NULL "
            + "GROUP BY m.alertId")
    List<AlertMessageCount> countUnreadPatientMessagesByAlert(@Param("alertIds") Collection<Long> alertIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlertMessage m SET m.readAt = :now "
            + "WHERE m.alertId = :alertId AND m.senderType = 'PATIENT' AND m.readAt IS NULL")
    int markPatientMessagesRead(@Param("alertId") Long alertId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE TriageAlertMessage m SET m.readAt = :now "
            + "WHERE m.alertId = :alertId AND m.senderType <> 'PATIENT' AND m.readAt IS NULL")
    int markStaffMessagesRead(@Param("alertId") Long alertId, @Param("now") Instant now);
}
