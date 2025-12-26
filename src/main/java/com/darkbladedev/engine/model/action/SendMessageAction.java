package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
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
        
        // Internal variable replacement
        String text = StringUtil.parsePlaceholders(message, instance);
        
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        for (Player p : players) {
            String processed = text;
            if (hasPapi) {
                processed = PlaceholderAPI.setPlaceholders(p, processed);
            }
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', processed));
        }
    }
}
