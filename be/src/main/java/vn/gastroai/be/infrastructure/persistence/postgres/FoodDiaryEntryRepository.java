package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FoodDiaryEntryRepository extends JpaRepository<FoodDiaryEntry, Long> {

    Page<FoodDiaryEntry> findByPatientIdOrderByEatenAtDesc(Long patientId, Pageable pageable);

    List<FoodDiaryEntry> findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(Long patientId, Instant from, Instant to);

    /** Danh sach thung rac, moi nhat truoc, gioi han 200 dong de tranh tra ve qua nhieu. */
    @Query(value = "SELECT * FROM food_diary_entries " +
            "WHERE patient_id = :patientId AND deleted_at IS NOT NULL AND deleted_at >= :cutoff " +
            "ORDER BY deleted_at DESC LIMIT 200", nativeQuery = true)
    List<FoodDiaryEntry> findTrash(@Param("patientId") Long patientId, @Param("cutoff") Instant cutoff);

    /** Tim 1 dong DA XOA thuoc dung benh nhan - dung khi khoi phuc (findById se khong thay vi bi @SQLRestriction an). */
    @Query(value = "SELECT * FROM food_diary_entries " +
            "WHERE id = :id AND patient_id = :patientId AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<FoodDiaryEntry> findDeletedOwned(@Param("id") Long id, @Param("patientId") Long patientId);

    /** Xoa han cac dong da qua han giu trong thung rac. Bat buoc @Modifying vi day la cau DELETE. */
    @Modifying
    @Query(value = "DELETE FROM food_diary_entries " +
            "WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff", nativeQuery = true)
    int purgeDeletedBefore(@Param("cutoff") Instant cutoff);
}