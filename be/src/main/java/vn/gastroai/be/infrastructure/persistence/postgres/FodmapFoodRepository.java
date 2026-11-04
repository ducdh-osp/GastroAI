package vn.gastroai.be.infrastructure.persistence.postgres;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.catalog.FodmapFood;

public interface FodmapFoodRepository extends JpaRepository<FodmapFood, Long> {

    List<FodmapFood> findAllByOrderByNameAsc();

    List<FodmapFood> findByActiveTrue();
}