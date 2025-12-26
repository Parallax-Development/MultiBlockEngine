package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public record SpawnItemAction(Material material, int amount, Vector offset) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Location loc = instance.anchorLocation().clone().add(offset);
        if (loc.getWorld() != null) {
            loc.getWorld().dropItemNaturally(loc, new ItemStack(material, amount));
        }
    }
}
