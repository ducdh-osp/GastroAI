package vn.gastroai.be.api.cms.doctor;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.util.List;

/**
 * API cung cấp danh sách Bác sĩ cho các chức năng kiểm duyệt y khoa CMS (FAQ, Bài viết).
 */
@RestController
@RequestMapping("/api/v1/cms/doctors")
public class CmsDoctorController {

    private final DoctorRepository doctorRepository;

    public CmsDoctorController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public record CmsDoctorOptionResponse(Long id, String email, String fullName, String displayName) {}

    @GetMapping
    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public ResponseEntity<List<CmsDoctorOptionResponse>> listDoctors() {
        List<CmsDoctorOptionResponse> doctors = doctorRepository.findAll().stream()
                .map(doctor -> {
                    String name = doctor.getFullName() != null && !doctor.getFullName().isBlank()
                            ? doctor.getFullName().trim()
                            : doctor.getEmail();
                    String display = name.startsWith("BS") || name.startsWith("ThS") || name.startsWith("GS") || name.startsWith("PGS")
                            ? name
                            : "BS. " + name;
                    return new CmsDoctorOptionResponse(doctor.getId(), doctor.getEmail(), doctor.getFullName(), display);
                })
                .toList();
        return ResponseEntity.ok(doctors);
    }
}
