package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.PlayerResolver;
import com.darkbladedev.engine.util.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;

public record TitleAction(String title, String subtitle, int fadeIn, int stay, int fadeOut, Object target) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Collection<Player> targets = PlayerResolver.resolve(target, instance, player);
        
        String parsedTitle = StringUtil.parsePlaceholders(title, instance);
        String parsedSubtitle = StringUtil.parsePlaceholders(subtitle, instance);
        
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        
        // Adventure API logic (Standard in Paper 1.20.4)
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L), 
            Duration.ofMillis(stay * 50L), 
            Duration.ofMillis(fadeOut * 50L)
        );

        for (Player p : targets) {
            String finalTitle = parsedTitle;
            String finalSubtitle = parsedSubtitle;
            
            if (hasPapi) {
                finalTitle = PlaceholderAPI.setPlaceholders(p, finalTitle);
                finalSubtitle = PlaceholderAPI.setPlaceholders(p, finalSubtitle);
            }
            
            Title t = Title.title(StringUtil.legacyText(finalTitle), StringUtil.legacyText(finalSubtitle), times);
            p.showTitle(t);
        }
    }
}
