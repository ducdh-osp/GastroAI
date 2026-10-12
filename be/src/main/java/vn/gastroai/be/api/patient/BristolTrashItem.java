package vn.gastroai.be.api.patient;

import java.time.Instant;

public record BristolTrashItem(
        BristolLogResponse entry,
        Instant deletedAt,
        Instant purgeAt
) {
}