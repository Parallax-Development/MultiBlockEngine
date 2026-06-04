package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBELocation;
import dev.darkblade.mbe.api.platform.MBEWorld;
import dev.darkblade.mbe.api.platform.MBEBlock;
import org.bukkit.Location;

public class BukkitMBELocation implements MBELocation {

    private final Location bukkitLocation;

    public BukkitMBELocation(Location bukkitLocation) {
        this.bukkitLocation = bukkitLocation;
    }

    @Override
    public MBEWorld getWorld() {
        return new BukkitMBEWorld(bukkitLocation.getWorld());
    }

    @Override
    public double getX() {
        return bukkitLocation.getX();
    }

    @Override
    public double getY() {
        return bukkitLocation.getY();
    }

    @Override
    public double getZ() {
        return bukkitLocation.getZ();
    }

    @Override
    public int getBlockX() {
        return bukkitLocation.getBlockX();
    }

    @Override
    public int getBlockY() {
        return bukkitLocation.getBlockY();
    }

    @Override
    public int getBlockZ() {
        return bukkitLocation.getBlockZ();
    }

    @Override
    public float getYaw() {
        return bukkitLocation.getYaw();
    }

    @Override
    public float getPitch() {
        return bukkitLocation.getPitch();
    }

    @Override
    public MBEBlock getBlock() {
        return new BukkitMBEBlock(bukkitLocation.getBlock());
    }
    
    public Location getBukkitLocation() {
        return bukkitLocation;
    }
}
