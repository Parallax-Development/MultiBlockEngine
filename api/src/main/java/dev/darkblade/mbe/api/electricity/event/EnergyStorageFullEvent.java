package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.electricity.EnergyStorage;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyStorageFullEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final EnergyStorage storage;

    public EnergyStorageFullEvent(@NotNull EnergyStorage storage) {
        this.storage = storage;
    }

    @NotNull
    public EnergyStorage getStorage() {
        return storage;
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

