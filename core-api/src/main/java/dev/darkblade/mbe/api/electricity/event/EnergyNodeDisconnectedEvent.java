package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.electricity.EnergyNode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyNodeDisconnectedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final EnergyNode a;
    private final EnergyNode b;

    public EnergyNodeDisconnectedEvent(@NotNull EnergyNode a, @NotNull EnergyNode b) {
        this.a = a;
        this.b = b;
    }

    @NotNull
    public EnergyNode getA() {
        return a;
    }

    @NotNull
    public EnergyNode getB() {
        return b;
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

