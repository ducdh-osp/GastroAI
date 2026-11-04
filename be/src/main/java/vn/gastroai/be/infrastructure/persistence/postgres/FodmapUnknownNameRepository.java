package vn.gastroai.be.infrastructure.persistence.postgres;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;

public interface FodmapUnknownNameRepository extends JpaRepository<FodmapUnknownName, Long> {

    Optional<FodmapUnknownName> findByNormalizedName(String normalizedName);

    List<FodmapUnknownName> findByDismissedFalseOrderByHitCountDescLastSeenAtDesc();

    void deleteByNormalizedName(String normalizedName);
}