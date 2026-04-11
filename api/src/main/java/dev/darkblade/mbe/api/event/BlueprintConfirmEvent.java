package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class BlueprintConfirmEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final MultiblockDefinition definition;
    private final Location origin;
    private boolean cancelled;

    public BlueprintConfirmEvent(@NotNull Player player, @NotNull MultiblockDefinition definition, @NotNull Location origin) {
        this.player = Objects.requireNonNull(player, "player");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public MultiblockDefinition getDefinition() {
        return definition;
    }

    @NotNull
    public Location getOrigin() {
        return origin.clone();
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
