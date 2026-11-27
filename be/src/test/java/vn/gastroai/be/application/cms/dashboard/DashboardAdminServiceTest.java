package vn.gastroai.be.application.cms.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.cms.dashboard.DashboardStatsResponse;
import vn.gastroai.be.api.cms.dashboard.SymptomMetric;
import vn.gastroai.be.api.cms.dashboard.TriageGroupMetric;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatSessionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MessageRatingRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.SymptomAssessmentRecordRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.SymptomStatProjection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardAdminServiceTest {

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatSessionRepository chatSessionRepository = mock(ChatSessionRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final TriageAlertRepository triageAlertRepository = mock(TriageAlertRepository.class);
    private final SymptomAssessmentRecordRepository symptomAssessmentRecordRepository = mock(SymptomAssessmentRecordRepository.class);
    private final MessageRatingRepository messageRatingRepository = mock(MessageRatingRepository.class);

    private final DashboardAdminService service = new DashboardAdminService(
            chatMessageRepository,
            chatSessionRepository,
            patientRepository,
            triageAlertRepository,
            symptomAssessmentRecordRepository,
            messageRatingRepository,
            new ObjectMapper()
    );

    @BeforeEach
    void setupDefaults() {
        when(chatMessageRepository.findDailyChatStats(any(), any())).thenReturn(List.of());
        when(symptomAssessmentRecordRepository.findSymptomStats(any(), any())).thenReturn(List.of());
        when(triageAlertRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any()))
                .thenReturn(List.of());
        when(triageAlertRepository.findStatusStats(any(), any())).thenReturn(List.of());
    }

    @Test
    void getStatsRejectsOutOfRangeDays() {
        assertThrows(IllegalArgumentException.class, () -> service.getStats(0));
        assertThrows(IllegalArgumentException.class, () -> service.getStats(-5));
        assertThrows(IllegalArgumentException.class, () -> service.getStats(366));
    }

    @Test
    void getStatsMapsAllSymptomsAndAllTriageGroupsAndUsesHalfOpenInterval() {
        SymptomStatProjection otherSymptom = mock(SymptomStatProjection.class);
        when(otherSymptom.getSymptom()).thenReturn("OTHER");
        when(otherSymptom.getTotalCount()).thenReturn(4L);
        when(otherSymptom.getEmergencyCount()).thenReturn(1L);

        SymptomStatProjection vomitingSymptom = mock(SymptomStatProjection.class);
        when(vomitingSymptom.getSymptom()).thenReturn("VOMITING");
        when(vomitingSymptom.getTotalCount()).thenReturn(2L);
        when(vomitingSymptom.getEmergencyCount()).thenReturn(0L);

        when(symptomAssessmentRecordRepository.findSymptomStats(any(), any()))
                .thenReturn(List.of(otherSymptom, vomitingSymptom));

        TriageAlert alert = new TriageAlert();
        alert.setMatchedGroups("""
                ["TAC_RUOT","KHONG_THE_DI_TIEU","SOC_MAT_MAU","NHIEM_TRUNG_NANG",\
                "DAU_LAN_CO_QUAN_KHAC","TIEU_DUONG_KEM_NON","MAT_NUOC_NANG"]""");
        when(triageAlertRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any()))
                .thenReturn(List.of(alert));

        DashboardStatsResponse stats = service.getStats(7);

        Map<String, String> symptomLabels = stats.topSymptoms().stream()
                .collect(Collectors.toMap(SymptomMetric::symptomCode, SymptomMetric::symptomName));
        assertEquals("Khác", symptomLabels.get("OTHER"));
        assertEquals("Nôn", symptomLabels.get("VOMITING"));

        assertEquals(7, stats.triageGroups().size());
        for (TriageGroupMetric group : stats.triageGroups()) {
            assertNotEquals(group.groupCode(), group.groupName(),
                    "Thieu nhan tieng Viet cho nhom triage: " + group.groupCode());
        }

        verify(triageAlertRepository).findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any());
    }
}

