package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevokedTokenCleanupServiceTest {
    private final RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
    private final RevokedTokenCleanupService service = new RevokedTokenCleanupService(repository);

    @Test
    void deletesOnlyTokensExpiredBeforeNow() {
        when(repository.deleteExpiredBefore(any(Instant.class))).thenReturn(3);

        assertEquals(3L, service.cleanupExpiredTokens());
        verify(repository).deleteExpiredBefore(any(Instant.class));
    }
}
