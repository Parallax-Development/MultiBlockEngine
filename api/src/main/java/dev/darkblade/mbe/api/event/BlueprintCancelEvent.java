package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class BlueprintCancelEvent implements MBEEvent {

    private final MBEPlayer player;
    private final MultiblockDefinition definition;
    private boolean cancelled;

    public BlueprintCancelEvent(@NotNull MBEPlayer player, MultiblockDefinition definition) {
        this.player = Objects.requireNonNull(player, "player");
        this.definition = definition;
    }

    @NotNull
    public MBEPlayer getPlayer() {
        return player;
    }

    public MultiblockDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}
