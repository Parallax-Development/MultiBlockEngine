package dev.darkblade.mbe.api.block;

import dev.darkblade.mbe.api.capability.Capability;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.action.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BlockBuilder {
    private final BlockKey key;
    private final String defaultCapabilityOwnerId;
    private String version = "1.0";
    private int tickInterval = 20;
    private String assemblyTrigger = "auto";
    private String blockMaterial = "STONE";
    private DisplayNameConfig displayName;
    
    private final Map<String, Object> behaviorConfig = new HashMap<>();
    private final Map<String, Object> defaultVariables = new HashMap<>();
    private final Map<String, PortDefinition> ports = new HashMap<>();
    private final Map<String, Object> extensions = new HashMap<>();

    private final List<Action> onCreateActions = new ArrayList<>();
    private final List<Action> onTickActions = new ArrayList<>();
    private final List<Action> onInteractActions = new ArrayList<>();
    private final List<Action> onBreakActions = new ArrayList<>();
    private final List<MultiblockType.CapabilityFactory> capabilityFactories = new ArrayList<>();

    public BlockBuilder(BlockKey key) {
        this(key, "core");
    }

    public BlockBuilder(BlockKey key, String defaultCapabilityOwnerId) {
        this.key = key;
        this.defaultCapabilityOwnerId = defaultCapabilityOwnerId;
    }

    public BlockBuilder version(String version) {
        this.version = version;
        return this;
    }

    public BlockBuilder tickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
        return this;
    }

    public BlockBuilder displayName(DisplayNameConfig displayName) {
        this.displayName = displayName;
        return this;
    }

    public BlockBuilder assemblyTrigger(String assemblyTrigger) {
        this.assemblyTrigger = assemblyTrigger;
        return this;
    }

    public BlockBuilder blockMaterial(String material) {
        this.blockMaterial = material;
        return this;
    }

    public BlockBuilder variable(String key, Object value) {
        this.defaultVariables.put(key, value);
        return this;
    }
    
    public BlockBuilder port(String name, PortDefinition definition) {
        this.ports.put(name, definition);
        return this;
    }

    public BlockBuilder onCreate(Action action) {
        this.onCreateActions.add(action);
        return this;
    }

    public BlockBuilder onTick(Action action) {
        this.onTickActions.add(action);
        return this;
    }

    public BlockBuilder onInteract(Action action) {
        this.onInteractActions.add(action);
        return this;
    }

    public BlockBuilder onBreak(Action action) {
        this.onBreakActions.add(action);
        return this;
    }

    public BlockBuilder withCapability(Function<MultiblockInstance, Capability> factory) {
        return withCapability(defaultCapabilityOwnerId, factory);
    }

    public BlockBuilder withCapability(String ownerId, Function<MultiblockInstance, Capability> factory) {
        this.capabilityFactories.add(new MultiblockType.CapabilityFactory(ownerId, factory));
        return this;
    }

    public BlockDefinition build() {
        return new SimpleBlockDefinition(
                key,
                version,
                displayName,
                assemblyTrigger,
                blockMaterial,
                new HashMap<>(behaviorConfig),
                new HashMap<>(defaultVariables),
                new HashMap<>(ports),
                new HashMap<>(extensions),
                new ArrayList<>(onCreateActions),
                new ArrayList<>(onTickActions),
                new ArrayList<>(onInteractActions),
                new ArrayList<>(onBreakActions),
                tickInterval,
                new ArrayList<>(capabilityFactories)
        );
    }
}
