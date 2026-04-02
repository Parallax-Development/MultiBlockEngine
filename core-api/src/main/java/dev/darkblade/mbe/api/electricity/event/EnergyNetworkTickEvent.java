package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.electricity.EnergyNetwork;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyNetworkTickEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final EnergyNetwork network;

    public EnergyNetworkTickEvent(@NotNull EnergyNetwork network) {
        this.network = network;
    }

    @NotNull
    public EnergyNetwork getNetwork() {
        return network;
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

