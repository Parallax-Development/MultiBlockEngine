package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockBreakEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final MultiblockInstance instance;
    private final MBEPlayer player;
    private boolean cancelled;

    public MultiblockBreakEvent(@NotNull MultiblockInstance instance, @Nullable MBEPlayer player) {
        this.instance = instance;
        this.player = player;
    }

    @NotNull
    public MultiblockInstance getMultiblock() {
        return instance;
    }

    @Nullable
    public MBEPlayer getPlayer() {
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
