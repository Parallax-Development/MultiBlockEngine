package dev.darkblade.mbe.api.addon.crossref;

public record CrossReferenceMetrics(
    int declaredReferences,
    int initializedReferences,
    int failedReferences,
    long compileNanos,
    long initializeNanos
) {
    public long totalNanos() {
        return compileNanos + initializeNanos;
    }
}
