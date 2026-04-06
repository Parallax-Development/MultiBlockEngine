package dev.darkblade.mbe.api.assembly;

import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public record AssemblyContext(
        Player player,
        Block origin,
        InteractionIntent intent
) {
}
