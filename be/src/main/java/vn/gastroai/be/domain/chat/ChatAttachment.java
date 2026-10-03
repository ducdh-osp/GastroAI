package vn.gastroai.be.domain.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 1 file/ảnh bệnh nhân đính kèm khi gửi tin nhắn chat — luôn gắn với 1 ChatMessage có
 * sender='patient'. storedName là tên file vật lý trên đĩa (xem ChatAttachmentStorage),
 * originalFilename chỉ dùng để hiển thị lại cho người dùng, không dùng để đọc file.
 */
@Entity
@Table(name = "chat_attachments")
@Getter
@Setter
@NoArgsConstructor
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(name = "stored_name", nullable = false, unique = true, length = 255)
    private String storedName;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChatAttachment(ChatMessage message, String storedName, String originalFilename,
                           String contentType, long sizeBytes) {
        this.message = message;
        this.storedName = storedName;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }
}
