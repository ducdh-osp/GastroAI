package vn.gastroai.be.api.knowledge;

import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.knowledge.FaqPatientService;

import java.util.List;

/** UC0021 - API xem và tra cứu FAQ cho bệnh nhân. */
@RestController
@Validated
@RequestMapping("/api/v1/faqs")
public class FaqController {

    private final FaqPatientService faqPatientService;

    public FaqController(FaqPatientService faqPatientService) {
        this.faqPatientService = faqPatientService;
    }

    @GetMapping
    public ResponseEntity<List<FaqResponse>> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(faqPatientService.list(q, category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(faqPatientService.listCategories());
    }
}
