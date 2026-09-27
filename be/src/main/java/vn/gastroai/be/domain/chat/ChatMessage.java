package vn.gastroai.be.domain.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 1 tin nhắn trong phiên chat — có thể là của patient hoặc assistant (AI).
 * sources và relatedQuestions lưu dạng JSON text (được serialize/deserialize ở service layer).
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    /** 'patient' hoặc 'assistant' — khớp với SenderType bên FE. */
    @Column(nullable = false, length = 20)
    private String sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** JSON array của SourceRef: [{documentTitle, snippet}]. Null khi sender='patient'. */
    @Column(columnDefinition = "TEXT")
    private String sources;

    /** JSON array of strings. Null khi sender='patient'. */
    @Column(name = "related_questions", columnDefinition = "TEXT")
    private String relatedQuestions;

    @Column(nullable = false)
    private boolean emergency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage(ChatSession session, String sender, String content,
                       String sources, String relatedQuestions, boolean emergency) {
        this.session = session;
        this.sender = sender;
        this.content = content;
        this.sources = sources;
        this.relatedQuestions = relatedQuestions;
        this.emergency = emergency;
    }
}
