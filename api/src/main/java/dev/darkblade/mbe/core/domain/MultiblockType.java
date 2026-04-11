package dev.darkblade.mbe.core.domain;

import dev.darkblade.mbe.api.capability.Capability;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.core.domain.action.Action;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;

public record MultiblockType(
    String id,
    String version,
    String assemblyTrigger,
    Vector controllerOffset,
    BlockMatcher controllerMatcher,
    List<PatternEntry> pattern,
    boolean persistent,
    Map<String, Object> behaviorConfig,
    Map<String, Object> defaultVariables,
    Map<String, PortDefinition> ports,
    Map<String, Object> extensions,
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
        ports = ports != null ? Collections.unmodifiableMap(ports) : Map.of();
        extensions = extensions != null ? Collections.unmodifiableMap(extensions) : Map.of();
        onCreateActions = onCreateActions != null ? Collections.unmodifiableList(onCreateActions) : List.of();
        onTickActions = onTickActions != null ? Collections.unmodifiableList(onTickActions) : List.of();
        onInteractActions = onInteractActions != null ? Collections.unmodifiableList(onInteractActions) : List.of();
        onBreakActions = onBreakActions != null ? Collections.unmodifiableList(onBreakActions) : List.of();
        capabilityFactories = capabilityFactories != null ? Collections.unmodifiableList(capabilityFactories) : List.of();
        if (tickInterval < 1) tickInterval = 20; // Default 1 second
    }
    
    // Constructor for YAML parser (no capabilities)
    public MultiblockType(String id, String version, String assemblyTrigger, Vector controllerOffset, BlockMatcher controllerMatcher, List<PatternEntry> pattern, boolean persistent, Map<String, Object> behaviorConfig, Map<String, Object> defaultVariables, Map<String, PortDefinition> ports, Map<String, Object> extensions, List<Action> onCreateActions, List<Action> onTickActions, List<Action> onInteractActions, List<Action> onBreakActions, DisplayNameConfig displayName, int tickInterval) {
        this(id, version, assemblyTrigger, controllerOffset, controllerMatcher, pattern, persistent, behaviorConfig, defaultVariables, ports, extensions, onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval, List.of());
    }

    public MultiblockType(String id, String version, String assemblyTrigger, Vector controllerOffset, BlockMatcher controllerMatcher, List<PatternEntry> pattern, boolean persistent, Map<String, Object> behaviorConfig, Map<String, Object> defaultVariables, List<Action> onCreateActions, List<Action> onTickActions, List<Action> onInteractActions, List<Action> onBreakActions, DisplayNameConfig displayName, int tickInterval) {
        this(id, version, assemblyTrigger, controllerOffset, controllerMatcher, pattern, persistent, behaviorConfig, defaultVariables, Map.of(), Map.of(), onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval, List.of());
    }

    public MultiblockType(String id, String version, Vector controllerOffset, BlockMatcher controllerMatcher, List<PatternEntry> pattern, boolean persistent, Map<String, Object> behaviorConfig, Map<String, Object> defaultVariables, List<Action> onCreateActions, List<Action> onTickActions, List<Action> onInteractActions, List<Action> onBreakActions, DisplayNameConfig displayName, int tickInterval, List<CapabilityFactory> capabilityFactories) {
        this(id, version, AssemblyTriggerType.WRENCH_USE.id(), controllerOffset, controllerMatcher, pattern, persistent, behaviorConfig, defaultVariables, Map.of(), Map.of(), onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval, capabilityFactories);
    }

    public MultiblockType(String id, String version, Vector controllerOffset, BlockMatcher controllerMatcher, List<PatternEntry> pattern, boolean persistent, Map<String, Object> behaviorConfig, Map<String, Object> defaultVariables, List<Action> onCreateActions, List<Action> onTickActions, List<Action> onInteractActions, List<Action> onBreakActions, DisplayNameConfig displayName, int tickInterval) {
        this(id, version, AssemblyTriggerType.WRENCH_USE.id(), controllerOffset, controllerMatcher, pattern, persistent, behaviorConfig, defaultVariables, Map.of(), Map.of(), onCreateActions, onTickActions, onInteractActions, onBreakActions, displayName, tickInterval, List.of());
    }
}
