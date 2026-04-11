package dev.darkblade.mbe.api.addon.crossref;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class CrossReferenceDeclaration<T> {
    private final String referenceId;
    private final Class<T> contractType;
    private final Function<CrossReferenceResolver, T> factory;
    private final List<CrossReferenceDependency> dependencies;

    private CrossReferenceDeclaration(
        String referenceId,
        Class<T> contractType,
        Function<CrossReferenceResolver, T> factory,
        List<CrossReferenceDependency> dependencies
    ) {
        this.referenceId = normalizeReferenceId(referenceId);
        this.contractType = Objects.requireNonNull(contractType, "contractType");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    }

    public static <T> Builder<T> builder(String referenceId, Class<T> contractType, Function<CrossReferenceResolver, T> factory) {
        return new Builder<>(referenceId, contractType, factory);
    }

    public String referenceId() {
        return referenceId;
    }

    public Class<T> contractType() {
        return contractType;
    }

    public Function<CrossReferenceResolver, T> factory() {
        return factory;
    }

    public List<CrossReferenceDependency> dependencies() {
        return dependencies;
    }

    public static final class Builder<T> {
        private final String referenceId;
        private final Class<T> contractType;
        private final Function<CrossReferenceResolver, T> factory;
        private final List<CrossReferenceDependency> dependencies = new ArrayList<>();

        private Builder(String referenceId, Class<T> contractType, Function<CrossReferenceResolver, T> factory) {
            this.referenceId = normalizeReferenceId(referenceId);
            this.contractType = Objects.requireNonNull(contractType, "contractType");
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        public Builder<T> dependsOnRequiredEager(String dependencyReferenceId) {
            dependencies.add(CrossReferenceDependency.requiredEager(dependencyReferenceId));
            return this;
        }

        public Builder<T> dependsOnRequiredLazy(String dependencyReferenceId) {
            dependencies.add(CrossReferenceDependency.requiredLazy(dependencyReferenceId));
            return this;
        }

        public Builder<T> dependsOnOptionalLazy(String dependencyReferenceId) {
            dependencies.add(CrossReferenceDependency.optionalLazy(dependencyReferenceId));
            return this;
        }

        public Builder<T> dependsOn(CrossReferenceDependency dependency) {
            dependencies.add(Objects.requireNonNull(dependency, "dependency"));
            return this;
        }

        public CrossReferenceDeclaration<T> build() {
            return new CrossReferenceDeclaration<>(referenceId, contractType, factory, dependencies);
        }
    }

    private static String normalizeReferenceId(String referenceId) {
        Objects.requireNonNull(referenceId, "referenceId");
        String normalized = referenceId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("referenceId");
        }
        return normalized;
    }
}
