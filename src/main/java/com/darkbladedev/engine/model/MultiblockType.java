package com.darkbladedev.engine.model;

import org.bukkit.util.Vector;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public record MultiblockType(
    String id,
    String version,
    Vector controllerOffset,
    BlockMatcher controllerMatcher,
    List<PatternEntry> pattern,
    boolean persistent,
    Map<String, Object> behaviorConfig
) {
    public MultiblockType {
        pattern = Collections.unmodifiableList(pattern);
        behaviorConfig = behaviorConfig != null ? Collections.unmodifiableMap(behaviorConfig) : Map.of();
    }
}
