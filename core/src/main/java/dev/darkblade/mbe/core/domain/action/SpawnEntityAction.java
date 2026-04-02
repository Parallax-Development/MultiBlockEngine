package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public record SpawnEntityAction(EntityType type, Vector offset, String customName) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Location loc = instance.anchorLocation().clone().add(offset);
        if (loc.getWorld() != null) {
            Entity entity = loc.getWorld().spawnEntity(loc, type);
            if (customName != null) {
                entity.customName(StringUtil.legacyText(customName));
                entity.setCustomNameVisible(true);
            }
        }
    }
}
