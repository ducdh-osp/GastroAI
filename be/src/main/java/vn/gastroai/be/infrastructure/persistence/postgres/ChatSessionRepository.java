package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.chat.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /** Danh sách phiên chat của 1 bệnh nhân, phân trang, sắp xếp mới nhất trước. */
    Page<ChatSession> findByPatientId(Long patientId, Pageable pageable);
}
