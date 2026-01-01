package com.darkbladedev.engine.model;

import com.darkbladedev.engine.api.capability.Capability;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Optional;

public class MultiblockInstance {
    private final MultiblockType type;
    private final Location anchorLocation;
    private final BlockFace facing;
    private MultiblockState state;
    private final Map<String, Object> variables;
    private final Map<Class<? extends Capability>, Capability> capabilities;

    public MultiblockInstance(MultiblockType type, Location anchorLocation, BlockFace facing, MultiblockState state, Map<String, Object> variables) {
        this.type = type;
        this.anchorLocation = anchorLocation;
        this.facing = facing;
        this.state = state;
        this.variables = new HashMap<>(variables != null ? variables : type.defaultVariables());
        this.capabilities = new HashMap<>();
    }

    public MultiblockInstance(MultiblockType type, Location anchorLocation, BlockFace facing) {
        this(type, anchorLocation, facing, MultiblockState.ACTIVE, null);
    }
    
    public MultiblockType type() { return type; }
    public Location anchorLocation() { return anchorLocation; }
    public BlockFace facing() { return facing; }
    public MultiblockState state() { return state; }
    
    public void setState(MultiblockState state) {
        this.state = state;
    }
    
    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }
    
    public Object getVariable(String key) {
        return variables.get(key);
    }
    
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public <T extends Capability> Optional<T> getCapability(Class<T> capabilityClass) {
        for (Capability capability : capabilities.values()) {
            if (capabilityClass.isInstance(capability)) {
                return Optional.of(capabilityClass.cast(capability));
            }
        }
        return Optional.empty();
    }

    public void addCapability(Capability capability) {
        if (capability == null) {
            throw new IllegalArgumentException("capability");
        }
        Class<? extends Capability> key = capability.getClass();
        if (capabilities.containsKey(key)) {
            throw new IllegalStateException("Duplicate capability: " + key.getName());
        }
        capabilities.put(key, capability);
    }
}
