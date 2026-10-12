package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.chat.ChatAttachment;

import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    /** Lấy đính kèm của nhiều tin nhắn 1 lượt - dùng khi liệt kê cả phiên để tránh N+1 query. */
    List<ChatAttachment> findByMessageIdIn(List<Long> messageIds);

    Optional<ChatAttachment> findByIdAndMessageId(Long id, Long messageId);
}
