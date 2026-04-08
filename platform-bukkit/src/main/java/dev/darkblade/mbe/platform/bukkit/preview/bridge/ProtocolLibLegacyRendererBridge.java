package dev.darkblade.mbe.platform.bukkit.preview.bridge;

import dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayBlockState;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayEntityHandle;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import dev.darkblade.mbe.platform.bukkit.preview.impl.fallback.FallbackRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_19_4.Renderer_1_19_4;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_20.Renderer_1_20;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_20_2.Renderer_1_20_2;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_20_5.Renderer_1_20_5;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_21.Renderer_1_21;
import dev.darkblade.mbe.platform.bukkit.preview.impl.v1_21.Renderer_1_21_2;
import dev.darkblade.mbe.platform.bukkit.preview.version.ProtocolVersion;
import dev.darkblade.mbe.platform.bukkit.preview.version.VersionResolver;
import dev.darkblade.mbe.platform.bukkit.preview.version.VersionedRenderer;
import dev.darkblade.mbe.preview.DisplayEntityRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProtocolLibLegacyRendererBridge implements DisplayEntityRenderer {
    private final BlockDisplayRenderer renderer;
    private final AtomicInteger entityIdCounter = new AtomicInteger(2_000_000);
    private final Map<Integer, UUID> ownersByEntityId = new ConcurrentHashMap<>();

    public ProtocolLibLegacyRendererBridge() {
        ProtocolLibAdapter adapter = new ProtocolLibAdapter();
        Map<ProtocolVersion, BlockDisplayRenderer> byVersion = new java.util.EnumMap<>(ProtocolVersion.class);
        BlockDisplayRenderer renderer_1_21 = new Renderer_1_21(adapter);
        BlockDisplayRenderer renderer_1_21_2 = new Renderer_1_21_2(adapter);
        byVersion.put(ProtocolVersion.V1_19_4, new Renderer_1_19_4(adapter));
        byVersion.put(ProtocolVersion.V1_20, new Renderer_1_20(adapter));
        byVersion.put(ProtocolVersion.V1_20_2, new Renderer_1_20_2(adapter));
        byVersion.put(ProtocolVersion.V1_20_4, new Renderer_1_20_2(adapter));
        byVersion.put(ProtocolVersion.V1_20_5, new Renderer_1_20_5(adapter));
        byVersion.put(ProtocolVersion.V1_21, renderer_1_21);
        byVersion.put(ProtocolVersion.V1_21_2, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_4, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_5, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_6, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_7, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_9, renderer_1_21_2);
        byVersion.put(ProtocolVersion.V1_21_11, renderer_1_21_2);
        this.renderer = new VersionedRenderer(new VersionResolver(), byVersion, new FallbackRenderer());
    }

    public BlockDisplayRenderer delegate() {
        return renderer;
    }

    @Override
    public int spawnBlockDisplay(Player player, Location location, BlockData blockData) {
        if (player == null || location == null || blockData == null) {
            return -1;
        }
        int entityId = nextEntityId();
        DisplayBlockState blockState = DisplayBlockState.of(
            blockData.getMaterial().getKey().toString(),
            blockData.getAsString()
        );
        DisplaySpawnRequest request = new DisplaySpawnRequest(
            entityId,
            location.getX(),
            location.getY(),
            location.getZ(),
            blockState,
            DisplayTransform.identity()
        );
        DisplayEntityHandle handle = renderer.spawn(player, request);
        if (handle.isValid()) {
            ownersByEntityId.put(handle.entityId(), player.getUniqueId());
            return handle.entityId();
        }
        return -1;
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
        renderer.updateBlock(
            player,
            new DisplayEntityHandle(entityId, UUID.randomUUID()),
            DisplayBlockState.of(blockData.getMaterial().getKey().toString(), blockData.getAsString())
        );
    }

    @Override
    public void destroyEntities(Player player, Collection<Integer> entityIds) {
        if (player == null || entityIds == null || entityIds.isEmpty()) {
            return;
        }
        for (Integer entityId : entityIds) {
            if (entityId == null || entityId <= 0) {
                continue;
            }
            renderer.destroy(player, new DisplayEntityHandle(entityId, UUID.randomUUID()));
            ownersByEntityId.remove(entityId);
        }
    }

    private int nextEntityId() {
        int id = entityIdCounter.incrementAndGet();
        if (id > 0) {
            return id;
        }
        entityIdCounter.set(2_000_000);
        return entityIdCounter.incrementAndGet();
    }
}
