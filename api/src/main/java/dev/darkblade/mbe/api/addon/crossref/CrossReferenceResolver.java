package dev.darkblade.mbe.api.addon.crossref;

import java.util.Optional;

public interface CrossReferenceResolver {
    <T> Optional<T> resolve(String referenceId, Class<T> type);

    <T> CrossReferenceHandle<T> handle(String referenceId, Class<T> type);
}
