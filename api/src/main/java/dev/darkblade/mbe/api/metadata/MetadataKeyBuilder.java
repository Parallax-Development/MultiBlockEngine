package dev.darkblade.mbe.api.metadata;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class MetadataKeyBuilder<T> {
    private final String id;
    private final Class<T> type;
    private MetadataAccess apiAccess = MetadataAccess.READ_WRITE;
    private MetadataAccess papiAccess = MetadataAccess.NONE;
    private Function<T, String> formatter = value -> value == null ? "" : String.valueOf(value);
    private Predicate<MetadataContext> visibility = context -> true;

    private MetadataKeyBuilder(String id, Class<T> type) {
        this.id = normalizeId(id);
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> MetadataKeyBuilder<T> of(String id, Class<T> type) {
        return new MetadataKeyBuilder<>(id, type);
    }

    public MetadataKeyBuilder<T> apiAccess(MetadataAccess apiAccess) {
        this.apiAccess = Objects.requireNonNull(apiAccess, "apiAccess");
        return this;
    }

    public MetadataKeyBuilder<T> papiAccess(MetadataAccess papiAccess) {
        this.papiAccess = Objects.requireNonNull(papiAccess, "papiAccess");
        return this;
    }

    public MetadataKeyBuilder<T> formatter(Function<T, String> formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        return this;
    }

    public MetadataKeyBuilder<T> visibility(Predicate<MetadataContext> visibility) {
        this.visibility = Objects.requireNonNull(visibility, "visibility");
        return this;
    }

    public MetadataKey<T> build() {
        return new SimpleMetadataKey<>(id, type, apiAccess, papiAccess, formatter, visibility);
    }

    public ComputedMetadataKey<T> computed(Function<MetadataContext, T> computer) {
        Objects.requireNonNull(computer, "computer");
        return new ComputedKey<>(id, type, apiAccess, papiAccess, formatter, visibility, computer);
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Metadata key id cannot be blank");
        }
        String out = id.trim().toLowerCase(Locale.ROOT);
        if (out.startsWith("mbe_")) {
            return out;
        }
        return "mbe_" + out;
    }

    private record SimpleMetadataKey<T>(
            String id,
            Class<T> type,
            MetadataAccess apiAccess,
            MetadataAccess papiAccess,
            Function<T, String> formatter,
            Predicate<MetadataContext> visibility
    ) implements MetadataKey<T> {
    }

    private record ComputedKey<T>(
            String id,
            Class<T> type,
            MetadataAccess apiAccess,
            MetadataAccess papiAccess,
            Function<T, String> formatter,
            Predicate<MetadataContext> visibility,
            Function<MetadataContext, T> computer
    ) implements ComputedMetadataKey<T> {
        @Override
        public T compute(MetadataContext context) {
            return computer.apply(context);
        }
    }
}
