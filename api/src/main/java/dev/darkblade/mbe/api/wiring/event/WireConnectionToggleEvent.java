package dev.darkblade.mbe.api.wiring.event;

import dev.darkblade.mbe.api.wiring.NetworkNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WireConnectionToggleEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public enum Action {
        CONNECT,
        DISCONNECT
    }

    private final @Nullable Player player;
    private final @NotNull Action action;
    private final @NotNull NetworkNode a;
    private final @NotNull NetworkNode b;
    private final boolean success;

    public WireConnectionToggleEvent(@Nullable Player player, @NotNull Action action, @NotNull NetworkNode a, @NotNull NetworkNode b, boolean success) {
        this.player = player;
        this.action = action;
        this.a = a;
        this.b = b;
        this.success = success;
    }

    public @Nullable Player getPlayer() {
        return player;
    }

    public @NotNull Action getAction() {
        return action;
    }

    public @NotNull NetworkNode getA() {
        return a;
    }

    public @NotNull NetworkNode getB() {
        return b;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}

