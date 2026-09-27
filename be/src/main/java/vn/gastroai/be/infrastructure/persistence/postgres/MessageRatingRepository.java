package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.chat.MessageRating;

import java.util.Optional;

public interface MessageRatingRepository extends JpaRepository<MessageRating, Long> {

    /** Tìm đánh giá hiện có của 1 tin nhắn (để UPSERT khi đổi ý). */
    Optional<MessageRating> findByMessageId(Long messageId);
}
