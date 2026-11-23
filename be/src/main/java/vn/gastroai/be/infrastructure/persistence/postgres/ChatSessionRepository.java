package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.chat.ChatSession;

import java.time.Instant;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /** Danh sách phiên chat của 1 bệnh nhân, phân trang, sắp xếp mới nhất trước. */
    Page<ChatSession> findByPatientId(Long patientId, Pageable pageable);

    @Query("""
            SELECT s FROM ChatSession s
            WHERE s.patient.id = :patientId
              AND (
                LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
                OR EXISTS (
                  SELECT 1 FROM ChatMessage m
                  WHERE m.session = s
                    AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
                )
              )
            """)
    Page<ChatSession> searchByPatientAndContent(
            @Param("patientId") Long patientId,
            @Param("q") String q,
            Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startDate, Instant endDate);
}
