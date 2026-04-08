package dev.darkblade.mbe.core.platform.interaction;

import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionSource;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class BukkitInteractionIntentFactory {

    public InteractionIntent from(PlayerInteractEvent event) {
        if (event == null) {
            return null;
        }
        return new InteractionIntent(
                event.getPlayer(),
                mapAction(event),
                event.getClickedBlock(),
                event.getItem(),
                InteractionSource.PLAYER
        );
    }

    private InteractionType mapAction(PlayerInteractEvent event) {
        if (event == null) {
            return InteractionType.PROGRAMMATIC;
        }
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            return event.getPlayer() != null && event.getPlayer().isSneaking()
                    ? InteractionType.SHIFT_RIGHT_CLICK
                    : InteractionType.RIGHT_CLICK_BLOCK;
        }
        if (action == Action.LEFT_CLICK_BLOCK) {
            return InteractionType.LEFT_CLICK_BLOCK;
        }
        return InteractionType.PROGRAMMATIC;
    }
}
