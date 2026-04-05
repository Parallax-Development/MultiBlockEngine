package dev.darkblade.mbe.api.metadata;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.jetbrains.annotations.Nullable;

public interface MetadataService extends MBEService {

    <T> void define(MetadataKey<T> key);

    <T> void set(MultiblockInstance instance, MetadataKey<T> key, T value);

    <T> @Nullable T get(MultiblockInstance instance, MetadataKey<T> key);

    @Nullable String resolveForPlaceholder(String id, MetadataContext context);
}
