package dev.darkblade.mbe.platform.bukkit.preview.impl;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.util.Vector3d;
import dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayBlockState;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayEntityHandle;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import dev.darkblade.mbe.platform.bukkit.preview.bridge.PacketEventsAdapter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractPacketEventsRenderer implements BlockDisplayRenderer {
    protected final PacketEventsAdapter adapter;
    private final int blockStateIndex;
    private final int interpolationDurationIndex;
    private final int startInterpolationIndex;
    private final int translationIndex;
    private final int scaleIndex;

    protected AbstractPacketEventsRenderer(
        PacketEventsAdapter adapter,
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
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                request.entityId(),
                Optional.of(uuid),
                EntityTypes.BLOCK_DISPLAY,
                new Vector3d(request.x(), request.y(), request.z()),
                0, 0, 0, 0, Optional.empty()
        );
        sendSpawn(player, spawn, request);
        return new DisplayEntityHandle(request.entityId(), uuid);
    }

    @Override
    public void updateTransform(Player player, DisplayEntityHandle handle, DisplayTransform transform) {
        if (player == null || handle == null || !handle.isValid() || transform == null) {
            return;
        }
        List<EntityData<?>> values = new ArrayList<>();
        values.add(new EntityData(interpolationDurationIndex, EntityDataTypes.INT, transform.interpolationDuration()));
        values.add(new EntityData(startInterpolationIndex, EntityDataTypes.INT, transform.startInterpolation()));
        
        // Use the proper Vector3f from packetevents for translation and scale
        com.github.retrooper.packetevents.util.Vector3f translationVector = new com.github.retrooper.packetevents.util.Vector3f((float) transform.translationX(), (float) transform.translationY(), (float) transform.translationZ());
        values.add(new EntityData(translationIndex, EntityDataTypes.VECTOR3F, translationVector));
        
        com.github.retrooper.packetevents.util.Vector3f scaleVector = new com.github.retrooper.packetevents.util.Vector3f((float) transform.scaleX(), (float) transform.scaleY(), (float) transform.scaleZ());
        values.add(new EntityData(scaleIndex, EntityDataTypes.VECTOR3F, scaleVector));
        
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(handle.entityId(), values);
        adapter.sendPacket(player, metadata);
    }

    @Override
    public void updateBlock(Player player, DisplayEntityHandle handle, DisplayBlockState blockState) {
        if (player == null || handle == null || !handle.isValid() || blockState == null) {
            return;
        }
        // Resolving the block state using PacketEvents WrapperBlockState from bukkit string
        org.bukkit.block.data.BlockData blockData = Bukkit.createBlockData(
            blockState.serializedState() != null && !blockState.serializedState().isBlank() ? 
            blockState.serializedState() : blockState.namespacedId()
        );
        com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState wrappedState = 
            com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState.getByString(blockData.getAsString());
        
        // Fallback for wrapped state if parsing fails
        if (wrappedState == null) {
            wrappedState = com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState.getDefaultState(
                com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.STONE
            );
        }

        int globalId = wrappedState.getGlobalId();
        
        List<EntityData<?>> values = new ArrayList<>();
        values.add(new EntityData(blockStateIndex, EntityDataTypes.BLOCK_STATE, globalId));
        
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(handle.entityId(), values);
        adapter.sendPacket(player, metadata);
    }

    @Override
    public void destroy(Player player, DisplayEntityHandle handle) {
        if (player == null || handle == null || !handle.isValid()) {
            return;
        }
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(handle.entityId());
        adapter.sendPacket(player, destroy);
    }

    protected void sendSpawn(Player player, WrapperPlayServerSpawnEntity spawnPacket, DisplaySpawnRequest request) {
        adapter.sendPacket(player, spawnPacket);
        
        DisplayEntityHandle tempHandle = new DisplayEntityHandle(request.entityId(), spawnPacket.getUUID().orElse(UUID.randomUUID()));
        updateBlock(player, tempHandle, request.blockState());
        
        DisplayTransform transform = request.transform() == null ? DisplayTransform.identity() : request.transform();
        updateTransform(player, tempHandle, transform);
    }
}
