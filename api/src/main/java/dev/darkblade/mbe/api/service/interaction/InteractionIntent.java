package dev.darkblade.mbe.api.service.interaction;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record InteractionIntent(
        Player player,
        InteractionType type,
        Block targetBlock,
        ItemStack itemInHand,
        InteractionSource source
) {
}
