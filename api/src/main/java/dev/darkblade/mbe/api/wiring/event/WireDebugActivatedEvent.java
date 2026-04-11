package dev.darkblade.mbe.api.wiring.event;

import dev.darkblade.mbe.api.wiring.debug.NetworkSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class WireDebugActivatedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final @Nullable Player player;
    private final @NotNull NetworkSnapshot snapshot;
    private final @NotNull Duration duration;

    public WireDebugActivatedEvent(@Nullable Player player, @NotNull NetworkSnapshot snapshot, @NotNull Duration duration) {
        this.player = player;
        this.snapshot = snapshot;
        this.duration = duration;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public @NotNull NetworkSnapshot getSnapshot() {
        return snapshot;
    }

    public @NotNull Duration getDuration() {
        return duration;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}

