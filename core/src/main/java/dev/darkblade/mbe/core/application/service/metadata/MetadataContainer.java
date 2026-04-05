package dev.darkblade.mbe.core.application.service.metadata;

import dev.darkblade.mbe.api.metadata.MetadataKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MetadataContainer {
    private final Map<MetadataKey<?>, Object> values = new ConcurrentHashMap<>();

    Map<MetadataKey<?>, Object> values() {
        return values;
    }
}
