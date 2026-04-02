package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.PlayerResolver;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
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
            p.sendActionBar(StringUtil.legacyText(text));
        }
    }
}
