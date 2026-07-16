package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultiblockBreakEvent implements MBEEvent {

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

}
