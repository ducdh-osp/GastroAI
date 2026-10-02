package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;

import java.time.Instant;

/** Deletes blacklist entries once their corresponding JWT has expired naturally. */
@Service
public class RevokedTokenCleanupService {
    private final RevokedTokenRepository revokedTokens;

    public RevokedTokenCleanupService(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Transactional("postgresTransactionManager")
    public long cleanupExpiredTokens() {
        return revokedTokens.deleteExpiredBefore(Instant.now());
    }
}
