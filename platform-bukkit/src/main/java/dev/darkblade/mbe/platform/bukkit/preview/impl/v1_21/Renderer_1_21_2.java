package dev.darkblade.mbe.platform.bukkit.preview.impl.v1_21;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import dev.darkblade.mbe.platform.bukkit.preview.bridge.ProtocolLibAdapter;
import dev.darkblade.mbe.platform.bukkit.preview.impl.AbstractProtocolLibRenderer;
import org.bukkit.entity.Player;

import java.util.List;

public final class Renderer_1_21_2 extends AbstractProtocolLibRenderer {

    public Renderer_1_21_2(ProtocolLibAdapter adapter) {
        /*
         * Documentation:
         * https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Block_Display
         * https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Display
         */
        super(adapter, 23, 9, 8, 11, 12);
    }

    @Override
    protected void sendSpawn(Player player, PacketContainer spawnPacket, DisplaySpawnRequest request) {
        PacketContainer metadata = adapter.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadata.getIntegers().write(0, request.entityId());
        DisplayTransform transform = request.transform() == null ? DisplayTransform.identity() : request.transform();
        List<WrappedDataValue> values = List.of(
            new WrappedDataValue(23, adapter.blockDataSerializer(), adapter.blockDataHandle(request.blockState().namespacedId(), request.blockState().serializedState())),
            new WrappedDataValue(9, WrappedDataWatcher.Registry.get(Integer.class), transform.interpolationDuration()),
            new WrappedDataValue(8, WrappedDataWatcher.Registry.get(Integer.class), transform.startInterpolation())
        );
        metadata.getDataValueCollectionModifier().write(0, values);
        adapter.sendBundle(player, List.of(spawnPacket, metadata));
    }
}
