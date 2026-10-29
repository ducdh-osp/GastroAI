package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.chat.ChatMessage;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.DailyChatStatProjection;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Toàn bộ tin nhắn trong 1 phiên, sắp xếp theo thứ tự thời gian. */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    long countByCreatedAtGreaterThanEqual(Instant since);

    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') AS day,
                   count(*) AS totalMessages,
                   count(*) FILTER (WHERE emergency = true) AS emergencyMessages,
                   count(*) FILTER (WHERE sender = 'patient') AS patientMessages,
                   count(*) FILTER (WHERE sender = 'assistant') AS botMessages
            FROM chat_messages
            WHERE created_at >= :startDate AND created_at <= :endDate
            GROUP BY day
            ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyChatStatProjection> findDailyChatStats(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
