package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.chat.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Toàn bộ tin nhắn trong 1 phiên, sắp xếp theo thứ tự thời gian. */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
