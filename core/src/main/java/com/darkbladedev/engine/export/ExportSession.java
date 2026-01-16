package com.darkbladedev.engine.export;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ExportSession {

    private final UUID playerId;

    private Location pos1;
    private Location pos2;

    private String pendingRole;

    private final Map<BlockKey, String> rolesByBlock = new LinkedHashMap<>();
    private final Map<BlockKey, Map<String, Object>> propsByBlock = new LinkedHashMap<>();

    public ExportSession(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
    }

    public UUID playerId() {
        return playerId;
    }

    public Location pos1() {
        return pos1;
    }

    public Location pos2() {
        return pos2;
    }

    public void setPos1(Location loc) {
        this.pos1 = loc == null ? null : loc.clone();
    }

    public void setPos2(Location loc) {
        this.pos2 = loc == null ? null : loc.clone();
    }

    public String pendingRole() {
        return pendingRole;
    }

    public void setPendingRole(String role) {
        this.pendingRole = role == null ? null : role.trim();
        if (this.pendingRole != null && this.pendingRole.isBlank()) {
            this.pendingRole = null;
        }
    }

    public void clearPendingRole() {
        this.pendingRole = null;
    }

    public Optional<BlockKey> controller() {
        for (Map.Entry<BlockKey, String> e : rolesByBlock.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase("controller")) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    public Map<BlockKey, String> rolesSnapshot() {
        return Map.copyOf(rolesByBlock);
    }

    public Map<BlockKey, Map<String, Object>> propsSnapshot() {
        Map<BlockKey, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map.Entry<BlockKey, Map<String, Object>> e : propsByBlock.entrySet()) {
            out.put(e.getKey(), e.getValue() == null ? Map.of() : Map.copyOf(e.getValue()));
        }
        return Map.copyOf(out);
    }

    public void markRole(Location loc, String role) {
        if (loc == null || loc.getWorld() == null || role == null || role.isBlank()) {
            return;
        }
        String r = role.trim();
        BlockKey key = BlockKey.of(loc);

        if (r.equalsIgnoreCase("controller")) {
            rolesByBlock.entrySet().removeIf(e -> e.getValue() != null && e.getValue().equalsIgnoreCase("controller"));
        }

        rolesByBlock.put(key, r);
    }

    public void putProperty(Location loc, String key, Object value) {
        if (loc == null || loc.getWorld() == null || key == null || key.isBlank()) {
            return;
        }
        BlockKey bk = BlockKey.of(loc);
        propsByBlock.compute(bk, (k, prev) -> {
            Map<String, Object> next = prev == null ? new LinkedHashMap<>() : new LinkedHashMap<>(prev);
            next.put(key.trim(), value);
            return Map.copyOf(next);
        });
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
        pendingRole = null;
        rolesByBlock.clear();
        propsByBlock.clear();
    }
}

