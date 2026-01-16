package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.PlayerResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public record TeleportAction(Vector offset, Object target) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Collection<Player> targets = PlayerResolver.resolve(target, instance, player);
        Location dest = instance.anchorLocation().clone().add(offset);
        
        // Center on block
        dest.add(0.5, 0, 0.5);
        // Look logic could be added later (e.g. face multiblock)
        
        // Safety check: Don't teleport into void or unloaded chunk if possible
        if (dest.getWorld() == null) return;
        
        for (Player p : targets) {
            p.teleport(dest);
        }
    }
}
