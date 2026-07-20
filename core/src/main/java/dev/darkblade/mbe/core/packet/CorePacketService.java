package dev.darkblade.mbe.core.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.darkblade.mbe.api.packet.PacketService;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CorePacketService implements PacketService {

    @Override
    public void sendPacket(Player player, PacketWrapper<?> wrapper) {
        if (player == null || wrapper == null) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    @Override
    public void sendBundle(Player player, Collection<PacketWrapper<?>> wrappers) {
        if (player == null || wrappers == null || wrappers.isEmpty()) {
            return;
        }
        for (PacketWrapper<?> wrapper : wrappers) {
            sendPacket(player, wrapper);
        }
    }
}
