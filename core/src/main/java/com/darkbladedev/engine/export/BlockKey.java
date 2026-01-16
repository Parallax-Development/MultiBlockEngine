package com.darkbladedev.engine.export;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public record BlockKey(UUID worldId, int x, int y, int z) {
    public static BlockKey of(Location loc) {
        Objects.requireNonNull(loc, "loc");
        World w = Objects.requireNonNull(loc.getWorld(), "world");
        return new BlockKey(w.getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}

