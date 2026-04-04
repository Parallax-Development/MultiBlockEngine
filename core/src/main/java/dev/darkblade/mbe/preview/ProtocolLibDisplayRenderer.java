package dev.darkblade.mbe.preview;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolLibDisplayRenderer implements DisplayEntityRenderer {
    private static final int BLOCK_DISPLAY_METADATA_INDEX = 23;
    private final ProtocolManager protocolManager;
    private final EntityIdAllocator entityIdAllocator;
    private final Map<Integer, UUID> ownersByEntityId = new ConcurrentHashMap<>();

    public ProtocolLibDisplayRenderer(EntityIdAllocator entityIdAllocator) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.entityIdAllocator = entityIdAllocator;
    }

    @Override
    public int spawnBlockDisplay(Player player, Location location, BlockData blockData) {
        if (player == null || location == null || blockData == null) {
            return -1;
        }
        int entityId = entityIdAllocator.nextId();
        PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, UUID.randomUUID());
        spawn.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
        spawn.getDoubles().write(0, location.getX());
        spawn.getDoubles().write(1, location.getY());
        spawn.getDoubles().write(2, location.getZ());
        try {
            protocolManager.sendServerPacket(player, spawn);
        } catch (RuntimeException ex) {
            logPacketError("SPAWN_ENTITY", entityId, ex);
            return -1;
        }
        sendBlockMetadata(player, entityId, blockData);
        ownersByEntityId.put(entityId, player.getUniqueId());
        return entityId;
    }

    @Override
    public void updateBlockDisplay(int entityId, BlockData blockData) {
        if (entityId <= 0 || blockData == null) {
            return;
        }
        UUID ownerId = ownersByEntityId.get(entityId);
        if (ownerId == null) {
            return;
        }
        Player player = Bukkit.getPlayer(ownerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        sendBlockMetadata(player, entityId, blockData);
    }

    @Override
    public void destroyEntities(Player player, Collection<Integer> entityIds) {
        if (player == null || entityIds == null || entityIds.isEmpty()) {
            return;
        }
        PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, new ArrayList<>(entityIds));
        try {
            protocolManager.sendServerPacket(player, destroy);
        } catch (RuntimeException ex) {
            logPacketError("ENTITY_DESTROY", -1, ex);
        }
        for (Integer entityId : entityIds) {
            if (entityId != null) {
                ownersByEntityId.remove(entityId);
            }
        }
    }

    private void sendBlockMetadata(Player player, int entityId, BlockData blockData) {
        PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, entityId);
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.getBlockDataSerializer(false);
        WrappedBlockData wrappedBlockData = WrappedBlockData.createData(blockData == null ? org.bukkit.Material.STONE : blockData.getMaterial());
        WrappedDataValue blockDataValue = new WrappedDataValue(
            BLOCK_DISPLAY_METADATA_INDEX,
            serializer,
            wrappedBlockData.getHandle()
        );
        metadata.getDataValueCollectionModifier().write(0, List.of(blockDataValue));
        try {
            protocolManager.sendServerPacket(player, metadata);
        } catch (RuntimeException ex) {
            logPacketError("ENTITY_METADATA", entityId, ex);
        }
    }

    private void logPacketError(String packetType, int entityId, RuntimeException ex) {
        Bukkit.getLogger().warning("[MBE Preview] Packet send failed type=" + packetType + " entityId=" + entityId + " reason=" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
