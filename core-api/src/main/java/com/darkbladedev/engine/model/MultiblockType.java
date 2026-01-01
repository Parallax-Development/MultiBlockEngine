package com.darkbladedev.engine.model;

import com.darkbladedev.engine.api.capability.Capability;
import com.darkbladedev.engine.model.action.Action;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;

public record MultiblockType(
    String id,
    String version,
    Vector controllerOffset,
    BlockMatcher controllerMatcher,
    List<PatternEntry> pattern,
    boolean persistent,
    Map<String, Object> behaviorConfig,
    Map<String, Object> defaultVariables,
    List<Action> onCreateActions,
    List<Action> onTickActions,
    List<Action> onInteractActions,
    List<Action> onBreakActions,
    DisplayNameConfig displayName,
    int tickInterval,
    List<CapabilityFactory> capabilityFactories
) {
    public record CapabilityFactory(String ownerId, Function<MultiblockInstance, Capability> factory) {}

    public MultiblockType {
        pattern = Collections.unmodifiableList(pattern);
        behaviorConfig = behaviorConfig != null ? Collections.unmodifiableMap(behaviorConfig) : Map.of();
        defaultVariables = defaultVariables != null ? Collections.unmodifiableMap(defaultVariables) : Map.of();
        onCreateActions = onCreateActions != null ? Collections.unmodifiableList(onCreateActions) : List.of();
        onTickActions = onTickActions != null ? Collections.unmodifiableList(onTickActions) : List.of();
        onInteractActions = onInteractActions != null ? Collections.unmodifiableList(onInteractActions) : List.of();
        onBreakActions = onBreakActions != null ? Collections.unmodifiableList(onBreakActions) : List.of();
        capabilityFactories = capabilityFactories != null ? Collections.unmodifiableList(capabilityFactories) : List.of();
        if (tickInterval < 1) tickInterval = 20; // Default 1 second
    }
    
    // Constructor for YAML parser (no capabilities)
    public MultiblockType(String id, String version, Vector controllerOffset, BlockMatcher controllerMatcher, List<PatternEntry> pattern, boolean persistent, Map<String, Object> behaviorConfig, Map<String, Object> defaultVariables, List<Action> onCreateActions, List<Action> onTickActions, List<Action> onInteractActions, List<Action> onBreakActions, DisplayNameConfig displayName, int tickInterval) {
        this(id, version, controllerOffset, controllerMatcher, pattern, persistent, behaviorConfig, defaultVariables, onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval, List.of());
    }
}
