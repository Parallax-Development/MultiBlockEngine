package com.darkbladedev.engine.api.builder;

import com.darkbladedev.engine.api.capability.Capability;
import com.darkbladedev.engine.model.*;
import com.darkbladedev.engine.model.action.Action;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MultiblockBuilder {
    private final String id;
    private final String defaultCapabilityOwnerId;
    private String version = "1.0";
    private int tickInterval = 20;
    private Vector controllerOffset = new Vector(0, 0, 0);
    private BlockMatcher controllerMatcher;
    private final List<PatternEntry> pattern = new ArrayList<>();
    private final Map<String, Object> defaultVariables = new HashMap<>();
    private final List<Action> onCreateActions = new ArrayList<>();
    private final List<Action> onTickActions = new ArrayList<>();
    private final List<Action> onInteractActions = new ArrayList<>();
    private final List<Action> onBreakActions = new ArrayList<>();
    private final List<MultiblockType.CapabilityFactory> capabilityFactories = new ArrayList<>();
    private DisplayNameConfig displayName;

    public MultiblockBuilder(String id) {
        this(id, "core");
    }

    public MultiblockBuilder(String id, String defaultCapabilityOwnerId) {
        this.id = id;
        this.defaultCapabilityOwnerId = defaultCapabilityOwnerId;
    }

    public MultiblockBuilder version(String version) {
        this.version = version;
        return this;
    }

    public MultiblockBuilder tickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
        return this;
    }

    public MultiblockBuilder controller(BlockMatcher matcher) {
        this.controllerMatcher = matcher;
        return this;
    }

    public MultiblockBuilder pattern(Vector offset, BlockMatcher matcher) {
        this.pattern.add(new PatternEntry(offset, matcher, false));
        return this;
    }
    
    public MultiblockBuilder variable(String key, Object value) {
        this.defaultVariables.put(key, value);
        return this;
    }

    public MultiblockBuilder onCreate(Action action) {
        this.onCreateActions.add(action);
        return this;
    }

    public MultiblockBuilder onTick(Action action) {
        this.onTickActions.add(action);
        return this;
    }

    public MultiblockBuilder onInteract(Action action) {
        this.onInteractActions.add(action);
        return this;
    }

    public MultiblockBuilder onBreak(Action action) {
        this.onBreakActions.add(action);
        return this;
    }
    
    public MultiblockBuilder withCapability(Function<MultiblockInstance, Capability> factory) {
        return withCapability(defaultCapabilityOwnerId, factory);
    }

    public MultiblockBuilder withCapability(String ownerId, Function<MultiblockInstance, Capability> factory) {
        this.capabilityFactories.add(new MultiblockType.CapabilityFactory(ownerId, factory));
        return this;
    }

    public MultiblockType build() {
        if (controllerMatcher == null) throw new IllegalStateException("Controller matcher must be set");
        
        return new MultiblockType(
            id,
            version,
            controllerOffset,
            controllerMatcher,
            pattern,
            true, // persistent by default
            new HashMap<>(), // behaviorConfig
            defaultVariables,
            onCreateActions,
            onTickActions,
            onInteractActions,
            onBreakActions,
            displayName,
            tickInterval,
            capabilityFactories
        );
    }
}
