package dev.darkblade.mbe.api.assembly;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public record AssemblyContext(
        Cause cause,
        Player player,
        Block block,
        Action action,
        ItemStack item,
        EquipmentSlot hand,
        boolean sneaking,
        Map<String, Object> attributes
) {

    public enum Cause {
        PLAYER_INTERACT,
        BLOCK_PLACE,
        MANUAL
    }

    public AssemblyContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public boolean attributeBoolean(String key) {
        Object v = attributes.get(key);
        return v instanceof Boolean b && b;
    }
}

