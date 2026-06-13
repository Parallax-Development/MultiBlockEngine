package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.api.platform.MBELocation;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitMBEPlayer implements MBEPlayer {

    private final Player bukkitPlayer;

    public BukkitMBEPlayer(Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public String getName() {
        return bukkitPlayer.getName();
    }

    @Override
    public void sendMessage(String message) {
        bukkitPlayer.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String permission) {
        return bukkitPlayer.hasPermission(permission);
    }

    @Override
    public MBELocation getLocation() {
        return new BukkitMBELocation(bukkitPlayer.getLocation());
    }
    
    public Player getBukkitPlayer() {
        return bukkitPlayer;
    }
}
