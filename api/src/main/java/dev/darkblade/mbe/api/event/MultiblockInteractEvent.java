package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.api.platform.MBEBlock;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MultiblockInteractEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final MultiblockInstance instance;
    private final MBEPlayer player;
    private final InteractionType action;
    private final MBEBlock clickedBlock;
    private boolean cancelled;

    public MultiblockInteractEvent(@NotNull MultiblockInstance instance, @NotNull MBEPlayer player, @NotNull InteractionType action, @NotNull MBEBlock clickedBlock) {
        this.instance = instance;
        this.player = player;
        this.action = action;
        this.clickedBlock = clickedBlock;
    }

    @NotNull
    public MultiblockInstance getMultiblock() {
        return instance;
    }

    @NotNull
    public MBEPlayer getPlayer() {
        return player;
    }
    
    @NotNull
    public InteractionType getAction() {
        return action;
    }
    
    @NotNull
    public MBEBlock getClickedBlock() {
        return clickedBlock;
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
