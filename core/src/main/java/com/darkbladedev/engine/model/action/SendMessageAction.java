package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.PlayerResolver;
import com.darkbladedev.engine.util.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public record SendMessageAction(String message, Object targetSelector) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player contextPlayer) {
        // Resolve targets using the utility
        Collection<Player> players = PlayerResolver.resolve(targetSelector, instance, contextPlayer);
        
        // Internal variable replacement (pre-calculated for performance, though PAPI might need per-player)
        // We do basic replacement first.
        String text = StringUtil.parsePlaceholders(message, instance);
        
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        for (Player p : players) {
            String processed = text;
            if (hasPapi) {
                processed = PlaceholderAPI.setPlaceholders(p, processed);
            }
            p.sendMessage(StringUtil.legacyText(processed));
        }
    }
}
