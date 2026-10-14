package vn.gastroai.be.application.config;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.config.AppRuntimeConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.AppRuntimeConfigRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppRuntimeConfigServiceTest {

    private final AppRuntimeConfigRepository repository = mock(AppRuntimeConfigRepository.class);
    private final AppRuntimeConfigService service = new AppRuntimeConfigService(repository);

    private static AppRuntimeConfig row(int topK, Double threshold, boolean realtimeAlerts) {
        AppRuntimeConfig config = new AppRuntimeConfig();
        config.setId(1L);
        config.setRagTopK(topK);
        config.setRagSimilarityThreshold(threshold);
        config.setTriageRealtimeAlertsEnabled(realtimeAlerts);
        return config;
    }

    @Test
    void getConfigReturnsSnapshotOfRow() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, null, true)));

        AppRuntimeConfigSnapshot snapshot = service.getConfig();

        assertEquals(5, snapshot.ragTopK());
        assertEquals(null, snapshot.ragSimilarityThreshold());
        assertTrue(snapshot.triageRealtimeAlertsEnabled());
    }

    @Test
    void getConfigFailsFastWhenSeedRowMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, service::getConfig);
    }

    @Test
    void updateConfigSavesNewValuesAndStampsWhoChangedIt() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, null, true)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppRuntimeConfigSnapshot snapshot = service.updateConfig(
                new UpdateAppRuntimeConfigRequest(10, 0.8, false), "ADMIN", 3L);

        assertEquals(10, snapshot.ragTopK());
        assertEquals(0.8, snapshot.ragSimilarityThreshold());
        assertEquals(false, snapshot.triageRealtimeAlertsEnabled());
        assertEquals("ADMIN", snapshot.updatedByType());
        assertEquals(3L, snapshot.updatedById());
    }

    @Test
    void updateConfigRejectsTopKOutOfRange() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, null, true)));

        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(0, null, true), "ADMIN", 3L));
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(21, null, true), "ADMIN", 3L));
    }

    @Test
    void updateConfigRejectsSimilarityThresholdOutOfRange() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, null, true)));

        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(5, -0.1, true), "ADMIN", 3L));
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(5, 2.1, true), "ADMIN", 3L));
    }

    @Test
    void updateConfigRejectsMissingRequiredFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, null, true)));

        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(null, null, true), "ADMIN", 3L));
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(
                new UpdateAppRuntimeConfigRequest(5, null, null), "ADMIN", 3L));
    }

    @Test
    void updateConfigAllowsNullThresholdToDisableSimilarityFiltering() {
        when(repository.findById(1L)).thenReturn(Optional.of(row(5, 0.5, true)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppRuntimeConfigSnapshot snapshot = service.updateConfig(
                new UpdateAppRuntimeConfigRequest(5, null, true), "ADMIN", 3L);

        assertEquals(null, snapshot.ragSimilarityThreshold());
    }
}
