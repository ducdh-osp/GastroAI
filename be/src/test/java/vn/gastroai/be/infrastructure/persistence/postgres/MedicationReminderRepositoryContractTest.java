package vn.gastroai.be.infrastructure.persistence.postgres;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicationReminderRepositoryContractTest {

    @Test
    void activeReminderQueriesFetchPatientBeforeEntitiesAreDetached() throws NoSuchMethodException {
        assertFetchesPatient(MedicationReminderRepository.class.getMethod(
                "findActiveForDate", LocalDate.class));
        assertFetchesPatient(MedicationReminderRepository.class.getMethod(
                "findActiveForDateAndPatientId", Long.class, LocalDate.class));
    }

    private static void assertFetchesPatient(Method method) {
        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, () -> method.getName() + " must declare an explicit query");

        String normalizedQuery = query.value()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        assertTrue(normalizedQuery.contains("join fetch r.patient"),
                () -> method.getName()
                        + " must initialize patient because callers process reminders outside the repository transaction");
    }
}
