package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.electricity.EnergyConsumer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyConsumedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final EnergyConsumer consumer;
    private final long amount;

    public EnergyConsumedEvent(@NotNull EnergyConsumer consumer, long amount) {
        this.consumer = consumer;
        this.amount = amount;
    }

    @NotNull
    public EnergyConsumer getConsumer() {
        return consumer;
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

