package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBEBlock;
import dev.darkblade.mbe.api.platform.MBEWorld;
import org.bukkit.World;

import java.util.UUID;

public class BukkitMBEWorld implements MBEWorld {

    private final World bukkitWorld;

    public BukkitMBEWorld(World bukkitWorld) {
        this.bukkitWorld = bukkitWorld;
    }

    @Override
    public UUID getUniqueId() {
        return bukkitWorld.getUID();
    }

    @Override
    public String getName() {
        return bukkitWorld.getName();
    }

    @Override
    public MBEBlock getBlockAt(int x, int y, int z) {
        return new BukkitMBEBlock(bukkitWorld.getBlockAt(x, y, z));
    }
    
    public World getBukkitWorld() {
        return bukkitWorld;
    }
}
