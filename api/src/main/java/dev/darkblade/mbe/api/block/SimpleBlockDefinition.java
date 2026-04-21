package dev.darkblade.mbe.api.block;

import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.action.Action;

import java.util.List;
import java.util.Map;
import java.util.Collections;

public record SimpleBlockDefinition(
        BlockKey key,
        String version,
        DisplayNameConfig displayName,
        String assemblyTrigger,
        String blockMaterial,
        Map<String, Object> behaviorConfig,
        Map<String, Object> defaultVariables,
        Map<String, PortDefinition> ports,
        Map<String, Object> extensions,
        List<Action> onCreateActions,
        List<Action> onTickActions,
        List<Action> onInteractActions,
        List<Action> onBreakActions,
        int tickInterval,
        List<MultiblockType.CapabilityFactory> capabilityFactories
) implements BlockDefinition {

    public SimpleBlockDefinition {
        behaviorConfig = behaviorConfig != null ? Collections.unmodifiableMap(behaviorConfig) : Map.of();
        defaultVariables = defaultVariables != null ? Collections.unmodifiableMap(defaultVariables) : Map.of();
        ports = ports != null ? Collections.unmodifiableMap(ports) : Map.of();
        extensions = extensions != null ? Collections.unmodifiableMap(extensions) : Map.of();
        onCreateActions = onCreateActions != null ? Collections.unmodifiableList(onCreateActions) : List.of();
        onTickActions = onTickActions != null ? Collections.unmodifiableList(onTickActions) : List.of();
        onInteractActions = onInteractActions != null ? Collections.unmodifiableList(onInteractActions) : List.of();
        onBreakActions = onBreakActions != null ? Collections.unmodifiableList(onBreakActions) : List.of();
        capabilityFactories = capabilityFactories != null ? Collections.unmodifiableList(capabilityFactories) : List.of();
        if (tickInterval < 1) tickInterval = 20;
    }
}
