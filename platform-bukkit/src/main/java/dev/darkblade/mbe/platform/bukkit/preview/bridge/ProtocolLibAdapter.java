package dev.darkblade.mbe.platform.bukkit.preview.bridge;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class ProtocolLibAdapter {
    private final ProtocolManager protocolManager;
    private final PacketType bundlePacketType;

    public ProtocolLibAdapter() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.bundlePacketType = resolveBundlePacketType();
    }

    public PacketContainer createPacket(PacketType packetType) {
        return protocolManager.createPacket(packetType);
    }

    public void sendPacket(Player player, PacketContainer packet) {
        if (player == null || packet == null || protocolManager == null) {
            return;
        }
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (RuntimeException ex) {
            Bukkit.getLogger().log(Level.WARNING, "[MBE Preview] Packet send failed: " + ex.getMessage(), ex);
        }
    }

    public void sendBundle(Player player, Collection<PacketContainer> packets) {
        if (player == null || packets == null || packets.isEmpty()) {
            return;
        }
        if (bundlePacketType != null) {
            PacketContainer bundle = createPacket(bundlePacketType);
            if (writeBundlePackets(bundle, packets)) {
                sendPacket(player, bundle);
                return;
            }
        }
        for (PacketContainer packet : packets) {
            sendPacket(player, packet);
        }
    }

    public Object blockDataHandle(String namespacedId, String serializedState) {
        Material material = resolveMaterial(namespacedId);
        BlockData blockData;
        try {
            if (serializedState != null && !serializedState.isBlank()) {
                blockData = Bukkit.createBlockData(serializedState);
            } else {
                blockData = material.createBlockData();
            }
        } catch (RuntimeException ignored) {
            blockData = material.createBlockData();
        }
        WrappedBlockData wrapped = WrappedBlockData.createData(blockData);
        return wrapped.getHandle();
    }

    public WrappedDataWatcher.Serializer blockDataSerializer() {
        return WrappedDataWatcher.Registry.getBlockDataSerializer(false);
    }

    private boolean writeBundlePackets(PacketContainer bundle, Collection<PacketContainer> packets) {
        try {
            Method packetBundlesMethod = PacketContainer.class.getMethod("getPacketBundles");
            Object modifier = packetBundlesMethod.invoke(bundle);
            Method writeMethod = modifier.getClass().getMethod("write", int.class, Object.class);
            writeMethod.invoke(modifier, 0, new ArrayList<>(packets));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private PacketType resolveBundlePacketType() {
        try {
            return (PacketType) PacketType.Play.Server.class.getField("BUNDLE").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Material resolveMaterial(String namespacedId) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return Material.STONE;
        }
        String normalized = namespacedId.trim().toLowerCase(Locale.ROOT);
        Material namespaced = Material.matchMaterial(normalized);
        if (namespaced != null) {
            return namespaced;
        }
        int separator = normalized.indexOf(':');
        String fallback = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        Material simple = Material.matchMaterial(fallback.toUpperCase(Locale.ROOT));
        return Objects.requireNonNullElse(simple, Material.STONE);
    }
}
