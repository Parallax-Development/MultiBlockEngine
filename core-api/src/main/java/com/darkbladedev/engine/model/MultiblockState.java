package com.darkbladedev.engine.model;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockState {
    private static final Map<String, MultiblockState> REGISTRY = new ConcurrentHashMap<>();

    public static final MultiblockState ACTIVE = register("ACTIVE");
    public static final MultiblockState INACTIVE = register("INACTIVE");
    public static final MultiblockState DISABLED = register("DISABLED");
    public static final MultiblockState DAMAGED = register("DAMAGED");
    public static final MultiblockState OVERLOADED = register("OVERLOADED");

    private final String name;

    private MultiblockState(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiblockState that = (MultiblockState) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static MultiblockState register(String name) {
        return REGISTRY.computeIfAbsent(name.toUpperCase(), MultiblockState::new);
    }

    public static MultiblockState valueOf(String name) {
        return register(name); // Auto-register if not exists, or retrieve
    }
    
    public static Collection<MultiblockState> values() {
        return REGISTRY.values();
    }
}
