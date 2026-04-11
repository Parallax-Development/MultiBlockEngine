package dev.darkblade.mbe.api.metadata;

public interface ComputedMetadataKey<T> extends MetadataKey<T> {

    T compute(MetadataContext context);
}
