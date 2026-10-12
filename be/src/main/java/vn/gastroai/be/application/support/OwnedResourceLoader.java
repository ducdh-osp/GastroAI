package vn.gastroai.be.application.support;

import java.util.Optional;
import java.util.function.Predicate;

/** Shared ownership check for patient-owned resources. */
public final class OwnedResourceLoader {

    private OwnedResourceLoader() {
    }

    public static <T> T loadOwned(Optional<T> resource,
                                  Predicate<T> belongsToPatient,
                                  String notFoundMessage) {
        return resource
                .filter(belongsToPatient)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }
}
