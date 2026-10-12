package vn.gastroai.be.api.patient;

import java.time.Instant;


public record FoodDiaryTrashItem(
        FoodDiaryEntryResponse entry,
        Instant deletedAt,
        Instant purgeAt
) {
}