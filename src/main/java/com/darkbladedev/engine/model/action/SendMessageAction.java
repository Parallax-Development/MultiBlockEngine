package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public record SendMessageAction(String message) implements Action {
    @SuppressWarnings("deprecation")
    @Override
    public void execute(MultiblockInstance instance) {
        // Find players nearby or use a context player if we had one.
        // For now, let's broadcast to nearby players (radius 10)
        Collection<Player> players = instance.anchorLocation().getNearbyPlayers(10);
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        for (Player p : players) {
            p.sendMessage(colored);
        }
    }
}
