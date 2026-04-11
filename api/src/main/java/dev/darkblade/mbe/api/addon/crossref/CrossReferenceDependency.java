package dev.darkblade.mbe.api.addon.crossref;

import java.util.Objects;

public record CrossReferenceDependency(
    String targetReferenceId,
    CrossReferenceMode mode,
    boolean required
) {
    public CrossReferenceDependency {
        targetReferenceId = normalizeReferenceId(targetReferenceId);
        mode = Objects.requireNonNull(mode, "mode");
    }

    public static CrossReferenceDependency requiredEager(String targetReferenceId) {
        return new CrossReferenceDependency(targetReferenceId, CrossReferenceMode.EAGER, true);
    }

    public static CrossReferenceDependency requiredLazy(String targetReferenceId) {
        return new CrossReferenceDependency(targetReferenceId, CrossReferenceMode.LAZY, true);
    }

    public static CrossReferenceDependency optionalLazy(String targetReferenceId) {
        return new CrossReferenceDependency(targetReferenceId, CrossReferenceMode.LAZY, false);
    }

    private static String normalizeReferenceId(String referenceId) {
        Objects.requireNonNull(referenceId, "referenceId");
        String normalized = referenceId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("referenceId");
        }
        return normalized;
    }
}
