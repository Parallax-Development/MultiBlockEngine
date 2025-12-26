package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.PlayerResolver;
import com.darkbladedev.engine.util.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

public record ActionBarAction(String message, Object target) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Collection<Player> targets = PlayerResolver.resolve(target, instance, player);
        String parsed = StringUtil.parsePlaceholders(message, instance);
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        for (Player p : targets) {
            String text = parsed;
            if (hasPapi) {
                text = PlaceholderAPI.setPlaceholders(p, text);
            }
            text = ChatColor.translateAlternateColorCodes('&', text);
            p.sendActionBar(Component.text(text));
        }
    }
}
