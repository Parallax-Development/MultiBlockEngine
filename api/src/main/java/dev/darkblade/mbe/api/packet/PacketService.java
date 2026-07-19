package dev.darkblade.mbe.api.packet;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface PacketService {
    
    /**
     * Sends a single PacketEvents wrapper to a specific player.
     * 
     * @param player The player to send the packet to
     * @param wrapper The packet wrapper to send
     */
    void sendPacket(Player player, PacketWrapper<?> wrapper);
    
    /**
     * Sends a collection of PacketEvents wrappers to a specific player.
     * 
     * @param player The player to send the packets to
     * @param wrappers The collection of packet wrappers to send
     */
    void sendBundle(Player player, Collection<PacketWrapper<?>> wrappers);
}
