package dev.darkblade.mbe.core.graph.dependency;

import java.util.Objects;

public record DependencyEdge(
    String targetId,
    boolean required
) {
    public DependencyEdge {
        targetId = Objects.requireNonNull(targetId, "targetId").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
