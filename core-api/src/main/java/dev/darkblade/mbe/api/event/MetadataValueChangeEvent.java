package dev.darkblade.mbe.api.event;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataValueChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final MultiblockInstance instance;
    private final String metadataId;
    private final Object oldValue;
    private final Object newValue;

    public MetadataValueChangeEvent(@NotNull MultiblockInstance instance, @NotNull String metadataId, @Nullable Object oldValue, @Nullable Object newValue) {
        this.instance = instance;
        this.metadataId = metadataId;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @NotNull
    public MultiblockInstance getMultiblock() {
        return instance;
    }

    @NotNull
    public String getMetadataId() {
        return metadataId;
    }

    public @Nullable Object getOldValue() {
        return oldValue;
    }

    public @Nullable Object getNewValue() {
        return newValue;
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
