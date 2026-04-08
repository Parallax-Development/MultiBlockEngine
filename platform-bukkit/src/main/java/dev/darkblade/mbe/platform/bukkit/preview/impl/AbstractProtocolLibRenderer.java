package dev.darkblade.mbe.platform.bukkit.preview.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayBlockState;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayEntityHandle;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import dev.darkblade.mbe.platform.bukkit.preview.bridge.ProtocolLibAdapter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractProtocolLibRenderer implements BlockDisplayRenderer {
    protected final ProtocolLibAdapter adapter;
    private final int blockStateIndex;
    private final int interpolationDurationIndex;
    private final int startInterpolationIndex;
    private final int translationIndex;
    private final int scaleIndex;

    protected AbstractProtocolLibRenderer(
        ProtocolLibAdapter adapter,
        int blockStateIndex,
        int interpolationDurationIndex,
        int startInterpolationIndex,
        int translationIndex,
        int scaleIndex
    ) {
        this.adapter = adapter;
        this.blockStateIndex = blockStateIndex;
        this.interpolationDurationIndex = interpolationDurationIndex;
        this.startInterpolationIndex = startInterpolationIndex;
        this.translationIndex = translationIndex;
        this.scaleIndex = scaleIndex;
    }

    @Override
    public DisplayEntityHandle spawn(Player player, DisplaySpawnRequest request) {
        if (player == null || request == null || request.entityId() <= 0) {
            return new DisplayEntityHandle(-1, null);
        }
        UUID uuid = UUID.randomUUID();
        PacketContainer spawn = adapter.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawn.getIntegers().write(0, request.entityId());
        spawn.getUUIDs().write(0, uuid);
        spawn.getEntityTypeModifier().write(0, EntityType.BLOCK_DISPLAY);
        spawn.getDoubles().write(0, request.x());
        spawn.getDoubles().write(1, request.y());
        spawn.getDoubles().write(2, request.z());
        sendSpawn(player, spawn, request);
        return new DisplayEntityHandle(request.entityId(), uuid);
    }

    @Override
    public void updateTransform(Player player, DisplayEntityHandle handle, DisplayTransform transform) {
        if (player == null || handle == null || !handle.isValid() || transform == null) {
            return;
        }
        PacketContainer metadata = adapter.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, handle.entityId());
        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(interpolationDurationIndex, WrappedDataWatcher.Registry.get(Integer.class), transform.interpolationDuration()));
        values.add(new WrappedDataValue(startInterpolationIndex, WrappedDataWatcher.Registry.get(Integer.class), transform.startInterpolation()));
        values.add(new WrappedDataValue(translationIndex, WrappedDataWatcher.Registry.getVectorSerializer(), new org.bukkit.util.Vector(transform.translationX(), transform.translationY(), transform.translationZ())));
        values.add(new WrappedDataValue(scaleIndex, WrappedDataWatcher.Registry.getVectorSerializer(), new org.bukkit.util.Vector(transform.scaleX(), transform.scaleY(), transform.scaleZ())));
        metadata.getDataValueCollectionModifier().write(0, values);
        adapter.sendPacket(player, metadata);
    }

    @Override
    public void updateBlock(Player player, DisplayEntityHandle handle, DisplayBlockState blockState) {
        if (player == null || handle == null || !handle.isValid() || blockState == null) {
            return;
        }
        PacketContainer metadata = adapter.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, handle.entityId());
        WrappedDataValue value = new WrappedDataValue(
            blockStateIndex,
            adapter.blockDataSerializer(),
            adapter.blockDataHandle(blockState.namespacedId(), blockState.serializedState())
        );
        metadata.getDataValueCollectionModifier().write(0, List.of(value));
        adapter.sendPacket(player, metadata);
    }

    @Override
    public void destroy(Player player, DisplayEntityHandle handle) {
        if (player == null || handle == null || !handle.isValid()) {
            return;
        }
        PacketContainer destroy = adapter.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, List.of(handle.entityId()));
        adapter.sendPacket(player, destroy);
    }

    protected void sendSpawn(Player player, PacketContainer spawnPacket, DisplaySpawnRequest request) {
        adapter.sendPacket(player, spawnPacket);
        updateBlock(player, new DisplayEntityHandle(request.entityId(), spawnPacket.getUUIDs().read(0)), request.blockState());
        DisplayTransform transform = request.transform() == null ? DisplayTransform.identity() : request.transform();
        updateTransform(player, new DisplayEntityHandle(request.entityId(), spawnPacket.getUUIDs().read(0)), transform);
    }
}
