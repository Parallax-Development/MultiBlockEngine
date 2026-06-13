package dev.darkblade.mbe.platform.bukkit.preview.bridge;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class PacketEventsAdapter {

    public PacketEventsAdapter() {
    }

    public void sendPacket(Player player, PacketWrapper<?> wrapper) {
        if (player == null || wrapper == null) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }

    public void sendBundle(Player player, Collection<PacketWrapper<?>> wrappers) {
        if (player == null || wrappers == null || wrappers.isEmpty()) {
            return;
        }
        for (PacketWrapper<?> wrapper : wrappers) {
            sendPacket(player, wrapper);
        }
    }
}
