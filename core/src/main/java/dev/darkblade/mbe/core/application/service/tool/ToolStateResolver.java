package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ToolState;
import org.bukkit.inventory.ItemStack;

public interface ToolStateResolver {

    ToolState resolve(ItemStack item);

    void save(ItemStack item, ToolState state);
}
