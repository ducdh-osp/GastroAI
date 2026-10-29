package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.notification.BroadcastAnnouncement;

public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncement, Long> {

    Page<BroadcastAnnouncement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
