package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.config.AppRuntimeConfig;

public interface AppRuntimeConfigRepository extends JpaRepository<AppRuntimeConfig, Long> {
}
