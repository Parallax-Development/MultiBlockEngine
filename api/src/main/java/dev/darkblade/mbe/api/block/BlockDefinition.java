package dev.darkblade.mbe.api.block;

import dev.darkblade.mbe.api.capability.Capability;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;

import java.util.List;
import java.util.Map;

public interface BlockDefinition {
    BlockKey key();

    String version();

    DisplayNameConfig displayName();

    String assemblyTrigger();

    String blockMaterial();
    
    Map<String, Object> behaviorConfig();

    Map<String, Object> defaultVariables();

    Map<String, PortDefinition> ports();

    Map<String, Object> extensions();

    List<Action> onCreateActions();

    List<Action> onTickActions();

    List<Action> onInteractActions();

    List<Action> onBreakActions();

    int tickInterval();

    List<MultiblockType.CapabilityFactory> capabilityFactories();
}
