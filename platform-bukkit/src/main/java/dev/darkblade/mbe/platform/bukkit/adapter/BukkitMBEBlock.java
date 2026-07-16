package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBEBlock;
import dev.darkblade.mbe.api.platform.MBELocation;
import dev.darkblade.mbe.api.platform.MBEWorld;

import org.bukkit.block.Block;

public class BukkitMBEBlock implements MBEBlock {

    private final Block bukkitBlock;

    public BukkitMBEBlock(Block bukkitBlock) {
        this.bukkitBlock = bukkitBlock;
    }

    @Override
    public MBELocation getLocation() {
        return new BukkitMBELocation(bukkitBlock.getLocation());
    }

    @Override
    public MBEWorld getWorld() {
        return new BukkitMBEWorld(bukkitBlock.getWorld());
    }

    @Override
    public int getX() {
        return bukkitBlock.getX();
    }

    @Override
    public int getY() {
        return bukkitBlock.getY();
    }

    @Override
    public int getZ() {
        return bukkitBlock.getZ();
    }

    @Override
    public String getType() {
        return bukkitBlock.getType().getKey().toString();
    }

    public Block getBukkitBlock() {
        return bukkitBlock;
    }
}
