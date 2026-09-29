package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;

import java.time.Instant;
import java.util.List;

public interface FoodDiaryEntryRepository extends JpaRepository<FoodDiaryEntry, Long> {

    Page<FoodDiaryEntry> findByPatientIdOrderByEatenAtDesc(Long patientId, Pageable pageable);

    List<FoodDiaryEntry> findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(Long patientId, Instant from, Instant to);
}
