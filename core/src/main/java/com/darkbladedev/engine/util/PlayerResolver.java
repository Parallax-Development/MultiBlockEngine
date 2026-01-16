package com.darkbladedev.engine.util;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerResolver {

    /**
     * Resolves a target object (String selector or List of names) into a collection of players.
     *
     * @param target  The target selector (e.g. "nearby:10", "trigger", "all", or List of names)
     * @param instance The multiblock instance context
     * @param context The player who triggered the action (may be null)
     * @return A collection of matching online players
     */
    public static Collection<Player> resolve(Object target, MultiblockInstance instance, Player context) {
        if (target == null) {
            // Default behavior:
            // If context player exists (interaction), target them.
            // If no context (tick), target nearby players (radius 10).
            if (context != null) {
                return Collections.singletonList(context);
            } else {
                return resolveNearby(instance, 10);
            }
        }

        if (target instanceof List<?>) {
            List<?> list = (List<?>) target;
            List<Player> players = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof String name) {
                    Player p = Bukkit.getPlayer(name);
                    if (p != null) players.add(p);
                }
            }
            return players;
        }

        if (target instanceof String selector) {
            return resolveSelector(selector, instance, context);
        }

        return Collections.emptyList();
    }

    private static Collection<Player> resolveSelector(String selector, MultiblockInstance instance, Player context) {
        selector = selector.toLowerCase().trim();

        if (selector.equals("trigger") || selector.equals("self") || selector.equals("@p")) {
            return context != null ? Collections.singletonList(context) : Collections.emptyList();
        }

        if (selector.equals("all") || selector.equals("@a")) {
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        }
        
        if (selector.equals("world")) {
             if (instance.anchorLocation().getWorld() != null) {
                 return instance.anchorLocation().getWorld().getPlayers();
             }
             return Collections.emptyList();
        }

        if (selector.startsWith("nearby")) {
            double radius = 10;
            if (selector.contains(":")) {
                try {
                    radius = Double.parseDouble(selector.split(":")[1]);
                } catch (NumberFormatException ignored) {}
            }
            return resolveNearby(instance, radius);
        }
        
        if (selector.startsWith("perm:") || selector.startsWith("permission:")) {
            String perm = selector.substring(selector.indexOf(":") + 1);
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(perm))
                    .collect(Collectors.toList());
        }

        // Specific player name
        Player p = Bukkit.getPlayer(selector);
        return p != null ? Collections.singletonList(p) : Collections.emptyList();
    }

    private static Collection<Player> resolveNearby(MultiblockInstance instance, double radius) {
        if (instance.anchorLocation().getWorld() == null) return Collections.emptyList();
        // Check chunk loaded to avoid loading chunks
        if (!instance.anchorLocation().getChunk().isLoaded()) return Collections.emptyList();
        
        return instance.anchorLocation().getNearbyPlayers(radius);
    }
}
