package com.darkbladedev.engine.api.event;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

public class MultiblockInteractEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final MultiblockInstance instance;
    private final Player player;
    private final Action action;
    private final Block clickedBlock;
    private boolean cancelled;

    public MultiblockInteractEvent(@NotNull MultiblockInstance instance, @NotNull Player player, @NotNull Action action, @NotNull Block clickedBlock) {
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
    public Player getPlayer() {
        return player;
    }
    
    @NotNull
    public Action getAction() {
        return action;
    }
    
    @NotNull
    public Block getClickedBlock() {
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
