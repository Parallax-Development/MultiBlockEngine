package dev.darkblade.mbe.api.metadata;

import java.util.function.Function;
import java.util.function.Predicate;

public interface MetadataKey<T> {

    String id();

    Class<T> type();

    MetadataAccess apiAccess();

    MetadataAccess papiAccess();

    Function<T, String> formatter();

    Predicate<MetadataContext> visibility();
}
