package dev.darkblade.mbe.api.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface ToolModeExecutionService {
    void registerToolItem(ToolItem toolItem);

    Optional<String> resolveToolId(ItemStack stack);

    WrenchResult dispatch(WrenchContext context);
}
