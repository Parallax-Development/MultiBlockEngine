package com.darkbladedev.engine.api.event;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockBreakEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final MultiblockInstance instance;
    private final Player player;
    private boolean cancelled;

    public MultiblockBreakEvent(@NotNull MultiblockInstance instance, @Nullable Player player) {
        this.instance = instance;
        this.player = player;
    }

    @NotNull
    public MultiblockInstance getMultiblock() {
        return instance;
    }

    @Nullable
    public Player getPlayer() {
        return player;
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
