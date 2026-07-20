package dev.darkblade.mbe.api.event.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the MultiBlockEngine admin reload command is executed.
 * This happens after internal configs, addons and systems have been reloaded.
 * <p>
 * This event is synchronous.
 */
public final class MbeReloadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public MbeReloadEvent() {
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
