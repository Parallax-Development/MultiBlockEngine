package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class BlueprintCancelEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final MultiblockDefinition definition;
    private boolean cancelled;

    public BlueprintCancelEvent(@NotNull Player player, MultiblockDefinition definition) {
        this.player = Objects.requireNonNull(player, "player");
        this.definition = definition;
    }

    @NotNull
    public Player getPlayer() {
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

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
