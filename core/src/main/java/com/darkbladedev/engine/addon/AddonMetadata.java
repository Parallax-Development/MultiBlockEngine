package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.addon.Version;

import java.util.List;
import java.util.Map;

public record AddonMetadata(
    String id,
    Version version,
    int api,
    String mainClass,
    Map<String, Version> requiredDependencies,
    Map<String, Version> optionalDependencies,
    List<String> dependsIds
) {
}

