package com.darkbladedev.engine.model;

import com.darkbladedev.engine.model.action.Action;
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
    Map<String, Object> behaviorConfig,
    List<Action> onCreateActions,
    List<Action> onTickActions,
    int tickInterval
) {
    public MultiblockType {
        pattern = Collections.unmodifiableList(pattern);
        behaviorConfig = behaviorConfig != null ? Collections.unmodifiableMap(behaviorConfig) : Map.of();
        onCreateActions = onCreateActions != null ? Collections.unmodifiableList(onCreateActions) : List.of();
        onTickActions = onTickActions != null ? Collections.unmodifiableList(onTickActions) : List.of();
        if (tickInterval < 1) tickInterval = 20; // Default 1 second
    }
}
