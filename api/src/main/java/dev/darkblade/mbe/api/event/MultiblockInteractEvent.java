package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.api.platform.MBEBlock;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import org.jetbrains.annotations.NotNull;

public class MultiblockInteractEvent implements MBEEvent {

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

}
