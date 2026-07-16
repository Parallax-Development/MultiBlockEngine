package dev.darkblade.mbe.platform.bukkit.adapter;

import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.api.platform.MBEWorld;
import dev.darkblade.mbe.api.platform.PlatformService;
import dev.darkblade.mbe.api.service.ManagedRuntimeService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class BukkitPlatformService implements PlatformService, ManagedRuntimeService {

    private static final String SERVICE_ID = "mbe:platform_service";

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public Optional<MBEPlayer> getPlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? Optional.of(new BukkitMBEPlayer(player)) : Optional.empty();
    }

    @Override
    public Optional<MBEPlayer> getPlayerExact(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player != null ? Optional.of(new BukkitMBEPlayer(player)) : Optional.empty();
    }

    @Override
    public Optional<MBEWorld> getWorld(UUID uuid) {
        World world = Bukkit.getWorld(uuid);
        return world != null ? Optional.of(new BukkitMBEWorld(world)) : Optional.empty();
    }

    @Override
    public Optional<MBEWorld> getWorld(String name) {
        World world = Bukkit.getWorld(name);
        return world != null ? Optional.of(new BukkitMBEWorld(world)) : Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Object wrapped, Class<T> type) {
        if (wrapped instanceof BukkitMBEPlayer bukkitPlayer && Player.class.isAssignableFrom(type)) {
            return (T) bukkitPlayer.getBukkitPlayer();
        }
        if (wrapped instanceof BukkitMBELocation bukkitLocation && Location.class.isAssignableFrom(type)) {
            return (T) bukkitLocation.getBukkitLocation();
        }
        if (wrapped instanceof BukkitMBEWorld bukkitWorld && World.class.isAssignableFrom(type)) {
            return (T) bukkitWorld.getBukkitWorld();
        }
        if (wrapped instanceof BukkitMBEBlock bukkitBlock && Block.class.isAssignableFrom(type)) {
            return (T) bukkitBlock.getBukkitBlock();
        }
        if (wrapped instanceof BukkitMBEItemStack bukkitItemStack && ItemStack.class.isAssignableFrom(type)) {
            return (T) bukkitItemStack.getBukkitItemStack();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T wrap(Object raw, Class<T> type) {
        if (raw instanceof Player player && dev.darkblade.mbe.api.platform.MBEPlayer.class.isAssignableFrom(type)) {
            return (T) new BukkitMBEPlayer(player);
        }
        if (raw instanceof Location location && dev.darkblade.mbe.api.platform.MBELocation.class.isAssignableFrom(type)) {
            return (T) new BukkitMBELocation(location);
        }
        if (raw instanceof World world && dev.darkblade.mbe.api.platform.MBEWorld.class.isAssignableFrom(type)) {
            return (T) new BukkitMBEWorld(world);
        }
        if (raw instanceof Block block && dev.darkblade.mbe.api.platform.MBEBlock.class.isAssignableFrom(type)) {
            return (T) new BukkitMBEBlock(block);
        }
        if (raw instanceof ItemStack itemStack && dev.darkblade.mbe.api.platform.MBEItemStack.class.isAssignableFrom(type)) {
            return (T) new BukkitMBEItemStack(itemStack);
        }
        return null;
    }
}
