package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.electricity.EnergyProducer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyProducedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final EnergyProducer producer;
    private final long amount;

    public EnergyProducedEvent(@NotNull EnergyProducer producer, long amount) {
        this.producer = producer;
        this.amount = amount;
    }

    @NotNull
    public EnergyProducer getProducer() {
        return producer;
    }

    public long getAmount() {
        return amount;
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

