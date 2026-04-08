package dev.darkblade.mbe.platform.bukkit.preview.impl.v1_19_4;

import com.comphenix.protocol.events.PacketContainer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.bridge.ProtocolLibAdapter;
import dev.darkblade.mbe.platform.bukkit.preview.impl.AbstractProtocolLibRenderer;
import org.bukkit.entity.Player;

import java.util.List;

public final class Renderer_1_19_4 extends AbstractProtocolLibRenderer {
    public Renderer_1_19_4(ProtocolLibAdapter adapter) {
        super(adapter, 23, 8, 9, 10, 11);
    }

    @Override
    protected void sendSpawn(Player player, PacketContainer spawnPacket, DisplaySpawnRequest request) {
        PacketContainer metadata = adapter.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, request.entityId());
        metadata.getDataValueCollectionModifier().write(0, List.of(
            new com.comphenix.protocol.wrappers.WrappedDataValue(
                23,
                adapter.blockDataSerializer(),
                adapter.blockDataHandle(request.blockState().namespacedId(), request.blockState().serializedState())
            )
        ));
        adapter.sendPacket(player, spawnPacket);
        adapter.sendPacket(player, metadata);
    }
}
