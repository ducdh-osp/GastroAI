package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.notification.PatientNotificationSetting;

public interface PatientNotificationSettingRepository extends JpaRepository<PatientNotificationSetting, Long> {
}
