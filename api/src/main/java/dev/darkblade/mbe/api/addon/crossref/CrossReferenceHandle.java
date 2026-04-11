package dev.darkblade.mbe.api.addon.crossref;

import java.util.NoSuchElementException;
import java.util.Optional;

public interface CrossReferenceHandle<T> {
    Optional<T> resolve();

    default boolean isAvailable() {
        return resolve().isPresent();
    }

    default T require() {
        return resolve().orElseThrow(() -> new NoSuchElementException("Cross-reference value is not available"));
    }

    static <T> CrossReferenceHandle<T> unresolved() {
        return Optional::empty;
    }
}
