package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.api.addon.Version;

import java.util.List;
import java.util.Map;

public record AddonMetadata(
    String id,
    Version version,
    int api,
    String mainClass,
    Map<String, Version> requiredDependencies,
    Map<String, Version> optionalDependencies,
    List<String> dependsIds,
    String name,
    String description,
    List<String> authors,
    String website,
    Environment environment,
    List<String> capabilities,
    List<String> loadBefore,
    List<String> loadAfter
) {
    public record Environment(
        Version minecraft,
        Version java,
        Map<String, Version> plugins
    ) {}
}

