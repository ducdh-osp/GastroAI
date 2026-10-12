package vn.gastroai.be.domain.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Đánh giá phản hồi AI — HELPFUL hoặc UNHELPFUL — sau mỗi câu trả lời.
 * Dùng làm dữ liệu cải thiện chất lượng RAG (UC đánh giá).
 * UNIQUE trên message_id: bệnh nhân chỉ có 1 đánh giá/câu trả lời, ChatHistoryService dùng
 * UPSERT để cho phép đổi ý (helpful → unhelpful hoặc ngược lại).
 */
@Entity
@Table(name = "message_ratings")
@Getter
@Setter
@NoArgsConstructor
public class MessageRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private ChatMessage message;

    /** HELPFUL | UNHELPFUL */
    @Column(nullable = false, length = 20)
    private String rating;

    @Column(name = "rated_at", nullable = false)
    private Instant ratedAt = Instant.now();

    public MessageRating(ChatMessage message, String rating) {
        this.message = message;
        this.rating = rating;
    }
}
