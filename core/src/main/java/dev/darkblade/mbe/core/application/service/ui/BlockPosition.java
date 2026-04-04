package dev.darkblade.mbe.core.application.service.ui;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.Optional;

public record BlockPosition(String world, int x, int y, int z) {
    public BlockPosition {
        Objects.requireNonNull(world, "world");
        world = world.trim();
        if (world.isEmpty()) {
            throw new IllegalArgumentException("world");
        }
    }

    public static Optional<BlockPosition> fromBlock(Block block) {
        if (block == null || block.getWorld() == null) {
            return Optional.empty();
        }
        World world = block.getWorld();
        return Optional.of(new BlockPosition(world.getName(), block.getX(), block.getY(), block.getZ()));
    }

    public static Optional<BlockPosition> fromLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        World world = location.getWorld();
        return Optional.of(new BlockPosition(world.getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }
}
