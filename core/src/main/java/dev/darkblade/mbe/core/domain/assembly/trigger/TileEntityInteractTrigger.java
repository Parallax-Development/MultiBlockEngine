package dev.darkblade.mbe.core.domain.assembly.trigger;

import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyTrigger;
import dev.darkblade.mbe.api.assembly.AssemblyTriggerType;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;

public final class TileEntityInteractTrigger implements AssemblyTrigger {

    @Override
    public String id() {
        return AssemblyTriggerType.TILE_ENTITY_INTERACT.id();
    }

    @Override
    public boolean supports(InteractionIntent intent) {
        return intent != null
                && (intent.type() == InteractionType.RIGHT_CLICK_BLOCK || intent.type() == InteractionType.SHIFT_RIGHT_CLICK);
    }

    @Override
    public boolean shouldTrigger(AssemblyContext context) {
        if (context == null) {
            return false;
        }
        if (context.intent() == null) {
            return false;
        }
        Block block = context.origin();
        if (block == null) {
            return false;
        }
        return isTileEntity(block);
    }

    private boolean isTileEntity(Block block) {
        if (block.getState() instanceof TileState) {
            return true;
        }
        Material material = block.getType();
        return material != null && material.isInteractable();
    }
}
