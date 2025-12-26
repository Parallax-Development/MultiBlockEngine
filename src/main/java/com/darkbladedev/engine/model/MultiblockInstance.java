package com.darkbladedev.engine.model;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class MultiblockInstance {
    private final MultiblockType type;
    private final Location anchorLocation;
    private final BlockFace facing;
    private MultiblockState state;
    private final Map<String, Object> variables;

    public MultiblockInstance(MultiblockType type, Location anchorLocation, BlockFace facing, MultiblockState state, Map<String, Object> variables) {
        this.type = type;
        this.anchorLocation = anchorLocation;
        this.facing = facing;
        this.state = state;
        this.variables = new HashMap<>(variables != null ? variables : type.defaultVariables());
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
}
