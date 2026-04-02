package dev.darkblade.mbe.api.command;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record WrenchContext(
        Player player,
        Block clickedBlock,
        Action action,
        @Nullable ItemStack item,
        @Nullable EquipmentSlot hand
) {
    public WrenchContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedBlock, "clickedBlock");
        Objects.requireNonNull(action, "action");
    }
}

